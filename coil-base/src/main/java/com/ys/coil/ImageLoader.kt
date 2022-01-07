package com.ys.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.ys.coil.disk.DiskCache
import com.ys.coil.drawable.CrossfadeDrawable
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.CachePolicy
import com.ys.coil.request.DefaultRequestOptions
import com.ys.coil.request.Disposable
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.size.Precision
import com.ys.coil.transition.CrossfadeTransition
import com.ys.coil.transition.Transition
import com.ys.coil.util.DEFAULT_REQUEST_OPTIONS
import com.ys.coil.util.ImageLoaderOptions
import com.ys.coil.util.Logger
import com.ys.coil.util.Utils
import com.ys.coil.util.getDrawableCompat
import com.ys.coil.util.unsupported
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * [ImageRequest]를 실행하여 이미지를 로드하는 서비스 클래스입니다.
 *
 * 이미지 로더는 캐싱, 데이터 가져오기, 이미지 디코딩, 요청 관리, 메모리 관리 등을 처리합니다.
 * 이미지 로더는 공유 가능하도록 설계되었으며 단일 인스턴스를 만들고 앱 전체에서 공유할 때 가장 잘 작동합니다.
 */
interface ImageLoader {

    /**
     * 설정되지 않은 [ImageRequest] 값을 채우는 데 사용되는 기본 옵션입니다.
     */
    val defaults: DefaultRequestOptions

    /**
     * 이미지 요청을 수행하는 데 사용되는 구성 요소입니다.
     */
    val components: ComponentRegistry

    /**
     * 이전에 로드된 이미지의 인-메모리 캐시입니다.
     */
    val memoryCache: MemoryCache?

    /**
     * 이전에 로드된 이미지의 디스크 캐시입니다.
     */
    val diskCache: DiskCache?

    /**
     * 비동기적으로 실행할 [request]을 대기열에 넣습니다.
     *
     * 참고: 요청은 실행되기 전에 [ImageRequest.lifecycle]이 [Lifecycle.State.STARTED] 이상이 될 때까지 대기합니다.
     *
     * @param request 실행할 요청입니다.
     * @return 요청을 취소하거나 상태를 확인하는 데 사용할 수 있는 [Disposable]입니다.
     */
    fun enqueue(request: ImageRequest): Disposable

    /**
     * 현재 코루틴 범위에서 [request]을 실행합니다.
     *
     * 참고: [ImageRequest.target]이 [ViewTarget]인 경우 해당 뷰가 분리되면 작업이 자동으로 취소됩니다.
     *
     * @param request 실행할 요청입니다.
     * @return 요청이 성공적으로 완료되면 [SuccessResult] 그렇지 않으면 [ErrorResult]를 반환합니다.
     */
    fun execute(request: ImageRequest): ImageResult

    /**
     * 신규 및 진행 중인 요청을 취소하고 [MemoryCache]를 지우고 열려 있는 시스템 리소스를 모두 닫습니다.
     *
     * 이미지 로더 종료는 선택 사항입니다. 역참조되면 자동으로 종료됩니다.
     */
    fun shutdown()

    /**
     * 이 이미지 로더와 동일한 리소스 및 구성을 공유하는 [ImageLoader.Builder]를 만듭니다.
     */
    fun newBuilder(): Builder

    class Builder {

        private val applicationContext: Context
        private var defaults: DefaultRequestOptions
        private var memoryCache: Lazy<MemoryCache?>?
        private var diskCache: Lazy<DiskCache?>?
        private var callFactory: Lazy<Call.Factory>?
        private var eventListenerFactory: EventListener.Factory?
        private var componentRegistry: ComponentRegistry?
        private var options: ImageLoaderOptions
        private var logger: Logger?

        constructor(context: Context) {
            applicationContext = context.applicationContext
            defaults = DEFAULT_REQUEST_OPTIONS
            memoryCache = null
            diskCache = null
            callFactory = null
            eventListenerFactory = null
            componentRegistry = null
            options = ImageLoaderOptions()
            logger = null
        }

