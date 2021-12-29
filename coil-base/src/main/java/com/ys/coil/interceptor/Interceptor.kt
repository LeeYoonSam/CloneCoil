package com.ys.coil.interceptor

import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.size.Size

/**
 * [ImageLoader]의 이미지 엔진에 대한 요청을 관찰, 변환, 단락 또는 재시도하십시오.
 */
interface Interceptor {

	suspend fun interceptor(chain: Chain): ImageResult

	interface Chain {
		val request: ImageRequest
		val size: Size

		/**
		 * 이미지를 로드할 요청한 [size]를 설정합니다.
		 *
		 * @param size 이미지에 대해 요청된 크기입니다.
		 */
		fun withSize(size: Size): Chain

		/**
		 * 체인을 계속 실행하십시오.
		 *
		 * @param request 계속할 요청입니다.
		 */
		suspend fun proceed(request: ImageRequest): ImageResult
	}
}