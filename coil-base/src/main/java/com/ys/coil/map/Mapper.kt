package com.ys.coil.map

import com.ys.coil.fetch.Fetcher
import com.ys.coil.request.Options

/**
 * [T] 유형의 데이터를 [V]로 변환하는 인터페이스입니다.
 *
 * 사용자 정의 데이터 유형을 [Fetcher]가 처리할 수 있는 유형에 매핑하는 데 사용합니다.
 */
interface Mapper<T : Any, V : Any> {

    /**
     * [data] 를 변환할수 있으면 true 를 반환
     */
    fun handles(data: T): Boolean = true

    /**
     * [data] 를 [V] 로 변환
     * 이 매퍼가 [data]를 변환할 수 없으면 'null'을 반환합니다.
     *
     * @param data 변환할 데이터입니다.
     * @param options 이 요청에 대한 옵션입니다.
     */
    fun map(data: T, options: Options): V?
}
