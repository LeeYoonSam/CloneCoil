package com.ys.coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import com.ys.coil.DefaultRequestOptions
import com.ys.coil.size.Precision
import com.ys.coil.size.Scale
import com.ys.coil.size.SizeResolver
import com.ys.coil.target.Target
import com.ys.coil.transition.Transition
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers

class ImageRequest private constructor(
	val context: Context,

	/** @see Builder.data */
	val data: Any,

	/** @see Builder.target */
	val target: Target?,

	// /** @see Builder.listener */
	// val listener: Listener?,
	//
	// /** @see Builder.memoryCacheKey */
	// val memoryCacheKey: MemoryCache.Key?,

	/** @see Builder.diskCacheKey */
	val diskCacheKey: String?,

	/** @see Builder.colorSpace */
	val colorSpace: ColorSpace?,

	// /** @see Builder.fetcherFactory */
	// val fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?,
	//
	// /** @see Builder.decoderFactory */
	// val decoderFactory: Decoder.Factory?,
	//
	// /** @see Builder.transformations */
	// val transformations: List<Transformation>,

	/** @see Builder.headers */
	val headers: Headers,

	/** @see Builder.parameters */
	val parameters: Parameters,

	/** @see Builder.lifecycle */
	val lifecycle: Lifecycle,

	/** @see Builder.sizeResolver */
	val sizeResolver: SizeResolver,

	/** @see Builder.scale */
	val scale: Scale,

	/** @see Builder.interceptorDispatcher */
	val interceptorDispatcher: CoroutineDispatcher,

	/** @see Builder.fetcherDispatcher */
	val fetcherDispatcher: CoroutineDispatcher,

	/** @see Builder.decoderDispatcher */
	val decoderDispatcher: CoroutineDispatcher,

	/** @see Builder.transformationDispatcher */
	val transformationDispatcher: CoroutineDispatcher,

	/** @see Builder.transitionFactory */
	val transitionFactory: Transition.Factory,

	/** @see Builder.precision */
	val precision: Precision,

	/** @see Builder.bitmapConfig */
	val bitmapConfig: Bitmap.Config,

	/** @see Builder.allowConversionToBitmap */
	val allowConversionToBitmap: Boolean,

	/** @see Builder.allowHardware */
	val allowHardware: Boolean,

	/** @see Builder.allowRgb565 */
	val allowRgb565: Boolean,

	/** @see Builder.premultipliedAlpha */
	val premultipliedAlpha: Boolean,

	/** @see Builder.memoryCachePolicy */
	val memoryCachePolicy: CachePolicy,

	/** @see Builder.diskCachePolicy */
	val diskCachePolicy: CachePolicy,

	/** @see Builder.networkCachePolicy */
	val networkCachePolicy: CachePolicy,

	// /** @see Builder.placeholderMemoryCacheKey */
	// val placeholderMemoryCacheKey: MemoryCache.Key?,

	private val placeholderResId: Int?,
	private val placeholderDrawable: Drawable?,
	private val errorResId: Int?,
	private val errorDrawable: Drawable?,
	private val fallbackResId: Int?,
	private val fallbackDrawable: Drawable?,

	// /** The raw values set on [Builder]. */
	// val defined: DefinedRequestOptions,

	/** The defaults used to fill unset values. */
	val defaults: DefaultRequestOptions
) {

}