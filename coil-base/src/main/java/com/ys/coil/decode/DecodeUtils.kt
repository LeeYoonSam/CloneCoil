package com.ys.coil.decode

import android.graphics.BitmapFactory
import androidx.annotation.Px
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 이미지 디코딩에 유용한 유틸리티 메서드 모음입니다.
 */
object DecodeUtils {

    // https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
    private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
    private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

    // https://developers.google.com/speed/webp/docs/riff_container
    private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
    private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
    private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()

    // https://nokiatech.github.io/heif/technical.html
    private val HEIF_HEADER_FTYP = "ftyp".encodeUtf8()
    private val HEIF_HEADER_MSF1 = "msf1".encodeUtf8()
    private val HEIF_HEADER_HEVC = "hevc".encodeUtf8()
    private val HEIF_HEADER_HEVX = "hevx".encodeUtf8()

    /**
     * [source]에 GIF 이미지가 포함되어 있으면 true를 반환합니다. [source]는 소모되지 않습니다.
     */
    @JvmStatic
    fun isGif(source: BufferedSource): Boolean {
        return source.rangeEquals(0, GIF_HEADER_89A) || source.rangeEquals(0, GIF_HEADER_87A)
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
     * [source]에 HEIF 이미지가 포함되어 있으면 'true'를 반환합니다. [source]는 소모되지 않습니다.
     */
    @JvmStatic
    fun isHeif(source: BufferedSource): Boolean {
        return source.rangeEquals(4, HEIF_HEADER_FTYP)
    }

    @JvmStatic
    fun isAnimatedHeif(source: BufferedSource): Boolean {
        return isHeif(source) &&
            (source.rangeEquals(8, HEIF_HEADER_MSF1) ||
                source.rangeEquals(8, HEIF_HEADER_HEVC) ||
                source.rangeEquals(8, HEIF_HEADER_HEVX))
    }

    /**
     * 이미지의 소스 치수([srcWidth] 및 [srcHeight]), 출력 치수([dstWidth], [dstHeight]) 및 [scale]가 주어지면
     * [BitmapFactory.Options.inSampleSize]를 계산합니다.
     */
    @JvmStatic
    fun calculateInSampleSize(
        @Px srcWidth: Int,
        @Px srcHeight: Int,
        @Px dstWidth: Int,
        @Px dstHeight: Int,
        scale: Scale
    ): Int {
        val widthInSampleSize = Integer.highestOneBit(srcWidth / dstWidth).coerceAtLeast(1)
        val heightInSampleSize = Integer.highestOneBit(srcHeight / dstHeight).coerceAtLeast(1)
        return when (scale) {
            Scale.FILL -> min(widthInSampleSize, heightInSampleSize)
            Scale.FIT -> max(widthInSampleSize, heightInSampleSize)
        }
    }

    /**
     * 종횡비를 유지하면서 대상 치수에 FILL/FIT 을 위해 소스 치수를 곱할 백분율을 계산합니다.
     */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Int,
        @Px srcHeight: Int,
        @Px dstWidth: Int,
        @Px dstHeight: Int,
        scale: Scale
    ): Double {
        val widthPercent = dstWidth / srcWidth.toDouble()
        val heightPercent = dstHeight / srcHeight.toDouble()
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Float,
        @Px srcHeight: Float,
        @Px dstWidth: Float,
        @Px dstHeight: Float,
        scale: Scale
    ): Float {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Double,
        @Px srcHeight: Double,
        @Px dstWidth: Double,
        @Px dstHeight: Double,
        scale: Scale
    ): Double {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /**
     * 종횡비를 유지하면서 대상 크기 안에 소스 치수를 맞추거나 채우는 데 필요한 픽셀 크기를 계산합니다.
     */
    @JvmStatic
    fun computePixelSize(
        srcWidth: Int,
        srcHeight: Int,
        dstSize: Size,
        scale: Scale
    ): PixelSize {
        return when (dstSize) {
            is OriginalSize -> {
                PixelSize(srcWidth, srcHeight)
            }

            is PixelSize -> {
                val multiplier = computeSizeMultiplier(
                    srcWidth = srcWidth,
                    srcHeight = srcHeight,
                    dstWidth = dstSize.width,
                    dstHeight = dstSize.height,
                    scale = scale
                )

                PixelSize(
                    width = (srcWidth * multiplier).roundToInt(),
                    height = (srcHeight * multiplier).roundToInt()
                )
            }
        }
    }
}