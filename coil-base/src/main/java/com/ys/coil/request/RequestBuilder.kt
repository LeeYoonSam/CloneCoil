package com.ys.coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.DefaultRequestOptions
import com.ys.coil.annotation.BuilderMarker
import com.ys.coil.decode.DataSource
import com.ys.coil.drawable.CrossfadeDrawable
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.size.SizeResolver
import com.ys.coil.transform.Transformation
import com.ys.coil.util.Utils
import com.ys.coil.util.self
import kotlinx.coroutines.CoroutineDispatcher
import com.ys.coil.memory.RequestService
import com.ys.coil.size.PixelSize
import com.ys.coil.fetch.Fetcher
import com.ys.coil.target.ImageViewTarget
import com.ys.coil.target.Target

/** [LoadRequestBuilder] 및 [GetRequestBuilder]의 기본 클래스입니다. */
@BuilderMarker
sealed class RequestBuilder<T : RequestBuilder<T>> {

    protected var data: Any?

    protected var keyOverride: String?
    protected var listener: Request.Listener?
    protected var sizeResolver: SizeResolver?
    protected var scale: Scale?
    protected var dispatcher: CoroutineDispatcher
    protected var transformations: List<Transformation>
    protected var bitmapConfig: Bitmap.Config
    protected var colorSpace: ColorSpace? = null

    protected var networkCachePolicy: CachePolicy
    protected var diskCachePolicy: CachePolicy
    protected var memoryCachePolicy: CachePolicy

    protected var allowHardware: Boolean
    protected var allowRgb565: Boolean

    constructor(defaults: DefaultRequestOptions) {
        data = null

        keyOverride = null
        listener = null
        sizeResolver = null
        scale = null
        dispatcher = defaults.dispatcher
        transformations = emptyList()
        bitmapConfig = Utils.getDefaultBitmapConfig()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            colorSpace = null
        }

        networkCachePolicy = CachePolicy.ENABLED
        diskCachePolicy = CachePolicy.ENABLED
        memoryCachePolicy = CachePolicy.ENABLED

