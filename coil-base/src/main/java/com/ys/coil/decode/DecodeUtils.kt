package com.ys.coil.decode

import android.graphics.BitmapFactory
import androidx.annotation.Px
import com.ys.coil.size.Scale
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

/**
 * 이미지 디코딩에 유용한 유틸리티 메서드 모음입니다.
 */
object DecodeUtils {

    // https://www.onicos.com/staff/iz/formats/gif.html
    private val GIF_HEADER = "GIF".encodeUtf8()

    // https://developers.google.com/speed/webp/docs/riff_container
    private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
    private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
    private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()

    /**
     * [source]에 GIF 이미지가 포함되어 있으면 true를 반환합니다. [source]는 소모되지 않습니다.
     */
    @JvmStatic
    fun isGif(source: BufferedSource): Boolean {
        return source.rangeEquals(0, GIF_HEADER)
    }

    /**
     * [source]에 WebP 이미지가 포함되어 있으면 true를 반환합니다. [source]는 소모되지 않습니다.
     */
    @JvmStatic
    fun isWebP(source: BufferedSource): Boolean {
        return source.rangeEquals(0, WEBP_HEADER_RIFF) && source.rangeEquals(8, WEBP_HEADER_WEBP)
    }

    /**
     * [source]에 애니메이션 WebP 이미지가 포함되어 있으면 true를 반환합니다. [source]는 소모되지 않습니다.
     */
    @JvmStatic
    fun isAnimatedWebP(source: BufferedSource): Boolean {
        return isWebP(source) &&
                source.rangeEquals(12, WEBP_HEADER_VPX8) &&
                source.request(17) &&
                (source.buffer[16] and 0b00000010) > 0
    }

    /**
     * 이미지의 소스 치수([inWidth] 및 [inHeight]), 출력 치수([outWidth], [outHeight]) 및 [scale]이 주어지면 [BitmapFactory.Options.inSampleSize]를 계산합니다.
     */
    @JvmStatic
    fun calculateInSampleSize(
        @Px inWidth: Int,
        @Px inHeight: Int,
        @Px outWidth: Int,
        @Px outHeight: Int,
        scale: Scale
    ): Int {
        val widthInSampleSize = max(1, Integer.highestOneBit(inWidth / outWidth))
        val heightInSampleSize = max(1, Integer.highestOneBit(inHeight / outHeight))
        return when (scale) {
            Scale.FILL -> min(widthInSampleSize, heightInSampleSize)
            Scale.FIT -> max(widthInSampleSize, heightInSampleSize)
        }
    }
}