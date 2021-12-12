package com.ys.coil

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.multidex.MultiDexApplication
import com.ys.coil.util.CoilLogger
import com.ys.coil_default.Coil

class Application : MultiDexApplication() {

	override fun onCreate() {
		super.onCreate()

		CoilLogger.setEnabled(true)
		Coil.setDefaultImageLoader(::buildDefaultImageLoader)
	}

	private fun buildDefaultImageLoader(): ImageLoader {
		return ImageLoader(this) {
			availableMemoryPercentage(0.5)
			bitmapPoolPercentage(0.5)
			crossfade(true)
			okHttpClient {
				// The Unsplash API requires TLS 1.2, which isn't enabled by default before Lollipop.
				if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) forceTls12()
			}
		}
	}
}