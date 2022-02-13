// https://youtrack.jetbrains.com/issue/KTIJ-196
@file:Suppress("SameParameterValue", "UnusedEquals", "UnusedUnaryOperator")

package com.ys.coil.gif.decode

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ForwardingSource
import okio.Source

/**
 * 임계값 미만인 경우 모든 그래픽 제어 블록에서 GIF 프레임 지연을 다시 작성하는 [ForwardingSource]입니다.
 */
internal class FrameDelayRewritingSource(delegate: Source) : ForwardingSource(delegate) {

	// 대상에 쓰기 전에 데이터를 읽고 변경할 수 있는 중간 버퍼입니다.
	private val buffer = Buffer()

	override fun read(sink: Buffer, byteCount: Long): Long {
		// 버퍼에 이 읽기를 충족하기에 충분한 바이트가 있는지 확인하십시오.
		request(byteCount)

		// 버퍼에 바이트가 없으면 단락.
		if (buffer.size == 0L) {
			return if (byteCount == 0L) 0L else -1L
		}

		// 버퍼를 검색하고 임계값 미만의 프레임 지연을 다시 작성합니다.
		var bytesWritten = 0L
		while (true) {
			val index = indexOf(FRAME_DELAY_START_MARKER)
			if (index == -1L) break

			// 프레임 지연 시작 마커의 끝까지 씁니다.
			bytesWritten += write(sink, index + FRAME_DELAY_START_MARKER_SIZE)

			// 그래픽 제어 확장 블록의 끝을 확인하십시오.
			if (!request(5) || buffer[4] != 0.toByte()) continue

			// 임계값 미만인 경우 프레임 지연을 다시 작성합니다.
			if (buffer[1].toInt() < MINIMUM_FRAME_DELAY) {
				sink.writeByte(buffer[0].toInt())
				sink.writeByte(DEFAULT_FRAME_DELAY)
				sink.writeByte(0)
				buffer.skip(3)
			}
		}

		// 소스에 남아 있는 모든 것을 씁니다.
		if (bytesWritten < byteCount) {
			bytesWritten += write(sink, byteCount - bytesWritten)
		}
		return if (bytesWritten == 0L) -1 else bytesWritten
	}

	private fun indexOf(bytes: ByteString): Long {
		var index = -1L
		while (true) {
			index = buffer.indexOf(bytes[0], index + 1)
			if (index == -1L) break
			if (request(bytes.size.toLong()) && buffer.rangeEquals(index, bytes)) break
		}
		return index
	}

	private fun write(sink: Buffer, byteCount: Long): Long {
		return buffer.read(sink, byteCount).coerceAtLeast(0)
	}

	private fun request(byteCount: Long): Boolean {
		if (buffer.size >= byteCount) return true
		val toRead = byteCount - buffer.size
		return super.read(buffer, toRead) == toRead
	}

	private companion object {
		// https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
		// See: "Graphics Control Extension"
		private val FRAME_DELAY_START_MARKER = "0021F904".decodeHex()
		private const val FRAME_DELAY_START_MARKER_SIZE = 4
		private const val MINIMUM_FRAME_DELAY = 2
		private const val DEFAULT_FRAME_DELAY = 10
	}
}
