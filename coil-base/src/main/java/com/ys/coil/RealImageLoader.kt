package com.ys.coil

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import com.ys.coil.bitmappool.RealBitmapPool
import com.ys.coil.decode.BitmapFactoryDecoder
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.DrawableDecoderService
import com.ys.coil.decode.EmptyDecoder
import com.ys.coil.fetch.*
import com.ys.coil.map.FileMapper
import com.ys.coil.map.HttpUriMapper
import com.ys.coil.map.StringMapper
import com.ys.coil.memory.*
import com.ys.coil.memory.RequestService
import com.ys.coil.network.NetworkObserver
import com.ys.coil.request.*
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import com.ys.coil.size.SizeResolver
import com.ys.coil.target.ViewTarget
import com.ys.coil.transform.Transformation
import com.ys.coil.util.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

internal class RealImageLoader(
    private val context: Context,
    override val defaults: DefaultRequestOptions,
    bitmapPoolSize: Long,
    memoryCacheSize: Int,
    okHttpClient: OkHttpClient,
    registry: ComponentRegistry
) : ImageLoader, ComponentCallbacks {

    companion object {
        private const val TAG = "RealImageLoader"
    }

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log(TAG, throwable) }

    private val bitmapPool = RealBitmapPool(bitmapPoolSize)
    private val referenceCounter = BitmapReferenceCounter(bitmapPool)
    private val delegateService = DelegateService(this, referenceCounter)
    private val requestService = RequestService()
    private val drawableDecoder = DrawableDecoderService(context, bitmapPool)
    private val memoryCache = MemoryCache(referenceCounter, memoryCacheSize)
    private val networkObserver = NetworkObserver(context)

    private val registry = ComponentRegistry(registry) {
        add(StringMapper())
        add(HttpUriMapper())
        add(FileMapper())

        add(BitmapFactoryDecoder(context))
    }

    private var isShutdown = false

    init {
        context.registerComponentCallbacks(this)
    }

    override fun load(request: LoadRequest): RequestDisposable {
        // 데이터가 null인 경우 연결된 요청을 단락시키고 취소합니다.
        val data = request.data
        val target = request.target
        if (data == null) {
            if (target is ViewTarget<*>) {
                target.cancel()
            }
            return EmptyRequestDisposable
        }

        // 데이터 로드를 시작합니다.
        val job = loaderScope.launch(exceptionHandler) {
            execute(data, request)
        }

        return if (target is ViewTarget<*>) {
            ViewTargetRequestDisposable(target, request)
        } else {
            BaseTargetRequestDisposable(job)
        }
    }

    override suspend fun get(request: GetRequest): Drawable = execute(request.data, request)

    private suspend fun execute(
        data: Any,
        request: Request
    ): Drawable = withContext(Dispatchers.Main.immediate) outerJob@{
        // 이 이미지 로더가 종료되지 않았는지 확인합니다.
        assertNotShutdown()

        // 메인 스레드의 라이프사이클 정보를 계산합니다.
        val (lifecycle, mainDispatcher) = requestService.lifecycleInfo(request)

        // 비트맵 풀링을 지원하도록 대상을 래핑합니다.
        val targetDelegate = delegateService.createTargetDelegate(request)

        val deferred = async<Drawable>(mainDispatcher, CoroutineStart.LAZY) innerJob@{
            request.listener?.onStart(data)

            // 필요한 경우 대상을 수명 주기 관찰자로 추가합니다.
            val target = request.target
            if (target is ViewTarget<*> && target is LifecycleObserver) {
                lifecycle.addObserver(target)
            }

            // 비트맵이 입력으로 제공된 경우 무효화합니다.
            when (data) {
                is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
                is Bitmap -> referenceCounter.invalidate(data)
            }

            var sizeResolver: SizeResolver? = null
            var size: Size? = null

            // 데이터 변환을 수행하고 필요한 경우 미리 크기를 조절합니다.
            val measuredMapper = registry.getMeasuredMapper(data)
            val mappedData = if (measuredMapper != null) {
                targetDelegate.start(null, request.placeholder)
                sizeResolver = requestService.sizeResolver(request, context)
                size = sizeResolver.size().also { ensureActive() }

                measuredMapper.map(data, size)
            } else {
                val options = Options(context)
                registry.getMapper(data)?.map(data, options) ?: data
            }

            // 캐시 키를 계산합니다.
            val fetcher = registry.requireFetcher(mappedData)
            val cacheKey = request.keyOverride ?: computeCacheKey(fetcher, mappedData, request.transformations)

            // 메모리 캐시를 확인하고 자리 표시자를 설정합니다.
            val cachedValue = takeIf(request.memoryCachePolicy.readEnabled) {
                memoryCache.getValue(cacheKey)
            }

            val cachedDrawable = cachedValue?.bitmap?.toDrawable(context)

            // 이전에 크기를 해결하지 않았다면 지금 해결하십시오.
            if (sizeResolver == null || size == null) {
                targetDelegate.start(cachedDrawable, cachedDrawable ?: request.placeholder)
                sizeResolver = requestService.sizeResolver(request, context)
                size = sizeResolver.size().also { ensureActive() }
            }

            // 캐시된 드로어블이 대상에 대해 유효한 경우 단락.
            if (cachedDrawable != null && isCachedDrawableValid(cachedDrawable, cachedValue.isSampled, size, request)) {
                log(TAG, Log.INFO) { "${Emoji.BRAIN} Cached - $data" }
                targetDelegate.success(cachedDrawable, 0)
                request.listener?.onSuccess(data, DataSource.MEMORY)
                return@innerJob cachedDrawable
            }

            // 이미지를 로드합니다.
            val (drawable, isSampled, source) = loadData(data, request, sizeResolver, fetcher, mappedData, size)

            // 결과를 캐시합니다.
            if (request.memoryCachePolicy.writeEnabled) {
                memoryCache.putValue(cacheKey, drawable, isSampled)
            }

            // 타겟에 최종 결과를 설정합니다.
            log(TAG, Log.INFO) { "${source.emoji} Successful (${source.name}) - $data" }
            targetDelegate.success(drawable, request.crossfadeMillis)
            request.listener?.onSuccess(data, source)

            return@innerJob drawable
        }

        // 요청을 래핑하여 수명 주기를 관리합니다.
        val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, lifecycle, mainDispatcher, deferred)

        deferred.invokeOnCompletion { throwable ->
            // 콜백이 메인 스레드에서 실행되는지 확인합니다.
            loaderScope.launch(Dispatchers.Main.immediate) {
                requestDelegate.onComplete()
                throwable ?: return@launch

                if (throwable is CancellationException) {
                    log(TAG, Log.INFO) { "${Emoji.CONSTRUCTION} Cancelled - $data" }
                    request.listener?.onCancel(data)
                } else {
                    log(TAG, Log.INFO) { "${Emoji.SIREN} Failed - $data - $throwable" }
                    targetDelegate.error(request.error, request.crossfadeMillis)
                    request.listener?.onError(data, throwable)
                }
            }
        }

        // 내부 작업이 완료될 때까지 외부 작업을 일시 중단합니다.
        return@outerJob deferred.await()
    }

    /**
     * [data] + [transformations]에 대한 캐시 키를 계산합니다.
     */
    private fun <T : Any> computeCacheKey(
        fetcher: Fetcher<T>,
        data: T,
        transformations: List<Transformation>
    ): String? {
        val baseCacheKey = fetcher.key(data) ?: return null
        return buildString {
            append(baseCacheKey)
            transformations.forEach { append(it.key()) }
        }
    }

    /**
     * [MemoryCache]에서 반환된 [Bitmap]이 [Request]를 만족하면 true를 반환합니다.
     */
    @VisibleForTesting
    internal fun isCachedDrawableValid(
        cached: BitmapDrawable,
        isSampled: Boolean,
        size: Size,
        request: Request
    ): Boolean {
        if (size !is PixelSize) {
            return !isSampled
        }

        if (isSampled && (cached.bitmap.width < size.width || cached.bitmap.height < size.height)) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !request.allowHardware && cached.bitmap.config == Bitmap.Config.HARDWARE) {
            return false
        }

        val cachedConfig = cached.bitmap.config.normalize()
        val requestedConfig = request.bitmapConfig.normalize()
        if (cachedConfig >= requestedConfig) {
            return true
        }
        if (request.allowRgb565 && cachedConfig == Bitmap.Config.RGB_565 && requestedConfig == Bitmap.Config.ARGB_8888) {
            return true
        }

        return false
    }

    /**
     * [data]를 [Drawable]로 로드합니다. [Transformation]을 적용합니다.
     */
    private suspend inline fun loadData(
        data: Any,
        request: Request,
        sizeResolver: SizeResolver,
        fetcher: Fetcher<Any>,
        mappedData: Any,
        size: Size
    ): DrawableResult = withContext(request.dispatcher) {
        // 데이터를 Drawable로 변환합니다.
        val scale = requestService.scale(request, sizeResolver)
        val options = requestService.options(request, scale, networkObserver.isOnline())
        val result = when (val fetchResult = fetcher.fetch(bitmapPool, mappedData, size, options)) {
            is SourceResult -> {
                val decodeResult = try {
                    // 취소되었는지 확인하세요.
                    ensureActive()

                    // 관련 디코더를 찾으십시오.
                    val decoder = if (request.isDiskPreload()) {
                        // 데이터를 미리 로드하고 메모리 캐시에 쓰는 것이 비활성화된 경우 결과 디코딩을 건너뜁니다.
                        // 대신 소스를 소진하고 빈 결과를 반환합니다.
                        EmptyDecoder
                    } else {
                        registry.requireDecoder(data, fetchResult.source, fetchResult.mimeType)
                    }

                    // 스트림을 디코딩합니다.
                    decoder.decode(bitmapPool, fetchResult.source, size, options)
                } catch (rethrown: Exception) {
                    // 참고: 포착되지 않은 예외가 있는 경우에만 스트림을 자동으로 닫습니다.
                    // 이것은 사용자 정의 디코더가 드로어블을 반환한 후에도 소스를 계속 읽을 수 있도록 합니다.
                    fetchResult.source.closeQuietly()
                    throw rethrown
                }

                // 가져오기 및 디코딩 작업 결과를 결합합니다.
                DrawableResult(
                    drawable = decodeResult.drawable,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
            is DrawableResult -> fetchResult
            is SourceResult -> {
                val decodeResult = try {
                    // 취소되었는지 확인하세요.
                    ensureActive()

                    // 관련 디코더를 찾으십시오.
                    val decoder = if (request.isDiskPreload()) {
                        // 데이터를 미리 로드하고 메모리 캐시에 쓰는 것이 비활성화된 경우 결과 디코딩을 건너뜁니다.
                        // 대신 소스를 소진하고 빈 결과를 반환합니다.
                        EmptyDecoder
                    } else {
                        registry.requireDecoder(data, fetchResult.source.source(), fetchResult.mimeType)
                    }

                    // 스트림을 디코딩합니다.
                    decoder.decode(bitmapPool, fetchResult.source.source(), size, options)
                } catch (rethrown: Exception) {
                    // 참고: 포착되지 않은 예외가 있는 경우에만 스트림을 자동으로 닫습니다.
                    // 이것은 사용자 정의 디코더가 드로어블을 반환한 후에도 소스를 계속 읽을 수 있도록 합니다.
                    fetchResult.source.closeQuietly()
                    throw rethrown
                }

                // 가져오기 및 디코딩 작업 결과를 결합합니다.
                DrawableResult(
                    drawable = decodeResult.drawable,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
        }

        // 취소되었는지 확인하세요.
        ensureActive()

        // 변환은 BitmapDrawables에만 적용할 수 있습니다.
        val transformedResult = if (result.drawable is BitmapDrawable && request.transformations.isNotEmpty()) {
            val bitmap = request.transformations.fold(result.drawable.bitmap) { bitmap, transformation ->
                transformation.transform(bitmapPool, bitmap).also { ensureActive() }
            }
            result.copy(drawable = bitmap.toDrawable(context))
        } else {
            result
        }

        (transformedResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        return@withContext transformedResult
    }

    override fun onTrimMemory(level: Int) {
        memoryCache.trimMemory(level)
        bitmapPool.trimMemory(level)
    }

    override fun clearMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

    @Synchronized
    override fun shutdown() {
        if (isShutdown) {
            return
        }
        isShutdown= true

        loaderScope.cancel()
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
        clearMemory()
    }

    private fun Request.isDiskPreload(): Boolean {
        return this is LoadRequest && target == null && !memoryCachePolicy.writeEnabled
    }

    private fun assertNotShutdown() {
        check(!isShutdown) { "The image loader is shutdown!" }
    }
}