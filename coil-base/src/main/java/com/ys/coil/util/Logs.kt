package com.ys.coil.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 내부 로깅에서 사용하는 이모티콘
 *
 * TODO: Some emojis require an extra space to display correctly in the logs. Figure out why.
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