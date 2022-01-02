package com.ys.coil.util

/** 테스트를 지원하기 위한 [System.currentTimeMillis]에 대한 간단한 래퍼. */
internal object Time {

    private var provider: () -> Long = System::currentTimeMillis

    fun currentMillis() = provider()

    fun setCurrentMillis(currentMillis: Long) {
        provider = { currentMillis }
    }

    fun reset() {
        provider = System::currentTimeMillis
    }
}
