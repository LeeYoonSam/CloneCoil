package com.ys.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.ys.coil.annotation.BuilderMarker
import com.ys.coil.drawable.CrossfadeDrawable
import com.ys.coil.util.Utils
import com.ys.coil.util.getDrawableCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/** [ImageLoader]용 빌더. */
@BuilderMarker
class ImageLoaderBuilder(private val context: Context) {

    private var okHttpClient: OkHttpClient? = null
    private var okHttpClientBuilder: (OkHttpClient.Builder.() -> Unit)? = null

    private var registry: ComponentRegistry? = null

    private var availableMemoryPercentage: Double = Utils.getDefaultAvailableMemoryPercentage(context)
    private var bitmapPoolPercentage: Double = Utils.getDefaultBitmapPoolPercentage()

    private var defaults = DefaultRequestOptions()

    /**
     * 네트워크 요청에 사용할 [OkHttpClient]를 설정합니다.
     *
     * 가능하면 `okHttpClient(OkHttpClient.Builder.() -> Unit)`를 선호하고,
     * 기본 [OkHttpClient] 인스턴스는 Coil에 최적화되어 있습니다.
     */
    fun okHttpClient(client: OkHttpClient) = apply {
        this.okHttpClient = client
        this.okHttpClientBuilder = null
    }

    /**
     * 기본 [OkHttpClient]를 빌드할 때 호출되는 콜백을 설정합니다.
     *
     * @buildDefaultOkHttpClient 참조
     */
    fun okHttpClient(builder: OkHttpClient.Builder.() -> Unit) = apply {
        this.okHttpClientBuilder = builder
        this.okHttpClient = null
    }

    /**
     * [ComponentRegistry]를 빌드하고 설정합니다.
     */
    inline fun componentRegistry(builder: ComponentRegistry.Builder.() -> Unit) = apply {
        componentRegistry(ComponentRegistry(builder))
    }

    /**
     * [ComponentRegistry]를 설정합니다.
     */
    fun componentRegistry(registry: ComponentRegistry) = apply {
        this.registry = registry
    }

    /**
     * 이 [ImageLoader]의 메모리 캐시와 비트맵 풀에 할당할 사용 가능한 메모리의 백분율을 설정합니다.
     *
     * 이것을 0으로 설정하면 메모리 캐싱 및 비트맵 풀링이 비활성화됩니다.
     *
     * 기본값: [Utils.getDefaultAvailableMemoryPercentage]
     */
    fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) multiplier: Double) = apply {
        require(multiplier in 0.0..1.0) { "Multiplier must be within the range [0.0, 1.0]." }
        this.availableMemoryPercentage = multiplier
    }

    /**
     * 이 [ImageLoader]에 할당된 메모리의 백분율을 설정하여 비트맵 풀링에 할당합니다.
     *
     * 즉, [availableMemoryPercentage]를 0.25로, [bitmapPoolPercentage]를 0.5로 설정하면 이 ImageLoader가 허용됩니다.
     * 앱 총 메모리의 25%를 사용하고 해당 메모리를 비트맵 풀과 메모리 캐시 간에 50/50으로 분할합니다.
     *
     * 이것을 0으로 설정하면 비트맵 풀링이 비활성화됩니다.
     *
     * 기본값: [Utils.getDefaultBitmapPoolPercentage]
     */
    fun bitmapPoolPercentage(@FloatRange(from = 0.0, to = 1.0) multiplier: Double) = apply {
        require(multiplier in 0.0..1.0) { "Multiplier must be within the range [0.0, 1.0]." }
        this.bitmapPoolPercentage = multiplier
    }

    /**
     * 이미지 요청을 실행할 기본 [CoroutineDispatcher]입니다.
     *
     * Default: [Dispatchers.IO]
     */
    fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
        this.defaults = this.defaults.copy(dispatcher = dispatcher)
    }

    /**
     * 이미지에 알파가 없는 것이 보장될 때 [Bitmap.Config.RGB_565] 사용을 자동으로 허용합니다.
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
     * 로드할 때 [CrossfadeDrawable.DEFAULT_DURATION]밀리초 지속 시간으로 크로스페이드 애니메이션 활성화
     * 이미지를 [ImageViewTarget]에 넣습니다.
     *
     * 참고: 크로스페이드는 [ImageViewTarget]에만 적용됩니다.
     *
     * Default: false
     */
    fun crossfade(enable: Boolean) = apply {
        this.defaults = this.defaults.copy(crossfadeMillis = if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)
    }

    /**
     * 이미지를 [ImageViewTarget]에 로드할 때 [durationMillis]밀리초로 크로스페이드 애니메이션을 활성화합니다.
     *
     * @see `crossfade(Boolean)`
     */
    fun crossfade(durationMillis: Int) = apply {
        require(durationMillis >= 0) { "Duration must be >= 0." }
        this.defaults = this.defaults.copy(crossfadeMillis = durationMillis)
    }

    /**
     * 요청이 시작될 때 사용할 기본 자리 표시자 드로어블을 설정합니다.
     */
    fun placeholder(@DrawableRes drawableResId: Int) = apply {
        this.defaults = this.defaults.copy(placeholder = context.getDrawableCompat(drawableResId))
    }

    /**
     * 요청이 시작될 때 사용할 기본 자리 표시자 드로어블을 설정합니다.
     */
    fun placeholder(drawable: Drawable?) = apply {
        this.defaults = this.defaults.copy(placeholder = drawable)
    }

    /**
     * 요청이 실패할 때 사용할 기본 오류 드로어블을 설정합니다.
     */
    fun error(@DrawableRes drawableResId: Int) = apply {
        this.defaults = this.defaults.copy(error = context.getDrawableCompat(drawableResId))
    }

    /**
     * 요청이 실패할 때 사용할 기본 오류 드로어블을 설정합니다.
     */
    fun error(drawable: Drawable?) = apply {
        this.defaults = this.defaults.copy(error = drawable)
    }

    /**
     * 새 [ImageLoader] 인스턴스를 만듭니다.
     */
    fun build(): ImageLoader {
        val availableMemorySize = Utils.calculateAvailableMemorySize(context, availableMemoryPercentage)
        val bitmapPoolSize = (bitmapPoolPercentage * availableMemorySize).toLong()
        val memoryCacheSize = (availableMemorySize - bitmapPoolSize).toInt()

        return RealImageLoader(
            context = context,
            defaults = defaults,
            bitmapPoolSize = bitmapPoolSize,
            memoryCacheSize = memoryCacheSize,
            okHttpClient = okHttpClient ?: buildDefaultOkHttpClient(),
            registry = registry ?: ComponentRegistry()
        )
    }

    private fun buildDefaultOkHttpClient(): OkHttpClient {
        // 기본 이미지 디스크 캐시를 만듭니다.
        val cacheDirectory = Utils.getDefaultCacheDirectory(context)
        val cacheSize = Utils.calculateDiskCacheSize(cacheDirectory)
        val cache = Cache(cacheDirectory, cacheSize)

        // 호스트별 요청 수를 제한하지 마십시오.
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = maxRequests
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .dispatcher(dispatcher)
            .apply { okHttpClientBuilder?.invoke(this) }
            .build()
    }
}