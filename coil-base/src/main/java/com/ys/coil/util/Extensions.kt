package com.ys.coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toDrawable

internal inline fun Bitmap.toDrawable(context: Context): BitmapDrawable = toDrawable(context.resources)