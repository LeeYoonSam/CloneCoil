package com.ys.coil.fetch

import android.graphics.Bitmap
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.Options
import com.ys.coil.size.Size

interface Fetcher<T : Any> {

    /**
     * [data]를 로드할 수 있으면 true를 반환합니다.
     */
    fun handles(data: T): Boolean = true

    /**
     * [data]에 대한 메모리 캐시 키를 계산합니다.
     *
     * 캐시 키가 동일한 항목은 [MemoryCache]에서 동일하게 취급됩니다.
     *
     * null을 반환하면 [fetch] 결과가 메모리 캐시에 추가되지 않습니다.
     */
    fun key(data: T): String? = null

    /**
     * [data]를 메모리에 로드합니다. 필요한 모든 가져오기 작업을 수행합니다.
     *
     * @param pool [Bitmap] 인스턴스를 요청하는 데 사용할 수 있는 [BitmapPool]입니다.
     * @param data 로드할 데이터입니다.
     * @param size 이미지에 대해 요청된 크기입니다.
     * @param options 이 요청에 대한 구성 옵션 집합입니다.
     */
    suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult
}