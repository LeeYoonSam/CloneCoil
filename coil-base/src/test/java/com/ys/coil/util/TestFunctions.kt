package com.ys.coil.util

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.robolectric.Shadows

fun createBitmap(
    width: Int = 100,
    height: Int = 100,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    isMutable: Boolean = true
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, config)
    Shadows.shadowOf(bitmap).setMutable(isMutable)
    return bitmap
}