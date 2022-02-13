package com.ys.coil.gif.transform

import android.graphics.Canvas
import com.ys.coil.annotation.ExperimentalCoilApi

/**
 * 애니메이션 이미지의 픽셀 데이터를 변환하기 위한 인터페이스입니다.
 */
@ExperimentalCoilApi
fun interface AnimatedTransformation {

    /**
     * [canvas]에 변형을 적용합니다.
     *
     * @param canvas 그릴 [Canvas]입니다.
     * @return 그리기 후 이미지의 불투명도입니다.
     */
    fun transform(canvas: Canvas): PixelOpacity
}
