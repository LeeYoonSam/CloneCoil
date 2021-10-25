package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.RequiresApi
import com.ys.coil.collection.GroupedLinkedMap
import com.ys.coil.util.Utils
import com.ys.coil.util.getAllocationByteCountCompat
import java.util.*

/**
 * [Bitmap.reconfigure]에 의존하는 비트맵 재사용 전략.
 *
 * [Bitmap.getAllocationByteCountCompat]를 사용하는 [Bitmap]의 키
 * 이렇게 하면 다른 구성으로 비트맵을 재사용할 수 있으므로 [SizeConfigStrategy]보다 적중률이 향상됩니다.
 *
 * 기술적으로 이 전략에 대한 API는 [KITKAT] 이후로 사용할 수 있지만 프레임워크 버그로 인해 [M]까지 이 전략을 사용해서는 안 됩니다.
 */
@RequiresApi(M)
class SizeStrategy : BitmapPoolStrategy {

    companion object {
        private const val MAX_SIZE_MULTIPLE = 8

        @Suppress("NOTHING_TO_INLINE")
        private inline fun getBitmapString(size: Int) = "[$size]"
    }

    private val groupedMap = GroupedLinkedMap<Int, Bitmap>()
    private val sortedSizes = TreeMap<Int, Int>()

    override fun put(bitmap: Bitmap) {
        val size = bitmap.getAllocationByteCountCompat()
        groupedMap[size] = bitmap

        val current = sortedSizes[size]
        sortedSizes[size] = if (current == null) 1 else current + 1
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        val bestSize = findBestSize(size)

        // 키가 없어도 LRU 풀의 맨 앞으로 이동하도록 get을 수행하십시오.
        val result = groupedMap[bestSize]
        if (result != null) {
            // 재구성하기 전에 decrementBitmapOfSize() 를 호출해야 합니다.
            decrementBitmapOfSize(bestSize, result)
            result.reconfigure(width, height, config)
        }
        return result
    }

    private fun findBestSize(size: Int): Int {
        val possibleSize: Int? = sortedSizes.ceilingKey(size)
        return if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
            possibleSize
        } else {
            size
        }
    }

    private fun decrementBitmapOfSize(size: Int, removed: Bitmap) {
        val current = sortedSizes[size] ?: run {
            throw NullPointerException("Tried to decrement empty size, size: $size, " +
                    "removed: ${logBitmap(removed)}, this: $this")
        }

        if (current == 1) {
            sortedSizes.remove(size)
        } else {
            sortedSizes[size] = current - 1
        }
    }

    override fun removeLast(): Bitmap? {
        val removed = groupedMap.removeLast()
        if (removed != null) {
            decrementBitmapOfSize(removed.getAllocationByteCountCompat(), removed)
        }
        return removed
    }

    override fun logBitmap(bitmap: Bitmap): String {
        return getBitmapString(bitmap.getAllocationByteCountCompat())
    }

    override fun logBitmap(width: Int, height: Int, config: Bitmap.Config): String {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        return getBitmapString(size)
    }

    override fun toString(): String {
        return "SizeStrategy: groupedMap=$groupedMap, sortedSizes=($sortedSizes)"
    }
}
