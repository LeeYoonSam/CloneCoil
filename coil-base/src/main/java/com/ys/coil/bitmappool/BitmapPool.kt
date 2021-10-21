package com.ys.coil.bitmappool

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.Px

/**
 * 호출자가 [Bitmap] 개체를 재사용할 수 있도록 하는 개체 풀입니다.
 */
interface BitmapPool {

    /**
     * 재사용할 수 있고 풀이 적합할 수 있는 경우 지정된 [Bitmap]을 풀에 추가합니다.
     * 그렇지 않으면 이 메서드는 Bitmap에서 [Bitmap.recycle]을 호출하여 폐기합니다.
     *
     * 호출자는 이 메서드를 호출한 후 Bitmap을 계속 사용 해서는 **안됩니다**.
     */
    fun put(bitmap: Bitmap)

    /**
     * 정확히 주어진 너비, 높이 및 구성의 [Bitmap]을 반환하고 투명한 픽셀만 포함합니다.
     *
     * 요청된 속성을 가진 비트맵이 풀에 없으면 새 비트맵이 할당됩니다.
     *
     * 이 방법은 [Bitmap]의 모든 픽셀을 지우기 때문에 이 방법은 약간 느립니다.
     * [getDirty]보다. [BitmapFactory]에서 사용하기 위해 [Bitmap]을 얻는 경우
     * 또는 [Bitmap]의 모든 픽셀이 항상 덮어쓰이거나 지워지는 다른 경우,
     * [getDirty]가 더 빠릅니다. 확실하지 않은 경우 이 방법을 사용하여 정확성을 확인하십시오.
     */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * 풀에 사용 가능한 비트맵이 포함되어 있지 않으면 null이 반환된다는 점을 제외하면 [get]과 동일합니다.
     */
    fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * 반환된 [Bitmap]이 지워지지 **않을 수 있고** 임의의 데이터가 포함될 수 있다는 점을 제외하고 [get]과 동일합니다.
     *
     * 요청된 속성을 가진 비트맵이 풀에 없으면 새 비트맵이 할당됩니다.
     *
     * 이 방법이 [BitmapPool.get]보다 약간 더 효율적이지만 호출자가 새 데이터를 쓰기 전에
     * [Bitmap]을 완전히 지울 것이라고 확신하는 경우에만 주의해서 사용해야 합니다.
     */
    fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * 풀에 사용 가능한 비트맵이 포함되어 있지 않으면 null이 반환된다는 점을 제외하면 [getDirty]와 동일합니다.
     */
    fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
}