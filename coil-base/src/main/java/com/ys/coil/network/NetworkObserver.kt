package com.ys.coil.network

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.content.getSystemService
import com.ys.coil.network.NetworkObserver.Listener
import com.ys.coil.util.Logger
import com.ys.coil.util.isPermissionGranted
import com.ys.coil.util.log

private const val TAG = "NetworkObserver"

/** 새 [NetworkObserver]를 만듭니다. */
internal fun NetworkObserver(
    context: Context,
    listener: Listener,
    logger: Logger?
): NetworkObserver {
    val connectivityManager: ConnectivityManager? = context.getSystemService()
    if (connectivityManager == null || !context.isPermissionGranted(ACCESS_NETWORK_STATE)) {
        logger?.log(TAG, Log.WARN) { "Unable to register network observer." }
        return EmptyNetworkObserver()
    }

    return try {
    	RealNetworkObserver(connectivityManager, listener)
    } catch (e: Exception) {
        logger?.log(TAG, RuntimeException("Failed to register network observer.", e))
        EmptyNetworkObserver()
    }
}

internal interface NetworkObserver {

    /** 장치가 온라인 상태인지 동기적으로 확인합니다. */
    val isOnline: Boolean

    /** 네트워크 변경 관찰을 중지합니다. */
    fun shutdown()

    /** 연결 변경 이벤트가 발생하면 [onConnectivityChange]를 호출합니다. */
    fun interface Listener {
        @MainThread
        fun onConnectivityChange(isOnline: Boolean)
    }
}

internal class EmptyNetworkObserver : NetworkObserver {
    override val isOnline get() = true

    override fun shutdown() {}
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION") // TODO: Remove uses of 'allNetworks'.
private class RealNetworkObserver(
    private val connectivityManager: ConnectivityManager,
    private val listener: Listener
) : NetworkObserver {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    override val isOnline: Boolean
        get() = connectivityManager.allNetworks.any { it.isOnline() }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun shutdown() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun onConnectivityChange(network: Network, isOnline: Boolean) {
        val isAnyOnline = connectivityManager.allNetworks.any {
            if (it == network) {
                // 방금 변경된 네트워크에 대한 네트워크 기능을 신뢰하지 마십시오.
                isOnline
            } else {
                it.isOnline()
            }
        }
        listener.onConnectivityChange(isAnyOnline)
    }

    private fun Network.isOnline(): Boolean  {
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}