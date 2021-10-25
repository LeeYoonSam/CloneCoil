package com.ys.coil.util

import android.graphics.Bitmap
import androidx.annotation.Px

object Utils {
    /** 주어진 너비, 높이 및 [Bitmap.Config]를 사용하여 [Bitmap]의 메모리 내 크기를 반환합니다. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.getBytesPerPixel()
    }
}