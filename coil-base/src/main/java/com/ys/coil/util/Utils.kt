package com.ys.coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Looper
import android.os.StatFs
import android.view.View
import android.webkit.MimeTypeMap
import androidx.annotation.Px
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.ys.coil.ComponentRegistry
import com.ys.coil.R
import com.ys.coil.decode.Decoder
import com.ys.coil.disk.DiskCache
import com.ys.coil.fetch.Fetcher
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.DefaultRequestOptions
import com.ys.coil.request.Parameters
import com.ys.coil.request.ViewTargetRequestManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
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

internal val Drawable.isVector: Boolean
    get() = this is VectorDrawable || this is VectorDrawableCompat

/**
 * 특수 문자에 더 관대하도록 [MimeTypeMap.getFileExtensionFromUrl]에서 수정되었습니다.
 */
internal fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) {
        return null
    }

    val extension = url
        .substringBeforeLast('#') // Fragment 를 제거합니다.
        .substringBeforeLast('?') // 쿼리를 제거합니다.
        .substringAfterLast('/') // 마지막 경로 세그먼트를 가져옵니다.
        .substringAfterLast('.', missingDelimiterValue = "") // 파일 확장자를 가져옵니다.

    return getMimeTypeFromExtension(extension)
}

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

/**
 * [Transformation.transform]의 입력 및 출력 비트맵에 대한 유효한 비트맵 구성의 허용 목록입니다.
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

/** A simple [Optional] replacement. */
internal class Option<T : Any>(@JvmField val value: T?)

internal object Utils {

    private const val STANDARD_MEMORY_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15
    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256
    private const val SINGLETON_DISK_CACHE_NAME = "image_cache"

    /**
     * 디스크 캐시의 싱글톤 인스턴스를 보유합니다.
     * 디스크 캐시 디렉토리를 지정하지 않고 여러 [ImageLoader] 생성을 지원하려면 싱글톤 디스크 캐시 인스턴스가 필요합니다.
     *
     * @see DiskCache.Builder.directory
     */
    private var singletonDiskCache: DiskCache? = null

    private const val MIN_DISK_CACHE_SIZE: Long = 10 * 1024 * 1024 // 10MB
    private const val MAX_DISK_CACHE_SIZE: Long = 250 * 1024 * 1024 // 250MB

    private const val DISK_CACHE_PERCENTAGE = 0.02

    private const val STANDARD_MULTIPLIER = 0.25

    /** 주어진 너비, 높이 및 [Bitmap.Config]를 사용하여 [Bitmap]의 메모리 내 크기를 반환합니다. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.getBytesPerPixel()
    }

    fun getDefaultBitmapConfig(): Bitmap.Config {
        // Android O 이상의 하드웨어 비트맵은 변형 없이 그리기에 최적화되어 있습니다.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888
    }

    fun getDefaultAvailableMemoryPercentage(context: Context): Double {
        val activityManager: ActivityManager = context.requireSystemService()
        return if (activityManager.isLowRawDeviceCompat()) LOW_MEMORY_MULTIPLIER else STANDARD_MULTIPLIER
    }

    fun getDefaultBitmapPoolPercentage(): Double {
        // 풀에 추가할 수 없는 하드웨어 비트맵을 기본으로 하기 때문에 Android O 이상에서 비트맵 풀링에 더 적은 메모리를 할당합니다.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 0.25 else 0.5
    }

    fun getDefaultCacheDirectory(context: Context): File {
        return File(context.cacheDir, SINGLETON_DISK_CACHE_NAME).apply { mkdirs() }
    }

    /** Modified from Picasso. */
    fun calculateAvailableMemorySize(context: Context, percentage: Double): Long {
        val activityManager: ActivityManager = context.requireSystemService()
        val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        val memoryClassMegabytes = if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        return (percentage * memoryClassMegabytes * 1024 * 1024).toLong()
    }

    /** Modified from Picasso. */
    fun calculateDiskCacheSize(cacheDirectory: File): Long {
        return try {
            val cacheDir = StatFs(cacheDirectory.absolutePath)
            val size = DISK_CACHE_PERCENTAGE * cacheDir.getBlockCountCompat() * cacheDir.getBlockSizeCompat()
            return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE, MAX_DISK_CACHE_SIZE)
        } catch (ignored: Exception) {
            MIN_DISK_CACHE_SIZE
        }
    }

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
            // 싱글톤 디스크 캐시 인스턴스를 만듭니다.
            DiskCache.Builder(context)
                .directory(context.safeCacheDir.resolve(SINGLETON_DISK_CACHE_NAME))
                .build()
                .also { singletonDiskCache = it }
        }
    }
}