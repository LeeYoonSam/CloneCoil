package com.ys.coil.target

import android.view.View

/**
 * 연관된 [View]가 있는 [Target].
 *
 * 로드된 [Drawable]이 하나의 [View]와 함께 사용되는 경우 [Target]보다 이것을 선호하십시오.
 *
 * [Target]과 달리 [ViewTarget]은 수명 주기 메서드를 여러 번 호출할 수 있습니다.
 *
 * 선택적으로 [ViewTarget]은 [LifecycleObserver]로 선언될 수 있습니다.
 * 요청이 시작되면 자동으로 등록되고 요청이 삭제되면 등록이 취소됩니다.
 */
interface ViewTarget<T: View>: Target {
    /**
     *
     *
     */
    val view: T
}
