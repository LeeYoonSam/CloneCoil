package com.ys.coil.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ys.coil.RealImageLoader
import com.ys.coil.network.EmptyNetworkObserver
import com.ys.coil.network.NetworkObserver
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 프록시 [ComponentCallbacks2] 및 [NetworkObserver.Listener]는 약하게 참조된 [imageLoader]에 대한 호출입니다.
 *
 * 이것은 시스템이 [imageLoader]에 대한 강력한 참조를 갖는 것을 방지하여 가비지 수집기에 의해 자연스럽게 해제되도록 합니다.
 * [imageLoader]가 해제되면 콜백을 등록 취소합니다.
 */
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

	val isOnline get() = _isOnline
	val isShutdown get() = _isShutdown.get()

	init {
		context.registerComponentCallbacks(this)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		imageLoader.get() ?: shutdown()
	}

	override fun onTrimMemory(level: Int) = withImageLoader { imageLoader ->
		imageLoader.logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
		imageLoader.onTrimMemory(level)
	}

	override fun onLowMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

	override fun onConnectivityChange(isOnline: Boolean) = withImageLoader { imageLoader ->
		imageLoader.logger?.log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
	}

	fun shutdown() {
		if (_isShutdown.getAndSet(true)) return
		context.unregisterComponentCallbacks(this)
		networkObserver.shutdown()
	}

	private fun withImageLoader(block: (RealImageLoader) -> Unit) {
		imageLoader.get()?.let(block) ?: shutdown()
	}

	companion object {
		private const val TAG = "NetworkObserver"
		private const val ONLINE = "ONLINE"
		private const val OFFLINE = "OFFLINE"
	}
}