        allowHardware = true
        allowRgb565 = defaults.allowRgb565
    }

    constructor(request: Request) {
        data = request.data

        keyOverride = request.keyOverride
        listener = request.listener
        sizeResolver = request.sizeResolver
        scale = request.scale
        dispatcher = request.dispatcher
        transformations = request.transformations
        bitmapConfig = request.bitmapConfig
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            colorSpace = request.colorSpace
        }

        networkCachePolicy = request.networkCachePolicy
        diskCachePolicy = request.diskCachePolicy
        memoryCachePolicy = request.memoryCachePolicy

        allowHardware = request.allowHardware
        allowRgb565 = request.allowRgb565
    }

    /**
     * [Request.Listener] 생성 및 설정을 위한 편리한 기능.
     */
    inline fun listener(
        crossinline onStart: (data: Any) -> Unit = {},
        crossinline onCancel: (data: Any) -> Unit = {},
        crossinline onError: (data: Any, throwable: Throwable) -> Unit = { _, _ -> },
        crossinline onSuccess: (data: Any, source: DataSource) -> Unit = { _, _ -> }
    ): T = self {
        listener(object : Request.Listener {
            override fun onStart(data: Any) = onStart(data)
            override fun onCancel(data: Any) = onCancel(data)
            override fun onError(data: Any, throwable: Throwable) = onError(data, throwable)
            override fun onSuccess(data: Any, source: DataSource) = onSuccess(data, source)
        })
    }

    /**
     * [Request.Listener]를 설정합니다.
     */
    fun listener(listener: Request.Listener?): T = self {
        this.listener = listener
    }

    /**
     * 이 요청에 적용할 [Transformation] 목록을 설정합니다.
     */
    fun transformations(vararg transformations: Transformation): T = self {
        this.transformations = transformations.toList()
    }

    /**
     * [CoroutineDispatcher]를 설정합니다.
     */
    fun dispatcher(dispatcher: CoroutineDispatcher): T = self {
        this.dispatcher = dispatcher
    }

    /**
     * 요청된 너비/높이를 설정합니다. 코일은 이러한 치수로 이미지를 메모리에 로드하려고 시도합니다.
     */
    fun size(@Px size: Int): T = self {
        size(size, size)
    }

    /**
     * 요청된 너비/높이를 설정합니다. 코일은 이러한 치수로 이미지를 메모리에 로드하려고 시도합니다.
     */
    fun size(@Px width: Int, @Px height: Int): T = self {
        size(PixelSize(width, height))
    }

    /**
     * 요청된 너비/높이를 설정합니다. 코일은 이러한 치수로 이미지를 메모리에 로드하려고 시도합니다.
     */
    fun size(size: Size): T = self {
        this.sizeResolver = SizeResolver(size)
    }

    /**
     * 요청된 너비/높이를 설정합니다. 코일은 이러한 치수로 이미지를 메모리에 로드하려고 시도합니다.
     *
     * 이것이 설정되지 않으면 Coil은 [RequestService.sizeResolver]의 로직을 사용하여 요청의 크기를 결정하려고 시도합니다.
     */
    fun size(resolver: SizeResolver): T = self {
        this.sizeResolver = resolver
    }

    /**
     * [sizeResolver]에서 제공하는 치수에 이미지를 맞추거나 채우는 데 사용할 크기 조정 알고리즘을 설정합니다.
     *
     * 이것이 설정되지 않으면 Coil은 [RequestService.scale]의 로직을 사용하여 요청의 규모를 결정하려고 시도합니다.
     *
     * 참고: [scale]이 설정되지 않은 경우 [ImageView] 대상에 대해 자동으로 계산됩니다.
     */
    fun scale(scale: Scale): T = self {
        this.scale = scale
    }

    /**
     * 이 요청에 대해 [Bitmap.Config.HARDWARE] 사용을 활성화/비활성화합니다.
     *
     * false인 경우 [Bitmap.Config.HARDWARE] 사용은 [Bitmap.Config.ARGB_8888]로 처리됩니다.
     *
     * 하드웨어 비트맵을 지원하지 않는 공유 요소 전환에 유용합니다.
     */
    fun allowHardware(enable: Boolean): T = self {
        this.allowHardware = enable
    }

    /**
     * 참조: [ImageLoaderBuilder.allowRgb565]
     */
    fun allowRgb565(enable: Boolean): T = self {
        this.allowRgb565 = enable
    }

    /**
     * 원하는 [Bitmap.Config]를 설정합니다.
     *
     * 이것은 보장되지 않으며 상황에 따라 다른 구성이 사용될 수 있습니다.
     */
    fun bitmapConfig(bitmapConfig: Bitmap.Config): T = self {
        this.bitmapConfig = bitmapConfig
    }

    /**
     * 원하는 [ColorSpace]를 설정합니다.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun colorSpace(colorSpace: ColorSpace): T = self {
        this.colorSpace = colorSpace
    }

    /**
     * 이 요청에 대한 캐시 키를 설정합니다.
     *
     * 기본적으로 캐시 키는 [Fetcher] 및 모든 [Transformation]에 의해 계산됩니다.
     */
    fun key(key: String?): T = self {
        this.keyOverride = key
    }

    /**
     * 네트워크에서 읽기/쓰기를 활성화/비활성화합니다.
     *
     * 참고: 쓰기를 비활성화해도 효과가 없습니다.
     */
    fun networkCachePolicy(policy: CachePolicy): T = self {
        this.networkCachePolicy = policy
    }

    /**
     * 디스크 캐시에서 읽기/쓰기를 활성화/비활성화합니다.
     */
    fun diskCachePolicy(policy: CachePolicy): T = self {
        this.diskCachePolicy = policy
    }

    /**
     * 메모리에서 읽기/쓰기를 활성화/비활성화합니다.
     */
    fun memoryCachePolicy(policy: CachePolicy): T = self {
        this.memoryCachePolicy = policy
    }
}

/** [GetRequest]를 위한 빌더. */
class GetRequestBuilder : RequestBuilder<GetRequestBuilder> {

    constructor(defaults: DefaultRequestOptions) : super(defaults)

    constructor(request: GetRequest) : super(request)

    /**
     * 로드할 데이터를 설정합니다.
     */
    fun data(data: Any) = apply {
        this.data = data
    }

    fun build(): GetRequest {
        return GetRequest(
            checkNotNull(data) { "data == null" },
            keyOverride = keyOverride,
            listener = listener,
            sizeResolver = sizeResolver,
            scale = scale,
            dispatcher = dispatcher,
            transformations = transformations,
            bitmapConfig = bitmapConfig,
            colorSpace = colorSpace,
            networkCachePolicy = networkCachePolicy,
            diskCachePolicy = diskCachePolicy,
            memoryCachePolicy = memoryCachePolicy,
            allowHardware = allowHardware,
            allowRgb565 = allowRgb565
        )
    }
}

/** [LoadRequest]를 위한 빌더. */
class LoadRequestBuilder : RequestBuilder<LoadRequestBuilder> {

    private val context: Context

    private var target: Target?
    private var lifecycle: Lifecycle?
    private var crossfadeMillis: Int

    @DrawableRes private var placeholderResId: Int
    @DrawableRes private var errorResId: Int
    private var placeholderDrawable: Drawable?
    private var errorDrawable: Drawable?