        internal constructor(imageLoader: RealImageLoader) {
            applicationContext = imageLoader.context.applicationContext
            defaults = imageLoader.defaults
            memoryCache = imageLoader.memoryCacheLazy
            diskCache = imageLoader.diskCacheLazy
            callFactory = imageLoader.callFactoryLazy
            eventListenerFactory = imageLoader.eventListenerFactory
            componentRegistry = imageLoader.componentRegistry
            options = imageLoader.options
            logger = imageLoader.logger
        }

        fun okHttpClient(okHttpClient: OkHttpClient) = callFactory(okHttpClient)

        /**
         * 네트워크 요청에 사용되는 [OkHttpClient]를 생성하기 위해 지연 콜백을 설정합니다.
         *
         * 이것은 백그라운드 스레드에서 [OkHttpClient]의 지연 생성을 허용합니다.
         * [initializer]는 최대 한 번만 호출되도록 보장됩니다.
         *
         * `okHttpClient(OkHttpClient)` 대신 이것을 사용하는 것을 선호합니다.
         *
         * 'callFactory(() -> Call.Factory)'를 호출하기 위한 편의 함수입니다.
         */
        fun okHttpClient(initializer: () -> OkHttpClient) = callFactory(initializer)

        /**
         * 네트워크 요청에 사용되는 [Call.Factory]를 설정합니다.
         */
        fun callFactory(callFactory: Call.Factory) = apply {
            this.callFactory = lazyOf(callFactory)
        }

        /**
         * 네트워크 요청에 사용되는 [Call.Factory]를 생성하기 위해 지연 콜백을 설정합니다.
         *
         * 이것은 백그라운드 스레드에서 [Call.Factory]의 지연 생성을 허용합니다.
         * [이니셜라이저]는 최대 한 번만 호출되도록 보장됩니다.
         *
         * `callFactory(Call.Factory)` 대신 이것을 사용하는 것을 선호합니다.
         */
        fun callFactory(initializer: () -> Call.Factory) = apply {
            this.callFactory = lazy(initializer)
        }

        /**
         * [ComponentRegistry]를 빌드하고 설정합니다.
         */
        @JvmSynthetic
        inline fun components(
            builder: ComponentRegistry.Builder.() -> Unit
        ) = components(ComponentRegistry.Builder().apply(builder).build())

        /**
         * [ComponentRegistry]를 설정합니다.
         */
        fun components(components: ComponentRegistry) = apply {
            this.componentRegistry = components
        }
        /**
         * [MemoryCache]를 설정합니다.
         */
        fun memoryCache(memoryCache: MemoryCache?) = apply {
            this.memoryCache = lazyOf(memoryCache)
        }

        /**
         * [MemoryCache]를 생성하기 위해 지연 콜백을 설정합니다.
         *
         * `memoryCache(MemoryCache)` 대신 이것을 사용하는 것을 선호합니다.
         */
        fun memoryCache(initializer: () -> MemoryCache?) = apply {
            this.memoryCache = lazy(initializer)
        }

        /**
         * [DiskCache]를 설정합니다.
         *
         * 참고: 기본적으로 [ImageLoader]는 동일한 디스크 캐시 인스턴스를 공유합니다. 이것은 필요하다
         * 동일한 디렉토리에서 동시에 여러 디스크 캐시 인스턴스가 활성화됨
         * 디스크 캐시를 손상시킬 수 있습니다.
         *
         * @see DiskCache.directory
         */
        fun diskCache(diskCache: DiskCache?) = apply {
            this.diskCache = lazyOf(diskCache)
        }

