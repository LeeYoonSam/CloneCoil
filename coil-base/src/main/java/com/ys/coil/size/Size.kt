package com.ys.coil.size

import androidx.annotation.Px

/**
 * 이미지 요청 대상의 크기를 나타낸다.
 *
 * @see RequestBuilder.size
 * @see SizeResolver.size
 */
sealed class Size

/**
 * 소스 이미지의 너비와 높이를 나타낸다.
 */
object OriginalSize: Size()

/**
 * 0이 아닌 너비와 높이(픽셀) 입니다.
 */
data class PixelSize(
    @Px val width: Int,
    @Px val height: Int
): Size() {

    init {
        require(width > 0 && height > 0) { "Width and height must be > 0." }
    }
}