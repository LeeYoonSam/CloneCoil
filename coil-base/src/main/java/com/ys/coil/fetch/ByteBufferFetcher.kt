package com.ys.coil.fetch

import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.ImageSource
import com.ys.coil.request.Options
import okio.Buffer
import java.nio.ByteBuffer

internal class ByteBufferFetcher(
	private val data: ByteBuffer,
	private val options: Options
) : Fetcher {

	override suspend fun fetch(): FetchResult {
		val source = try {
			Buffer().apply {
				write(data)
			}
		} finally {
			// 바이트 버퍼를 다시 읽을 수 있도록 위치를 재설정합니다.
			data.position(0)
		}

		return SourceResult(
			source = ImageSource(source, options.context),
			mimeType = null,
			dataSource = DataSource.MEMORY
		)
	}

	class Factory : Fetcher.Factory<ByteBuffer> {
		override fun create(
			data: ByteBuffer,
			options: Options,
			imageLoader: ImageLoader
		): Fetcher {
			return ByteBufferFetcher(data, options)
		}
	}
}