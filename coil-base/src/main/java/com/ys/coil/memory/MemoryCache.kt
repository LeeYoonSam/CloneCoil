package com.ys.coil.memory

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.FloatRange
import com.ys.coil.util.Utils
import kotlinx.parcelize.Parcelize

/**
 * 이전에 로드된 이미지의 메모리 내 캐시입니다.
 */
interface MemoryCache {
    /** 캐시의 현재 크기(바이트)입니다. */
    val size: Int

    /** 캐시의 최대 크기(바이트)입니다. */
    val maxSize: Int

    /** The keys present in the cache. */
    val keys: Set<Key>

    /** Get the [Value] associated with [key]. */
    operator fun get(key: Key): Value?

    /** Set the [Value] associated with [key]. */
    operator fun set(key: Key, value: Value)

    /**
     * Remove the [Value] referenced by [key].
     *
     * @return 'true' if [key] was present in the cache. Else, return 'false'.
     */
    fun remove(key: Key): Boolean

    /** Remove all values from the memory cache. */
    fun clear()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)

    /**
     * 메모리 캐시에 있는 이미지의 캐시 키입니다.
     *
     * @param key [Keyer.key]에서 반환된 값(또는 사용자 지정 값)입니다.
     * @param extras 연결된 캐시 값을 동일한 [key]를 가진 다른 값과 구별하는 추가 값입니다.
     * 이 map은 **반드시** 불변으로 처리되어야 하며 수정해서는 안 됩니다.
     */
    @Parcelize
    data class Key(
        val key: String,
        val extras: Map<String, String> = emptyMap(),
    ) : Parcelable

    data class Value(
        val bitmap: Bitmap,
        val extras: Map<String, Any> = emptyMap()
    )

    class Builder(private val context: Context) {
        private var maxSizePercent = Utils.defaultMemoryCacheSizePercent(context)
        private var maxSizeBytes = 0
        private var strongReferencesEnabled = true
        private var weakReferencesEnabled = true

        /**
         * 메모리 캐시의 최대 크기를 이 응용 프로그램의 사용 가능한 메모리에 대한 백분율로 설정합니다.
         */
        fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
            this.maxSizeBytes = 0
            this.maxSizePercent = percent
        }

        /**
         * 메모리 캐시의 최대 크기를 바이트 단위로 설정합니다.
         */
        fun maxSizeBytes(size: Int) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.maxSizePercent = 0.0
            this.maxSizeBytes = size
        }

        /**
         * 이 메모리 캐시에 추가된 값의 강력한 참조 추적을 활성화/비활성화합니다.
         */
        fun strongReferencesEnabled(enable: Boolean) = apply {
            this.strongReferencesEnabled = enable
        }

        /**
         * 이 메모리 캐시에 추가된 값의 약한 참조 추적을 활성화/비활성화합니다. 약한 참조는 메모리 캐시의 현재 크기에 기여하지 않습니다.
         * 이렇게 하면 이미지가 아직 가비지 수집되지 않은 경우 메모리 캐시에서 반환됩니다.
         */
        fun weakReferencesEnabled(enable: Boolean) = apply {
            this.weakReferencesEnabled = enable
        }

        /**
         * 새 [MemoryCache] 인스턴스를 만듭니다.
         */
        fun build(): MemoryCache {
            val weakMemoryCache = if (weakReferencesEnabled) {
                RealWeakMemoryCache()
            } else {
                EmptyWeakMemoryCache()
            }
            val strongMemoryCache = if (strongReferencesEnabled) {
                val maxSize = if (maxSizePercent > 0) {
                    Utils.calculateMemoryCacheSize(context, maxSizePercent)
                } else {
                    maxSizeBytes
                }
                if (maxSize > 0) {
                    RealStrongMemoryCache(maxSize, weakMemoryCache)
                } else {
                    EmptyStrongMemoryCache(weakMemoryCache)
                }
            } else {
                EmptyStrongMemoryCache(weakMemoryCache)
            }
            return RealMemoryCache(strongMemoryCache, weakMemoryCache)
        }
    }
}