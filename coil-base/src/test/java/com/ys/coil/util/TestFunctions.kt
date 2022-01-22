package com.ys.coil.util

import android.content.Context
import android.graphics.Bitmap
import com.ys.coil.request.ImageRequest
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

fun createRequest(
    context: Context,
    builder: ImageRequest.Builder.() -> Unit = {}
) : ImageRequest = ImageRequest.Builder(context).data(Unit).apply(builder).build()