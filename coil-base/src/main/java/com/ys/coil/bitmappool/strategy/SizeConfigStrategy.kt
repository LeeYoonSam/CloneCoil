package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import com.ys.coil.collection.GroupedLinkedMap
import com.ys.coil.util.Utils
import com.ys.coil.util.getAllocationByteCountCompat
import java.util.*

/**
 * [Bitmap.getAllocationByteCountCompat] 및 [Bitmap.getConfig]에서 반환된 [Bitmap.Config]를 모두 사용하는 [Bitmap] 키입니다.
 *
 * [Bitmap]의 할당 횟수와 구성을 모두 사용하면 더 다양한 [Bitmap]을 안전하게 재사용할 수 있으므로 풀의 적중률이 증가하여 애플리케이션의 성능이 향상됩니다.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class SizeConfigStrategy : BitmapPoolStrategy {

    companion object {
        private const val MAX_SIZE_MULTIPLE = 8

        private val ARGB_8888_IN_CONFIGS: Array<Bitmap.Config> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arrayOf(Bitmap.Config.ARGB_8888)
        }
        private val RGBA_F16_IN_CONFIGS: Array<Bitmap.Config> = ARGB_8888_IN_CONFIGS
        private val RGB_565_IN_CONFIGS: Array<Bitmap.Config> = arrayOf(Bitmap.Config.RGB_565)
        private val ARGB_4444_IN_CONFIGS: Array<Bitmap.Config> = arrayOf(Bitmap.Config.ARGB_4444)
        private val ALPHA_8_IN_CONFIGS: Array<Bitmap.Config> = arrayOf(Bitmap.Config.ALPHA_8)

        @Suppress("NOTHING_TO_INLINE")
        private inline fun getBitmapString(size: Int, config: Bitmap.Config) = "[$size]($config)"
    }

    private val groupedMap = GroupedLinkedMap<Key, Bitmap>()
    private val sortedSizes = HashMap<Bitmap.Config?, NavigableMap<Int, Int>>()

    override fun put(bitmap: Bitmap) {
        val key = Key(bitmap.getAllocationByteCountCompat(), bitmap.config)
        groupedMap[key] = bitmap

        val sizes = getSizesForConfig(bitmap.config)
        val current = sizes[key.size]
        sizes[key.size] = if (current == null) 1 else current + 1
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        val bestKey = findBestKey(size, config)

        val result = groupedMap[bestKey]
        if (result != null) {
            // 재구성하기 전에 decrementBitmapOfSize를 호출해야 합니다.
            decrementBitmapOfSize(bestKey.size, result)
            result.reconfigure(width, height, config)
        }
        return result
    }

    private fun findBestKey(size: Int, config: Bitmap.Config): Key {
        for (possibleConfig in getInConfigs(config)) {
            val possibleSize: Int? = getSizesForConfig(possibleConfig).ceilingKey(size)
            if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
                return Key(possibleSize, possibleConfig)
            }
        }
        return Key(size, config)
    }

    override fun removeLast(): Bitmap? {
        val removed = groupedMap.removeLast()
        if (removed != null) {
            decrementBitmapOfSize(removed.getAllocationByteCountCompat(), removed)
        }
        return removed
    }

    private fun decrementBitmapOfSize(size: Int, removed: Bitmap) {
        val config = removed.config
        val sizes = getSizesForConfig(config)
        val current = checkNotNull(sizes[size]) {
            "Tried to decrement empty size, size: $size, removed: ${logBitmap(removed)}, this: $this"
        }

        if (current == 1) {
            sizes.remove(size)
        } else {
            sizes[size] = current - 1
        }
    }

    private fun getSizesForConfig(config: Bitmap.Config?): NavigableMap<Int, Int> {
        return sortedSizes.getOrPut(config) { TreeMap() }
    }

    private fun getInConfigs(requested: Bitmap.Config): Array<Bitmap.Config> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Bitmap.Config.RGBA_F16 == requested -> RGBA_F16_IN_CONFIGS
            requested == Bitmap.Config.ARGB_8888 -> ARGB_8888_IN_CONFIGS
            requested == Bitmap.Config.RGB_565 -> RGB_565_IN_CONFIGS
            requested == Bitmap.Config.ARGB_4444 -> ARGB_4444_IN_CONFIGS
            requested == Bitmap.Config.ALPHA_8 -> ALPHA_8_IN_CONFIGS
            else -> arrayOf(requested)
        }
    }

    override fun logBitmap(bitmap: Bitmap): String {
        return getBitmapString(bitmap.getAllocationByteCountCompat(), bitmap.config)
    }

    override fun logBitmap(@Px width: Int, @Px height: Int, config: Bitmap.Config): String {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        return getBitmapString(size, config)
    }

    override fun toString() = buildString {
        append("SizeConfigStrategy: groupedMap=")
        append(groupedMap)
        append(", sortedSizes=(")

        sortedSizes.forEach { (key, value) ->
            append(key)
            append("[")
            append(value)
            append("], ")
        }

        if (sortedSizes.isNotEmpty()) {
            replace(length - 2, length, "")
        }
        append(")")
    }

    private data class Key(
        val size: Int,
        val config: Bitmap.Config
    ) {
        override fun toString() = getBitmapString(size, config)
    }
}
