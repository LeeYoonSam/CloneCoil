package com.ys.coil.gif.drawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import androidx.annotation.Px
import androidx.core.graphics.withScale
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.Scale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MovieDrawable(
	private val movie: Movie,
	private val config: Bitmap.Config,
	private val scale: Scale,
	private val pool: BitmapPool
) : Drawable(), Animatable {

	init {
		require(VERSION.SDK_INT < VERSION_CODES.O || config != Bitmap.Config.HARDWARE) { "Bitmap config must not be hardware." }
	}

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

	private var currentBounds: Rect? = null
	private var softwareCanvas: Canvas? = null
	private var softwareBitmap: Bitmap? = null

	private var softwareScale = 1f
	private var hardwareScale = 1f
	private var hardwareDx = 0f
	private var hardwareDy = 0f

	private var isRunning = false
	private var startTimeMillis = 0L

	override fun draw(canvas: Canvas) {
		val softwareCanvas = checkNotNull(softwareCanvas)
		val softwareBitmap = checkNotNull(softwareBitmap)

		val time = ((SystemClock.uptimeMillis() - startTimeMillis) % movie.duration()).toInt()
		movie.setTime(time)

		// 먼저 소프트웨어 캔버스에 그립니다.
		softwareCanvas.withScale(
			x = softwareScale,
			y = softwareScale
		) {
			movie.draw(this, 0f, 0f, paint)
		}

		// 입력 캔버스에 그립니다(하드웨어일 수도 있고 아닐 수도 있음).
		canvas.withScale(
			x = hardwareScale,
			y = hardwareScale,
			pivotX = hardwareDx,
			pivotY = hardwareDy
		) {
			drawBitmap(softwareBitmap, 0f, 0f, paint)
		}

		if (isRunning) {
			invalidateSelf()
		}
	}

	override fun setAlpha(alpha: Int) {
		require(alpha in 0..255) { "Invalid alpha: $alpha" }
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getOpacity(): Int {
		return if (paint.alpha == 255 && movie.isOpaque) {
			PixelFormat.OPAQUE
		} else {
			PixelFormat.TRANSLUCENT
		}
	}

	override fun onBoundsChange(bounds: Rect) {
		if (currentBounds == bounds) {
			return
		}
		currentBounds = bounds

		val boundsWidth = bounds.width().toFloat()
		val boundsHeight = bounds.height().toFloat()

		val movieWidth = movie.width().toFloat()
		val movieHeight = movie.height().toFloat()

		softwareScale = computeScale(movieWidth, movieHeight, boundsWidth, boundsHeight)
		val bitmapWidth = ceil(softwareScale * movieWidth)
		val bitmapHeight = ceil(softwareScale * movieHeight)

		val bitmap = pool.get(bitmapWidth.toInt(), bitmapHeight.toInt(), config)
		softwareBitmap?.let(pool::put)
		softwareBitmap = bitmap
		softwareCanvas = Canvas(bitmap)

		hardwareScale = computeScale(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight)
		hardwareDx = (boundsWidth - bitmapWidth / hardwareScale) / 2
		hardwareDy = (boundsHeight - bitmapHeight / hardwareScale) / 2
	}

	private fun computeScale(
		@Px srcWidth: Float,
		@Px srcHeight: Float,
		@Px destWidth: Float,
		@Px destHeight: Float
	): Float {
		val bitmapWidthPercent = srcWidth / min(destWidth, srcWidth)
		val bitmapHeightPercent = srcHeight / min(destHeight, srcHeight)
		return when (scale) {
			Scale.FILL -> max(bitmapWidthPercent, bitmapHeightPercent)
			Scale.FIT -> min(bitmapWidthPercent, bitmapHeightPercent)
		}
	}

	override fun getIntrinsicWidth() = movie.width()

	override fun getIntrinsicHeight() = movie.height()

	override fun isRunning() = isRunning

	override fun start() {
		if (isRunning) {
			return
		}

		isRunning = true
		startTimeMillis = SystemClock.uptimeMillis()

		invalidateSelf()
	}

	override fun stop() {
		isRunning = false
	}
}