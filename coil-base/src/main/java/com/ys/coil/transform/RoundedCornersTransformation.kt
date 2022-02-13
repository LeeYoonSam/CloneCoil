package com.ys.coil.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.Px
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.util.safeConfig
import kotlin.math.roundToInt

/**
 * 대상의 치수와 라운드에 맞게 이미지를 자르는 [Transformation]
 * 이미지의 모서리.
 *
 * @param topLeft 왼쪽 상단 모서리의 반경.
 * @param topRight 오른쪽 상단 모서리의 반경.
 * @param bottomLeft 왼쪽 하단 모서리의 반경.
 * @param bottomRight 오른쪽 하단 모서리의 반경.
 */
class RoundedCornersTransformation(
    @Px private val topLeft: Float = 0f,
    @Px private val topRight: Float = 0f,
    @Px private val bottomLeft: Float = 0f,
    @Px private val bottomRight: Float = 0f
) : Transformation {

    constructor(@Px radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0) {
            "All radii must be >= 0."
        }
    }

    override val cacheKey = "${javaClass.name}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val outputWidth: Int
        val outputHeight: Int
        when (size) {
            is PixelSize -> {
                val multiplier = DecodeUtils.computeSizeMultiplier(
                    srcWidth = input.width,
                    srcHeight = input.height,
                    dstWidth = size.width,
                    dstHeight = size.height,
                    scale = Scale.FILL
                )
                outputWidth = (size.width / multiplier).roundToInt()
                outputHeight = (size.height / multiplier).roundToInt()
            }
            is OriginalSize -> {
                outputWidth = input.width
                outputHeight = input.height
            }
        }

        val output = createBitmap(outputWidth, outputHeight, input.safeConfig)
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val matrix = Matrix()
            matrix.setTranslate((outputWidth - input.width) / 2f, (outputHeight - input.height) / 2f)
            val shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(matrix)
            paint.shader = shader

            val radii = floatArrayOf(
                topLeft, topLeft,
                topRight, topRight,
                bottomRight, bottomRight,
                bottomLeft, bottomLeft
            )
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            drawPath(path, paint)
        }

        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RoundedCornersTransformation &&
            topLeft == other.topLeft &&
            topRight == other.topRight &&
            bottomLeft == other.bottomLeft &&
            bottomRight == other.bottomRight
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + topRight.hashCode()
        result = 31 * result + bottomLeft.hashCode()
        result = 31 * result + bottomRight.hashCode()
        return result
    }
}
