package com.ys.coil.interceptor

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ys.coil.ComponentRegistry
import com.ys.coil.EventListener
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.DataSource.MEMORY_CACHE
import com.ys.coil.decode.DecodeResult
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.decode.FileImageSource
import com.ys.coil.fetch.DrawableResult
import com.ys.coil.fetch.FetchResult
import com.ys.coil.fetch.SourceResult
import com.ys.coil.interceptor.Interceptor.Chain
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.request.Options
import com.ys.coil.request.RequestService
import com.ys.coil.request.SuccessResult
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import com.ys.coil.transform.Transformation
import com.ys.coil.util.DrawableUtils
import com.ys.coil.util.Logger
import com.ys.coil.util.VALID_TRANSFORMATION_CONFIGS
import com.ys.coil.util.addFirst
import com.ys.coil.util.allowInexactSize
import com.ys.coil.util.closeQuietly
import com.ys.coil.util.foldIndices
import com.ys.coil.util.forEachIndices
import com.ys.coil.util.log
import com.ys.coil.util.safeConfig
import com.ys.coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** [ImageRequest]를 실행하는 체인의 마지막 인터셉터. */
internal class EngineInterceptor(
	private val imageLoader: ImageLoader,
	private val requestService: RequestService,
	private val logger: Logger?
) : Interceptor {

	override suspend fun interceptor(chain: Chain): ImageResult {
		try {
			val request = chain.request
			val context = request.context
			val data = request.data
			val size = chain.size
			val eventListener = chain.eventListener
			val options = requestService.options(request, size)

			// 모든 데이터 매핑을 수행합니다.
			eventListener.mapStart(request, data)
			val mappedData = imageLoader.components.map(data, options)
			eventListener.mapEnd(request, mappedData)

			// 메모리 캐시를 확인하십시오.
			val memoryCacheKey = getMemoryCacheKey(request, mappedData, options, eventListener)
			val memoryCacheValue = memoryCacheKey?.let { getMemoryCacheValue(request, it) }

			// 빠른 경로: 메모리 캐시에서 값을 반환합니다.
			if (memoryCacheValue != null &&
				isCachedValueValid(memoryCacheKey, memoryCacheValue, request, size)) {
				return SuccessResult(
					drawable = memoryCacheValue.bitmap.toDrawable(context),
					request = request,
					dataSource = MEMORY_CACHE,
					memoryCacheKey = memoryCacheKey,
					diskCacheKey = memoryCacheValue.diskCacheKey,
					isSampled = memoryCacheValue.isSampled,
					isPlaceholderCached = chain.isPlaceholderCached,
				)
			}

			// 느린 경로: 이미지를 가져오고, 디코딩하고, 변환하고, 캐시합니다.
			return withContext(request.fetcherDispatcher) {
				// 이미지를 가져와 디코딩합니다.
				val result = execute(request, mappedData, options, eventListener)

				// 결과를 메모리 캐시에 저장합니다.
				val isMemoryCached = setMemoryCacheValue(memoryCacheKey, request, result)

				// 결과를 반환합니다.
				SuccessResult(
					drawable = result.drawable,
					request = request,
					dataSource = result.dataSource,
					memoryCacheKey = memoryCacheKey.takeIf { isMemoryCached },
					diskCacheKey = result.diskCacheKey,
					isSampled = result.isSampled,
					isPlaceholderCached = chain.isPlaceholderCached,
				)
			}
		} catch (throwable: Throwable) {
			if (throwable is CancellationException) {
				throw throwable
			} else {
				return requestService.errorResult(chain.request, throwable)
			}
		}
	}

	/** 이 요청에 대한 메모리 캐시 키를 가져옵니다. */
	@VisibleForTesting
	internal fun getMemoryCacheKey(
		request: ImageRequest,
		mappedData: Any,
		options: Options,
		eventListener: EventListener
	): MemoryCache.Key? {
		// 빠른 경로: 명시적 메모리 캐시 키가 설정되었습니다.
		request.memoryCacheKey?.let { return it }

		// 느린 경로: 새 메모리 캐시 키를 만듭니다.
		eventListener.keyStart(request, mappedData)
		val base = imageLoader.components.key(mappedData, options)
		eventListener.keyEnd(request, base)
		if (base == null) return null

		val extras = mutableMapOf<String, String>()
		extras.putAll(request.parameters.cacheKeys())
		if (request.transformations.isNotEmpty()) {
			val transformations = StringBuilder()
			request.transformations.forEachIndices {
				transformations.append(it.cacheKey).append(TRANSFORMATIONS_DELIMITER)
			}
			extras[MEMORY_CACHE_KEY_TRANSFORMATIONS] = transformations.toString()

			val size = options.size
			if (size is PixelSize) {
				extras[MEMORY_CACHE_KEY_WIDTH] = size.width.toString()
				extras[MEMORY_CACHE_KEY_HEIGHT] = size.height.toString()
			}
		}
		return MemoryCache.Key(base, extras)
	}

	/** [cacheValue]가 [request]을 만족하면 'true'를 반환합니다. */
	@VisibleForTesting
	internal fun isCachedValueValid(
		cacheKey: MemoryCache.Key,
		cacheValue: MemoryCache.Value,
		request: ImageRequest,
		size: Size
	): Boolean {
		// 캐시된 비트맵의 크기가 요청에 유효한지 확인하십시오.
		if (!isSizeValid(cacheKey, cacheValue, request, size)) {
			return false
		}

		// 요청에서 허용하지 않는 경우 하드웨어 비트맵을 반환하지 않도록 합니다.
		if (!requestService.isConfigValidForHardware(request, cacheValue.bitmap.safeConfig)) {
			logger?.log(TAG, Log.DEBUG) {
				"${request.data}: Cached bitmap is hardware-backed, which is incompatible with the request."
			}
			return false
		}

		// 그렇지 않으면 캐시된 드로어블이 유효하며 요청을 단락시킬 수 있습니다.
		return true
	}

	/** [cacheValue]의 크기가 [request]을 만족하면 'true'를 반환합니다. */
	private fun isSizeValid(
		cacheKey: MemoryCache.Key,
		cacheValue: MemoryCache.Value,
		request: ImageRequest,
		size: Size
	): Boolean {
		when (size) {
			is OriginalSize -> {
				if (cacheValue.isSampled) {
					logger?.log(TAG, Log.DEBUG) {
						"${request.data}: Requested original size, but cached image is sampled."
					}
					return false
				}
			}
			is PixelSize -> {
				var cachedWidth = cacheKey.extras[MEMORY_CACHE_KEY_WIDTH]?.toInt()
				var cachedHeight = cacheKey.extras[MEMORY_CACHE_KEY_HEIGHT]?.toInt()
				if (cachedWidth == null || cachedHeight == null) {
					val bitmap = cacheValue.bitmap
					cachedWidth = bitmap.width
					cachedHeight = bitmap.height
				}

				val multiple = DecodeUtils.computeSizeMultiplier(
					srcWidth = cachedWidth,
					srcHeight = cachedHeight,
					dstWidth = size.width,
					dstHeight = size.height,
					scale = request.scale
				)

				// 크기가 어느 한 차원에서 최대 1픽셀 떨어져 있는지 크기 검사를 단락시킵니다.
				// 이것은 다운샘플링이 반올림으로 인해 최대 1픽셀 크기의 이미지를 생성할 수 있다는 사실을 설명합니다.
				val allowInexactSize = request.allowInexactSize
				if (allowInexactSize) {
					val downsampleMultiplier = multiple.coerceAtMost(1.0)
					if (abs(size.width - (downsampleMultiplier * cachedWidth)) <= 1 ||
						abs(size.height - (downsampleMultiplier * cachedHeight)) <= 1) {
						return true
					}
				} else {
					if (abs(size.width - cachedWidth) <= 1 && abs(size.height - cachedHeight) <= 1) {
						return true
					}
				}

				if (multiple != 1.0 && !allowInexactSize) {
					logger?.log(TAG, Log.DEBUG) {
						"${request.data}: Cached image's request size " +
							"($cachedWidth, $cachedHeight) does not exactly match the requested size " +
							"(${size.width}, ${size.height}, ${request.scale})."
					}
					return false
				}
				if (multiple > 1.0 && cacheValue.isSampled) {
					logger?.log(TAG, Log.DEBUG) {
						"${request.data}: Cached image's request size " +
							"($cachedWidth, $cachedHeight) is smaller than the requested size " +
							"(${size.width}, ${size.height}, ${request.scale})."
					}
					return false
				}
			}
		}

		return true
	}

	/** [Fetcher]를 실행하고 모든 데이터를 [Drawable]로 디코딩하고 [Transformation]을 적용합니다. */
	private suspend inline fun execute(
		request: ImageRequest,
		mappedData: Any,
		requestOptions: Options,
		eventListener: EventListener
	): ExecuteResult {
		var options = requestOptions
		var components = imageLoader.components
		var fetchResult: FetchResult? = null
		val executeResult = try {
			if (!requestService.allowHardwareWorkerThread(options)) {
				options = options.copy(config = Bitmap.Config.ARGB_8888)
			}
			if (request.fetcherFactory != null || request.decoderFactory != null) {
				components = components.newBuilder()
					.addFirst(request.fetcherFactory)
					.addFirst(request.decoderFactory)
					.build()
			}

			// 데이터를 가져옵니다.
			fetchResult = fetch(components, request, mappedData, options, eventListener)

			// 데이터를 디코딩합니다.
			when (fetchResult) {
				is SourceResult -> withContext(request.decoderDispatcher) {
					decode(fetchResult, components, request, mappedData, options, eventListener)
				}
				is DrawableResult -> {
					ExecuteResult(
						drawable = fetchResult.drawable,
						isSampled = fetchResult.isSampled,
						dataSource = fetchResult.dataSource,
						diskCacheKey = null // This result has no file source.
					)
				}
			}
		} finally {
			// 가져오기 결과의 소스가 항상 닫혀 있는지 확인합니다.
			(fetchResult as? SourceResult)?.source?.closeQuietly()
		}

		// 변형을 적용하고 그릴 준비를 합니다.
		val finalResult = transform(executeResult, request, options, eventListener)
		(finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
		return finalResult
	}

	private suspend inline fun fetch(
		components: ComponentRegistry,
		request: ImageRequest,
		mappedData: Any,
		options: Options,
		eventListener: EventListener
	): FetchResult {
		val fetchResult: FetchResult
		var searchIndex = 0
		while (true) {
			val pair = components.newFetcher(mappedData, options, imageLoader, searchIndex)
			checkNotNull(pair) { "Unable to create a fetcher that supports: $mappedData" }
			val fetcher = pair.first
			searchIndex = pair.second + 1

			eventListener.fetchStart(request, fetcher, options)
			val result = fetcher.fetch()
			try {
				eventListener.fetchEnd(request, fetcher, options, result)
			} catch (throwable: Throwable) {
				// 결과를 반환하기 전에 예외가 발생하면 소스가 닫혀 있는지 확인합니다.
				(result as? SourceResult)?.source?.closeQuietly()
				throw throwable
			}

			if (result != null) {
				fetchResult = result
				break
			}
		}
		return fetchResult
	}

	private suspend inline fun decode(
		fetchResult: SourceResult,
		components: ComponentRegistry,
		request: ImageRequest,
		mappedData: Any,
		options: Options,
		eventListener: EventListener
	): ExecuteResult {
		val decodeResult: DecodeResult
		var searchIndex = 0
		while (true) {
			val pair = components.newDecoder(fetchResult, options, imageLoader, searchIndex)
			checkNotNull(pair) { "Unable to create a decoder that supports: $mappedData" }
			val decoder = pair.first
			searchIndex = pair.second + 1

			eventListener.decodeStart(request, decoder, options)
			val result = decoder.decode()
			eventListener.decodeEnd(request, decoder, options, result)

			if (result != null) {
				decodeResult = result
				break
			}
		}

		// 가져오기 및 디코딩 작업 결과를 결합합니다.
		return ExecuteResult(
			drawable = decodeResult.drawable,
			isSampled = decodeResult.isSampled,
			dataSource = fetchResult.dataSource,
			diskCacheKey = (fetchResult.source as? FileImageSource)?.diskCacheKey
		)
	}

	/** [Transformation]을 적용하고 업데이트된 [ExecuteResult]를 반환합니다. */
	@VisibleForTesting
	internal suspend inline fun transform(
		result: ExecuteResult,
		request: ImageRequest,
		options: Options,
		eventListener: EventListener
	): ExecuteResult {
		val transformations = request.transformations
		if (transformations.isEmpty()) return result

		// 비트맵으로의 변환이 비활성화되어 있으므로 변환을 건너뜁니다.
		if (result.drawable !is BitmapDrawable && !request.allowConversionToBitmap) {
			logger?.log(TAG, Log.INFO) {
				val type = result.drawable::class.java.canonicalName
				"allowConversionToBitmap=false, skipping transformations for type $type"
			}
			return result
		}

		// 변환을 적용합니다.
		return withContext(request.transformationDispatcher) {
			val input = convertDrawableToBitmap(result.drawable, options, transformations)
			eventListener.transformStart(request, input)
			val output = transformations.foldIndices(input) { bitmap, transformation ->
				transformation.transform(bitmap, options.size).also { ensureActive() }
			}
			eventListener.transformEnd(request, output)
			result.copy(drawable = output.toDrawable(request.context))
		}
	}

	/** 이 요청에 대한 메모리 캐시 값을 가져옵니다. */
	private fun getMemoryCacheValue(
		request: ImageRequest,
		memoryCacheKey: MemoryCache.Key
	): MemoryCache.Value? {
		return if (request.memoryCachePolicy.readEnabled) {
			imageLoader.memoryCache?.get(memoryCacheKey)
		} else {
			null
		}
	}

	/** 메모리 캐시에 [drawable]을 씁니다. 캐시에 추가된 경우 'true'를 반환합니다. */
	private fun setMemoryCacheValue(
		key: MemoryCache.Key?,
		request: ImageRequest,
		result: ExecuteResult
	): Boolean {
		if (!request.memoryCachePolicy.writeEnabled) {
			return false
		}

		if (key != null) {
			val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
			if (bitmap != null) {
				val extras = mutableMapOf<String, Any>()
				extras[EXTRA_IS_SAMPLED] = result.isSampled
				result.diskCacheKey?.let { extras[EXTRA_DISK_CACHE_KEY] = it }
				imageLoader.memoryCache?.let { it[key] = MemoryCache.Value(bitmap, extras) }
				return true
			}
		}

		return false
	}

	/** [drawable]을 [Bitmap]으로 변환합니다. */
	private fun convertDrawableToBitmap(
		drawable: Drawable,
		options: Options,
		transformations: List<Transformation>
	) : Bitmap {
		if (drawable is BitmapDrawable) {
			var bitmap = drawable.bitmap
			val config = bitmap.safeConfig
			if (config !in VALID_TRANSFORMATION_CONFIGS) {
				logger?.log(TAG, Log.INFO) {
					"Converting bitmap with config $config to apply transformations: $transformations"
				}
				bitmap = DrawableUtils.convertToBitmap(drawable, options.config, options.size,
					options.scale, options.allowInexactSize)
			}

			return bitmap
		}

		logger?.log(TAG, Log.INFO) {
			val type = drawable::class.java.canonicalName
			"Converting drawable of type $type to apply transformations: $transformations"
		}

		return DrawableUtils.convertToBitmap(
			drawable = drawable,
			config = options.config,
			size = options.size,
			scale = options.scale,
			allowInexactSize = options.allowInexactSize
		)
	}

	private val MemoryCache.Value.isSampled: Boolean
		get() = (extras[EXTRA_IS_SAMPLED] as? Boolean) ?: false

	private val MemoryCache.Value.diskCacheKey: String?
		get() = extras[EXTRA_DISK_CACHE_KEY] as? String

	private val Interceptor.Chain.isPlaceholderCached: Boolean
		get() = this is RealInterceptorChain && isPlaceholderCached

	private val Interceptor.Chain.eventListener: EventListener
		get() = if (this is RealInterceptorChain) eventListener else EventListener.NONE

	@VisibleForTesting
	internal class ExecuteResult(
		val drawable: Drawable,
		val isSampled: Boolean,
		val dataSource: DataSource,
		val diskCacheKey: String?
	) {
		fun copy(
			drawable: Drawable = this.drawable,
			isSampled: Boolean = this.isSampled,
			dataSource: DataSource = this.dataSource,
			diskCacheKey: String? = this.diskCacheKey
		) = ExecuteResult(drawable, isSampled, dataSource, diskCacheKey)
	}

	companion object {
		private const val TAG = "EngineInterceptor"
		@VisibleForTesting internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
		@VisibleForTesting internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_WIDTH = "coil#width"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_HEIGHT = "coil#height"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_TRANSFORMATIONS = "coil#transformations"
		@VisibleForTesting internal const val TRANSFORMATIONS_DELIMITER = '~'
	}
}