    constructor(context: Context, defaults: DefaultRequestOptions) : super(defaults) {
        this.context = context

        target = null
        lifecycle = null
        crossfadeMillis = defaults.crossfadeMillis

        placeholderResId = 0
        errorResId = 0
        placeholderDrawable = defaults.placeholder
        errorDrawable = defaults.error
    }

    constructor(context: Context, request: LoadRequest) : super(request) {
        this.context = context

        target = request.target
        lifecycle = request.lifecycle
        crossfadeMillis = request.crossfadeMillis

        placeholderResId = request.placeholderResId
        errorResId = request.errorResId
        placeholderDrawable = request.placeholderDrawable
        errorDrawable = request.errorDrawable
    }

    /**
     * 로드할 데이터를 설정합니다.
     */
    fun data(data: Any?) = apply {
        this.data = data
    }

    /**
     * [imageView]를 [Target]으로 설정하는 편리한 기능입니다.
     */
    fun target(imageView: ImageView) = apply {
        target(ImageViewTarget(imageView))
    }

    /**
     * [Target]을 생성하고 설정할 수 있는 편리한 기능입니다.
     */
    inline fun target(
        crossinline onStart: (placeholder: Drawable?) -> Unit = {},
        crossinline onError: (error: Drawable?) -> Unit = {},
        crossinline onSuccess: (result: Drawable) -> Unit = {}
    ) = apply {
        target(object : Target {
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable) = onSuccess(result)
        })
    }

    /**
     * [Target]을 설정합니다. 대상이 null인 경우 이 요청은 이미지를 메모리에 미리 로드합니다.
     */
    fun target(target: Target?) = apply {
        this.target = target
    }

    /**
     * 이 요청에 대해 [Lifecycle]을 설정합니다.
     */
    fun lifecycle(owner: LifecycleOwner?) = apply {
        lifecycle(owner?.lifecycle)
    }

    /**
     * 이 요청에 대해 [Lifecycle]을 설정합니다.
     *
     * 수명 주기가 [Lifecycle.State.STARTED] 이상이 아닌 동안 요청이 대기열에 있습니다.
     * 수명 주기가 [Lifecycle.State.DESTROYED]에 도달하면 요청이 취소됩니다.
     *
     * 이것이 설정되지 않으면 Coil은 이 요청에 대한 수명 주기를 찾으려고 시도합니다.
     * [RequestService.lifecycleInfo]의 로직을 사용합니다.
     */
    fun lifecycle(lifecycle: Lifecycle?) = apply {
        this.lifecycle = lifecycle
    }

    /**
     * 참조: [ImageLoaderBuilder.crossfade]
     */
    fun crossfade(enable: Boolean) = apply {
        this.crossfadeMillis = if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0
    }

    /**
     * 참조: [ImageLoaderBuilder.crossfade]
     */
    fun crossfade(durationMillis: Int) = apply {
        require(durationMillis >= 0) { "Duration must be >= 0." }
        this.crossfadeMillis = durationMillis
    }

    /**
     * 요청이 시작될 때 사용할 placeholder에 드로어블을 설정합니다.
     */
    fun placeholder(@DrawableRes drawableResId: Int) = apply {
        this.placeholderResId = drawableResId
        this.placeholderDrawable = null
    }

    /**
     * 요청이 시작될 때 사용할 placeholder에 드로어블을 설정합니다.
     */
    fun placeholder(drawable: Drawable?) = apply {
        this.placeholderDrawable = drawable
        this.placeholderResId = 0
    }

    /**
     * 요청이 실패할 경우 사용할 오류 드로어블을 설정합니다.
     */
    fun error(@DrawableRes drawableResId: Int) = apply {
        this.errorResId = drawableResId
        this.errorDrawable = null
    }

    /**
     * 요청이 실패할 경우 사용할 오류 드로어블을 설정합니다.
     */
    fun error(drawable: Drawable?) = apply {
        this.errorDrawable = drawable
        this.errorResId = 0
    }

    /** 새 [LoadRequest] 인스턴스를 만듭니다. */
    fun build(): LoadRequest {
        return LoadRequest(
            context = context,
            data = data,
            target = target,
            lifecycle = lifecycle,
            crossfadeMillis = crossfadeMillis,
            keyOverride = keyOverride,
            listener = listener,
            sizeResolver = sizeResolver,
            scale = scale,
            dispatcher = dispatcher,
            transformations = transformations,
            bitmapConfig = bitmapConfig,
            colorSpace = colorSpace,
            networkCachePolicy = networkCachePolicy,
            diskCachePolicy = diskCachePolicy,
            memoryCachePolicy = memoryCachePolicy,
            allowHardware = allowHardware,
            allowRgb565 = allowRgb565,
            placeholderResId = placeholderResId,
            errorResId = errorResId,
            placeholderDrawable = placeholderDrawable,
            errorDrawable = errorDrawable
        )
    }
}