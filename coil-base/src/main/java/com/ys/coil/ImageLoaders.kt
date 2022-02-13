package com.ys.coil

import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import kotlinx.coroutines.runBlocking

/**
 * [request]을 실행하고 완료될 때까지 현재 스레드를 차단합니다.
 *
 * @see ImageLoader.execute
 */
fun ImageLoader.executeBlocking(request: ImageRequest): ImageResult {
	return runBlocking { execute(request) }
}
