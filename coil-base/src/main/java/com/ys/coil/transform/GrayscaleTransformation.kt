package com.ys.coil.transform

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import com.ys.coil.bitmappool.BitmapPool

/**
 * 이미지를 회색 음영으로 변환하는 [Transformation]입니다.
 */
class GrayscaleTransformation : Transformation {
    companion object {
        private val COLOR_FILTER = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    override fun key(): String = GrayscaleTransformation::class.java.name

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = COLOR_FILTER }

        val output = pool.get(input.width, input.height, input.config)
        output.applyCanvas {
            drawBitmap(input, 0f, 0f, paint)
        }
        pool.put(input)
        return output
    }
}