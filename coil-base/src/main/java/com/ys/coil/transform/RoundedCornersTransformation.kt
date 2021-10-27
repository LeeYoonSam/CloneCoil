package com.ys.coil.transform

import android.graphics.*
import androidx.core.graphics.applyCanvas
import com.ys.coil.bitmappool.BitmapPool

/**
 * 이미지의 모서리를 둥글게 만드는 [Transformation].
 */
class RoundedCornersTransformation(private val radius: Float): Transformation {

    init {
        require(radius >= 0) { "Radius must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-$radius"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = pool.get(input.width, input.height, input.config)
        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawRoundRect(rect, radius, radius, paint)
        }
        pool.put(input)
        return output
    }
}