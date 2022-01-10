package com.ys.coil.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.VisibleForTesting
import com.ys.coil.RealImageLoader
import com.ys.coil.network.EmptyNetworkObserver
import com.ys.coil.network.NetworkObserver
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

internal class SystemCallbacks(
	imageLoader: RealImageLoader,
	private val context: Context,
	isNetworkObserverEnabled: Boolean
) : ComponentCallbacks2, NetworkObserver.Listener  {

	@VisibleForTesting internal val imageLoader = WeakReference(imageLoader)
	private val networkObserver = if (isNetworkObserverEnabled) {
		NetworkObserver(context, this, imageLoader.logger)
	} else {
		EmptyNetworkObserver()
	}

	@Volatile private var _isOnline = networkObserver.isOnline
	private val _isShutdown = AtomicBoolean(false)

	override fun onConfigurationChanged(p0: Configuration) {
		TODO("Not yet implemented")
	}

	override fun onLowMemory() {
		TODO("Not yet implemented")
	}

	override fun onTrimMemory(p0: Int) {
		TODO("Not yet implemented")
	}

	fun shutdown() {
		if (_isShutdown.getAndSet(true)) return
		context.unregisterComponentCallbacks(this)
		networkObserver.shutdown()
	}

	override fun onConnectivityChange(isOnline: Boolean) {
		TODO("Not yet implemented")
	}
}