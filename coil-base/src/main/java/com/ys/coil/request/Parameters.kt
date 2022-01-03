@file:JvmName("-Parameters")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.ys.coil.request

import com.ys.coil.decode.Decoder
import com.ys.coil.fetch.Fetcher
import com.ys.coil.request.Parameters.Entry
import com.ys.coil.util.mapNotNullValues

/** 사용자 지정 데이터를 [Fetcher] 및 [Decoder]에 전달하는 데 사용할 수 있는 일반 값의 맵입니다. */
class Parameters private constructor(
    private val map: Map<String, Entry>
) : Iterable<Pair<String, Entry>> {

    constructor() : this(emptyMap())

    /** 이 개체의 매개변수 수를 반환합니다. */
    val size: Int @JvmName("size") get() = map.size

    /** [key]에 매핑이 없는 경우 [key] 또는 null과 관련된 값을 반환합니다. */
    fun value(key: String): Any? = map[key]?.value

    /** [key]에 매핑이 없는 경우 [key] 또는 null과 연결된 캐시 키를 반환합니다. */
    fun cacheKey(key: String): String? = map[key]?.cacheKey

    /** [key]에 매핑이 없는 경우 [key] 또는 null과 관련된 항목을 반환합니다. */
    fun entry(key: String): Entry? = map[key]

    /** 이 객체에 매개변수가 없으면 'true'를 반환합니다. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** 값에 대한 키 맵을 반환합니다. */
    fun values(): Map<String, Any?> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapValues { it.value.value }
        }
    }

    /** null이 아닌 캐시 키에 대한 키 맵을 반환합니다. null 캐시 키가 있는 키는 필터링됩니다. */
    fun cacheKeys(): Map<String, String> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapNotNullValues { it.value.cacheKey }
        }
    }

    /** [Parameters]의 항목에 대해 [Iterator]를 반환합니다. */
    override operator fun iterator(): Iterator<Pair<String, Entry>> {
        return map.map { (key, value) -> key to value }.iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Parameters && map == other.map
    }

    override fun hashCode() = map.hashCode()

    override fun toString() = "Parameters(map=$map)"

    fun newBuilder() = Builder(this)

    data class Entry(
        val value: Any?,
        val cacheKey: String?,
    )

    class Builder {

        private val map: MutableMap<String, Entry>

        constructor() {
            map = mutableMapOf()
        }

        constructor(parameters: Parameters) {
            map = parameters.map.toMutableMap()
        }

        /**
         * 매개변수를 설정합니다.
         *
         * @param key 매개변수의 키입니다.
         * @param value 매개변수의 값입니다.
         * @param cacheKey 매개변수의 캐시 키입니다.
         *  null이 아니면 이 값이 요청의 캐시 키에 추가됩니다.
         */
        @JvmOverloads
        fun set(key: String, value: Any?, cacheKey: String? = value?.toString()) = apply {
            map[key] = Entry(value, cacheKey)
        }

        /**
         * 매개변수를 제거합니다.
         *
         * @param key 매개변수의 키입니다.
         */
        fun remove(key: String) = apply {
            map.remove(key)
        }

        /** 새 [Parameters] 인스턴스를 만듭니다. */
        fun build() = Parameters(map.toMap())
    }

    companion object {
        @JvmField val EMPTY = Parameters()
    }
}

/** 이 개체의 매개변수 수를 반환합니다. */
inline fun Parameters.count(): Int = size

/** 집합에 요소가 포함되어 있으면 true를 반환합니다. */
inline fun Parameters.isNotEmpty(): Boolean = !isEmpty()

/** [key]에 매핑이 없는 경우 [key] 또는 null과 관련된 값을 반환합니다. */
inline operator fun Parameters.get(key: String): Any? = value(key)
