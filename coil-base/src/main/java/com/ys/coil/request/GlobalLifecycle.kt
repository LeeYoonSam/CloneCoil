package com.ys.coil.request

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 항상 재개되고 절대 소멸되지 않는 [Lifecycle] 구현입니다.
 *
 * 이는 [getLifecycle]이 더 긴밀한 범위의 [Lifecycle]을 찾을 수 없는 경우 대체 수단으로 사용됩니다.
 */
internal object GlobalLifecycle : Lifecycle() {

	private val owner = LifecycleOwner { this }

	override fun addObserver(observer: LifecycleObserver) {
		require(observer is DefaultLifecycleObserver) {
			"$observer must implement androidx.lifecycle.DefaultLifecycleObserver."
		}

		// 수명 주기 메서드를 순서대로 호출하고 관찰자에 대한 참조를 보유하지 않습니다.
		observer.onCreate(owner)
		observer.onStart(owner)
		observer.onResume(owner)
	}

	override fun removeObserver(observer: LifecycleObserver) {}

	override fun getCurrentState() = State.RESUMED

	override fun toString() = "com.ys.coil.request.GlobalLifecycle"
}