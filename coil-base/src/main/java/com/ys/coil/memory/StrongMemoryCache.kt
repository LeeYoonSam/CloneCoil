package com.ys.coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.collection.lruCache
import com.ys.coil.memory.MemoryCache.Key
import com.ys.coil.memory.MemoryCache.Value
import com.ys.coil.util.allocationByteCountCompat

/** 강력한 참조[Bitmap]를 보유하는 메모리 내 캐시입니다. */
internal interface StrongMemoryCache {

    val size: Int

    val maxSize: Int

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>)

    fun remove(key: Key): Boolean

    fun clearMemory()

    fun trimMemory(level: Int)
}

/** 아무것도 캐시하지 않고 [set]만 [WeakMemoryCache]에 위임하는 [StrongMemoryCache] 구현입니다. */
internal class EmptyStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>) {
        weakMemoryCache.set(key, bitmap, extras, bitmap.allocationByteCountCompat)
    }

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** [LruCache]가 지원하는 [StrongMemoryCache] 구현. */
internal class RealStrongMemoryCache(
    maxSize: Int,
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    private val cache = lruCache<Key, InternalValue>(
        maxSize = maxSize,
        sizeOf = { _, value -> value.size },
        onEntryRemoved = { _, key, oldValue, _ ->
            weakMemoryCache.set(key, oldValue.bitmap, oldValue.extras, oldValue.size)
        }
    )

    override val size get() = cache.size()

    override val maxSize get() = cache.maxSize()

    override val keys get() = cache.snapshot().keys

    override fun get(key: Key): Value? {
        return cache.get(key)?.let { Value(it.bitmap, it.extras) }
    }

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>) {
        val size = bitmap.allocationByteCountCompat
        if (size <= maxSize) {
            cache.put(key, InternalValue(bitmap, extras, size))
        } else {
            // 비트맵이 캐시에 비해 너무 큰 경우 캐시가 지워질 수 있으므로 저장하지 마십시오.
            // 대신 동일한 키가 있는 기존 요소가 있는 경우 제거하고 약한 메모리 캐시에 비트맵을 추가합니다.
            cache.remove(key)
            weakMemoryCache.set(key, bitmap, extras, size)
        }
    }

    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    override fun clearMemory() {
        cache.trimToSize(-1)
    }

    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size / 2)
        }
    }

    private class InternalValue(
        val bitmap: Bitmap,
        val extras: Map<String, Any>,
        val size: Int
    )
}
