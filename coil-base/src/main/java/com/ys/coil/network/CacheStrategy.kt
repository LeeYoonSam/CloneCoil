package com.ys.coil.network

import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

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
		/** Returns a strategy to satisfy [request] using [cacheResponse]. */
		fun compute(): CacheStrategy {
			return CacheStrategy(null, null)
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