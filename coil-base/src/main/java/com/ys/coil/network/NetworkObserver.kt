package com.ys.coil.network

import android.content.Context
import android.util.Log
import com.ys.coil.util.log

internal class NetworkObserver(context: Context) : NetworkObserverStrategy.Listener {

    companion object {
        private const val TAG = "NetworkObserver"
        private const val ONLINE = "ONLINE"
        private const val OFFLINE = "OFFLINE"
    }

    private val strategy = NetworkObserverStrategy(context, this)

    private var isOnline = strategy.isOnline()
    private var isShutdown = false

    init {
        logStatus()
        strategy.start()
    }

    override fun onConnectivityChange(isOnline: Boolean) {
        this.isOnline = isOnline
        logStatus()
    }

    fun isOnline() = isOnline

    fun shutdown() {
        if (!isShutdown) {
            isShutdown = true
            strategy.stop()
        }
    }

    private fun logStatus() {
        log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
    }
}