package com.ys.coil.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.ys.coil.util.isPermissionGranted
import com.ys.coil.util.log

internal interface NetworkObserverStrategy {

    companion object {
        private const val TAG = "NetworkObserverStrategy"

        /**
         * 새로운 [NetworkObserverStrategy] 인스턴스 생성
         */
        operator fun invoke(context: Context, listener: Listener): NetworkObserverStrategy {
            val connectivityManager: ConnectivityManager? = context.getSystemService()
            return if (connectivityManager == null || !context.isPermissionGranted(Manifest.permission.ACCESS_NETWORK_STATE)) {
                log(TAG, Log.WARN) { "Unable to register network observer." }
                EmptyNetworkObserverStrategy
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                NetworkObserverStrategyApi21(connectivityManager, listener)
            } else {
                NetworkObserverStrategyApi14(context, connectivityManager, listener)
            }
        }
    }

    /**
     * 연결 변경 이벤트가 발생하면 [onConnectivityChange]를 호출합니다.
     */
    interface Listener {
        fun onConnectivityChange(isOnline: Boolean)
    }

    /**
     * Start observing network changes.
     */
    fun start()

    /**
     * Stop observing network changes.
     */
    fun stop()

    /**
     * Synchronously checks if the device is online.
     */
    fun isOnline(): Boolean
}

private object EmptyNetworkObserverStrategy : NetworkObserverStrategy {

    override fun start() {}

    override fun stop() {}

    override fun isOnline() = true
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
private class NetworkObserverStrategyApi21(
    private val connectivityManager: ConnectivityManager,
    private val listener: NetworkObserverStrategy.Listener
) : NetworkObserverStrategy {

    companion object {
        private val NETWORK_REQUEST = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    override fun start() {
        connectivityManager.registerNetworkCallback(NETWORK_REQUEST, networkCallback)
    }

    override fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun isOnline(): Boolean {
        return connectivityManager.allNetworks.any { it.isOnline() }
    }

    private fun onConnectivityChange(network: Network, isOnline: Boolean) {
        val isAnyOnline = connectivityManager.allNetworks.any {
            if (it == network) {
                // Don't trust the network capabilities for the network that just changed.
                isOnline
            } else {
                it.isOnline()
            }
        }
        listener.onConnectivityChange(isAnyOnline)
    }

    private fun Network.isOnline(): Boolean {
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private class NetworkObserverStrategyApi14(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    listener: NetworkObserverStrategy.Listener
) : NetworkObserverStrategy {

    companion object {
        private val INTENT_FILTER = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                listener.onConnectivityChange(isOnline())
            }
        }
    }

    override fun start() {
        context.registerReceiver(connectionReceiver, INTENT_FILTER)
    }

    override fun stop() {
        context.unregisterReceiver(connectionReceiver)
    }

    override fun isOnline(): Boolean {
        return connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}