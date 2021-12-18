package com.ys.coil.decode

import android.graphics.drawable.Drawable
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.Fetcher
import com.ys.coil.fetch.SourceResult
import okio.BufferedSource

/**
 * [Decoder]는 [SourceResult]를 [Drawable]로 변환합니다.
 *
 * 이 인터페이스를 사용하여 사용자 지정 파일 형식(예: GIF, SVG, TIFF 등)에 대한 지원을 추가합니다.
 */
fun interface DecoderNew {

    /**
     * [Factory.create]에서 제공하는 [SourceResult]를 디코딩하거나 'null'을 반환하여 component registry 의 다음 [Decoder]에 위임합니다.
     */
    suspend fun decode(): DecodeResult?

    fun interface Factory {

        /**
         * 이 Factory 에서 소스에 대한 디코더를 생성할 수 없는 경우 [result] 또는 'null'을 디코딩할 수 있는 [DecoderNew]를 반환합니다.
         *
         * 구현은 [result]의 [ImageSource]를 사용 **해서는 안 됩니다**. 이는 후속 디코더에 대한 호출이 실패할 수 있기 때문입니다. [ImageSource]는 [decode]에서만 사용해야 합니다.
         *
         * [BufferedSource.peek], [BufferedSource.rangeEquals] 또는 기타 비파괴 방법을 사용하여 헤더 바이트 또는 기타 마커가 있는지 확인하는 것을 선호합니다.
         * 구현은 [SourceResult.mimeType]에 의존할 수도 있지만 정확하다는 보장은 없습니다(예: .png로 끝나지만 .jpg로 인코딩된 파일).
         *
         * @param result [Fetcher]의 결과입니다.
         * @param options 이 요청에 대한 구성 옵션 세트.
         * @param imageLoader 이 요청을 실행하는 [ImageLoader]입니다.
         */
        fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): DecoderNew?
    }
}