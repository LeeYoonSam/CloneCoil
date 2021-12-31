package com.ys.coil

import com.ys.coil.annotation.BuilderMarker
import com.ys.coil.decode.Decoder
import com.ys.coil.fetch.Fetcher
import com.ys.coil.map.Mapper
import com.ys.coil.map.MeasuredMapper
import com.ys.coil.util.MultiList
import com.ys.coil.util.MultiMutableList
import okio.BufferedSource

/**
 * [ImageLoader]가 이미지 요청을 이행하기 위해 사용하는 모든 구성 요소에 대한 레지스트리.
 *
 * 이 클래스를 사용하여 사용자 정의 [Mapper], [MeasuredMapper], [Fetcher] 및 [Decoder]에 대한 지원을 등록합니다.
 */
class ComponentRegistry private constructor(
    private val mappers: MultiList<Class<*>, Mapper<*, *>>,
    private val measuredMappers: MultiList<Class<*>, MeasuredMapper<*, *>>,
    private val fetchers: MultiList<Class<*>, Fetcher>,
    private val decoders: List<Decoder>
) {

    companion object {
        /**
         * 새 [ComponentRegistry] 인스턴스를 만듭니다.
         *
         * 예시:
         * ```
         * val registry = ComponentRegistry {
         *     add(GifDecoder())
         * }
         * ```
         */
        inline operator fun invoke(
            builder: Builder.() -> Unit = {}
        ): ComponentRegistry = Builder().apply(builder).build()

        /** 새 [ComponentRegistry] 인스턴스를 만듭니다.*/
        inline operator fun invoke(
            registry: ComponentRegistry,
            builder: Builder.() -> Unit = {}
        ): ComponentRegistry = Builder(registry).apply(builder).build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMapper(data: T): Mapper<T, *>? {
        val result = mappers.find { (type, converter) ->
            type.isAssignableFrom(data::class.java) && (converter as Mapper<Any, *>).handles(data)
        }
        return result?.second as Mapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMeasuredMapper(data: T): MeasuredMapper<T, *>? {
        val result = measuredMappers.find { (type, converter) ->
            type.isAssignableFrom(data::class.java) && (converter as MeasuredMapper<Any, *>).handles(data)
        }
        return result?.second as MeasuredMapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> requireFetcher(data: T): Fetcher {
        val result = fetchers.find { (type, loader) ->
            type.isAssignableFrom(data::class.java)
        }
        checkNotNull(result) { "Unable to fetch data. No fetcher supports: $data" }
        return result.second as Fetcher
    }

    fun <T : Any> requireDecoder(
        data: T,
        source: BufferedSource,
        mimeType: String?
    ): Decoder {
        val decoder = decoders.find { it.handles(source, mimeType) }
        return checkNotNull(decoder) { "Unable to decode data. No decoder supports: $data" }
    }

    fun newBuilder(): Builder = Builder(this)

    @BuilderMarker
    class Builder {

        private val mappers: MultiMutableList<Class<*>, Mapper<*, *>>
        private val measuredMappers: MultiMutableList<Class<*>, MeasuredMapper<*, *>>
        private val fetchers: MultiMutableList<Class<*>, Fetcher>
        private val decoders: MutableList<Decoder>

        constructor() {
            mappers = mutableListOf()
            measuredMappers = mutableListOf()
            fetchers = mutableListOf()
            decoders = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            mappers = registry.mappers.toMutableList()
            measuredMappers = registry.measuredMappers.toMutableList()
            fetchers = registry.fetchers.toMutableList()
            decoders = registry.decoders.toMutableList()
        }

        /** 사용자 정의 [Mapper]를 추가합니다. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(T::class.java, mapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, mapper: Mapper<T, *>) = apply {
            mappers += type to mapper
        }

        /** 사용자 정의 [MeasuredMapper]를 추가합니다. */
        inline fun <reified T : Any> add(measuredMapper: MeasuredMapper<T, *>) = add(T::class.java, measuredMapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, measuredMapper: MeasuredMapper<T, *>) = apply {
            measuredMappers += type to measuredMapper
        }

        // /** 사용자 정의 [Fetcher]를 추가합니다. */
        // inline fun <reified T : Any> add(fetcher: Fetcher<T>) = add(T::class.java, fetcher)
        //
        // @PublishedApi
        // internal fun <T : Any> add(type: Class<T>, fetcher: Fetcher<T>) = apply {
        //     fetchers += type to fetcher
        // }

        /** 사용자 정의 [Decoder]를 추가합니다. */
        fun add(decoder: Decoder) = apply {
            decoders += decoder
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                mappers = mappers,
                measuredMappers = measuredMappers,
                fetchers = fetchers,
                decoders = decoders
            )
        }
    }
}