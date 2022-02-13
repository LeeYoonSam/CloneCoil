package com.ys.coil.network

import com.ys.coil.util.Time
import com.ys.coil.util.toNonNegativeInt
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.util.Date
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.max
import kotlin.math.min

/**
 * OkHttp의 `okhttp3.internal.cache.CacheStrategy`에서 수정되었습니다.
 */
internal class CacheStrategy private constructor(
	/** 네트워크에서 보낼 요청이거나 이 호출이 네트워크를 사용하지 않는 경우 null입니다. */
	val networkRequest: Request?,
	/** 반환 또는 유효성 검사를 위한 캐시된 응답 또는 이 호출이 캐시를 사용하지 않는 경우 null입니다. */
	val cacheResponse: CacheResponse?
) {
	class Factory(
		private val request: Request,
		private val cacheResponse: CacheResponse?
	) {
		/** 알려진 경우 캐시된 응답이 제공된 서버 시간입니다. */
		private var servedDate: Date? = null
		private var servedDateString: String? = null

		/** 알려진 경우 캐시된 응답의 마지막 수정 날짜입니다. */
		private var lastModified: Date? = null
		private var lastModifiedString: String? = null

		/**
		 * 알려진 경우 캐시된 응답의 만료 날짜입니다.
		 * 이 필드와 최대 연령을 모두 설정하면 최대 연령이 우선 적용됩니다.
		 */
		private var expires: Date? = null

		/** @see [Response.sentRequestAtMillis] */
		private var sentRequestMillis = 0L

		/** @see [Response.receivedResponseAtMillis] */
		private var receivedResponseMillis = 0L

		/** 캐시된 응답의 Etag입니다. */
		private var etag: String? = null

		/** 캐시된 응답의 기간입니다. */
		private var ageSeconds = -1

		init {
			if (cacheResponse != null) {
				sentRequestMillis = cacheResponse.sentRequestAtMillis
				receivedResponseMillis = cacheResponse.receivedResponseAtMillis
				val headers = cacheResponse.responseHeaders

				for (i in 0 until headers.size) {
					val name = headers.name(i)
					val value = headers.value(i)
					when {
						name.equals("Date", ignoreCase = true) -> {
							servedDate = headers.getDate("Date")
							servedDateString = value
						}
						name.equals("Expires", ignoreCase = true) -> {
							expires = headers.getDate("Expires")
						}
						name.equals("Last-Modified", ignoreCase = true) -> {
							lastModified = headers.getDate("Last-Modified")
							lastModifiedString = value
						}
						name.equals("ETag", ignoreCase = true) -> {
							etag = value
						}
						name.equals("Age", ignoreCase = true) -> {
							ageSeconds = value.toNonNegativeInt(-1)
						}
					}
				}
			}
		}

		/** [cacheResponse]를 사용하여 [request]을 만족시키는 전략을 반환합니다. */
		fun compute(): CacheStrategy {
			// 캐시된 응답이 없습니다.
			if (cacheResponse == null) {
				return CacheStrategy(request, null)
			}

			// 필요한 핸드셰이크가 누락된 경우 캐시된 응답을 삭제합니다.
			if (request.isHttps && !cacheResponse.isTls) {
				return CacheStrategy(request, null)
			}

			// 이 응답을 저장하지 말았어야 하는 경우 응답 소스로 사용해서는 안 됩니다.
			// 지속성 저장소가 올바르게 작동하고 규칙이 일정하다면 이 검사는 중복되어야 합니다.
			val responseCaching = cacheResponse.cacheControl()
			if (!isCacheable(request, cacheResponse)) {
				return CacheStrategy(request, null)
			}

			val requestCaching = request.cacheControl
			if (requestCaching.noCache || hasConditions(request)) {
				return CacheStrategy(request, null)
			}

			val ageMillis = cacheResponseAge()
			var freshMillis = computeFreshnessLifetime()

			if (requestCaching.maxAgeSeconds != -1) {
				freshMillis = min(freshMillis, SECONDS.toMillis(requestCaching.maxAgeSeconds.toLong()))
			}

			var minFreshMillis = 0L
			if (requestCaching.minFreshSeconds != -1) {
				minFreshMillis = SECONDS.toMillis(requestCaching.minFreshSeconds.toLong())
			}

			var maxStaleMillis = 0L
			if (!responseCaching.mustRevalidate && requestCaching.maxStaleSeconds != -1) {
				maxStaleMillis = SECONDS.toMillis(requestCaching.maxStaleSeconds.toLong())
			}

			if (!responseCaching.noCache && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
				return CacheStrategy(null, cacheResponse)
			}

			// 요청에 추가할 조건을 찾습니다.
			// 조건이 충족되면 응답 본문을 전송하지 않습니다.
			val conditionName: String
			val conditionValue: String?
			when {
				etag != null -> {
					conditionName = "If-None-Match"
					conditionValue = etag
				}
				lastModified != null -> {
					conditionName = "If-Modified-Since"
					conditionValue = lastModifiedString
				}
				servedDate != null -> {
					conditionName = "If-Modified-Since"
					conditionValue = servedDateString
				}
				// 조건이 없습니다! 정기적으로 요청하십시오.
				else -> return CacheStrategy(request, null)
			}

			val conditionalRequestHeaders = request.headers.newBuilder()
			conditionalRequestHeaders.add(conditionName, conditionValue!!)

			val conditionalRequest = request.newBuilder()
				.headers(conditionalRequestHeaders.build())
				.build()
			return CacheStrategy(conditionalRequest, cacheResponse)
		}

		/**
		 * 제공된 날짜부터 응답이 새로워진 시간(밀리초)을 반환합니다.
		 */
		private fun computeFreshnessLifetime(): Long {
			val responseCaching = cacheResponse!!.cacheControl()
			if (responseCaching.maxAgeSeconds != -1) {
				return SECONDS.toMillis(responseCaching.maxAgeSeconds.toLong())
			}

			val expires = expires
			if (expires != null) {
				val servedMillis = servedDate?.time ?: receivedResponseMillis
				val delta = expires.time - servedMillis
				return if (delta > 0L) delta else 0L
			}

			if (lastModified != null && request.url.query == null) {
				// HTTP RFC에서 권장하고 Firefox에서 구현된 대로 문서의 최대 사용 기간은 기본적으로 문서가 제공된 당시의 문서 사용 기간의 10%로 설정되어야 합니다.
				// 기본 만료 날짜는 쿼리가 포함된 URI에 사용되지 않습니다.
				val servedMillis = servedDate?.time ?: sentRequestMillis
				val delta = servedMillis - lastModified!!.time
				return if (delta > 0L) delta / 10 else 0L
			}

			return 0L
		}

		/**
		 * 응답의 현재 기간을 밀리초 단위로 반환합니다.
		 * 계산은 RFC 7234, 4.2.3 연령 계산에 따라 지정됩니다.
		 */
		private fun cacheResponseAge(): Long {
			val servedDate = servedDate
			val apparentReceivedAge = if (servedDate != null) {
				max(0, receivedResponseMillis - servedDate.time)
			} else {
				0
			}

			val receivedAge = if (ageSeconds != -1) {
				max(apparentReceivedAge, SECONDS.toMillis(ageSeconds.toLong()))
			} else {
				apparentReceivedAge
			}

			val responseDuration = receivedResponseMillis - sentRequestMillis
			val residentDuration = Time.currentMillis() - receivedResponseMillis
			return receivedAge + responseDuration + residentDuration
		}

		/**
		 * 요청에 클라이언트가 로컬로 응답을 보내지 못하도록 하는 조건이 포함된 경우 true를 반환합니다.
		 * 요청이 자체 조건으로 대기열에 추가되면 기본 제공 응답 캐시가 사용되지 않습니다.
		 */
		private fun hasConditions(request: Request): Boolean {
			return request.header("If-Modified-Since") != null ||
				request.header("If-None-Match") != null
		}
	}

	companion object {
		/** 나중에 다른 요청을 처리하기 위해 응답을 저장할 수 있으면 true를 반환합니다. */
		fun isCacheable(request: Request, response: Response): Boolean {
			// 요청 또는 응답에 대한 'no-store' 지시문은 응답이 캐시되는 것을 방지합니다.
			return !request.cacheControl.noStore && !response.cacheControl.noStore &&
				// Vary 모든 응답은 캐시할 수 없습니다.
				response.headers["Vary"] != "*"
		}

		/** 나중에 다른 요청을 처리하기 위해 응답을 저장할 수 있으면 true를 반환합니다. */
		fun isCacheable(request: Request, response: CacheResponse): Boolean {
			// 요청 또는 응답에 대한 'no-store' 지시문은 응답이 캐시되는 것을 방지합니다.
			return !request.cacheControl.noStore && !response.cacheControl().noStore &&
				// Vary 모든 응답은 캐시할 수 없습니다.
				response.responseHeaders["Vary"] != "*"
		}

		/** RFC 7234, 4.3.4에 정의된 대로 캐시된 헤더를 네트워크 헤더와 결합합니다. */
		fun combineHeaders(cachedHeaders: Headers, networkHeaders: Headers): Headers {
			val result = Headers.Builder()

			for (index in 0 until cachedHeaders.size) {
				val name = cachedHeaders.name(index)
				val value = cachedHeaders.value(index)
				if ("Warning".equals(name, ignoreCase = true) && value.startsWith("1")) {
					// 100레벨 신선도 경고를 삭제합니다.
					continue
				}
				if (isContentSpecificHeader(name) ||
					!isEndToEnd(name) ||
					networkHeaders[name] == null) {
					result.add(name, value)
				}
			}

			for (index in 0 until networkHeaders.size) {
				val fieldName = networkHeaders.name(index)
				if (!isContentSpecificHeader(fieldName) && isEndToEnd(fieldName)) {
					result.add(fieldName, networkHeaders.value(index))
				}
			}

			return result.build()
		}

		/** [name]이 RFC 2616, 13.5.1에 정의된 종단 간 HTTP 헤더인 경우 true를 반환합니다. */
		private fun isEndToEnd(name: String): Boolean {
			return !"Connection".equals(name, ignoreCase = true) &&
				!"Keep-Alive".equals(name, ignoreCase = true) &&
				!"Proxy-Authenticate".equals(name, ignoreCase = true) &&
				!"Proxy-Authorization".equals(name, ignoreCase = true) &&
				!"TE".equals(name, ignoreCase = true) &&
				!"Trailers".equals(name, ignoreCase = true) &&
				!"Transfer-Encoding".equals(name, ignoreCase = true) &&
				!"Upgrade".equals(name, ignoreCase = true)
		}

		/** [name]이 콘텐츠에 따라 다르므로 항상 캐시된 헤더에서 사용해야 하는 경우 true를 반환합니다. */
		private fun isContentSpecificHeader(name: String): Boolean {
			return "Content-Length".equals(name, ignoreCase = true) ||
				"Content-Encoding".equals(name, ignoreCase = true) ||
				"Content-Type".equals(name, ignoreCase = true)
		}
	}
}
