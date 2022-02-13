package com.ys.coil.fetch

import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.ImageSource
import com.ys.coil.disk.DiskCache
import com.ys.coil.network.CacheResponse
import com.ys.coil.network.CacheStrategy
import com.ys.coil.network.CacheStrategy.Companion.combineHeaders
import com.ys.coil.network.HttpException
import com.ys.coil.request.Options
import com.ys.coil.request.Parameters
import com.ys.coil.util.abortQuietly
import com.ys.coil.util.await
import com.ys.coil.util.closeQuietly
import com.ys.coil.util.dispatcher
import com.ys.coil.util.getMimeTypeFromUrl
import kotlinx.coroutines.MainCoroutineDispatcher
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.IOException
import okio.buffer
import okio.sink
import okio.source
import okio.use
import java.net.HttpURLConnection
import kotlin.coroutines.coroutineContext

class HttpUriFetcher(
	private val url: String,
	private val options: Options,
	private val callFactory: Lazy<Call.Factory>,
	private val diskCache: Lazy<DiskCache?>,
	private val respectCacheHeaders: Boolean
) : Fetcher {
	override suspend fun fetch(): FetchResult? {
		var snapshot = readFromDiskCache()
		try {
			// 빠른 경로: 네트워크 요청을 수행하지 않고 디스크 캐시에서 이미지를 가져옵니다.
			val cacheStrategy: CacheStrategy

			if (snapshot != null) {
				// 수동으로 추가되었을 가능성이 있으므로 항상 빈 메타데이터가 있는 캐시된 이미지를 반환합니다.
				if (snapshot.metadata.length() == 0L) {
					return SourceResult(
						source = snapshot.toImageSource(),
						mimeType = getMimeType(url, null),
						dataSource = DataSource.DISK
					)
				}

				// 적격한 경우 캐시에서 후보자를 반환합니다.
				if (respectCacheHeaders) {
					cacheStrategy = CacheStrategy.Factory(newRequest(), snapshot.toCacheResponse()).compute()
					if (cacheStrategy.networkRequest == null && cacheStrategy.cacheResponse != null) {
						return SourceResult(
							source = snapshot.toImageSource(),
							mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType()),
							dataSource = DataSource.DISK
						)
					}
				} else {
					// Skip checking the cache headers if the option is disabled.
					return SourceResult(
						source = snapshot.toImageSource(),
						mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType()),
						dataSource = DataSource.DISK
					)
				}
			} else {
				cacheStrategy = CacheStrategy.Factory(newRequest(), null).compute()
			}

			// 느린 경로: 네트워크에서 이미지를 가져옵니다.
			val request = cacheStrategy.networkRequest ?: newRequest()
			val response = executeNetworkRequest(request)
			val responseBody = checkNotNull(response.body) { "response body == null" }
			try {
				// 쓰기 후 디스크 캐시에서 응답을 읽습니다.
				snapshot = writeToDiskCache(snapshot, request, response, cacheStrategy.cacheResponse)
				if (snapshot != null) {
					return SourceResult(
						source = snapshot.toImageSource(),
						mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType()),
						dataSource = DataSource.NETWORK
					)
				}

				// 캐시에서 읽을 수 없거나 캐시에 쓸 수 없으면 응답 본문에서 직접 응답을 읽으십시오.
				return SourceResult(
					source = responseBody.toImageSource(),
					mimeType = getMimeType(url, responseBody.contentType()),
					dataSource = if (response.networkResponse != null) DataSource.NETWORK else DataSource.DISK
				)
			} catch (e: Exception) {
				responseBody.closeQuietly()
				throw e
			}
		} catch (e: Exception) {
			snapshot?.closeQuietly()
			throw e
		}
	}

	private fun readFromDiskCache(): DiskCache.Snapshot? {
		return if (options.diskCachePolicy.readEnabled) diskCache.value?.get(diskCacheKey) else null
	}

	private fun writeToDiskCache(
		snapshot: DiskCache.Snapshot?,
		request: Request,
		response: Response,
		cacheResponse: CacheResponse?
	): DiskCache.Snapshot? {
		// 이 응답을 캐시할 수 없는 경우 단락입니다.
		if (!isCacheable(request, response)) {
			snapshot?.closeQuietly()
			return null
		}

		val editor = if (snapshot != null) {
			snapshot.closeAndEdit()
		} else {
			diskCache.value?.edit(diskCacheKey)
		} ?: return null
		try {
			// 디스크 캐시에 대한 응답을 씁니다.
			if (response.code == HttpURLConnection.HTTP_NOT_MODIFIED && cacheResponse != null) {
				// 메타데이터만 업데이트하십시오.
				val combinedResponse = response.newBuilder()
					.headers(combineHeaders(CacheResponse(response).responseHeaders, response.headers))
					.build()
				editor.metadata.sink().buffer().use { CacheResponse(combinedResponse).writeTo(it) }
				response.body!!.closeQuietly()
			} else {
				// 메타데이터 및 이미지 데이터를 업데이트합니다.
				editor.metadata.sink().buffer().use { CacheResponse(response).writeTo(it) }
				response.body!!.source().use { editor.data.sink().use(it::readAll) }
			}
			return editor.commitAndGet()
		} catch (e: Exception) {
			editor.abortQuietly()
			throw e
		}
	}

	private fun newRequest(): Request {
		val request = Request.Builder()
			.url(url)
			.headers(options.headers)
			// 네트워크 요청에 사용자 지정 데이터 첨부를 지원합니다.
			.tag(Parameters::class.java, options.parameters)

		val diskRead = options.diskCachePolicy.readEnabled
		val networkRead = options.networkCachePolicy.readEnabled

		when {
			!networkRead && diskRead -> {
				request.cacheControl(CacheControl.FORCE_CACHE)
			}
			networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
				request.cacheControl(CacheControl.FORCE_NETWORK)
			} else {
				request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
			}
			!networkRead && !diskRead -> {
				// 이로 인해 요청이 504 Unsatisfiable Request와 함께 실패합니다.
				request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
			}
		}

		return request.build()
	}

	private suspend fun executeNetworkRequest(request: Request): Response {
		val response = if (coroutineContext.dispatcher is MainCoroutineDispatcher) {
			if (options.networkCachePolicy.readEnabled) {
				// 네트워킹 작업으로 인해 차단될 수 있는 기본 스레드에서 실행 요청을 방지합니다.
				throw NetworkOnMainThreadException()
			} else {
				callFactory.value.newCall(request).execute()
			}
		} else {
			// OkHttp의 디스패처 스레드 중 하나에서 요청을 일시 중단하고 대기열에 넣습니다.
			callFactory.value.newCall(request).await()
		}
		if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
			response.body?.closeQuietly()
			throw HttpException(response)
		}
		return response
	}

	/**
	 * 응답의 'content-type' 헤더를 구문 분석합니다.
	 *
	 * "text/plain"은 종종 기본/대체 MIME 유형으로 사용됩니다.
	 * 파일 확장자에서 더 나은 MIME 유형을 추측해 봅니다.
	 */
	@VisibleForTesting
	internal fun getMimeType(url: String, contentType: MediaType?): String? {
		val rawContentType = contentType?.toString()
		if (rawContentType == null || rawContentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
			MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)?.let { return it }
		}
		return rawContentType?.substringBefore(';')
	}

	private fun isCacheable(request: Request, response: Response): Boolean {
		return options.diskCachePolicy.writeEnabled &&
			(!respectCacheHeaders || CacheStrategy.isCacheable(request, response))
	}

	private fun DiskCache.Snapshot.toCacheResponse(): CacheResponse? {
		try {
			return metadata.source().buffer().use(::CacheResponse)
		} catch (_: IOException) {
			// 메타데이터를 구문 분석할 수 없으면 이 항목을 무시하십시오.
			return null
		}
	}

	private fun DiskCache.Snapshot.toImageSource(): ImageSource {
		return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
	}

	private fun ResponseBody.toImageSource(): ImageSource {
		return ImageSource(source = source(), context = options.context)
	}

	private val diskCacheKey get() = options.diskCacheKey ?: url

	class Factory(
		private val callFactory: Lazy<Call.Factory>,
		private val diskCache: Lazy<DiskCache?>,
		private val respectCacheHeaders: Boolean
	) : Fetcher.Factory<Uri> {

		override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
			if (!isApplicable(data)) return null
			return HttpUriFetcher(data.toString(), options, callFactory, diskCache, respectCacheHeaders)
		}

		private fun isApplicable(data: Uri): Boolean {
			return data.scheme == "http" || data.scheme == "https"
		}
	}

	companion object {
		private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
		private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE =
			CacheControl.Builder().noCache().noStore().build()
		private val CACHE_CONTROL_NO_NETWORK_NO_CACHE =
			CacheControl.Builder().noCache().onlyIfCached().build()
	}
}
