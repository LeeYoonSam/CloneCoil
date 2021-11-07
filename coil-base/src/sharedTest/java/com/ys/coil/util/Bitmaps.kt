package com.ys.coil.util

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * 4개의 [IntArray], 알파, 빨강, 녹색 및 파랑 픽셀 값의 [Array]을 반환합니다.
 */
fun Bitmap.getPixels(): Array<IntArray> {

    require(config == Bitmap.Config.ARGB_8888) { "Config must be ARGB_8888." }

    val size = width * height
    val pixels = IntArray(size)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val alpha = IntArray(size)
    val red = IntArray(size)
    val green = IntArray(size)
    val blue = IntArray(size)

    pixels.forEachIndexed { index, pixel ->
        alpha[index] = pixel.alpha
        red[index] = pixel.red
        green[index] = pixel.green
        blue[index] = pixel.blue
    }

    return arrayOf(alpha, red, green, blue)
}

/**
 * RGB 채널의 상호 상관이 [threshold] 이상이고 알파 채널이 정확히 일치하는지 확인하여 두 개의 [Bitmap]을 비교합니다.
 */
fun Bitmap.sameAs(other: Bitmap, @FloatRange(from = -1.0, to = 1.0) threshold: Double): Boolean {
    if (width != other.width || height!= other.height) {
        return false
    }

    val (xAlpha, xRed, xGreen, xBlue) = getPixels()
    val (yAlpha, yRed, yGreen, yBlue) = other.getPixels()

    return xAlpha.contentEquals(yAlpha) &&
            crossCorrelation(xRed, yRed) >= threshold &&
            crossCorrelation(xGreen, yGreen) >= threshold &&
            crossCorrelation(xBlue, yBlue) >= threshold
}