package com.ys.coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

class FakeLifecycle : Lifecycle() {

    val observers = mutableListOf<LifecycleObserver>()

    var state: State = State.INITIALIZED

    override fun addObserver(observer: LifecycleObserver) {
        observers += observer
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    override fun getCurrentState() = state
}