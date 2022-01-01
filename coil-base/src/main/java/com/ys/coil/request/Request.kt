package com.ys.coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import com.ys.coil.decode.DataSource
import com.ys.coil.size.Scale
import com.ys.coil.size.SizeResolver
import com.ys.coil.target.Target
import com.ys.coil.transform.Transformation
import com.ys.coil.util.getDrawableCompat
import kotlinx.coroutines.CoroutineDispatcher

/**
 * 이미지 요청을 나타내는 값 개체입니다.
 *
 * @see LoadRequest
 * @see GetRequest
 */
sealed class Request {

    abstract val data: Any?

    abstract val target: Target?
    abstract val lifecycle: Lifecycle?
    abstract val crossfadeMillis: Int

    abstract val keyOverride: String?
    abstract val listener: Listener?
    abstract val sizeResolver: SizeResolver?
    abstract val scale: Scale?
    abstract val dispatcher: CoroutineDispatcher
    abstract val transformations: List<Transformation>
    abstract val bitmapConfig: Bitmap.Config
    abstract val colorSpace: ColorSpace?

    abstract val networkCachePolicy: CachePolicy
    abstract val diskCachePolicy: CachePolicy
    abstract val memoryCachePolicy: CachePolicy

    abstract val allowHardware: Boolean
    abstract val allowRgb565: Boolean

    abstract val placeholder: Drawable?
    abstract val error: Drawable?

    /**
     * [Request]에 대한 콜백 집합입니다. 모든 콜백은 기본 스레드에서 호출되도록 보장됩니다.
     */
    interface Listener {

        /**
         * 요청이 전달되고 이미지 로드를 시작할 때 호출됩니다.
         */
        fun onStart(data: Any) {}

        /**
         * 요청이 이미지를 성공적으로 로드할 때 호출됩니다.
         */
        fun onSuccess(data: Any, source: DataSource) {}

        /**
         * 요청이 취소될 때 호출됩니다.
         */
        fun onCancel(data: Any) {}

        /**
         * 요청이 이미지 로드에 실패할 때 호출됩니다.
         */
        fun onError(data: Any, throwable: Throwable) {}
    }
}

/**
 * *load* 이미지 요청을 나타내는 값 개체입니다.
 *
 * Instances can be created ad hoc:
 * ```
 * imageLoader.load(context, "https://www.example.com/image.jpg") {
 *     crossfade(true)
 *     target(imageView)
 * }
 * ```
 *
 * 또는 인스턴스를 실행하는 호출과 별도로 인스턴스를 만들 수 있습니다.
 * ```
 * val request = LoadRequest(context, imageLoader.defaults) {
 *     data("https://www.example.com/image.jpg")
 *     crossfade(true)
 *     target(imageView)
 * }
 * imageLoader.load(request)
 * ```
 *
 * @see LoadRequestBuilder
 * @see ImageLoader.load
 */
class LoadRequest internal constructor(
    val context: Context,
    override val data: Any?,
    override val target: Target?,
    override val lifecycle: Lifecycle?,
    override val crossfadeMillis: Int,
    override val keyOverride: String?,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean,
    @DrawableRes internal val placeholderResId: Int,
    @DrawableRes internal val errorResId: Int,
    internal val placeholderDrawable: Drawable?,
    internal val errorDrawable: Drawable?
) : Request() {

    companion object {
        /** 새 [LoadRequest] 인스턴스를 만듭니다. */
        inline operator fun invoke(
            context: Context,
            defaults: DefaultRequestOptions,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context, defaults).apply(builder).build()

        /** 새 [LoadRequest] 인스턴스를 만듭니다. */
        inline operator fun invoke(
            context: Context,
            request: LoadRequest,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context, request).apply(builder).build()
    }

    override val placeholder: Drawable?
        get() = context.getDrawable(placeholderDrawable, placeholderResId)

    override val error: Drawable?
        get() = context.getDrawable(errorDrawable, errorResId)

    private fun Context.getDrawable(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
        return drawable ?: if (resId != 0) getDrawableCompat(resId) else null
    }

    /** 이를 기반으로 새 [LoadRequestBuilder] 인스턴스를 만듭니다. */
    @JvmOverloads
    fun newBuilder(context: Context = this.context) = LoadRequestBuilder(context, this)
}

/**
 * *get* 이미지 요청을 나타내는 값 개체입니다.
 *
 * 인스턴스는 임시로 생성할 수 있습니다:
 * ```
 * val drawable = imageLoader.get("https://www.example.com/image.jpg") {
 *     size(1080, 1920)
 * }
 * ```
 *
 * 또는 인스턴스를 실행하는 호출과 별도로 인스턴스를 만들 수 있습니다:
 * ```
 * val request = GetRequest(imageLoader.defaults) {
 *     data("https://www.example.com/image.jpg")
 *     size(1080, 1920)
 * }
 * imageLoader.get(request)
 * ```
 *
 * @see GetRequestBuilder
 * @see ImageLoader.get
 */
class GetRequest internal constructor(
    override val data: Any,
    override val keyOverride: String?,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean
) : Request() {

    companion object {
        /** 새 [GetRequest] 인스턴스를 만듭니다. */
        inline operator fun invoke(
            defaults: DefaultRequestOptions,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(defaults).apply(builder).build()

        /** 새 [GetRequest] 인스턴스를 만듭니다. */
        inline operator fun invoke(
            request: GetRequest,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(request).apply(builder).build()
    }

    override val target: Target? = null

    override val lifecycle: Lifecycle? = null

    override val crossfadeMillis: Int = 0

    override val placeholder: Drawable? = null

    override val error: Drawable? = null

    /** 이를 기반으로 새 [GetRequestBuilder] 인스턴스를 만듭니다. */
    fun newBuilder() = GetRequestBuilder(this)
}