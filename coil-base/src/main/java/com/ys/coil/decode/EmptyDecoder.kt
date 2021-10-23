package com.ys.coil.decode

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.Size
import com.ys.coil.util.closeQuietly
import okio.BufferedSource
import okio.blackholeSink

/**
 * 소스를 소진하고 하드코딩된 빈 결과를 반환하는 [Decoder]입니다.
 *
 * 참고: **이 항목을 [ComponentRegistry]에 등록하지 마십시오**. 디스크 전용 사전 로드 요청에 자동으로 사용됩니다.
 */
internal object EmptyDecoder : Decoder {

    private val result = DecodeResult(
        drawable = ColorDrawable(Color.TRANSPARENT),
        isSampled = false
    )

    private val sink = blackholeSink()

    // 잘못된 사용을 방지하려면 이것을 false로 하드코딩
    override fun handles(source: BufferedSource, mimeType: String?) = false

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        source.readAll(sink)
        source.closeQuietly()
        return result
    }
}