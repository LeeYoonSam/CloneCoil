package com.ys.coil.gif.transform

import com.ys.coil.annotation.ExperimentalCoilApi

/**
 * [AnimatedTransformation]을 적용한 후 이미지 픽셀의 불투명도를 나타냅니다.
 */
@ExperimentalCoilApi
enum class PixelOpacity {

    /**
     * [AnimatedTransformation]이 이미지의 불투명도를 변경하지 않았음을 나타냅니다.
     *
     * 이미지에 투명 픽셀을 추가하거나 이미지의 모든 투명 픽셀을 제거하지 않는 한 이것을 반환합니다.
     */
    UNCHANGED,

    /**
     * [AnimatedTransformation]이 이미지에 투명 픽셀을 추가했음을 나타냅니다.
     */
    TRANSLUCENT,

    /**
     * [AnimatedTransformation]이 이미지의 모든 투명 픽셀을 제거했음을 나타냅니다.
     */
    OPAQUE
}
