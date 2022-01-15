package com.ys.coil

import com.ys.coil.decode.Decoder
import com.ys.coil.fetch.Fetcher
import com.ys.coil.fetch.SourceResult
import com.ys.coil.interceptor.Interceptor
import com.ys.coil.key.Keyer
import com.ys.coil.map.Mapper
import com.ys.coil.request.Options
import com.ys.coil.util.forEachIndices
import com.ys.coil.util.toImmutableList

/**
 * [ImageLoader]가 이미지 요청을 수행하는 데 사용하는 모든 구성 요소에 대한 레지스트리입니다.
 *
 * 이 클래스를 사용하여 사용자 정의 [Interceptor], [Mapper], [Keyer],
 * [Fetcher] 및 [Decoder].
 *
 * [ImageLoader]가 이미지 요청을 이행하기 위해 사용하는 모든 구성 요소에 대한 레지스트리.
 *
 * 이 클래스를 사용하여 사용자 정의 [Mapper], [MeasuredMapper], [Fetcher] 및 [Decoder]에 대한 지원을 등록합니다.
 */
class ComponentRegistry private constructor(
    val interceptors: List<Interceptor>,
    val mappers: List<Pair<Mapper<out Any, out Any>, Class<out Any>>>,
    val keyers: List<Pair<Keyer<out Any>, Class<out Any>>>,
    val fetcherFactories: List<Pair<Fetcher.Factory<out Any>, Class<out Any>>>,
    val decoderFactories: List<Decoder.Factory>
) {
    constructor() : this(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    /**
     * 등록된 [mappers]를 사용하여 [data]를 변환합니다.
     *
     * @return The mapped data.
     */
    fun map(data: Any, options: Options): Any {
        var mappedData = data
        mappers.forEachIndices { (mapper, type) ->
            if (type.isAssignableFrom(mappedData::class.java)) {
                (mapper as Mapper<Any, *>).map(mappedData, options)?.let { mappedData = it }
            }
        }
        return mappedData
    }

    /**
     * 등록된 [keyers]를 이용하여 [data]를 문자열 키로 변환합니다.
     *
     * @return 캐시 키 또는 [data]가 캐시되지 않아야 하는 경우 'null'입니다.
     */
    fun key(data: Any, options: Options): String? {
        keyers.forEachIndices { (keyer, type) ->
            if (type.isAssignableFrom(data::class.java)) {
                (keyer as Keyer<Any>).key(data, options)?.let { return it }
            }
        }
        return null
    }

    /**
     * 등록된 [fetcherFactories]를 사용하여 새 [Fetcher]를 만듭니다.
     *
     * @return 첫 번째 요소가 새 [Fetcher]이고 두 번째 요소가 이를 생성한 [fetcherFactories]의 팩토리 인덱스인 [Pair]입니다.
     * [data]에 대해 [Fetcher]를 생성할 수 없으면 'null'을 반환합니다.
     */
    @JvmOverloads
    fun newFetcher(
        data: Any,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0
    ): Pair<Fetcher, Int>? {
        for (index in startIndex until fetcherFactories.size) {
            val (factory, type) = fetcherFactories[index]
            if (type.isAssignableFrom(data::class.java)) {
                val fetcher = (factory as Fetcher.Factory<Any>).create(data, options, imageLoader)
                if (fetcher != null) return fetcher to index
            }
        }
        return null
    }

    /**
     * 등록된 [decoderFactories]를 이용하여 새로운 [Decoder]를 생성합니다.
     *
     * @return 첫 번째 요소가 새로운 [Decoder]이고 두 번째 요소가 이를 생성한 [decoderFactories]의 팩토리 인덱스인 [Pair].
     * [result]에 대해 [Decoder]를 생성할 수 없는 경우 'null'을 반환합니다.
     */
    @JvmOverloads
    fun newDecoder(
        result: SourceResult,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0
    ): Pair<Decoder, Int>? {
        for (index in startIndex until decoderFactories.size) {
            val factory = decoderFactories[index]
            val decoder = factory.create(result, options, imageLoader)
            if (decoder != null) return decoder to index
        }
        return null
    }

    fun newBuilder() = Builder(this)

    class Builder {

        internal val interceptors: MutableList<Interceptor>
        internal val mappers: MutableList<Pair<Mapper<out Any, *>, Class<out Any>>>
        internal val keyers: MutableList<Pair<Keyer<out Any>, Class<out Any>>>
        internal val fetcherFactories: MutableList<Pair<Fetcher.Factory<out Any>, Class<out Any>>>
        internal val decoderFactories: MutableList<Decoder.Factory>

        constructor() {
            interceptors = mutableListOf()
            mappers = mutableListOf()
            keyers = mutableListOf()
            fetcherFactories = mutableListOf()
            decoderFactories = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            interceptors = registry.interceptors.toMutableList()
            mappers = registry.mappers.toMutableList()
            keyers = registry.keyers.toMutableList()
            fetcherFactories = registry.fetcherFactories.toMutableList()
            decoderFactories = registry.decoderFactories.toMutableList()
        }

        /** 사용자 정의 [Mapper]를 추가합니다. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(mapper, T::class.java)

        fun <T : Any> add(mapper: Mapper<T, *>, type: Class<T>) = apply {
            mappers += mapper to type
        }

        /** [Keyer]를 등록합니다. */
        inline fun <reified T : Any> add(keyer: Keyer<T>) = add(keyer, T::class.java)

        /** [Keyer]를 등록합니다. */
        fun <T : Any> add(keyer: Keyer<T>, type: Class<T>) = apply {
            keyers += keyer to type
        }

        /** [Fetcher.Factory]를 등록합니다. */
        inline fun <reified T : Any> add(factory: Fetcher.Factory<T>) = add(factory, T::class.java)

        /** [Fetcher.Factory]를 등록합니다. */
        fun <T : Any> add(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
            fetcherFactories += factory to type
        }

        /** [Decoder.Factory]를 등록합니다. */
        fun add(factory: Decoder.Factory) = apply {
            decoderFactories += factory
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                interceptors.toImmutableList(),
                mappers.toImmutableList(),
                keyers.toImmutableList(),
                fetcherFactories.toImmutableList(),
                decoderFactories.toImmutableList()
            )
        }
    }
}