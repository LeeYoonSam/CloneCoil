package com.ys.coil.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 내부 로깅에서 사용하는 이모티콘
 *
 * 일부 이모티콘은 logcat에 올바르게 표시하기 위해 추가 공간이 필요합니다. 이유를 모르겠습니다. 🤷
 */
internal object Emoji {
    const val BRAIN = "🧠" + " "
    const val FLOPPY = "💾"
    const val CLOUD = "☁️" + " "
    const val CONSTRUCTION = "🏗" + " "
    const val SIREN = "🚨"
}

internal inline fun log(tag: String, priority: Int, lazyMessage: () -> String) {
    if (CoilLogger.enabled && CoilLogger.level <= priority) {
        Log.println(priority, tag, lazyMessage())
    }
}

internal fun log(tag: String, throwable: Throwable) {
    if (CoilLogger.enabled && CoilLogger.level <= Log.ERROR) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        Log.println(Log.ERROR, tag, writer.toString())
    }
}

internal inline fun Logger.log(tag: String, priority: Int, lazyMessage: () -> String) {
    if (level <= priority) {
        log(tag, priority, lazyMessage(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (level <= Log.ERROR) {
        log(tag, Log.ERROR, null, throwable)
    }
}
