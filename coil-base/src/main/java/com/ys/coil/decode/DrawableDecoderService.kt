package com.ys.coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.annotation.WorkerThread
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import com.ys.coil.util.HAS_APPCOMPAT_RESOURCES
import com.ys.coil.util.height
import com.ys.coil.util.toDrawable
import com.ys.coil.util.width

internal class DrawableDecoderService(
    private val context: Context,
    private val bitmapPool: BitmapPool
) {

    @WorkerThread
    fun convertIfNecessary(
        drawable: Drawable,
        size: Size,
        config: Bitmap.Config
    ): Drawable {
        return if (shouldConvertToBitmap(drawable)) {
            convert(drawable, size, config).toDrawable(context)
        } else {
            drawable
        }
    }

    /**
     * 제공된 [Drawable]을 [Bitmap]으로 변환합니다.
     */
    private fun convert(
        drawable: Drawable,
        size: Size,
        config: Bitmap.Config
    ): Bitmap {

        // 비트맵을 반환하는 빠른 경로입니다.
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap.config == config) {
                return bitmap
            }
        }

        val (width, height) = when (size) {
            is OriginalSize -> PixelSize(drawable.width, drawable.height)
            is PixelSize -> size
        }

        val (oldLeft, oldTop, oldRight, oldBottom) = drawable.bounds

        val bitmap = bitmapPool.get(width, height, config)
        drawable.apply {
            setBounds(0, 0, width, height)
            draw(Canvas(bitmap))
            setBounds(oldLeft, oldTop, oldRight, oldBottom)
        }

        return bitmap
    }

    private fun shouldConvertToBitmap(drawable: Drawable): Boolean {
        return (HAS_APPCOMPAT_RESOURCES && drawable is VectorDrawableCompat) ||
                (SDK_INT > LOLLIPOP && drawable is VectorDrawable)
    }
}