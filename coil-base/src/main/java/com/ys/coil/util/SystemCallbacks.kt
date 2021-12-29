package com.ys.coil.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.ys.coil.RealImageLoader

internal class SystemCallbacks(
	imageLoader: RealImageLoader,
	private val context: Context,
	isNetworkObserverEnable: Boolean
) : ComponentCallbacks2 {
	override fun onConfigurationChanged(p0: Configuration) {
		TODO("Not yet implemented")
	}

	override fun onLowMemory() {
		TODO("Not yet implemented")
	}

	override fun onTrimMemory(p0: Int) {
		TODO("Not yet implemented")
	}
}