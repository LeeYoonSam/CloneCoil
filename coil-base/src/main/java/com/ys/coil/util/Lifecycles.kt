package com.ys.coil.util

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** [Lifecycle.getCurrentState]가 [STARTED] 이상이 될 때까지 일시 중단 */
@MainThread
internal suspend inline fun Lifecycle.awaitStarted() {
	// 빠른 경로: 이미 시작했습니다.
	if (currentState.isAtLeast(STARTED)) return

	// 느린 경로: 시작할 때까지 수명 주기를 관찰합니다.
	observeStarted()
}

/** 컴파일러 버그로 인해 '인라인'될 수 없습니다. 이 버그를 방지하는 테스트가 있습니다. */
@MainThread
internal suspend fun Lifecycle.observeStarted() {
	var observer: LifecycleObserver? = null
	try {
		suspendCancellableCoroutine<Unit> { continuation ->
			observer = object : DefaultLifecycleObserver {
				override fun onStart(owner: LifecycleOwner) {
					continuation.resume(Unit)
				}
			}
			addObserver(observer!!)
		}
	} finally {
		// 'observer' will always be null if this method is marked as 'inline'.
		observer?.let(::removeObserver)
	}
}

/** 관찰자를 제거했다가 다시 추가하여 모든 수명 주기 콜백이 호출되도록 합니다. */
@MainThread
internal fun Lifecycle.removeAndAddObserver(observer: LifecycleObserver) {
	removeObserver(observer)
	addObserver(observer)
}
