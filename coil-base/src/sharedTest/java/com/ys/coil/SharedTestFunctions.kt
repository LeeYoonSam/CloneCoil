package com.ys.coil

import android.graphics.Bitmap
import com.ys.coil.size.PixelSize

val Bitmap.size: PixelSize
    get() = PixelSize(width, height)