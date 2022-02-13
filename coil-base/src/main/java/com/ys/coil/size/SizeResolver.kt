package com.ys.coil.size

import androidx.annotation.MainThread

/**
 * 이미지 요청 대상의 크기를 측정하기 위한 인터페이스
 *
 * @see RequestBuilder.size
 */
fun interface SizeResolver {

    companion object {
        /**
         * 고정된 [Size]로 [SizeResolver] 인스턴스를 생성합니다.
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(size: Size): SizeResolver = RealSizeResolver(size)

    }

    /**
     * 이미지가 로드되어야 하는 [Size]를 반환합니다.
     */
    @MainThread
    suspend fun size() : Size
}
