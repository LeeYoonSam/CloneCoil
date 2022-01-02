package com.ys.coil.util

import android.util.Log

/**
 * [ImageLoader]에 대한 로깅 인터페이스입니다.
 *
 * @see ImageLoader.Builder.logger
 * @see DebugLogger
 */
interface Logger {

    /**
     * 이 로거가 기록할 최소 수준입니다.
     *
     * @see Log
     */
    var level: Int

    /**
     * [message] 및/또는 [throwable]을 로깅 대상에 씁니다.
     *
     * [priority]는 [level]보다 크거나 같습니다.
     */
    fun log(tag: String, priority: Int, message: String?, throwable: Throwable?)
}
