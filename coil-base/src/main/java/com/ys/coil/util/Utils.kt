package com.ys.coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION
import android.os.Looper
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.ys.coil.ComponentRegistry
import com.ys.coil.base.R
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.Decoder
import com.ys.coil.disk.DiskCache
import com.ys.coil.fetch.Fetcher
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.DefaultRequestOptions
import com.ys.coil.request.Parameters
import com.ys.coil.request.ViewTargetRequestManager
import com.ys.coil.size.Scale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
import java.io.Closeable
import java.io.File
import java.util.Optional
import kotlin.coroutines.CoroutineContext

internal val View.requestManager: ViewTargetRequestManager
    get() {
        var manager = getTag(R.id.coil_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = synchronized(this) {
                // Check again in case coil_request_manager was just set.
                (getTag(R.id.coil_request_manager) as? ViewTargetRequestManager)
                    ?.let { return@synchronized it }

                ViewTargetRequestManager(this).apply {
                    addOnAttachStateChangeListener(this)
                    setTag(R.id.coil_request_manager, this)
                }
            }
        }
        return manager
    }

/**
 * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
 * transformations.
 */
internal val DEFAULT_BITMAP_CONFIG = if (VERSION.SDK_INT >= 26) {
    Bitmap.Config.HARDWARE
} else {
    Bitmap.Config.ARGB_8888
}

/** Required for compatibility with API 25 and below. */
internal val NULL_COLOR_SPACE: ColorSpace? = null

internal val DEFAULT_REQUEST_OPTIONS = DefaultRequestOptions()

internal val EMPTY_HEADERS = Headers.Builder().build()

internal fun Headers?.orEmpty() = this ?: EMPTY_HEADERS

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

internal inline val Any.identityHashCode: Int
    get() = System.identityHashCode(this)

@OptIn(ExperimentalStdlibApi::class)
internal inline val CoroutineContext.dispatcher: CoroutineDispatcher
    get() = get(CoroutineDispatcher) ?: error("Current context doesn't contain CoroutineDispatcher in it: $this")

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.getCompletedOrNull(): T? {
    return try {
        getCompleted()
    } catch (_: Throwable) {
        null
    }
}

internal inline operator fun MemoryCache.get(key: MemoryCache.Key?) = key?.let(::get)

internal val Context.safeCacheDir: File get() = cacheDir.apply { mkdirs() }

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY_CACHE,
        DataSource.MEMORY -> Emoji.BRAIN
        DataSource.DISK -> Emoji.FLOPPY
        DataSource.NETWORK -> Emoji.CLOUD
    }

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

internal val Drawable.isVector: Boolean
    get() = this is VectorDrawable || this is VectorDrawableCompat

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

/**
 * ?????? ????????? ??? ??????????????? [MimeTypeMap.getFileExtensionFromUrl]?????? ?????????????????????.
 */
internal fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) {
        return null
    }

    val extension = url
        .substringBeforeLast('#') // Fragment ??? ???????????????.
        .substringBeforeLast('?') // ????????? ???????????????.
        .substringAfterLast('/') // ????????? ?????? ??????????????? ???????????????.
        .substringAfterLast('.', missingDelimiterValue = "") // ?????? ???????????? ???????????????.

    return getMimeTypeFromExtension(extension)
}

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

/**
 * [Transformation.transform]??? ?????? ??? ?????? ???????????? ?????? ????????? ????????? ????????? ?????? ???????????????.
 */
internal val VALID_TRANSFORMATION_CONFIGS = if (VERSION.SDK_INT >= 26) {
    arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
} else {
    arrayOf(Bitmap.Config.ARGB_8888)
}

internal inline fun ComponentRegistry.Builder.addFirst(
    pair: Pair<Fetcher.Factory<*>, Class<*>>?
) = apply { if (pair != null) fetcherFactories.add(0, pair) }

internal inline fun ComponentRegistry.Builder.addFirst(
    factory: Decoder.Factory?
) = apply { if (factory != null) decoderFactories.add(0, factory) }

internal fun String.toNonNegativeInt(defaultValue: Int): Int {
    val value = toLongOrNull() ?: return defaultValue
    return when {
        value > Int.MAX_VALUE -> Int.MAX_VALUE
        value < 0 -> 0
        else -> value.toInt()
    }
}

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

internal fun unsupported(): Nothing = error("Unsupported")

/** ????????? [Optional] ??????. */
internal class Option<T : Any>(@JvmField val value: T?)

/** ????????? ?????? ?????????????????? ?????? ???????????? ?????????. */
internal object Utils {

    private const val STANDARD_MEMORY_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15
    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256
    private const val SINGLETON_DISK_CACHE_NAME = "image_cache"

    /**
     * ????????? ????????? ????????? ??????????????? ???????????????.
     * ????????? ?????? ??????????????? ???????????? ?????? ?????? [ImageLoader] ????????? ??????????????? ????????? ????????? ?????? ??????????????? ???????????????.
     *
     * @see DiskCache.Builder.directory
     */
    private var singletonDiskCache: DiskCache? = null

    fun calculateMemoryCacheSize(context: Context, percent: Double): Int {
        val memoryClassMegabytes = try {
            val activityManager: ActivityManager = context.requireSystemService()
            val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
            if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        } catch (_: Exception) {
            DEFAULT_MEMORY_CLASS_MEGABYTES
        }
        return (percent * memoryClassMegabytes * 1024 * 1024).toInt()
    }

    fun defaultMemoryCacheSizePercent(context: Context): Double {
        return try {
            val activityManager: ActivityManager = context.requireSystemService()
            if (activityManager.isLowRamDevice) LOW_MEMORY_MULTIPLIER else STANDARD_MEMORY_MULTIPLIER
        } catch (_: Exception) {
            STANDARD_MEMORY_MULTIPLIER
        }
    }

    @Synchronized
    fun singletonDiskCache(context: Context): DiskCache {
        return singletonDiskCache ?: run {
            // ????????? ????????? ?????? ??????????????? ????????????.
            DiskCache.Builder(context)
                .directory(context.safeCacheDir.resolve(SINGLETON_DISK_CACHE_NAME))
                .build()
                .also { singletonDiskCache = it }
        }
    }
}
