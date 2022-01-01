package com.ys.coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.decode.DecoderNew
import com.ys.coil.fetch.Fetcher
import com.ys.coil.lifecycle.GlobalLifecycle
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.ImageRequest.Builder
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Precision
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.size.SizeResolver
import com.ys.coil.size.ViewSizeResolver
import com.ys.coil.target.Target
import com.ys.coil.target.ViewTarget
import com.ys.coil.transform.Transformation
import com.ys.coil.transition.CrossfadeTransition
import com.ys.coil.transition.Transition
import com.ys.coil.util.DEFAULT_REQUEST_OPTIONS
import com.ys.coil.util.getDrawableCompat
import com.ys.coil.util.getLifecycle
import com.ys.coil.util.orEmpty
import com.ys.coil.util.scale
import com.ys.coil.util.unsupported
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File
import java.nio.ByteBuffer

class ImageRequest private constructor(
	val context: Context,

	/** @see Builder.data */
	val data: Any,

	/** @see Builder.target */
	val target: Target?,

	/** @see Builder.listener */
	val listener: Listener?,
	//
	/** @see Builder.memoryCacheKey */
	val memoryCacheKey: MemoryCache.Key?,

	/** @see Builder.diskCacheKey */
	val diskCacheKey: String?,

	/** @see Builder.colorSpace */
	val colorSpace: ColorSpace?,

	/** @see Builder.fetcherFactory */
	val fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?,

	/** @see Builder.decoderFactory */
	val decoderFactory: DecoderNew.Factory?,

	/** @see Builder.transformations */
	val transformations: List<Transformation>,

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

	/** @see Builder.placeholderMemoryCacheKey */
	val placeholderMemoryCacheKey: MemoryCache.Key?,

	private val placeholderResId: Int?,
	private val placeholderDrawable: Drawable?,
	private val errorResId: Int?,
	private val errorDrawable: Drawable?,
	private val fallbackResId: Int?,
	private val fallbackDrawable: Drawable?,

	/** [Builder]에 설정된 원시 값입니다. */
	val defined: DefinedRequestOptions,

	/** 설정되지 않은 값을 채우는 데 사용되는 기본값입니다. */
	val defaults: DefaultRequestOptions
) {

	/** @see Builder.placeholder */
	val placeholder: Drawable? get() =
		getDrawableCompat(placeholderDrawable, placeholderResId, defaults.placeholder)

	/** @see Builder.error */
	val error: Drawable? get() =
		getDrawableCompat(errorDrawable, errorResId, defaults.error)

	/** @see Builder.fallback */
	val fallback: Drawable? get() =
		getDrawableCompat(fallbackDrawable, fallbackResId, defaults.fallback)


	@JvmOverloads
	fun newBuilder(context: Context = this.context) = Builder(this, context)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true

		return other is ImageRequest &&
			context == other.context &&
			data == other.data &&
			target == other.target &&
			listener == other.listener &&
			memoryCacheKey == other.memoryCacheKey &&
			diskCacheKey == other.diskCacheKey &&
			(VERSION.SDK_INT < 26 || colorSpace == other.colorSpace) &&
			fetcherFactory == other.fetcherFactory &&
			decoderFactory == other.decoderFactory &&
			transformations == other.transformations &&
			headers == other.headers &&
			parameters == other.parameters &&
			lifecycle == other.lifecycle &&
			sizeResolver == other.sizeResolver &&
			scale == other.scale &&
			interceptorDispatcher == other.interceptorDispatcher &&
			fetcherDispatcher == other.fetcherDispatcher &&
			decoderDispatcher == other.decoderDispatcher &&
			transformationDispatcher == other.transformationDispatcher &&
			transitionFactory == other.transitionFactory &&
			precision == other.precision &&
			bitmapConfig == other.bitmapConfig &&
			allowConversionToBitmap == other.allowConversionToBitmap &&
			allowHardware == other.allowHardware &&
			allowRgb565 == other.allowRgb565 &&
			premultipliedAlpha == other.premultipliedAlpha &&
			memoryCachePolicy == other.memoryCachePolicy &&
			diskCachePolicy == other.diskCachePolicy &&
			networkCachePolicy == other.networkCachePolicy &&
			placeholderMemoryCacheKey == other.placeholderMemoryCacheKey &&
			placeholderResId == other.placeholderResId &&
			placeholderDrawable == other.placeholderDrawable &&
			errorResId == other.errorResId &&
			errorDrawable == other.errorDrawable &&
			fallbackResId == other.fallbackResId &&
			fallbackDrawable == other.fallbackDrawable &&
			defined == other.defined &&
			defaults == other.defaults

	}

	override fun hashCode(): Int {
		var result = context.hashCode()
		result = 31 * result + data.hashCode()
		result = 31 * result + (target?.hashCode() ?: 0)
		result = 31 * result + (listener?.hashCode() ?: 0)
		result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
		result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
		result = 31 * result + if (VERSION.SDK_INT < 26) 0 else (colorSpace?.hashCode() ?: 0)
		result = 31 * result + (fetcherFactory?.hashCode() ?: 0)
		result = 31 * result + (decoderFactory?.hashCode() ?: 0)
		result = 31 * result + transformations.hashCode()
		result = 31 * result + headers.hashCode()
		result = 31 * result + parameters.hashCode()
		result = 31 * result + lifecycle.hashCode()
		result = 31 * result + sizeResolver.hashCode()
		result = 31 * result + scale.hashCode()
		result = 31 * result + interceptorDispatcher.hashCode()
		result = 31 * result + fetcherDispatcher.hashCode()
		result = 31 * result + decoderDispatcher.hashCode()
		result = 31 * result + transformationDispatcher.hashCode()
		result = 31 * result + transitionFactory.hashCode()
		result = 31 * result + precision.hashCode()
		result = 31 * result + bitmapConfig.hashCode()
		result = 31 * result + allowConversionToBitmap.hashCode()
		result = 31 * result + allowHardware.hashCode()
		result = 31 * result + allowRgb565.hashCode()
		result = 31 * result + premultipliedAlpha.hashCode()
		result = 31 * result + memoryCachePolicy.hashCode()
		result = 31 * result + diskCachePolicy.hashCode()
		result = 31 * result + networkCachePolicy.hashCode()
		result = 31 * result + (placeholderMemoryCacheKey?.hashCode() ?: 0)
		result = 31 * result + (placeholderResId ?: 0)
		result = 31 * result + (placeholderDrawable?.hashCode() ?: 0)
		result = 31 * result + (errorResId ?: 0)
		result = 31 * result + (errorDrawable?.hashCode() ?: 0)
		result = 31 * result + (fallbackResId ?: 0)
		result = 31 * result + (fallbackDrawable?.hashCode() ?: 0)
		result = 31 * result + defined.hashCode()
		result = 31 * result + defaults.hashCode()
		return result
	}

	/**
	 * [ImageRequest]에 대한 콜백 집합입니다.
	 */
	interface Listener {

		/**
		 * [Target.onStart] 직후에 호출됩니다.
		 */
		@MainThread
		fun onStart(request: ImageRequest) {}

		/**
		 * 요청이 취소되면 호출됩니다.
		 */
		fun onCancel(request: ImageRequest) {}

		/**
		 * 요청을 실행하는 동안 오류가 발생하면 호출됩니다.
		 */
		fun onError(request: ImageRequest, result: ErrorResult)

		/**
		 * 요청이 성공적으로 완료되면 호출됩니다.
		 */
		fun onSuccess(request: ImageRequest, result: SuccessResult)
	}

	class Builder {

		private val context: Context
		private var defaults: DefaultRequestOptions
		private var data: Any?

		private var target: Target?
		private var listener: Listener?
		private var memoryCacheKey: MemoryCache.Key?
		private var diskCacheKey: String?
		private var colorSpace: ColorSpace? = null
		private var fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?
		private var decoderFactory: DecoderNew.Factory?
		private var transformations: List<Transformation>

		private var headers: Headers.Builder?
		private var parameters: Parameters.Builder?

		private var lifecycle: Lifecycle?
		private var sizeResolver: SizeResolver?
		private var scale: Scale?

		private var interceptorDispatcher: CoroutineDispatcher?
		private var fetcherDispatcher: CoroutineDispatcher?
		private var decoderDispatcher: CoroutineDispatcher?
		private var transformationDispatcher: CoroutineDispatcher?

		private var transitionFactory: Transition.Factory?
		private var precision: Precision?
		private var bitmapConfig: Bitmap.Config?
		private var allowHardware: Boolean?
		private var allowRgb565: Boolean?
		private var premultipliedAlpha: Boolean
		private var allowConversionToBitmap: Boolean
		private var memoryCachePolicy: CachePolicy?
		private var diskCachePolicy: CachePolicy?
		private var networkCachePolicy: CachePolicy?

		private var placeholderMemoryCacheKey: MemoryCache.Key?
		@DrawableRes private var placeholderResId: Int?
		private var placeholderDrawable: Drawable?
		@DrawableRes private var errorResId: Int?
		private var errorDrawable: Drawable?
		@DrawableRes private var fallbackResId: Int?
		private var fallbackDrawable: Drawable?

		private var resolvedLifecycle: Lifecycle?
		private var resolvedSizeResolver: SizeResolver?
		private var resolvedScale: Scale?

		constructor(context: Context) {
			this.context = context
			defaults = DEFAULT_REQUEST_OPTIONS
			data = null
			target = null
			listener = null
			memoryCacheKey = null
			diskCacheKey = null
			if (VERSION.SDK_INT >= 26) colorSpace = null
			fetcherFactory = null
			decoderFactory = null
			transformations = emptyList()
			headers = null
			parameters = null
			lifecycle = null
			sizeResolver = null
			scale = null
			interceptorDispatcher = null
			fetcherDispatcher = null
			decoderDispatcher = null
			transformationDispatcher = null
			transitionFactory = null
			precision = null
			bitmapConfig = null
			allowHardware = null
			allowRgb565 = null
			premultipliedAlpha = true
			allowConversionToBitmap = true
			memoryCachePolicy = null
			diskCachePolicy = null
			networkCachePolicy = null
			placeholderMemoryCacheKey = null
			placeholderResId = null
			placeholderDrawable = null
			errorResId = null
			errorDrawable = null
			fallbackResId = null
			fallbackDrawable = null
			resolvedLifecycle = null
			resolvedSizeResolver = null
			resolvedScale = null
		}

		@JvmOverloads
		constructor(request: ImageRequest, context: Context = request.context) {
			this.context = context
			defaults = request.defaults
			data = request.data
			target = request.target
			listener = request.listener
			memoryCacheKey = request.memoryCacheKey
			diskCacheKey = request.diskCacheKey
			if (VERSION.SDK_INT >= 26) colorSpace = request.colorSpace
			fetcherFactory = request.fetcherFactory
			decoderFactory = request.decoderFactory
			transformations = request.transformations
			headers = request.headers.newBuilder()
			parameters = request.parameters.newBuilder()
			lifecycle = request.defined.lifecycle
			sizeResolver = request.defined.sizeResolver
			scale = request.defined.scale
			interceptorDispatcher = request.defined.interceptorDispatcher
			fetcherDispatcher = request.defined.fetcherDispatcher
			decoderDispatcher = request.defined.decoderDispatcher
			transformationDispatcher = request.defined.transformationDispatcher
			transitionFactory = request.defined.transitionFactory
			precision = request.defined.precision
			bitmapConfig = request.defined.bitmapConfig
			allowHardware = request.defined.allowHardware
			allowRgb565 = request.defined.allowRgb565
			premultipliedAlpha = request.premultipliedAlpha
			allowConversionToBitmap = request.allowConversionToBitmap
			memoryCachePolicy = request.defined.memoryCachePolicy
			diskCachePolicy = request.defined.diskCachePolicy
			networkCachePolicy = request.defined.networkCachePolicy
			placeholderMemoryCacheKey = request.placeholderMemoryCacheKey
			placeholderResId = request.placeholderResId
			placeholderDrawable = request.placeholderDrawable
			errorResId = request.errorResId
			errorDrawable = request.errorDrawable
			fallbackResId = request.fallbackResId
			fallbackDrawable = request.fallbackDrawable

			// If the context changes, recompute the resolved values.
			if (request.context === context) {
				resolvedLifecycle = request.lifecycle
				resolvedSizeResolver = request.sizeResolver
				resolvedScale = request.scale
			} else {
				resolvedLifecycle = null
				resolvedSizeResolver = null
				resolvedScale = null
			}
		}

		/**
		 * 로드할 데이터를 설정합니다.
		 *
		 * 기본적으로 지원되는 데이터 유형은 다음과 같습니다.
		 * - [String] ([Uri]에 매핑됨)
		 * - [Uri] ("android.resource", "content", "file", "http" 및 "https" 체계만 해당)
		 * - [HttpUrl]
		 * - [File]
		 * - [DrawableRes]
		 * - [Drawable]
		 * - [Bitmap]
		 * - [ByteBuffer]
		 */
		fun data(data: Any?) = apply {
			this.data = data
		}

		/**
		 * 이 요청에 대한 메모리 캐시 키를 설정합니다.
		 *
		 * null이거나 설정되지 않은 경우 [ImageLoader]는 메모리 캐시 키를 계산합니다.
		 */
		fun memoryCacheKey(key: String?) = memoryCacheKey(key?.let { MemoryCache.Key(it) })

		/**
		 * 이 요청에 대한 메모리 캐시 키를 설정합니다.
		 *
		 * null이거나 설정되지 않은 경우 [ImageLoader]는 메모리 캐시 키를 계산합니다.
		 */
		fun memoryCacheKey(key: MemoryCache.Key?) = apply {
			this.memoryCacheKey = key
		}

		/**
		 * 이 요청에 대한 디스크 캐시 키를 설정합니다.
		 *
		 * null이거나 설정되지 않은 경우 [ImageLoader]는 디스크 캐시 키를 계산합니다.
		 */
		fun diskCacheKey(key: String?) = apply {
			this.diskCacheKey = key
		}

		/**
		 * [Listener]를 생성하고 설정할 수 있는 편리한 기능입니다.
		 */
		inline fun listener(
			crossinline onStart: (request: ImageRequest) -> Unit = {},
			crossinline onCancel: (request: ImageRequest) -> Unit = {},
			crossinline onError: (request: ImageRequest, result: ErrorResult) -> Unit = { _, _ -> },
			crossinline onSuccess: (request: ImageRequest, result: SuccessResult) -> Unit = { _, _ -> }
		) = listener(object : Listener {
			override fun onStart(request: ImageRequest) = onStart(request)
			override fun onCancel(request: ImageRequest) = onCancel(request)
			override fun onError(request: ImageRequest, result: ErrorResult) = onError(request, result)
			override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess(request, result)
		})

		/**
		 * [Listener]를 설정합니다.
		 */
		fun listener(listener: Listener?) = apply {
			this.listener = listener
		}

		/**
		 * @see ImageLoader.Builder.dispatcher
		 */
		fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
			this.fetcherDispatcher = dispatcher
			this.decoderDispatcher = dispatcher
			this.transformationDispatcher = dispatcher
		}

		/**
		 * @see ImageLoader.Builder.fetcherDispatcher
		 */
		fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
			this.fetcherDispatcher = dispatcher
		}

		/**
		 * @see ImageLoader.Builder.decoderDispatcher
		 */
		fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
			this.decoderDispatcher = dispatcher
		}

		/**
		 * @see ImageLoader.Builder.transformationDispatcher
		 */
		fun transformationDispatcher(dispatcher: CoroutineDispatcher) = apply {
			this.transformationDispatcher = dispatcher
		}

		/**
		 * 이 요청에 적용할 [Transformation] 목록을 설정합니다.
		 */
		fun transformations(vararg transformations: Transformation) =
			transformations(transformations.toList())

		/**
		 * 이 요청에 적용할 [Transformation] 목록을 설정합니다.
		 */
		fun transformations(transformations: List<Transformation>) = apply {
			this.transformations = transformations.toList()
		}

		/**
		 * @see ImageLoader.Builder.bitmapConfig
		 */
		fun bitmapConfig(config: Bitmap.Config) = apply {
			this.bitmapConfig = config
		}

		/**
		 * 원하는 [ColorSpace]를 설정합니다.
		 *
		 * 이것은 보장되지 않으며 상황에 따라 다른 색 공간이 사용될 수 있습니다.
		 */
		@RequiresApi(26)
		fun colorSpace(colorSpace: ColorSpace) = apply {
			this.colorSpace = colorSpace
		}

		/**
		 * 요청한 너비/높이를 설정합니다.
		 */
		fun size(@Px size: Int) = size(size, size)

		/**
		 * 요청한 너비/높이를 설정합니다.
		 */
		fun size(@Px width: Int, @Px height: Int) = size(PixelSize(width, height))

		/**
		 * 요청한 너비/높이를 설정합니다.
		 */
		fun size(size: Size) = size(SizeResolver(size))

		/**
		 * Set the [SizeResolver] to resolve the requested width/height.
		 */
		fun size(resolver: SizeResolver) = apply {
			this.sizeResolver = resolver
			resetResolvedValues()
		}

		/**
		 * 이미지를 제공된 크기에 맞추거나 채우는 데 사용할 크기 조정 알고리즘을 설정합니다.
		 * [sizeResolver] 기준.
		 *
		 * 참고: [scale]이 설정되지 않은 경우 [ImageView] 대상에 대해 자동으로 계산됩니다.
		 */
		fun scale(scale: Scale) = apply {
			this.scale = scale
		}

		/**
		 * 로드된 이미지의 크기에 대한 정밀도를 설정합니다.
		 *
		 * 기본값은 [Precision.AUTOMATIC]이며, [allowInexactSize]의 논리를 사용하여 출력 이미지의 치수가 입력 [size] 및 [size]와 정확히 일치해야 하는지 여부를 결정합니다.
		 *
		 * 참고: [size]가 [OriginalSize]인 경우 반환된 이미지의 크기는 항상 이미지의 원본 크기보다 크거나 같습니다.
		 *
		 * @see Precision
		 */
		fun precision(precision: Precision) = apply {
			this.precision = precision
		}

		/**
		 * [factory]를 사용하여 이미지 데이터 가져오기를 처리합니다.
		 *
		 * 이것이 null이거나 설정되지 않은 경우 [ImageLoader]는 [ComponentRegistry]에서 적용 가능한 페처를 찾습니다.
		 */
		inline fun <reified T : Any> fetcherFactory(factory: Fetcher.Factory<T>) =
			fetcherFactory(factory, T::class.java)

		/**
		 * [factory]를 사용하여 이미지 데이터 가져오기를 처리합니다.
		 *
		 * 이것이 null이거나 설정되지 않은 경우 [ImageLoader]는 [ComponentRegistry]에서 해당 페처를 찾습니다.
		 */
		fun <T : Any> fetcherFactory(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
			this.fetcherFactory = factory to type
		}

		/**
		 * [factory]를 사용하여 이미지 데이터 디코딩을 처리합니다.
		 *
		 * 이것이 null이거나 설정되지 않은 경우 [ImageLoader]는 [ComponentRegistry]에서 해당 디코더를 찾습니다.
		 */
		fun decoderFactory(factory: DecoderNew.Factory) = apply {
			this.decoderFactory = factory
		}

		/**
		 * 결과 드로어블을 비트맵으로 변환하여 [transformations]을 적용할 수 있습니다.
		 *
		 * false이고 결과 드로어블이 [BitmapDrawable]이 아닌 경우 모든 [transformations]이 무시됩니다.
		 */
		fun allowConversionToBitmap(enable: Boolean) = apply {
			this.allowConversionToBitmap = enable
		}

		/**
		 * @see ImageLoader.Builder.allowHardware
		 */
		fun allowHardware(enable: Boolean) = apply {
			this.allowHardware = enable
		}

		/**
		 * @see ImageLoader.Builder.allowRgb565
		 */
		fun allowRgb565(enable: Boolean) = apply {
			this.allowRgb565 = enable
		}

		/**
		 * 디코딩된 이미지의 색상(RGB) 채널을 알파 채널로 미리 곱하는 것을 활성화/비활성화합니다.
		 *
		 * 기본 동작은 사전 곱셈을 활성화하는 것이지만 일부 환경에서는 소스 픽셀을 수정하지 않은 상태로 두려면 이 기능을 비활성화해야 할 수 있습니다.
		 */
		fun premultipliedAlpha(enable: Boolean) = apply {
			this.premultipliedAlpha = enable
		}

		/**
		 * 메모리 캐시에서 읽기/쓰기를 활성화/비활성화합니다.
		 */
		fun memoryCachePolicy(policy: CachePolicy) = apply {
			this.memoryCachePolicy = policy
		}

		/**
		 * 디스크 캐시에서 읽기/쓰기를 활성화/비활성화합니다.
		 */
		fun diskCachePolicy(policy: CachePolicy) = apply {
			this.diskCachePolicy = policy
		}

		/**
		 * 네트워크에서 읽기를 활성화/비활성화합니다.
		 *
		 * 참고: 쓰기를 비활성화해도 효과가 없습니다.
		 */
		fun networkCachePolicy(policy: CachePolicy) = apply {
			this.networkCachePolicy = policy
		}

		/**
		 * 이 요청에 의해 수행되는 모든 네트워크 작업에 대해 [Headers]를 설정합니다.
		 */
		fun headers(headers: Headers) = apply {
			this.headers = headers.newBuilder()
		}

		/**
		 * 이 요청에 의해 수행되는 모든 네트워크 작업에 대한 헤더를 추가합니다.
		 *
		 * @see Headers.Builder.add
		 */
		fun addHeader(name: String, value: String) = apply {
			this.headers = (this.headers ?: Headers.Builder()).add(name, value)
		}

		/**
		 * 이 요청에 의해 수행되는 모든 네트워크 작업에 대한 헤더를 설정합니다.
		 *
		 * @see Headers.Builder.set
		 */
		fun setHeader(name: String, value: String) = apply {
			this.headers = (this.headers ?: Headers.Builder()).set(name, value)
		}

		/**
		 * [name] 키가 있는 모든 네트워크 헤더를 제거합니다.
		 */
		fun removeHeader(name: String) = apply {
			this.headers = this.headers?.removeAll(name)
		}

		/**
		 * 이 요청에 대한 매개변수를 설정합니다.
		 */
		fun parameters(parameters: Parameters) = apply {
			this.parameters = parameters.newBuilder()
		}

		/**
		 * 이 요청에 대한 매개변수를 설정하십시오.
		 *
		 * @see Parameters.Builder.set
		 */
		@JvmOverloads
		fun setParameter(key: String, value: Any?, cacheKey: String? = value?.toString()) = apply {
			this.parameters = (this.parameters ?: Parameters.Builder()).apply { set(key, value, cacheKey) }
		}

		/**
		 * 이 요청에서 매개변수를 제거하십시오.
		 *
		 * @see Parameters.Builder.remove
		 */
		fun removeParameter(key: String) = apply {
			this.parameters?.remove(key)
		}

		/**
		 * 값이 자리 표시자 드로어블로 사용될 메모리 캐시 [key]를 설정합니다.
		 *
		 * 메모리 캐시에 [key] 값이 없으면 [placeholder]로 대체합니다.
		 */
		fun placeholderMemoryCacheKey(key: String?) =
			placeholderMemoryCacheKey(key?.let { MemoryCache.Key(it) })

		/**
		 * 값이 자리 표시자 드로어블로 사용될 메모리 캐시 [key]를 설정합니다.
		 *
		 * 메모리 캐시에 [key] 값이 없으면 [placeholder]로 대체합니다.
		 */
		fun placeholderMemoryCacheKey(key: MemoryCache.Key?) = apply {
			this.placeholderMemoryCacheKey = key
		}

		/**
		 * 요청이 시작될 때 사용할 자리 표시자 드로어블을 설정합니다.
		 */
		fun placeholder(@DrawableRes drawableResId: Int) = apply {
			this.placeholderResId = drawableResId
			this.placeholderDrawable = null
		}

		/**
		 * 요청이 시작될 때 사용할 자리 표시자 드로어블을 설정합니다.
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

		/**
		 * [data]가 null인 경우 사용할 대체 드로어블을 설정합니다.
		 */
		fun fallback(@DrawableRes drawableResId: Int) = apply {
			this.fallbackResId = drawableResId
			this.fallbackDrawable = null
		}

		/**
		 * [data]가 null인 경우 사용할 대체 드로어블을 설정합니다.
		 */
		fun fallback(drawable: Drawable?) = apply {
			this.fallbackDrawable = drawable
			this.fallbackResId = 0
		}

		/**
		 * [imageView]를 [Target]으로 설정하는 편리한 기능입니다.
		 */
		fun target(imageView: ImageView) = target(com.ys.coil.target.ImageViewTarget(imageView))

		/**
		 * [Target]을 생성하고 설정할 수 있는 편리한 기능입니다.
		 */
		inline fun target(
			crossinline onStart: (placeholder: Drawable?) -> Unit = {},
			crossinline onError: (error: Drawable?) -> Unit = {},
			crossinline onSuccess: (result: Drawable) -> Unit = {}
		) = target(object : Target {
			override fun onStart(placeholder: Drawable?) = onStart(placeholder)
			override fun onError(error: Drawable?) = onError(error)
			override fun onSuccess(result: Drawable) = onSuccess(result)
		})

		/**
		 * Set the [Target].
		 */
		fun target(target: Target?) = apply {
			this.target = target
			resetResolvedValues()
		}

		/**
		 * @see ImageLoader.Builder.crossfade
		 */
		fun crossfade(enable: Boolean) =
			crossfade(if (enable) com.ys.coil.drawable.CrossfadeDrawable.DEFAULT_DURATION else 0)

		/**
		 * @see ImageLoader.Builder.crossfade
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
		 * @see ImageLoader.Builder.transitionFactory
		 */
		fun transitionFactory(transition: Transition.Factory) = apply {
			this.transitionFactory = transition
		}

		/**
		 * 이 요청에 대해 [Lifecycle]을 설정합니다.
		 */
		fun lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

		/**
		 * 이 요청에 대해 [Lifecycle]을 설정합니다.
		 *
		 * 수명 주기가 [Lifecycle.State.STARTED] 이상이 아닌 동안 요청이 대기열에 있습니다.
		 * 수명 주기가 [Lifecycle.State.DESTROYED]에 도달하면 요청이 취소됩니다.
		 *
		 * null이거나 설정되지 않은 경우 [ImageLoader]는 [context]를 통해 이 요청의 수명 주기를 찾으려고 시도합니다.
		 */
		fun lifecycle(lifecycle: Lifecycle?) = apply {
			this.lifecycle = lifecycle
		}

		/**
		 * 설정되지 않은 요청 값의 기본값을 설정합니다.
		 */
		fun defaults(defaults: DefaultRequestOptions) = apply {
			this.defaults = defaults
			resetResolvedScale()
		}

		/**
		 * Create a new [ImageRequest].
		 */
		fun build(): ImageRequest {
			return ImageRequest(
				context = context,
				data = data ?: NullRequestData,
				target = target,
				listener = listener,
				memoryCacheKey = memoryCacheKey,
				diskCacheKey = diskCacheKey,
				colorSpace = colorSpace,
				fetcherFactory = fetcherFactory,
				decoderFactory = decoderFactory,
				transformations = transformations,
				headers = headers?.build().orEmpty(),
				parameters = parameters?.build().orEmpty(),
				lifecycle = lifecycle ?: resolvedLifecycle ?: resolveLifecycle(),
				sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
				scale = scale ?: resolvedScale ?: resolveScale(),
				interceptorDispatcher = interceptorDispatcher ?: defaults.interceptorDispatcher,
				fetcherDispatcher = fetcherDispatcher ?: defaults.fetcherDispatcher,
				decoderDispatcher = decoderDispatcher ?: defaults.decoderDispatcher,
				transformationDispatcher = transformationDispatcher ?: defaults.transformationDispatcher,
				transitionFactory = transitionFactory ?: defaults.transitionFactory,
				precision = precision ?: defaults.precision,
				bitmapConfig = bitmapConfig ?: defaults.bitmapConfig,
				allowConversionToBitmap = allowConversionToBitmap,
				allowHardware = allowHardware ?: defaults.allowHardware,
				allowRgb565 = allowRgb565 ?: defaults.allowRgb565,
				premultipliedAlpha = premultipliedAlpha,
				memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
				diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
				networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
				placeholderMemoryCacheKey = placeholderMemoryCacheKey,
				placeholderResId = placeholderResId,
				placeholderDrawable = placeholderDrawable,
				errorResId = errorResId,
				errorDrawable = errorDrawable,
				fallbackResId = fallbackResId,
				fallbackDrawable = fallbackDrawable,
				defined = DefinedRequestOptions(lifecycle, sizeResolver, scale, interceptorDispatcher,
					fetcherDispatcher, decoderDispatcher, transformationDispatcher, transitionFactory,
					precision, bitmapConfig, allowHardware, allowRgb565, memoryCachePolicy,
					diskCachePolicy, networkCachePolicy),
				defaults = defaults,
			)
		}

		/** [build]가 호출될 때 이러한 값이 다시 계산되는지 확인합니다. */
		private fun resetResolvedValues() {
			resolvedLifecycle = null
			resolvedSizeResolver = null
			resolvedScale = null
		}

		/** [build]가 호출될 때 스케일이 다시 계산되는지 확인하십시오. */
		private fun resetResolvedScale() {
			resolvedScale = null
		}

		private fun resolveLifecycle(): Lifecycle {
			val target = target
			val context = if (target is ViewTarget<*>) target.view.context else context
			return context.getLifecycle() ?: GlobalLifecycle
		}

		private fun resolveSizeResolver(): SizeResolver {
			val target = target
			if (target is ViewTarget<*>) {
				val view = target.view
				// CENTER and MATRIX scale types should be decoded at the image's original size.
				if (view !is ImageView || view.scaleType.let { it != CENTER && it != MATRIX }) {
					return ViewSizeResolver(view)
				}
			}
			return SizeResolver(OriginalSize)
		}

		private fun resolveScale(): Scale {
			val sizeResolver = sizeResolver
			if (sizeResolver is ViewSizeResolver<*>) {
				val view = sizeResolver.view
				if (view is ImageView) return view.scale
			}

			val target = target
			if (target is ViewTarget<*>) {
				val view = target.view
				if (view is ImageView) return view.scale
			}

			return Scale.FIT
		}

		@Deprecated(
			message = "Migrate to 'fetcherFactory'.",
			replaceWith = ReplaceWith("fetcherFactory<Any> { _, _, _ -> fetcher }"),
			level = DeprecationLevel.ERROR // Temporary migration aid.
		)
		fun fetcher(fetcher: Fetcher): Builder = unsupported()

		@Deprecated(
			message = "Migrate to 'decoderFactory'.",
			replaceWith = ReplaceWith("decoderFactory { _, _, _ -> decoder }"),
			level = DeprecationLevel.ERROR // Temporary migration aid.
		)
		fun decoder(decoder: DecoderNew): Builder = unsupported()

		@Deprecated(
			message = "Migrate to 'transitionFactory'.",
			replaceWith = ReplaceWith("transitionFactory { _, _ -> transition }"),
			level = DeprecationLevel.ERROR // Temporary migration aid.
		)
		fun transition(transition: Transition): Builder = unsupported()
	}
}