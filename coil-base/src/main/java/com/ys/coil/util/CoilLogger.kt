package com.ys.coil.util

import android.util.Log

/**
 * [Log]에 대한 로깅을 활성화/비활성화하는 싱글톤입니다.
 */
object CoilLogger {

    internal var enabled = false
        private set
    internal var level = Log.DEBUG
        private set

    /**
     * 로깅 활성화/비활성화
     *
     * 이 기능을 활성화 하면 성능이 저하된다. (릴리스 빌드에서는 이 기능을 비활성화)
     *
     * NOTE: Enabling this reduces performance. Additionally, this will log URLs which can contain
     * [PII](https://en.wikipedia.org/wiki/Personal_data). You should **not** enable this in release builds.
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Coil이 기록할 최소 중요도를 설정합니다.
     *
     * @see Log
     */
    @JvmStatic
    fun setLevel(level: Int) {
        require(level in Log.VERBOSE..Log.ASSERT) { "Invalid log level." }
        this.level = level
    }
}