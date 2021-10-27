package com.ys.coil.transform

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.collection.arraySetOf
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.fetch.DrawableResult
import com.ys.coil.decode.DecodeResult

/**
 * 이미지의 픽셀 데이터를 변환하기 위한 인터페이스입니다.
 *
 * 참고: 변환은 [DrawableResult.drawable] 또는 [DecodeResult.drawable]이 [BitmapDrawable]인 경우에만 적용됩니다.
 *
 * @see RequestBuilder.transformations
 */
interface Transformation {
    companion object {
        /**
         * [transform]의 입력 및 출력 비트맵에 대한 유효한 비트맵 구성의 화이트리스트입니다.
         */
        internal val VALID_CONFIGS = if (SDK_INT >= O) {
            arraySetOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arraySetOf(Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * 이 변환에 대한 고유 키를 반환합니다.
     *
     * 키는 이 변환의 일부인 매개변수를 포함해야 합니다(예: 크기, 크기, 색상, 반경 등).
     */
    fun key(): String

    /**
     * [Bitmap]에 변형을 적용합니다.
     *
     * 최적의 성능을 위해 이 메서드 내에서 [Bitmap.createBitmap]을 사용하지 마십시오. 대신 제공된 [BitmapPool]을 사용하여 새 [Bitmap]을 가져옵니다.
     * 또한 출력 [Bitmap]을 제외한 모든 Bitmap을 풀로 반환하여 재사용할 수 있도록 해야 합니다.
     *
     * @see BitmapPool.get
     * @see BitmapPool.put
     */
    suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap
}