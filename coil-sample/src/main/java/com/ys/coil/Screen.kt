package com.ys.coil

sealed class Screen {

	object List : Screen()

	data class Detail(val image: Image) : Screen()
}