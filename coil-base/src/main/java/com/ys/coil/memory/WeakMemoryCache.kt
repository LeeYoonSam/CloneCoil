package com.ys.coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.ys.coil.memory.MemoryCache.Key
import com.ys.coil.memory.MemoryCache.Value
import com.ys.coil.util.firstNotNullOfOrNullIndices
import com.ys.coil.util.identityHashCode
import com.ys.coil.util.removeIfIndices
import java.lang.ref.WeakReference

/**
 * [Bitmap]에 대한 약한 참조를 보유하는 메모리 내 캐시입니다.
 *
 * 비트맵은 [StrongMemoryCache]에서 제거될 때 [WeakMemoryCache]에 추가됩니다.
 */
internal interface WeakMemoryCache {

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int)

    fun remove(key: Key): Boolean

    fun clearMemory()

    fun trimMemory(level: Int)
}

/** 참조를 보유하지 않는 [WeakMemoryCache] 구현입니다. */
internal class EmptyWeakMemoryCache : WeakMemoryCache {

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int) {}

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** [HashMap]이 지원하는 [WeakMemoryCache] 구현입니다. */
internal class RealWeakMemoryCache : WeakMemoryCache {

    @VisibleForTesting internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys @Synchronized get() = cache.keys.toSet()

    @Synchronized
    override fun get(key: Key): Value? {
        val values = cache[key] ?: return null

        // 수집되지 않은 첫 번째 비트맵을 찾습니다.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.bitmap.get()?.let { Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    @Synchronized
    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // 크기 내림차순으로 정렬된 목록에 값을 삽입합니다.
        run {
            val identityHashCode = bitmap.identityHashCode
            val newValue = InternalValue(identityHashCode, WeakReference(bitmap), extras, size)
            for (index in values.indices) {
                val value = values[index]
                if (size >= value.size) {
                    if (value.identityHashCode == identityHashCode && value.bitmap.get() === bitmap) {
                        values[index] = newValue
                    } else {
                        values.add(index, newValue)
                    }
                    return@run
                }
            }
            values += newValue
        }

        cleanUpIfNecessary()
    }

    @Synchronized
    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    @Synchronized
    override fun clearMemory() {
        operationsSinceCleanUp = 0
        cache.clear()
    }

    @Synchronized
    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_RUNNING_LOW && level != TRIM_MEMORY_UI_HIDDEN) {
            cleanUp()
        }
    }

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    /** 캐시에서 역참조된 비트맵을 제거합니다. */
    @VisibleForTesting
    internal fun cleanUp() {
        operationsSinceCleanUp = 0

        // 참조가 수집된 모든 값을 제거합니다.
        val iterator = cache.values.iterator()
        while (iterator.hasNext()) {
            val list = iterator.next()

            if (list.count() <= 1) {
                // 일반적으로 목록에는 1개의 항목만 포함됩니다. 여기에서 이 경우를 최적의 방법으로 처리하십시오.
                if (list.firstOrNull()?.bitmap?.get() == null) {
                    iterator.remove()
                }
            } else {
                // 값 목록을 반복하고 수집된 개별 항목을 삭제합니다.
                list.removeIfIndices { it.bitmap.get() == null }

                if (list.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    @VisibleForTesting
    internal class InternalValue(
        val identityHashCode: Int,
        val bitmap: WeakReference<Bitmap>,
        val extras: Map<String, Any>,
        val size: Int
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }
}
