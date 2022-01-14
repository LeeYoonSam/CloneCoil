package com.ys.coil.transform

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.RGBA_F16
import android.graphics.drawable.BitmapDrawable
import com.ys.coil.decode.DecodeResult
import com.ys.coil.fetch.DrawableResult
import com.ys.coil.size.Size

/**
 * 이미지의 픽셀 데이터를 변환하기 위한 인터페이스입니다.
 *
 * 참고: [DrawableResult.drawable] 또는 [DecodeResult.drawable]이 [BitmapDrawable]이 아닌 경우,
 * 하나로 변환됩니다. 이렇게 하면 애니메이션 드로어블이 애니메이션의 첫 번째 프레임만 그리게 됩니다.
 *
 * @ImageRequest.Builder.transformations 참조
 */
interface Transformation {

    /**
     * 이 변환에 대한 고유한 캐시 키입니다.
     *
     * 키는 이미지 요청의 메모리 캐시 키에 추가되며 이 변환의 일부인 매개변수(예: 크기, 크기, 색상, 반경 등)를 포함해야 합니다.
     */
    val cacheKey: String

    /**
     * [input]에 변형을 적용하고 변형된 [Bitmap]을 반환합니다.
     *
     * @param input 변환할 입력 [Bitmap]입니다.
     * 구성은 항상 [ARGB_8888] 또는 [RGBA_F16]입니다.
     * @param size 이미지 요청의 크기입니다.
     * @return 변환된 [Bitmap].
     */
    suspend fun transform(input: Bitmap, size: Size): Bitmap
}