package com.ys.coil.fetch

import com.ys.coil.ImageLoader
import com.ys.coil.request.Options

/**
 * [Fetcher]는 데이터를 [ImageSource] 또는 [Drawable]로 변환합니다.
 *
 * 이를 수행하기 위해 페처는 다음 두 가지 유형 중 하나에 적합합니다.
 *
 * - 데이터를 키로 사용하여 원격 소스(예: 네트워크 또는 디스크)에서 바이트를 가져오고 [ImageSource]로 노출합니다. 예를 들어 [HttpUriFetcher]
 * - 데이터를 직접 읽어 [Drawable]로 변환합니다(예: [BitmapFetcher]).
 */
fun interface Fetcher {

    /**
     * [Factory.create]에서 제공하는 데이터를 가져오거나 'null'을 반환하여 구성 요소 레지스트리의 다음 [Fetcher]에 위임합니다.
     */
    suspend fun fetch(): FetchResult?

    fun interface Factory<T : Any> {

        /**
         * [data]를 가져올 수 있는 [Fetcher]를 반환하거나 이 팩토리가 데이터에 대한 fetcher를 생성할 수 없는 경우 'null'을 반환합니다.
         *
         * @param data 가져올 데이터.
         * @param options 이 요청에 대한 구성 옵션 집합입니다.
         * @param imageLoader 이 요청을 실행하는 [ImageLoader]입니다.
         */
        fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher?
    }
}
