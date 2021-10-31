package com.ys.coil.util

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.Px

object Utils {
    /** 주어진 너비, 높이 및 [Bitmap.Config]를 사용하여 [Bitmap]의 메모리 내 크기를 반환합니다. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.getBytesPerPixel()
    }

    fun getDefaultBitmapConfig(): Bitmap.Config {
        // Android O 이상의 하드웨어 비트맵은 변형 없이 그리기에 최적화되어 있습니다.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888
    }
}