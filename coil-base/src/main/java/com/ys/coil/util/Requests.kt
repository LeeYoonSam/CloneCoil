package com.ys.coil.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.ys.coil.request.ImageRequest
import com.ys.coil.size.Precision
import com.ys.coil.size.ViewSizeResolver
import com.ys.coil.target.ViewTarget

/**
 * [ImageRequest.placeholder], [ImageRequest.error] 및 [ImageRequest.fallback]을 해결하는 데 사용됩니다.
 */
internal fun ImageRequest.getDrawableCompat(
	drawable: Drawable?,
	@DrawableRes resId: Int?,
	default: Drawable?
): Drawable? {
	return when {
		drawable != null -> drawable
		resId != null -> if (resId == 0) null else context.getDrawableCompat(resId)
		else -> default
	}
}

/**
 * 요청에서 출력 이미지의 크기가 요청된 치수와 정확히 일치하도록 요구하지 않는 경우 'true'를 반환합니다.
 */
internal val ImageRequest.allowInexactSize: Boolean
	get() = when (precision) {
		Precision.EXACT -> false
		Precision.INEXACT -> true
		Precision.AUTOMATIC -> run {
			// 대상 및 크기 확인자가 동일한 ImageView를 참조하는 경우 ImageView가 출력 이미지의 크기를 자동으로 조정하므로 치수가 정확하지 않도록 허용합니다.
			if (target is ViewTarget<*> && target.view is ImageView &&
				sizeResolver is ViewSizeResolver<*> && sizeResolver.view === target.view) {
				return true
			}

			// 그렇지 않으면 치수가 정확해야 합니다.
			return false
		}
	}
