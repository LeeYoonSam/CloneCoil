package com.ys.coil.decode

import android.graphics.drawable.Drawable

/**
 * [Decoder.decode]의 결과입니다.
 *
 * @param drawable 로드된 [Drawable]
 * @param isSampled [drawable]이 샘플링되면 True (즉, 전체 크기로 메모리에 로드되지 않은 경우).
 *
 * @see Decoder
 */
data class DecodeResult(
    val drawable: Drawable,
    val isSampled: Boolean
)
