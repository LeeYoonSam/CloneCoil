package com.ys.coil.decode

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.core.graphics.applyCanvas
import androidx.exifinterface.media.ExifInterface
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.util.normalize
import com.ys.coil.util.toDrawable
import okio.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class BitmapFactoryDecoder(
    private val context: Context
) : Decoder {
    companion object {
        private const val MIME_TYPE_JPEG = "image/jpeg"
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun handles(source: BufferedSource, mimeType: String?) = true

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = BitmapFactory.Options().run {
        val safeSource = ExceptionCatchingSource(source)
        val safeBufferedSource = safeSource.buffer()

        // 이미지의 치수를 읽습니다.
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(safeBufferedSource.peek().inputStream(), null, this)
        safeSource.exception?.let { throw it }
        inJustDecodeBounds = false

        // 이미지의 EXIF 데이터를 읽습니다.
        val exifInterface = ExifInterface(safeBufferedSource.peek().inputStream())
        val isFlipped = exifInterface.isFlipped
        val rotationDegrees = exifInterface.rotationDegrees
        val isRotated = rotationDegrees > 0
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270

        // srcWidth 및 srcHeight는 EXIF 변환 후(그러나 샘플링 전) 이미지의 크기입니다.
        val srcWidth = if (isSwapped) outHeight else outWidth
        val srcHeight = if (isSwapped) outWidth else outHeight

        // EXIF 변환을 수행해야 하는 경우 하드웨어 비트맵을 비활성화합니다.
        val safeConfig= if (isFlipped || isRotated) options.config.normalize() else options.config
        inPreferredConfig = if (allowRgb565(options.allowRgb565, safeConfig, outMimeType)) Bitmap.Config.RGB_565 else safeConfig

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        inMutable = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || inPreferredConfig != Bitmap.Config.HARDWARE
        inScaled = false

        when {
            outWidth <= 0 || outHeight <= 0 -> {
                // 이것은 이미지의 크기를 디코딩하는 동안 오류가 발생한 경우 발생합니다.
                inSampleSize = 1
                inBitmap = null
            }
            size !is PixelSize -> {
                // 크기가 OriginalSize인 경우에 발생합니다.
                inSampleSize = 1

                if (inMutable) {
                    inBitmap = pool.getDirtyOrNull(outWidth, outHeight, inPreferredConfig)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                val (width, height) = size
                inSampleSize = DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, width, height, options.scale)

                // 이미지의 밀도 배율(density scaling)을 계산합니다.
                val sampledSrcWidth = srcWidth / inSampleSize.toDouble()
                val sampledSrcHeight = srcHeight / inSampleSize.toDouble()
                val widthPercent = min(1.0, width / sampledSrcWidth)
                val heightPercent = min(1.0, height / sampledSrcHeight)
                val scale = when (options.scale) {
                    Scale.FILL -> max(widthPercent, heightPercent)
                    Scale.FIT -> min(widthPercent, heightPercent)
                }

                inScaled = scale != 1.0
                if (inScaled) {
                    inDensity = Int.MAX_VALUE
                    inTargetDensity = (scale * Int.MAX_VALUE).roundToInt()
                }

                if (inMutable) {
                    // output Bitmap의 치수가 요청된 치수와 정확히 일치하지 않을 수 있으므로 필요한 것보다 약간 더 큰 비트맵을 할당하십시오. 이는 Android의 다운샘플링 알고리즘이 복잡하기 때문입니다.
                    val sampledOutWidth = outWidth / inSampleSize.toDouble()
                    val sampledOutHeight = outHeight / inSampleSize.toDouble()
                    inBitmap = pool.getDirtyOrNull(
                        // ceil: 주어진 값 x를 양의 무한대를 향한 정수로 반올림합니다.
                        width = ceil(scale * sampledOutWidth + 0.5).toInt(),
                        height = ceil(scale * sampledOutHeight + 0.5).toInt(),
                        config = inPreferredConfig
                    )
                }
            }
            else -> {
                // 이미지 크기와 정확히 일치하는 비트맵만 재사용할 수 있습니다.
                if (inMutable) {
                    inBitmap = pool.getDirtyOrNull(outWidth, outHeight, inPreferredConfig)
                }

                // Bitmap을 재사용하는 경우 샘플 크기는 1이어야 합니다.
                inSampleSize = if (inBitmap != null) {
                    1
                } else {
                    DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, size.width, size.height, options.scale)
                }
            }
        }

        // 비트맵을 디코딩, 예외가 발생하더라도 안전하게 종료되도록 코틀린 use 사용
        val rawBitmap: Bitmap? = safeBufferedSource.use {
            BitmapFactory.decodeStream(it.inputStream(), null, this)
        }
        safeSource.exception?.let { exception ->
            rawBitmap?.let(pool::put)
            throw exception
        }

        // EXIF 변환을 적용
        checkNotNull(rawBitmap) { "BitmapFactory returned a null Bitmap." }
        val bitmap = applyExifTransformations(pool, rawBitmap, inPreferredConfig, isFlipped, rotationDegrees)
        bitmap.density = Bitmap.DENSITY_NONE

        DecodeResult(
            drawable = bitmap.toDrawable(context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** TODO: MIME 유형에 의존하는 대신 데이터 유형(및 알파가 있는 경우)을 파악하기 위해 소스를 엿봅니다. */
    private fun allowRgb565(
        allowRgb565: Boolean,
        config: Bitmap.Config,
        mimeType: String?
    ): Boolean {
        return allowRgb565 && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || config == Bitmap.Config.ARGB_8888) && mimeType == MIME_TYPE_JPEG
    }

    /** 참고: 이 방법은 이미지를 변환해야 하는 경우 [config]가 [Bitmap.Config.HARDWARE]가 아니라고 가정합니다. */
    private fun applyExifTransformations(
        pool: BitmapPool,
        inBitmap: Bitmap,
        config: Bitmap.Config,
        isFlipped: Boolean,
        rotationDegrees: Int
    ): Bitmap {
        // 적용할 변환이 없는 경우 단락.
        val isRotated = rotationDegrees > 0
        if (!isFlipped && !isRotated) {
            return inBitmap
        }

        val matrix = Matrix()
        val centerX = inBitmap.width / 2f
        val centerY = inBitmap.height / 2f
        if (isFlipped) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }
        if (isRotated) {
            matrix.postRotate(rotationDegrees.toFloat(), centerX, centerY)
        }

        val rect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(rect)
        if (rect.left != 0f || rect.top != 0f) {
            matrix.postTranslate(-rect.left, -rect.top)
        }

        val outBitmap = if (rotationDegrees == 90 || rotationDegrees == 270) {
            pool.get(inBitmap.height, inBitmap.width, config)
        } else {
            pool.get(inBitmap.width, inBitmap.height, config)
        }

        outBitmap.applyCanvas {
            drawBitmap(inBitmap, matrix, paint)
        }
        pool.put(inBitmap)
        return outBitmap
    }

    /** [BitmapFactory.decodeStream]이 [Exception]를 삼키는 것을 방지합니다. */
    private class ExceptionCatchingSource(delegate: Source) : ForwardingSource(delegate) {

        var exception: Exception? = null
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            try {
                return super.read(sink, byteCount)
            } catch (e: Exception) {
                exception = e
                throw e
            }
        }
    }
}