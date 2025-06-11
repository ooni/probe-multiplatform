package org.ooni.engine

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.ooni.engine.models.NetworkType

class AndroidNetworkTypeFinder(
    private val connectivityManager: ConnectivityManager?,
) : NetworkTypeFinder {
    override fun invoke(): NetworkType {
        if (connectivityManager == null) return NetworkType.NoInternet

        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return NetworkType.NoInternet

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Mobile
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Unknown("wired_ethernet")
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.Unknown("bluetooth")
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> NetworkType.Unknown("usb")
            else -> NetworkType.NoInternet
        }
    }
}
