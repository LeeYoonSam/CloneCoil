package com.ys.coil.decode

import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.Size
import okio.BufferedSource

/**
 * [BufferedSource]를 [Drawable]로 변환합니다.
 *
 * 이 인터페이스를 사용하여 사용자 지정 파일 형식(예: GIF, SVG, TIFF 등)에 대한 지원을 추가합니다.
 */
interface Decoder {

    /**
     * 이 디코더가 [source] 디코딩을 지원하면 true를 반환합니다.
     *
     * 구현은 소스를 소비하지 **않으면 안됩니다**. 그러면 [handles] 및 [decode]에 대한 후속 호출이 실패할 수 있습니다.
     *
     * [BufferedSource.peek], [BufferedSource.rangeEquals] 또는 기타 비파괴적 방법을 사용하여 확인하는 것을 선호합니다.
     * 헤더 바이트 또는 기타 마커가 있는 경우. 구현은 [mimeType]에 의존할 수도 있습니다.
     * 그러나 정확하지는 않습니다(예: .png로 끝나지만 .jpg로 인코딩된 파일).
     *
     * @param source 읽을 [BufferedSource]입니다.
     * @param mimeType [source]에 대한 선택적 MIME 유형입니다.
     */
    fun handles(source: BufferedSource, mimeType: String?): Boolean

    /**
     * [source]를 [Drawable]로 디코딩합니다.
     *
     * 참고: 구현은 완료되면 [source]를 닫을 책임이 있습니다.
     *
     * @param pool [Bitmap] 인스턴스를 요청하는 데 사용할 수 있는 [BitmapPool].
     * @param source 읽을 [BufferedSource]입니다.
     * @param size 이미지에 대해 요청된 크기입니다.
     * @param options 이 요청에 대한 구성 옵션 집합입니다.
     */
    suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult
}