package com.ys.coil.target

import android.view.View
import androidx.annotation.MainThread
import kotlin.annotation.Target

/**
 * [Bitmap] 풀링을 지원하는 [ViewTarget]입니다.
 *
 * [PoolableViewTarget]을 구현하면 이 대상이 비트맵 풀링으로 선택됩니다. 이를 통해 Coil은 [Bitmap]을 재사용할 수 있습니다.
 * 이 대상에 지정되어 성능이 향상됩니다.
 *
 * 비트맵 풀링을 선택 해제하려면 대신 [ViewTarget]을 구현하세요.
 *
 * [PoolableViewTarget]을 구현하려면 이전 [Drawable] 사용을 즉시 중지해야 합니다.
 * 다음 [PoolableViewTarget] 수명 주기 메서드가 호출됩니다.
 * [Target.onStart], [Target.onSuccess], [Target.onError], [PoolableViewTarget.onClear].
 *
 * 예를 들어 [PoolableViewTarget]은 [Target.onStart]의 플레이스홀더 드로어블 사용을 중지해야 합니다.
 * [Target.onSuccess]가 호출되는 즉시.
 *
 * 다음 수명 주기 메서드가 호출된 후 이전 [Drawable]을 계속 사용하면 렌더링 문제가 발생할 수 있습니다.
 * 예외를 throw합니다.
 *
 * @see ViewTarget
 * @see ImageViewTarget
 */
interface PoolableViewTarget<T: View>: ViewTarget<T> {
    /**
     * 현재 Drawable을 더 이상 사용할 수 없을 때 호출됩니다. 대상은 **반드시** 현재 드로어블 사용을 중지해야 합니다.
     *
     * 실제로 이것은 뷰가 분리되거나 파괴되려고 할 때만 호출됩니다.
     */
    @MainThread
    fun onClear()
}