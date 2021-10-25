package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.Px

internal interface BitmapPoolStrategy {

    companion object {
        operator fun invoke(): BitmapPoolStrategy {
            return when {
                SDK_INT >= M -> SizeStrategy()
                SDK_INT >= KITKAT -> SizeConfigStrategy()
                else -> AttributeStrategy()
            }
        }
    }


    /**
     * LRU 캐시에 [Bitmap]을 저장합니다.ㄱㄷㅁ
     */
    fun put(bitmap: Bitmap)

    /**
     * 주어진 속성에 대해 "최고의" [Bitmap]을 얻습니다.
     * 주어진 속성에서 재사용할 수 있는 비트맵이 캐시에 없으면 null을 반환합니다.
     */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * 캐시에서 가장 최근에 사용한 비트맵을 제거하고 반환합니다.
     */
    fun removeLast(): Bitmap?

    /**
     * [bitmap]을 기록
     */
    fun logBitmap(bitmap: Bitmap): String

    /**
     * 주어진 속성을 기록합니다.
     */
    fun logBitmap(@Px width: Int, @Px height: Int, config: Bitmap.Config): String
}