package com.ys.coil.decode

import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.SourceResult
import com.ys.coil.request.Options
import com.ys.coil.request.videoFrameMicros
import com.ys.coil.request.videoFrameOption
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import kotlin.math.roundToInt

/**
 * [MediaMetadataRetriever]를 사용하여 비디오에서 프레임을 가져와 디코딩하는 [Decoder].
 */
class VideoFrameDecoder(
	private val source: ImageSource,
	private val options: Options
) : Decoder {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

	override suspend fun decode() = MediaMetadataRetriever().use { retriever ->
		retriever.setDataSource(source.file().path)
		val option = options.parameters.videoFrameOption() ?: MediaMetadataRetriever.OPTION_CLOSEST_SYNC
		val frameMicros = options.parameters.videoFrameMicros() ?: 0L

		// 소스의 종횡비와 대상의 크기를 고려하여 비디오 프레임을 디코딩할 크기를 확인합니다.
		var srcWidth = 0
		var srcHeight = 0
		val destSize = when (val size = options.size) {
			is PixelSize -> {
				val rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
				if (rotation == 90 || rotation == 270) {
					srcWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
					srcHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
				} else {
					srcWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
					srcHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
				}

				if (srcWidth > 0 && srcHeight > 0) {
					val rawScale = DecodeUtils.computeSizeMultiplier(
						srcWidth = srcWidth,
						srcHeight = srcHeight,
						dstWidth = size.width,
						dstHeight = size.height,
						scale = options.scale
					)
					val scale = if (options.allowInexactSize) rawScale.coerceAtMost(1.0) else rawScale
					val width = (scale * srcWidth).roundToInt()
					val height = (scale * srcHeight).roundToInt()
					PixelSize(width, height)
				} else {
					// We were unable to decode the video's dimensions.
					// Fall back to decoding the video frame at the original size.
					// We'll scale the resulting bitmap after decoding if necessary.
					OriginalSize
				}
			}
			is OriginalSize -> OriginalSize
		}

		val rawBitmap: Bitmap? = if (SDK_INT >= 27 && destSize is PixelSize) {
			retriever.getScaledFrameAtTime(frameMicros, option, destSize.width, destSize.height)
		} else {
			retriever.getFrameAtTime(frameMicros, option)?.also {
				srcWidth = it.width
				srcHeight = it.height
			}
		}

		// 이 예외가 발생하면 비디오가 지원되는 코덱으로 인코딩되었는지 확인하십시오.
		// https://developer.android.com/guide/topics/media/media-formats#video-formats
		checkNotNull(rawBitmap) { "Failed to decode frame at $frameMicros microseconds." }

		val bitmap = normalizeBitmap(rawBitmap, destSize, options)

		val isSampled = if (srcWidth > 0 && srcHeight > 0) {
			DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, bitmap.width, bitmap.height, options.scale) < 1.0
		} else {
			// 동영상의 원본 크기를 확인할 수 없습니다. 샘플링되었다고 가정합니다.
			true
		}

		DecodeResult(
			drawable = bitmap.toDrawable(options.context.resources),
			isSampled = isSampled
		)
	}

	/** 입력 [options] 및 [size]에 유효한 [inBitmap] 또는 [inBitmap] 복사본을 반환합니다. */
	private fun normalizeBitmap(
		inBitmap: Bitmap,
		size: Size,
		options: Options
	): Bitmap {
		// 빠른 경로: 입력 비트맵이 유효하면 반환합니다.
		if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
			return inBitmap
		}

		// 느린 경로: 올바른 크기 + 구성으로 비트맵을 다시 렌더링합니다.
		val scale: Float
		val dstWidth: Int
		val dstHeight: Int
		when (size) {
			is PixelSize -> {
				scale = DecodeUtils.computeSizeMultiplier(
					srcWidth = inBitmap.width,
					srcHeight = inBitmap.height,
					dstWidth = size.width,
					dstHeight = size.height,
					scale = options.scale
				).toFloat()
				dstWidth = (scale * inBitmap.width).roundToInt()
				dstHeight = (scale * inBitmap.height).roundToInt()
			}
			is OriginalSize -> {
				scale = 1f
				dstWidth = inBitmap.width
				dstHeight = inBitmap.height
			}
		}

		val safeConfig = when {
			SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
			else -> options.config
		}

		val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
		outBitmap.applyCanvas {
			scale(scale, scale)
			drawBitmap(inBitmap, 0f, 0f, paint)
		}
		inBitmap.recycle()

		return outBitmap
	}

	private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
		return SDK_INT < 26 || bitmap.config != Bitmap.Config.HARDWARE || options.config == Bitmap.Config.HARDWARE
	}

	private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
		return options.allowInexactSize || size is OriginalSize ||
			size == DecodeUtils.computePixelSize(bitmap.width, bitmap.height, size, options.scale)
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
