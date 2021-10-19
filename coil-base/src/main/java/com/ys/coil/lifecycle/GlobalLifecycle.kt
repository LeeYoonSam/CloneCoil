package com.ys.coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

/**
 * 항상 재개되고 절대 파괴되지 않는 [Lifecycle] 구현.
 *
 * 이것은 [Request.lifecycle]이 더 긴밀한 범위의 [Lifecycle]을 찾을 수 없는 경우에 대비하여 사용됩니다.
 */
internal object GlobalLifecycle : Lifecycle() {

    override fun addObserver(observer: LifecycleObserver) {}

    override fun removeObserver(observer: LifecycleObserver) {}

    override fun getCurrentState() = State.RESUMED
}