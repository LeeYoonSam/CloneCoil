package com.ys.coil.compose.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.Scale
import com.ys.coil.test.util.assertIsSimilarTo

fun resourceUri(@IdRes resId: Int): Uri {
	val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
	return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$resId".toUri()
}

fun ImageBitmap.assertIsSimilarTo(
	@IdRes resId: Int,
	@FloatRange(from = -1.0, to = 1.0) threshold: Double = 0.9 // 기본적으로 더 낮은 임계값을 사용합니다.
) {
	val context = InstrumentationRegistry.getInstrumentation().targetContext
	val expected = context.getDrawable(resId)!!.toBitmap().fitCenter(width, height)
	asAndroidBitmap().assertIsSimilarTo(expected, threshold)
}

private fun Bitmap.fitCenter(width: Int, height: Int): Bitmap {
	val input = this.apply { density = Bitmap.DENSITY_NONE }

	return createBitmap(width, height).applyCanvas {
		// 테스트 배경과 일치하도록 흰색 배경을 그립니다.
		drawColor(Color.WHITE)

		val scale = DecodeUtils.computeSizeMultiplier(
			srcWidth = input.width,
			srcHeight = input.height,
			dstWidth = width,
			dstHeight = height,
			scale = Scale.FIT
		).toFloat()
		val dx = (width - scale * input.width) / 2
		val dy = (height - scale * input.height) / 2

		translate(dx, dy)
		scale(scale, scale)
		drawBitmap(input, 0f, 0f, null)
	}
}

