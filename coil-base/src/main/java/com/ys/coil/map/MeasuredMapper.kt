package com.ys.coil.map

import com.ys.coil.size.Size
import com.ys.coil.target.Target

/**
 * 유형이 [T]인 데이터를 [V]로 변환하는 인터페이스입니다.
 * [Mapper]와 달리 [MeasuredMapper]는 [Target]이 측정될 때까지 기다려야 합니다.
 * 이로 인해 캐시된 드로어블이 동기적으로 설정되지 않을 수 있습니다.
 *
 * [Target]의 크기를 알 필요가 없다면 [Mapper]를 구현하는 것을 선호합니다.
 *
 * @see Mapper
 */
interface MeasuredMapper<T : Any, V : Any> {
    /**
     * [data] 를 변환할수 있으면 true 를 반환
     */
    fun handles(data: T): Boolean = true

    /**
     * [data] 를 [V] 로 변환
     */
    fun map(data: T, size: Size): V
}