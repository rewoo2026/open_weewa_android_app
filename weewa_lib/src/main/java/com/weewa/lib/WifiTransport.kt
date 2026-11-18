package com.weewa.lib

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.Socket

/** Camera traffic must use Wi-Fi even when Android prefers cellular or a VPN. */
internal object WifiTransport {
    fun network(context: Context): Network? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.allNetworks.firstOrNull { network ->
            val capabilities = manager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                manager.getLinkProperties(network)?.linkAddresses?.any {
                    it.address is Inet4Address && !it.address.isLoopbackAddress
                } == true
        }
    }

    fun socket(context: Context): Socket = network(context)?.socketFactory?.createSocket() ?: Socket()
}
