package com.ys.coil.sample

import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.ys.coil.request.Parameters

data class Image(
	val uri: String,
	@ColorInt val color: Int,
	@Px val width: Int,
	@Px val height: Int,
	val parameters: Parameters = Parameters.EMPTY
)