        /**
         * 지연 콜백을 설정하여 [DiskCache]를 생성합니다.
         *
         * `diskCache(DiskCache)` 대신 이것을 사용하는 것을 선호합니다.
         *
         * 참고: 기본적으로 [ImageLoader]는 동일한 디스크 캐시 인스턴스를 공유합니다.
         * 동일한 디렉토리에서 동시에 여러 디스크 캐시 인스턴스를 활성화하면 디스크 캐시가 손상될 수 있으므로 이 작업이 필요합니다.
         *
         * @see DiskCache.directory
         */
        fun diskCache(initializer: () -> DiskCache?) = apply {
            this.diskCache = lazy(initializer)
        }

        /**
         * [Bitmap.Config.HARDWARE] 사용을 허용합니다.
         *
         * false인 경우 [Bitmap.Config.HARDWARE] 사용은 [Bitmap.Config.ARGB_8888]로 처리됩니다.
         *
         * 참고: 이를 false로 설정하면 API 26 이상에서 성능이 저하됩니다. 필요한 경우에만 이 기능을 비활성화하십시오.
         *
         * Default: true
         */
        fun allowHardware(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowHardware = enable)
        }

        /**
         * 이미지가 보장되지 않는 경우 [Bitmap.Config.RGB_565] 사용을 자동으로 허용합니다.
         * 알파가 있습니다.
         *
         * 이렇게 하면 이미지의 시각적 품질이 저하되지만 메모리 사용량도 줄어듭니다.
         *
         * 메모리가 부족하고 리소스가 제한된 장치에 대해서만 활성화하는 것이 좋습니다.
         *
         * Default: false
         */
        fun allowRgb565(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowRgb565 = enable)
        }

        /**
         * [File]에서 이미지를 로드할 때 메모리 캐시 키에 [File.lastModified]를 추가할 수 있습니다.
         *
         * 이렇게 하면 파일이 업데이트된 경우 동일한 파일을 로드하는 후속 요청이 메모리 캐시를 놓칠 수 있습니다.
         * 그러나 메인 스레드([interceptorDispatcher] 참조)에서 메모리 캐시 검사가 발생하면 [File.lastModified]를 호출하면 엄격 모드 위반이 발생합니다.
         *
         * Default: true
         */
        fun addLastModifiedToFileCacheKey(enable: Boolean) = apply {
            this.options = this.options.copy(addLastModifiedToFileCacheKey = enable)
        }

        /**
         * 장치가 오프라인인 경우 단락 네트워크 요청을 활성화합니다.
         *
         * true인 경우 장치가 오프라인인 경우 네트워크에서 읽기가 자동으로 비활성화됩니다. 캐시된 응답을 사용할 수 없는 경우 '504 Unsatisfiable Request' 응답과 함께 요청이 실패합니다.
         *
         * false인 경우 이미지 로더는 장치가 오프라인인 경우에도 네트워크 요청을 시도합니다.
         *
         * Default: true
         */
        fun networkObserverEnabled(enable: Boolean) = apply {
            this.options = this.options.copy(networkObserverEnabled = enable)
        }

        /**
         * 네트워크 캐시 헤더 지원을 활성화합니다.
         * 활성화된 경우 이 이미지 로더는 이미지를 디스크 캐시에서 저장하거나 제공할 수 있는지 결정할 때 네트워크 응답에서 반환된 캐시 헤더를 존중합니다.
         * 비활성화된 경우 이미지는 항상 디스크 캐시(있는 경우)에서 제공되며 최대 크기 미만으로 유지되도록 제거됩니다.
         *
         * Default: true
         */
        fun respectCacheHeaders(enable: Boolean) = apply {
            this.options = this.options.copy(respectCacheHeaders = enable)
        }

        /**
         * 한 번에 병렬 [BitmapFactory] 디코딩 작업의 최대 수를 설정합니다.
         *
         * 이 수를 늘리면 더 많은 병렬 [BitmapFactory] 디코딩 작업이 허용되지만 UI 성능이 저하될 수 있습니다.
         *
         * Default: 4
         */
        fun bitmapFactoryMaxParallelism(maxParallelism: Int) = apply {
            require(maxParallelism > 0) { "maxParallelism must be > 0." }
            this.options = this.options.copy(bitmapFactoryMaxParallelism = maxParallelism)
        }

        /**
         * 이 이미지 로더에 의해 시작된 요청에 대한 모든 콜백을 수신할 단일 [EventListener]를 설정합니다.
         *
         * @see eventListenerFactory
         */
        fun eventListener(listener: EventListener) = eventListenerFactory { listener }

        /**
         * [EventListener.Factory]를 설정하여 요청당 [EventListener]를 생성합니다.
         */
        fun eventListenerFactory(factory: EventListener.Factory) = apply {
            this.eventListenerFactory = factory
        }

        /**
         * 요청이 성공적으로 완료되면 지속 시간이 [CrossfadeDrawable.DEFAULT_DURATION]밀리초인 크로스페이드 애니메이션을 활성화합니다.
         *
         * Default: false
         */
        fun crossfade(enable: Boolean) =
            crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * 요청이 성공적으로 완료되면 [durationMillis]밀리초로 크로스페이드 애니메이션을 활성화합니다.
         *
         * @see `crossfade(Boolean)`
         */
        fun crossfade(durationMillis: Int) = apply {
            val factory = if (durationMillis > 0) {
                CrossfadeTransition.Factory(durationMillis)
            } else {
                Transition.Factory.NONE
            }
            transitionFactory(factory)
        }

        /**
         * 각 요청에 대해 기본 [Transition.Factory]를 설정합니다.
         */
        fun transitionFactory(factory: Transition.Factory) = apply {
            this.defaults = this.defaults.copy(transitionFactory = factory)
        }

        /**
         * 요청에 대한 기본 정밀도를 설정합니다. [precision]는 크기를 제어합니다.
         * 로드된 이미지는 요청의 크기와 정확히 일치해야 합니다.
         *
         * Default: [Precision.AUTOMATIC]
         */
        fun precision(precision: Precision) = apply {
            this.defaults = this.defaults.copy(precision = precision)
        }

        /**
         * 선호하는 [Bitmap.Config]를 설정합니다.
         *
         * 이것은 보장되지 않으며 상황에 따라 다른 구성이 사용될 수 있습니다.
         *
         * Default: [DEFAULT_BITMAP_CONFIG]
         */
        fun bitmapConfig(bitmapConfig: Bitmap.Config) = apply {
            this.defaults = this.defaults.copy(bitmapConfig = bitmapConfig)
        }

        /**
         * 한 번의 호출로 [fetcherDispatcher], [decoderDispatcher], [transformationDispatcher]를 설정하는 편의 함수입니다.
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(
                fetcherDispatcher = dispatcher,
                decoderDispatcher = dispatcher,
                transformationDispatcher = dispatcher
            )
        }

        /**
         * [Interceptor] 체인이 실행될 [CoroutineDispatcher]입니다.
         *
         * Default: `Dispatchers.Main.immediate`
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(interceptorDispatcher = dispatcher)
        }

        /**
         * [Fetcher.fetch]가 실행될 [CoroutineDispatcher]입니다.
         *
         * Default: [Dispatchers.IO]
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(fetcherDispatcher = dispatcher)
        }

        /**
         * [Decoder.decode]가 실행될 [CoroutineDispatcher]입니다.
         *
         * Default: [Dispatchers.IO]
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(decoderDispatcher = dispatcher)
        }

        /**
         * [Transformation.transform]이 실행될 [CoroutineDispatcher]입니다.
         *
         * Default: [Dispatchers.IO]
         */
        fun transformationDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(transformationDispatcher = dispatcher)
        }

        /**
         * 요청이 시작될 때 사용할 기본 자리 표시자 드로어블을 설정합니다.
         */
        fun placeholder(@DrawableRes drawableResId: Int) =
            placeholder(applicationContext.getDrawableCompat(drawableResId))

        /**
         * 요청이 시작될 때 사용할 기본 자리 표시자 드로어블을 설정합니다.
         */
        fun placeholder(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(placeholder = drawable)
        }

        /**
         * 요청이 실패할 때 사용할 기본 오류 드로어블을 설정합니다.
         */
        fun error(@DrawableRes drawableResId: Int) =
            error(applicationContext.getDrawableCompat(drawableResId))

        /**
         * 요청이 실패할 때 사용할 기본 오류 드로어블을 설정합니다.
         */
        fun error(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(error = drawable)
        }

        /**
         * [ImageRequest.data]가 null인 경우 사용할 기본 대체 드로어블을 설정합니다.
         */
        fun fallback(@DrawableRes drawableResId: Int) =
            fallback(applicationContext.getDrawableCompat(drawableResId))

        /**
         * [ImageRequest.data]가 null인 경우 사용할 기본 대체 드로어블을 설정합니다.
         */
        fun fallback(drawable: Drawable?) = apply {
            this.defaults = this.defaults.copy(fallback = drawable)
        }

        /**
         * 기본 메모리 캐시 정책을 설정합니다.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(memoryCachePolicy = policy)
        }

        /**
         * 기본 디스크 캐시 정책을 설정합니다.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(diskCachePolicy = policy)
        }

        /**
         * 기본 네트워크 캐시 정책을 설정합니다.
         *
         * 참고: 쓰기를 비활성화해도 효과가 없습니다.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(networkCachePolicy = policy)
        }

        /**
         * 로그를 기록할 [로거]를 설정합니다.
         *
         * 참고: [Logger]를 설정하면 성능이 저하될 수 있으므로 릴리스 빌드에서는 피해야 합니다.
         */
        fun logger(logger: Logger?) = apply {
            this.logger = logger
        }

        /**
         * 새 [ImageLoader] 인스턴스를 만듭니다.
         */
        fun build(): ImageLoader {
            return RealImageLoader(
                context = applicationContext,
                defaults = defaults,
                memoryCacheLazy = memoryCache ?: lazy { MemoryCache.Builder(applicationContext).build() },
                diskCacheLazy = diskCache ?: lazy { Utils.singletonDiskCache(applicationContext) },
                callFactoryLazy = callFactory ?: lazy { OkHttpClient() },
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = componentRegistry ?: ComponentRegistry(),
                options = options,
                logger = logger
            )
        }

        @Deprecated(
            message = "Migrate to 'memoryCache'.",
            replaceWith = ReplaceWith(
                expression = "memoryCache(MemoryCache.Builder(context).maxSizePercent(percent).build())",
                imports = ["coil.memory.MemoryCache"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) percent: Double): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'memoryCache'.",
            replaceWith = ReplaceWith(
                expression = "memoryCache(MemoryCache.Builder(context).weakReferencesEnabled(percent).build())",
                imports = ["coil.memory.MemoryCache"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun trackWeakReferences(enable: Boolean): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'interceptorDispatcher'.",
            replaceWith = ReplaceWith(
                expression = "interceptorDispatcher(if (enable) Dispatchers.Main.immediate else Dispatchers.IO)",
                imports = ["kotlinx.coroutines.Dispatchers"]
            ),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun launchInterceptorChainOnMainThread(enable: Boolean): Builder = unsupported()

        @Deprecated(
            message = "Replace with 'components'.",
            replaceWith = ReplaceWith("components(builder)"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        @JvmSynthetic
        fun componentRegistry(builder: ComponentRegistry.Builder.() -> Unit): Builder = unsupported()

        @Deprecated(
            message = "Replace with 'components'.",
            replaceWith = ReplaceWith("components(registry)"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun componentRegistry(registry: ComponentRegistry): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'transitionFactory'.",
            replaceWith = ReplaceWith("transitionFactory { _, _ -> transition }"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun transition(transition: Transition): Builder = unsupported()
    }

    companion object {
        /** 구성 없이 새 [ImageLoader]를 만듭니다. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}