package com.ys.coil.decode

import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.caverock.androidsvg.SVG
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.SourceResult
import com.ys.coil.request.Options
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.util.indexOf
import com.ys.coil.util.toSoftware
import kotlinx.coroutines.runInterruptible
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

/**
 * [AndroidSVG](https://bigbadaboom.github.io/androidsvg/)를 사용하여 SVG 파일을 디코딩하는 [Decoder]입니다.
 *
 * @param useViewBoundsAsIntrinsicSize true인 경우 SVG의 뷰 경계를 SVG의 고유 크기로 사용합니다.
 * false인 경우 SVG의 너비/높이를 SVG의 고유 크기로 사용합니다.
 */
class SvgDecoder @JvmOverloads constructor(
	private val source: ImageSource,
	private val options: Options,
	val useViewBoundsAsIntrinsicSize: Boolean = true
) : Decoder {

	override suspend fun decode() = runInterruptible {
		val svg = source.source().use { SVG.getFromInputStream(it.inputStream()) }

		val svgWidth: Float
		val svgHeight: Float
		val bitmapWidth: Int
		val bitmapHeight: Int
		val viewBox: RectF? = svg.documentViewBox
		when (val size = options.size) {
			is PixelSize -> {
				if (useViewBoundsAsIntrinsicSize && viewBox != null) {
					svgWidth = viewBox.width()
					svgHeight = viewBox.height()
				} else {
					svgWidth = svg.documentWidth
					svgHeight = svg.documentHeight
				}

				if (svgWidth > 0 && svgHeight > 0) {
					val multiplier = DecodeUtils.computeSizeMultiplier(
						srcWidth = svgWidth,
						srcHeight = svgHeight,
						dstWidth = size.width.toFloat(),
						dstHeight = size.height.toFloat(),
						scale = options.scale
					)

					bitmapWidth = (multiplier * svgWidth).toInt()
					bitmapHeight = (multiplier * svgHeight).toInt()
				} else {
					bitmapWidth = size.width
					bitmapHeight = size.height
				}
			}

			is OriginalSize -> {
				svgWidth = svg.documentWidth
				svgHeight = svg.documentHeight

				if (svgWidth > 0 && svgHeight > 0) {
					bitmapWidth = svgWidth.toInt()
					bitmapHeight = svgHeight.toInt()
				} else if (useViewBoundsAsIntrinsicSize && viewBox != null) {
					bitmapWidth = viewBox.width().toInt()
					bitmapHeight = viewBox.height().toInt()
				} else {
					bitmapWidth = DEFAULT_SIZE
					bitmapHeight = DEFAULT_SIZE
				}
			}
		}

		// SVG의 보기 상자가 설정되지 않은 경우 크기 조정을 활성화하도록 설정합니다.
		if (viewBox == null && svgWidth > 0 && svgHeight > 0) {
			svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
		}

		svg.setDocumentWidth("100%")
		svg.setDocumentHeight("100%")
		val bitmap = createBitmap(bitmapWidth, bitmapHeight, options.config.toSoftware())
		svg.renderToCanvas(Canvas(bitmap))

		DecodeResult(
			drawable = bitmap.toDrawable(options.context.resources),
			isSampled = true // SVG는 항상 더 높은 해상도에서 다시 디코딩될 수 있습니다.
		)
	}

	class Factory @JvmOverloads constructor(
		val useViewBoundsAsIntrinsicSize: Boolean = true
	) : Decoder.Factory {

		override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
			if (!isApplicable(result)) return null
			return SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize)
		}

		private fun isApplicable(result: SourceResult): Boolean {
			return result.mimeType == MIME_TYPE_SVG || containsSvgTag(result.source.source())
		}

		private fun containsSvgTag(source: BufferedSource): Boolean {
			return source.rangeEquals(0, LEFT_ANGLE_BRACKET) &&
				source.indexOf(SVG_TAG, 0, SVG_TAG_SEARCH_THRESHOLD_BYTES) != -1L
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			return other is Factory && useViewBoundsAsIntrinsicSize == other.useViewBoundsAsIntrinsicSize
		}

		override fun hashCode() = useViewBoundsAsIntrinsicSize.hashCode()
	}

	private companion object {
		private const val MIME_TYPE_SVG = "image/svg+xml"
		private const val DEFAULT_SIZE = 512
		private const val SVG_TAG_SEARCH_THRESHOLD_BYTES = 1024L
		private val SVG_TAG = "<svg ".encodeUtf8()
		private val LEFT_ANGLE_BRACKET = "<".encodeUtf8()
	}
}
