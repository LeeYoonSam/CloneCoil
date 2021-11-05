package com.ys.coil.bitmappool

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.Px
import com.ys.coil.bitmappool.strategy.BitmapPoolStrategy
import com.ys.coil.util.arraySetOf
import com.ys.coil.util.getAllocationByteCountCompat
import com.ys.coil.util.log

/**
 * [BitmapPoolStrategy]를 사용하여 [Bitmap]을 버킷하는 [BitmapPool] 구현
 * 그런 다음 LRU 축출 정책을 사용하여 최소에서 [Bitmap]을 축출합니다.
 * 주어진 최대 크기 제한 아래로 풀을 유지하기 위해 최근에 사용한 버킷.
 */
internal class RealBitmapPool(
    private val maxSize: Long,
    private val allowedConfigs: Set<Bitmap.Config> = getDefaultAllowedConfigs(),
    private val strategy: BitmapPoolStrategy = BitmapPoolStrategy()
) : BitmapPool {

    companion object {
        private const val TAG = "RealBitmapPool"

        private fun getDefaultAllowedConfigs(): Set<Bitmap.Config> = arraySetOf {
            addAll(Bitmap.Config.values())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Hardware bitmaps cannot be recycled and cannot be added to the pool.
                remove(Bitmap.Config.HARDWARE)
            }
        }
    }

    private var currentSize: Long = 0
    private var hits: Int = 0
    private var misses: Int = 0
    private var puts: Int = 0
    private var evictions: Int = 0

    @Synchronized
    override fun put(bitmap: Bitmap) {
        require(!bitmap.isRecycled) { "Cannot pool recycled bitmap!" }

        val size = bitmap.getAllocationByteCountCompat()

        if (!bitmap.isMutable || size > maxSize || !allowedConfigs.contains(bitmap.config)) {
            log(TAG, Log.VERBOSE) {
                "Rejected bitmap from pool: bitmap: ${strategy.logBitmap(bitmap)}, " +
                        "is mutable: ${bitmap.isMutable}, " +
                        "is greater than max size: ${size > maxSize}" +
                        "is allowed config: ${allowedConfigs.contains(bitmap.config)}"
            }
            bitmap.recycle()
            return
        }

        strategy.put(bitmap)

        puts++
        currentSize += size

        log(TAG, Log.VERBOSE) { "Put bitmap in pool=${strategy.logBitmap(bitmap)}" }
        dump()

        trimToSize(maxSize)
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        val result = getOrNull(width, height, config)
        return result ?: Bitmap.createBitmap(width, height, config)
    }

    override fun getOrNull(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val result = getDirtyOrNull(width, height, config)
        result?.eraseColor(Color.TRANSPARENT)
        return result
    }

    override fun getDirty(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val result = getDirtyOrNull(width, height, config)
        return result ?: Bitmap.createBitmap(width, height, config)
    }

    override fun getDirtyOrNull(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        assertNotHardwareConfig(config)

        val result = strategy.get(width, height, config)
        if (result == null) {
            log(TAG, Log.DEBUG) { "Missing bitmap=${strategy.logBitmap(width, height, config)}" }
            misses++
        } else {
            hits++
            currentSize -= result.getAllocationByteCountCompat()
            normalize(result)
        }

        log(TAG, Log.VERBOSE) { "Get bitmap=${strategy.logBitmap(width, height, config)}" }
        dump()

        return result
    }

    fun clearMemory() {
        log(TAG, Log.DEBUG) { "clearMemory" }
        trimToSize(-1)
    }

    @Synchronized
    fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW until ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            trimToSize(currentSize / 2)
        }
    }

    /**
     * 이 두 값을 설정하면 [Bitmap.createBitmap]에서 반환된 것과 본질적으로 동일한 비트맵이 제공됩니다.
     */
    private fun normalize(bitmap: Bitmap) {
        bitmap.density = Bitmap.DENSITY_NONE
        bitmap.setHasAlpha(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bitmap.isPremultiplied = true
        }
    }

    @Synchronized
    private fun trimToSize(size: Long) {
        while (currentSize > size) {
            val removed = strategy.removeLast()
            if (removed == null) {
                log(TAG, Log.WARN) { "Size mismatch, resetting.\n${computeUnchecked()}" }
                currentSize = 0
                return
            }
            currentSize -= removed.getAllocationByteCountCompat()
            evictions++

            log(TAG, Log.DEBUG) { "Evicting bitmap=${strategy.logBitmap(removed)}" }
            dump()

            removed.recycle()
        }
    }

    private fun assertNotHardwareConfig(config: Bitmap.Config) {
        require(Build.VERSION.SDK_INT < Build.VERSION_CODES.O || config != Bitmap.Config.HARDWARE) { "Cannot create a mutable hardware Bitmap." }
    }

    private fun dump() {
        log(TAG, Log.VERBOSE) { computeUnchecked() }
    }

    private fun computeUnchecked(): String {
        return "Hits=$hits, misses=$misses, puts=$puts, evictions=$evictions, " +
                "currentSize=$currentSize, maxSize=$maxSize, strategy=$strategy"
    }
}