package com.ys.coil.compose

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.Scale
import kotlin.math.max

/** 주어진 [key]에 대해 [CrossfadePainter]를 반환합니다. */
@Composable
internal fun rememberCrossfadePainter(
	key: Any,
	start: Painter?,
	end: Painter?,
	scale: Scale,
	durationMillis: Int,
	fadeStart: Boolean
): Painter = remember(key) { CrossfadePainter(start, end, scale, durationMillis, fadeStart) }

/**
 * [start]에서 [end]으로 크로스페이드되는 [Painter].
 *
 * 참고: 애니메이션은 전환이 끝날 때 [start] 드로어블이 역참조되므로 한 번만 실행할 수 있습니다.
 */
@Stable
private class CrossfadePainter(
	private var start: Painter?,
	private val end: Painter?,
	private val scale: Scale,
	private val durationMillis: Int,
	private val fadeStart: Boolean,
) : Painter() {

	private var invalidateTick by mutableStateOf(0)
	private var startTimeMillis = -1L
	private var isDone = false

	private var maxAlpha: Float by mutableStateOf(1f)
	private var colorFilter: ColorFilter? by mutableStateOf(null)

	override val intrinsicSize get() = computeIntrinsicSize()

	override fun DrawScope.onDraw() {
		if (isDone) {
			drawPainter(end, maxAlpha)
			return
		}

		// 처음 그릴 때 startTimeMillis를 초기화합니다.
		val uptimeMillis = SystemClock.uptimeMillis()
		if (startTimeMillis == -1L) {
			startTimeMillis = uptimeMillis
		}

		val percent = (uptimeMillis - startTimeMillis) / durationMillis.toFloat()
		val endAlpha = percent.coerceIn(0f, 1f) * maxAlpha
		val startAlpha = if (fadeStart) maxAlpha - endAlpha else maxAlpha
		isDone = percent >= 1.0

		drawPainter(start, startAlpha)
		drawPainter(end, endAlpha)

		if (isDone) {
			start = null
		} else {
			// 페인터를 강제로 다시 그리려면 이 값을 증가시킵니다.
			invalidateTick++
		}
	}

	override fun applyAlpha(alpha: Float): Boolean {
		this.maxAlpha = alpha
		return true
	}

	override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
		this.colorFilter = colorFilter
		return true
	}

	private fun computeIntrinsicSize(): Size {
		val startSize = start?.intrinsicSize ?: Size.Zero
		val endSize = end?.intrinsicSize ?: Size.Zero

		return if (startSize.isSpecified && endSize.isSpecified) {
			Size(
				width = max(startSize.width, endSize.width),
				height = max(startSize.height, endSize.height)
			)
		} else {
			Size.Unspecified
		}
	}

	private fun DrawScope.drawPainter(painter: Painter?, alpha: Float) {
		if (painter == null || alpha <= 0) return

		with(painter) {
			val size = size
			val drawSize = computeDrawSize(intrinsicSize, size)

			if (size.isUnspecified || size.isEmpty()) {
				draw(drawSize, alpha, colorFilter)
			} else {
				inset(
					horizontal = (size.width - drawSize.width) / 2,
					vertical = (size.height - drawSize.height) / 2
				) { draw(drawSize, alpha, colorFilter) }
			}
		}
	}

	/** 종횡비를 유지하면서 src 크기를 dst 크기로 조정합니다. */
	private fun computeDrawSize(srcSize: Size, dstSize: Size): Size {
		if (srcSize.isUnspecified || srcSize.isEmpty()) return dstSize
		if (dstSize.isUnspecified || dstSize.isEmpty()) return dstSize

		val srcWidth = srcSize.width
		val srcHeight = srcSize.height
		val multiplier = DecodeUtils.computeSizeMultiplier(
			srcWidth = srcWidth,
			srcHeight = srcHeight,
			dstWidth = dstSize.width,
			dstHeight = dstSize.height,
			scale = scale
		)

		return Size(
			width = multiplier * srcWidth,
			height = multiplier * srcHeight
		)
	}
}
