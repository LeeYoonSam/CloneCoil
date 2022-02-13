package com.ys.coil.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Android의 [Log]에 쓰는 [Logger] 구현입니다.
 *
 * 참고: 릴리스 빌드에서 이것을 활성화하면 **안 됩니다**. 이것을 [ImageLoader]에 추가하면 성능이 저하됩니다.
 * 또한 [PII](https://en.wikipedia.org/wiki/Personal_data)를 포함할 수 있는 URL을 기록합니다.
 */
class DebugLogger @JvmOverloads constructor(level: Int = Log.DEBUG) : Logger {

	override var level = level
		set(value) {
			assertValidLevel(level)
			field = value
		}

	init {
		assertValidLevel(level)
	}

	override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
		if (message != null) {
			Log.println(priority, tag, message)
		}

		if (throwable != null) {
			val writer = StringWriter()
			throwable.printStackTrace(PrintWriter(writer))
			Log.println(priority, tag, writer.toString())
		}
	}

	private fun assertValidLevel(value: Int) {
		require(value in Log.VERBOSE..Log.ASSERT) { "Invalid log level: $value" }
	}
}
