package com.app.vc.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.app.vc.R

class ConnectivityBannerHandler(
    context: Context,
    private val rootViewProvider: () -> View?,
    private val onConnectionChanged: ((Boolean) -> Unit)? = null
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRegistered = false
    private var lastKnownConnected: Boolean? = null

    private val hideRecoveryRunnable = Runnable {
        if (onConnectionChanged == null) {
            setBannerVisible(false)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            dispatchConnectivity(isNetworkUsable(network))
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            dispatchConnectivity(
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
        }

        override fun onLost(network: Network) {
            dispatchConnectivity(isCurrentlyConnected())
        }

        override fun onUnavailable() {
            dispatchConnectivity(false)
        }
    }

    fun register() {
        if (isRegistered) return
        isRegistered = true

        attachDismissListener()
        dispatchConnectivity(isCurrentlyConnected())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun unregister() {
        if (!isRegistered) return
        isRegistered = false
        mainHandler.removeCallbacks(hideRecoveryRunnable)
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    private fun dispatchConnectivity(isConnected: Boolean) {
        mainHandler.post {
            val previous = lastKnownConnected
            lastKnownConnected = isConnected
            onConnectionChanged?.invoke(isConnected) ?: run {
                if (isConnected) {
                    if (previous == false) {
                        showRecoveryBanner()
                    } else {
                        setBannerVisible(false)
                    }
                } else {
                    showErrorBanner()
                }
            }
        }
    }

    private fun attachDismissListener() {
        findBannerRoot()
            ?.findViewById<View>(R.id.btnDismissNetworkError)
            ?.setOnClickListener { setBannerVisible(false) }
    }

    private fun showErrorBanner() {
        mainHandler.removeCallbacks(hideRecoveryRunnable)
        val banner = findBannerRoot() ?: return
        banner.setBackgroundColor(ContextCompat.getColor(appContext, R.color.red))
        banner.findViewById<TextView>(R.id.txtNetworkError)?.text =
            "Network error Please check your Internet connection."
        banner.visibility = View.VISIBLE
    }

    private fun showRecoveryBanner() {
        val banner = findBannerRoot() ?: return
        banner.setBackgroundColor(ContextCompat.getColor(appContext, R.color.green))
        banner.findViewById<TextView>(R.id.txtNetworkError)?.text = "Network is back"
        banner.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideRecoveryRunnable)
        mainHandler.postDelayed(hideRecoveryRunnable, 2000L)
    }

    private fun setBannerVisible(visible: Boolean) {
        findBannerRoot()?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun findBannerRoot(): View? {
        val root = rootViewProvider() ?: return null
        return root.findViewById(R.id.includeNetworkErrorBanner)
            ?: root.findViewById(R.id.networkErrorBanner)
    }

    private fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isNetworkUsable(network: Network): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
