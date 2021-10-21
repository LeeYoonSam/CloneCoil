package com.ys.coil.fetch

import android.graphics.drawable.Drawable
import com.ys.coil.decode.DataSource
import okio.BufferedSource

/**
 * [Fetcher.fetch]의 결과.
 */
sealed class FetchResult

/**
 * 관련 [Decoder]에서 사용되는 원시 [BufferedSource] 결과입니다.
 *
 * @param source [Decoder]에 의해 디코딩될 사용되지 않은 [BufferedSource]입니다.
 * @param mimeType [source]에 대한 선택적 MIME 유형입니다.
 * @param dataSource [source]가 로드된 위치입니다.
 */
data class SourceResult(
    val source: BufferedSource,
    val mimeType: String?,
    val dataSource: DataSource
) : FetchResult()

/**
 * 직접적인 [Drawable] 결과입니다. 데이터를 [BufferedSource]로 변환할 수 없는 경우 [Fetcher]에서 이것을 반환합니다.
 *
 * @param drawable 로드된 [Drawable].
 * @param dataSource [drawable]을 가져온 소스입니다.
 * @param isSampled [drawable]이 샘플링되면 참입니다(즉, 전체 크기로 메모리에 로드되지 않은 경우).
 */
data class DrawableResult(
    val drawable: Drawable,
    val isSampled: Boolean,
    val dataSource: DataSource
) : FetchResult()
