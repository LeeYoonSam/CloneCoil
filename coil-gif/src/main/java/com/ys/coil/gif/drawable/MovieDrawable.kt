package com.ys.coil.gif.drawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.gif.transform.AnimatedTransformation
import com.ys.coil.gif.transform.PixelOpacity.OPAQUE
import com.ys.coil.gif.transform.PixelOpacity.UNCHANGED
import com.ys.coil.gif.util.forEachIndices
import com.ys.coil.gif.util.isHardware
import com.ys.coil.size.Scale

/**
 * [Movie](예: GIF) 렌더링을 지원하는 [Drawable]입니다.
 *
 * NOTE: API 28 이상에서 [ImageDecoderDecoder] 및 [AnimatedImageDrawable] 사용을 선호합니다.
 */
class MovieDrawable @JvmOverloads constructor(
	private val movie: Movie,
	val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
	val scale: Scale = Scale.FIT
) : Drawable(), Animatable2Compat {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

	private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

	private val currentBounds = Rect()
	private val tempCanvasBounds = Rect()
	private var softwareCanvas: Canvas? = null
	private var softwareBitmap: Bitmap? = null

	private var softwareScale = 1f
	private var hardwareScale = 1f
	private var hardwareDx = 0f
	private var hardwareDy = 0f

	private var isRunning = false
	private var startTimeMillis = 0L
	private var frameTimeMillis = 0L

	private var repeatCount = REPEAT_INFINITE
	private var loopIteration = 0

	private var animatedTransformation: AnimatedTransformation? = null
	private var animatedTransformationPicture: Picture? = null
	private var pixelOpacity = UNCHANGED
	private var isSoftwareScalingEnabled = false

	init {
		require(!config.isHardware) { "Bitmap config must not be hardware." }
	}

	override fun draw(canvas: Canvas) {
		// 현재 프레임 시간을 계산합니다.
		val invalidate = updateFrameTime()

		// 크기 조정 속성을 업데이트하고 현재 프레임을 그립니다.
		if (isSoftwareScalingEnabled) {
			updateBounds(canvas.bounds)
			canvas.withSave {
				val scale = 1 / softwareScale
				scale(scale, scale)
				drawFrame(canvas)
			}
		} else {
			updateBounds(bounds)
			drawFrame(canvas)
		}

		// 필요한 경우 다음 프레임에 대한 새로운 드로우 패스를 요청하십시오.
		if (isRunning && invalidate) {
			invalidateSelf()
		} else {
			stop()
		}
	}

	/**
	 * 현재 프레임 시간을 계산하고 [movie]를 업데이트합니다.
	 * 렌더링할 후속 프레임이 있으면 'true'를 반환합니다.
	 */
	private fun updateFrameTime(): Boolean {
		val invalidate: Boolean
		val time: Int
		val duration = movie.duration()
		if (duration == 0) {
			invalidate = false
			time = 0
		} else {
			if (isRunning) {
				frameTimeMillis = SystemClock.uptimeMillis()
			}
			val elapsedTime = (frameTimeMillis - startTimeMillis).toInt()
			loopIteration = elapsedTime / duration
			invalidate = repeatCount == REPEAT_INFINITE || loopIteration <= repeatCount
			time = if (invalidate) elapsedTime - loopIteration * duration else duration
		}
		movie.setTime(time)
		return invalidate
	}

	/** [canvas]에 현재 [movie] 프레임을 그립니다. */
	private fun drawFrame(canvas: Canvas) {
		val softwareCanvas = softwareCanvas
		val softwareBitmap = softwareBitmap
		if (softwareCanvas == null || softwareBitmap == null) return

		// 소프트웨어 캔버스를 지웁니다.
		softwareCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

		// 먼저 소프트웨어 캔버스에 그립니다.
		softwareCanvas.withSave {
			scale(softwareScale, softwareScale)
			movie.draw(this, 0f, 0f, paint)
			animatedTransformationPicture?.draw(this)
		}

		// 입력 캔버스에 그립니다(하드웨어일 수도 있고 아닐 수도 있음).
		canvas.withSave {
			translate(hardwareDx, hardwareDy)
			scale(hardwareScale, hardwareScale)
			drawBitmap(softwareBitmap, 0f, 0f, paint)
		}
	}

	/**
	 * 애니메이션을 반복할 횟수를 설정합니다.
	 *
	 * 애니메이션이 이미 실행 중인 경우 이미 발생한 모든 반복이 새 카운트에 포함됩니다.
	 *
	 * 참고: 이 방법은 [AnimatedImageDrawable.setRepeatCount]의 동작과 일치합니다.
	 * 즉, [repeatCount]를 2로 설정하면 애니메이션이 3번 재생됩니다. [repeatCount]를 0으로 설정하면 애니메이션이 한 번 재생됩니다.
	 *
	 * Default: [REPEAT_INFINITE]
	 */
	fun setRepeatCount(repeatCount: Int) {
		require(repeatCount >= REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
		this.repeatCount = repeatCount
	}

	/** 애니메이션이 반복되는 횟수를 가져옵니다. */
	fun getRepeatCount(): Int = repeatCount

	/** 그릴 때 적용할 [AnimatedTransformation]을 설정합니다. */
	fun setAnimatedTransformation(animatedTransformation: AnimatedTransformation?) {
		this.animatedTransformation = animatedTransformation

		if (animatedTransformation != null && movie.width() > 0 && movie.height() > 0) {
			// 애니메이션 변환을 미리 계산합니다.
			val picture = Picture()
			val canvas = picture.beginRecording(movie.width(), movie.height())
			pixelOpacity = animatedTransformation.transform(canvas)
			picture.endRecording()
			animatedTransformationPicture = picture
			isSoftwareScalingEnabled = true
		} else {
			// 너비/높이가 양수가 아니면 영화를 그릴 수 없습니다.
			animatedTransformationPicture = null
			pixelOpacity = UNCHANGED
			isSoftwareScalingEnabled = false
		}

		// 드로어블을 다시 렌더링합니다.
		invalidateSelf()
	}

	/** [AnimatedTransformation]을 가져옵니다. */
	fun getAnimatedTransformation(): AnimatedTransformation? = animatedTransformation

	override fun setAlpha(alpha: Int) {
		require(alpha in 0..255) { "Invalid alpha: $alpha" }
		paint.alpha = alpha
	}

	override fun getOpacity(): Int {
		return if (paint.alpha == 255 && (pixelOpacity == OPAQUE || (pixelOpacity == UNCHANGED && movie.isOpaque))) {
			PixelFormat.OPAQUE
		} else {
			PixelFormat.TRANSLUCENT
		}
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	private fun updateBounds(bounds: Rect) {
		if (currentBounds == bounds) return
		currentBounds.set(bounds)

		val boundsWidth = bounds.width()
		val boundsHeight = bounds.height()

		// 너비/높이가 양수가 아니면 영화를 그릴 수 없습니다.
		val movieWidth = movie.width()
		val movieHeight = movie.height()
		if (movieWidth <= 0 || movieHeight <= 0) return

		softwareScale = DecodeUtils
			.computeSizeMultiplier(movieWidth, movieHeight, boundsWidth, boundsHeight, scale)
			.run { if (isSoftwareScalingEnabled) this else coerceAtMost(1.0) }
			.toFloat()
		val bitmapWidth = (softwareScale * movieWidth).toInt()
		val bitmapHeight = (softwareScale * movieHeight).toInt()

		val bitmap = createBitmap(bitmapWidth, bitmapHeight, config)
		softwareBitmap?.recycle()
		softwareBitmap = bitmap
		softwareCanvas = Canvas(bitmap)

		if (isSoftwareScalingEnabled) {
			hardwareScale = 1f
			hardwareDx = 0f
			hardwareDy = 0f
		} else {
			hardwareScale = DecodeUtils
				.computeSizeMultiplier(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight, scale)
				.toFloat()
			hardwareDx = bounds.left + (boundsWidth - hardwareScale * bitmapWidth) / 2
			hardwareDy = bounds.top + (boundsHeight - hardwareScale * bitmapHeight) / 2
		}
	}

	override fun getIntrinsicWidth() = movie.width()

	override fun getIntrinsicHeight() = movie.height()

	override fun isRunning() = isRunning

	override fun start() {
		if (isRunning) return
		isRunning = true

		loopIteration = 0
		startTimeMillis = SystemClock.uptimeMillis()

		callbacks.forEachIndices { it.onAnimationStart(this) }
		invalidateSelf()
	}

	override fun stop() {
		if (!isRunning) return
		isRunning = false

		callbacks.forEachIndices { it.onAnimationEnd(this) }
	}

	override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
		callbacks.add(callback)
	}

	override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
		return callbacks.remove(callback)
	}

	override fun clearAnimationCallbacks() = callbacks.clear()

	private val Canvas.bounds get() = tempCanvasBounds.apply { set(0, 0, width, height) }

	companion object {
		/** 이것을 [setRepeatCount]에 전달하여 무한 반복합니다. */
		const val REPEAT_INFINITE = -1
	}
}
