package com.ys.coil.util

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.WorkerThread
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.createBitmap
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size

internal object DrawableUtils {

	private const val DEFAULT_SIZE = 512

	/**
	 * 제공된 [Drawable]을 [Bitmap]으로 변환합니다.
	 *
	 * @param drawable 변환할 드로어블.
	 * @param config 비트맵에 대해 요청된 구성입니다.
	 * @param size 비트맵에 대해 요청된 크기입니다.
	 * @param scale 비트맵에 대해 요청된 크기입니다.
	 * @param allowInexactSize [size]보다 작은 비트맵 반환을 허용합니다.
	 */
	@WorkerThread
	fun convertToBitmap(
		drawable: Drawable,
		config: Bitmap.Config,
		size: Size,
		scale: Scale,
		allowInexactSize: Boolean
	): Bitmap {
		// 빠른 경로: 기본 비트맵을 반환합니다.
		if (drawable is BitmapDrawable) {
			val bitmap = drawable.bitmap
			if (isConfigValid(bitmap, config) && isSizeValid(allowInexactSize, size, bitmap, scale)) {
				return bitmap
			}
		}

		// 느린 경로: 새 비트맵에 드로어블을 그립니다.
		val safeDrawable = drawable.mutate()
		val srcWidth = safeDrawable.width.let { if (it > 0) it else DEFAULT_SIZE }
		val srcHeight = safeDrawable.height.let { if (it > 0) it else DEFAULT_SIZE }
		val (width, height) = DecodeUtils.computePixelSize(srcWidth, srcHeight, size, scale)

		val bitmap = createBitmap(width, height, config.toSoftware())
		safeDrawable.apply {
			val (oldLeft, oldTop, oldRight, oldBottom) = safeDrawable.bounds
			setBounds(0, 0, width, height)
			draw(Canvas(bitmap))
			setBounds(oldLeft, oldTop, oldRight, oldBottom)
		}

		return bitmap
	}

	private fun isConfigValid(bitmap: Bitmap, config: Config): Boolean {
		return bitmap.config == config.toSoftware()
	}

	private fun isSizeValid(allowInexactSize: Boolean, size: Size, bitmap: Bitmap, scale: Scale): Boolean {
		return allowInexactSize || size is OriginalSize ||
			size == DecodeUtils.computePixelSize(bitmap.width, bitmap.height, size, scale)
	}
}
