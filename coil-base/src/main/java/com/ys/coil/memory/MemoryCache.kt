package com.ys.coil.memory

import android.content.ComponentCallbacks2.*
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import com.ys.coil.util.getAllocationByteCountCompat
import com.ys.coil.util.log

/**
 * 최근에 메모리에 로드된 [Bitmap]에 대한 LRU 캐시입니다.
 */
internal class MemoryCache(
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int
) {

    companion object {
        private const val TAG = "MemoryCache"
    }

    private val cache = object : LruCache<String, Value>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Value,
            newValue: Value?
        ) = referenceCounter.decrement(oldValue.bitmap)

        override fun sizeOf(key: String, value: Value) = value.size
    }

    operator fun get(key: String): Value? = cache.get(key)

    fun set(key: String, value: Bitmap, isSampled: Boolean) {
        /*
        비트맵이 캐시에 비해 너무 크면 저장을 시도하지 마십시오.
        그렇게 하면 캐시가 지워집니다.
        대신 동일한 키가 있는 기존 요소가 있으면 제거하십시오.
        */
        val size = value.getAllocationByteCountCompat()
        if (size > maxSize()) {
            cache.remove(key)
            return
        }

        referenceCounter.increment(value)
        cache.put(key, Value(value, isSampled, size))
    }

    fun size(): Int = cache.size()

    fun maxSize(): Int = cache.maxSize()

    fun clearMemory() {
        log(TAG, Log.DEBUG) { "clearMemory" }
        cache.trimToSize(-1)
    }

    fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size() / 2)
        }
    }

    data class Value(
        val bitmap: Bitmap,
        val isSampled: Boolean,
        val size: Int
    )
}