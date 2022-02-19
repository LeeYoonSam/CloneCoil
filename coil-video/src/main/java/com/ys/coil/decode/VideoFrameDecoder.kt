package com.ys.coil.decode

import android.graphics.Paint
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.SourceResult
import com.ys.coil.request.Options

/**
 * [MediaMetadataRetriever]를 사용하여 비디오에서 프레임을 가져와 디코딩하는 [Decoder].
 */
class VideoFrameDecoder(
	private val source: ImageSource,
	private val options: Options
) : Decoder {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

	override suspend fun decode(): DecodeResult? {
		TODO("Not yet implemented")
	}

	class Factory : Decoder.Factory {

		override fun create(
			result: SourceResult,
			options: Options,
			imageLoader: ImageLoader
		): Decoder? {
			if (!isApplicable(result.mimeType)) return null
			return VideoFrameDecoder(result.source, options)
		}

		private fun isApplicable(mimeType: String?): Boolean {
			return mimeType != null && mimeType.startsWith("video/")
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}

	companion object {
		const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
		const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
	}
}
