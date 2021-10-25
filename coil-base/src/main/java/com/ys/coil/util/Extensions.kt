package com.ys.coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Response
import java.io.Closeable

internal suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback = ContinuationCallback(this, continuation)
        enqueue(callback)
        continuation.invokeOnCancellation(callback)
    }
}

internal fun Bitmap.Config?.getBytesPerPixel(): Int {
    return when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }
}

internal inline fun <T> MutableList<T>?.orEmpty(): MutableList<T> = this ?: mutableListOf()

internal inline fun <T> MutableList<T>.removeLast(): T? = if (isNotEmpty()) removeAt(lastIndex) else null

internal inline fun Bitmap.toDrawable(context: Context): BitmapDrawable = toDrawable(context.resources)

/**
 * 주어진 [Bitmap]의 메모리 크기를 바이트 단위로 반환합니다.
 */
internal fun Bitmap.getAllocationByteCountCompat(): Int {
    check(!isRecycled) { "Cannot obtain size for recycled Bitmap: $this [$width x $height] + $config" }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            allocationByteCount
        } else {
            rowBytes * height
        }
    } catch (ignored: Exception) {
        Utils.calculateAllocationByteCount(width, height, config)
    }
}

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (ignored: Exception) {}
}

/** null 및 [Bitmap.Config.HARDWARE] 구성을 [Bitmap.Config.ARGB_8888]로 변환합니다. */
internal fun Bitmap.Config?.normalize(): Bitmap.Config {
    return if (this == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.HARDWARE)) {
        Bitmap.Config.ARGB_8888
    } else {
        this
    }
}