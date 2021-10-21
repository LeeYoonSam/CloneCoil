package com.ys.coil.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat

internal val HAS_APPCOMPAT_RESOURCES = try {
    Class.forName(AppCompatResources::class.java.name)
    true
} catch (ignored: Throwable) {
    false
}

internal fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable {
    val drawable = if (HAS_APPCOMPAT_RESOURCES) {
        AppCompatResources.getDrawable(this, resId)
    } else {
        ContextCompat.getDrawable(this, resId)
    }

    return checkNotNull(drawable)
}

internal inline fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
