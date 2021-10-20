package com.ys.coil.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal inline fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
