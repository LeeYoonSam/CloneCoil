package com.ys.coil.size

import com.ys.coil.request.RequestBuilder

/**
 * 이미지 요청 대상의 크기를 측정하기 위한 인터페이스
 *
 * @see RequestBuilder.size
 */
interface SizeResolver {

    companion object {
        /**
         * 고정된 [Size]로 [SizeResolver] 인스턴스를 생성합니다.
         */
        operator fun invoke(size: Size): SizeResolver {
            return object : SizeResolver {
                override suspend fun size() = size
            }
        }
    }

    /**
     * 이미지가 로드되어야 하는 [Size]를 반환합니다.
     */
    suspend fun size() : Size
}