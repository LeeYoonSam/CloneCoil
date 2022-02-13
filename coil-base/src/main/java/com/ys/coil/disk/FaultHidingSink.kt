package com.ys.coil.disk

import okio.Buffer
import okio.ForwardingSink
import okio.IOException
import okio.Sink

/**
 * 기본 싱크가 발생하더라도 [IOException]을 발생시키지 않는 싱크입니다.
 */
internal open class FaultHidingSink(
	delegate: Sink,
	private val onException: (IOException) -> Unit
) : ForwardingSink(delegate) {

	private var hasErrors = false

	override fun write(source: Buffer, byteCount: Long) {
		if (hasErrors) {
			source.skip(byteCount)
			return
		}

		runOrIOException {
			super.write(source, byteCount)
		}
	}

	override fun flush() {
		if (hasErrors) return

		runOrIOException {
			super.flush()
		}
	}

	override fun close() {
		if (hasErrors) return

		runOrIOException {
			super.close()
		}
	}

	private inline fun runOrIOException(runBlock: () -> Unit) {

		try {
			runBlock()
		} catch (e: IOException) {
			hasErrors = true
			onException(e)
		}
	}
}
