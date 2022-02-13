package com.ys.coil.network

import okhttp3.Response

/**
 * 예기치 않은 2xx 코드가 아닌 HTTP 응답에 대한 예외입니다.
 *
 * @see HttpUrlFetcher
 */
class HttpException(response: Response) : RuntimeException("HTTP ${response.code}: ${response.message}")
