package com.ys.coil

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import com.ys.coil.ImageLoader.Builder
import com.ys.coil.disk.DiskCache
import com.ys.coil.fetch.*
import com.ys.coil.interceptor.EngineInterceptor
import com.ys.coil.interceptor.RealInterceptorChain
import com.ys.coil.key.FileKeyer
import com.ys.coil.key.UriKeyer
import com.ys.coil.map.FileUriMapper
import com.ys.coil.map.HttpUrlMapper
import com.ys.coil.map.ResourceIntMapper
import com.ys.coil.map.ResourceUriMapper
import com.ys.coil.map.StringMapper
import com.ys.coil.memory.*
import com.ys.coil.memory.RequestService
import com.ys.coil.request.*
import com.ys.coil.target.Target
import com.ys.coil.target.ViewTarget
import com.ys.coil.transition.NoneTransition
import com.ys.coil.transition.TransitionTarget
import com.ys.coil.util.*
import kotlinx.coroutines.*
import okhttp3.Call
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

internal class RealImageLoader(
    val context: Context,
    override val defaults: DefaultRequestOptions,
    val memoryCacheLazy: Lazy<MemoryCache?>,
    val diskCacheLazy: Lazy<DiskCache?>,
    val callFactoryLazy: Lazy<Call.Factory>,
    val eventListenerFactory: EventListener.Factory,
    val componentRegistry: ComponentRegistry,
    val options: ImageLoaderOptions,
    val logger: Logger?
) : ImageLoader, ComponentCallbacks {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
        CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) })
    private val systemCallbacks = SystemCallbacks(this, context, options.networkObserverEnabled)
    private val requestService = RequestService(this, systemCallbacks, logger)
    override val memoryCache by memoryCacheLazy
    override val diskCache by diskCacheLazy
    override val components = componentRegistry.newBuilder()
        // Mappers
        .add(HttpUrlMapper())
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper())
        .add(ResourceIntMapper())
        // Keyers
        .add(UriKeyer())
        .add(FileKeyer(options.addLastModifiedToFileCacheKey))
        // Fetchers
        .add(HttpUriFetcher.Factory(callFactoryLazy, diskCacheLazy, options.respectCacheHeaders))
        .add(FileFetcher.Factory())
        .add(AssetUriFetcher.Factory())
        .add(ContentUriFetcher.Factory())
        .add(ResourceUriFetcher.Factory())
        .add(DrawableFetcher.Factory())
        .add(BitmapFetcher.Factory())
        .add(ByteBufferFetcher.Factory())
        // Decoders
        // .add(BitmapFactoryDecoder.Factory(options.bitmapFactoryMaxParallelism))
        .build()

    private val interceptors = components.interceptors +
        EngineInterceptor(this, requestService, logger)

    private val isShutdown = AtomicBoolean(false)

    override fun enqueue(request: ImageRequest): Disposable {
        // 메인 스레드에서 요청 실행을 시작합니다.
        val job = scope.async {
            executeMain(request, REQUEST_TYPE_ENQUEUE).also { result ->
                if (result is ErrorResult) logger?.log(TAG, result.throwable)
            }
        }

        // 뷰에 연결된 현재 요청을 업데이트하고 새 일회용을 반환합니다.
        return if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.getDisposable(job)
        } else {
            OneShotDisposable(job)
        }
    }

    override suspend fun execute(request: ImageRequest) = coroutineScope {
        // 메인 스레드에서 요청 실행을 시작합니다.
        val job = async(Dispatchers.Main.immediate) {
            executeMain(request, REQUEST_TYPE_EXECUTE)
        }

        // 뷰에 연결된 현재 요청을 업데이트하고 결과를 기다립니다.
        if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.getDisposable(job)
        }
        return@coroutineScope job.await()
    }

    @MainThread
    private suspend fun executeMain(initialRequest: ImageRequest, type: Int): ImageResult {
        // 요청을 래핑하여 수명 주기를 관리합니다.
        val requestDelegate = requestService.requestDelegate(initialRequest, coroutineContext.job)
            .apply { assertActive() }

        // 이 이미지 로더의 기본값을 이 요청에 적용합니다.
        val request = initialRequest.newBuilder().defaults(defaults).build()

        // 새 이벤트 리스너를 만듭니다.
        val eventListener = eventListenerFactory.create(request)

        try {
            // 데이터가 null이면 시작하기 전에 실패합니다.
            if (initialRequest.data == NullRequestData) throw NullRequestDataException()

            // 요청의 수명 주기 관찰자를 설정합니다.
            requestDelegate.start()

            // 대기열에 있는 요청은 수명 주기가 시작될 때까지 일시 중단됩니다.
            if (type == REQUEST_TYPE_ENQUEUE) request.lifecycle.awaitStarted()

            // 대상에 자리 표시자를 설정합니다.
            val placeholderBitmap = memoryCache?.get(request.placeholderMemoryCacheKey)?.bitmap
            val placeholder = placeholderBitmap?.toDrawable(request.context) ?: request.placeholder
            request.target?.onStart(placeholder)
            eventListener.onStart(request)
            request.listener?.onStart(request)

            // 크기를 해결합니다.
            eventListener.resolveSizeStart(request)
            val size = request.sizeResolver.size()
            eventListener.resolveSizeEnd(request, size)

            // 인터셉터 체인을 실행합니다.
            val result = withContext(request.interceptorDispatcher) {
                RealInterceptorChain(
                    initialRequest = request,
                    interceptors = interceptors,
                    index = 0,
                    request = request,
                    size = size,
                    eventListener = eventListener,
                    isPlaceholderCached = placeholderBitmap != null
                ).proceed(request)
            }

            // 대상에 결과를 설정합니다.
            when (result) {
                is SuccessResult -> onSuccess(result, request.target, eventListener)
                is ErrorResult -> onError(result, request.target, eventListener)
            }

            return result
        } catch (throwable: Exception) {
            if (throwable is CancellationException) {
                onCancel(request, eventListener)
                throw throwable
            } else {
                // 잡히지 않은 예외가 있는 경우 기본 오류 결과를 만듭니다.
                val result = requestService.errorResult(request, throwable)
                onError(result, request.target, eventListener)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
    }

    /** [SystemCallbacks.onTrimMemory]에 의해 호출됩니다. */
    override fun onTrimMemory(level: Int) {
        memoryCache?.trimMemory(level)
    }

    override fun shutdown() {
        if (isShutdown.getAndSet(true)) return
        scope.cancel()
        systemCallbacks.shutdown()
        memoryCache?.clear()
    }

    override fun newBuilder(): Builder = ImageLoader.Builder(this)

    private fun onSuccess(
        result: SuccessResult,
        target: Target?,
        eventListener: EventListener
    ) {
        val request = result.request
        val dataSource = result.dataSource
        logger?.log(TAG, Log.INFO) {
            "${dataSource.emoji} Successful (${dataSource.name}) - ${request.data}"
        }
        transition(result, target, eventListener) { target?.onSuccess(result.drawable) }
        eventListener.onSuccess(request, result)
        request.listener?.onSuccess(request, result)
    }

    private fun onError(
        result: ErrorResult,
        target: Target?,
        eventListener: EventListener
    ) {
        val request = result.request
        logger?.log(TAG, Log.INFO) {
            "${Emoji.SIREN} Failed - ${request.data} - ${result.throwable}"
        }
        transition(result, target, eventListener) { target?.onError(result.drawable) }
        eventListener.onError(request, result)
        request.listener?.onError(request, result)
    }

    private fun onCancel(request: ImageRequest, eventListener: EventListener) {
        logger?.log(TAG, Log.INFO) {
            "${Emoji.CONSTRUCTION} Cancelled - ${request.data}"
        }
        eventListener.onCancel(request)
        request.listener?.onCancel(request)
    }

    private inline fun transition(
        result: ImageResult,
        target: Target?,
        eventListener: EventListener,
        setDrawable: () -> Unit
    ) {
        if (target !is TransitionTarget) {
            setDrawable()
            return
        }

        val transition = result.request.transitionFactory.create(target, result)
        if (transition is NoneTransition) {
            setDrawable()
            return
        }

        eventListener.transitionStart(result.request, transition)
        transition.transition()
        eventListener.transitionEnd(result.request, transition)
    }

    companion object {
        private const val TAG = "RealImageLoader"
        private const val REQUEST_TYPE_ENQUEUE = 0
        private const val REQUEST_TYPE_EXECUTE = 1
    }
}