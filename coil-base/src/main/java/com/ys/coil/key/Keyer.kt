package com.ys.coil.key

import com.ys.coil.request.Options

/**
 * [T] 유형의 데이터를 메모리 캐시의 문자열 키로 변환하는 인터페이스입니다.
 */
fun interface Keyer<T: Any> {

	/**
	 * [data]를 문자열 키로 변환합니다. 이 키어가 [data]를 변환할 수 없으면 'null'을 반환합니다.
	 *
	 * @param data 변환할 데이터입니다.
	 * @param options 이 요청에 대한 옵션입니다.
	 */
	fun key(data: T, options: Options): String?
}