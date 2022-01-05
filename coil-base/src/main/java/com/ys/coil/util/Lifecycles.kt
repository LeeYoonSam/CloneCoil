package com.ys.coil.util

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

/** 관찰자를 제거했다가 다시 추가하여 모든 수명 주기 콜백이 호출되도록 합니다. */
@MainThread
internal fun Lifecycle.removeAndAddObserver(observer: LifecycleObserver) {
	removeObserver(observer)
	addObserver(observer)
}