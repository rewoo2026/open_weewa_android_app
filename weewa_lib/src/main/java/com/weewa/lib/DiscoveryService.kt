package com.weewa.lib

import android.content.Context
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.weewa.lib.Prefs.serverPort
import com.weewa.lib.Prefs.serverPort2
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class HostMetadata(
    val ip: String,
    var name: String,
    var type: String,
    var port: Int,
    var port2: Int,
    var httpPort: Int,
    var mode: Int
)

@Serializable
internal data class BroadcastPacket(
    val request: Boolean,
    val device_name: String? = null,
    val device_type: String? = null,
    val port: Int? = null,
    val port2: Int? = null,
    val http_port: Int? = null,
    val mode: Int? = null,
    val ip: String? = null
)

interface IDiscoveryServiceDelegate {
    fun onNewHost(deviceName: String, ip: String, port: Int, httpPort: Int)
}

class DiscoveryService(ctx: Context) {
    private var _ipMetas: MutableMap<String, HostMetadata> = ConcurrentHashMap()
    private var _hostMetas: MutableMap<String, HostMetadata> = ConcurrentHashMap()
    private var _socket: DatagramSocket? = null
    private val _threadPool: ExecutorService = Executors.newFixedThreadPool(2)
    private var _receivedPacket: DatagramPacket? = null
    private val BUFFER_LENGTH = 1024
    private val _buf = ByteArray(BUFFER_LENGTH)
    private var _thread: Thread? = null
    private var _refreshThread: Thread? = null
    private val tag = javaClass.simpleName
    @Volatile private var _running = false
    private val protocolJson = Json { ignoreUnknownKeys = true }
    private var _appCtx: Context? = null
    private var _delegate: IDiscoveryServiceDelegate? = null
    private var _boundNetwork: Network? = null
    private val networkLock = Any()

    init {
        _appCtx = ctx.applicationContext
    }

    fun setDelegate(d: IDiscoveryServiceDelegate) {
        _delegate = d
    }

    @Synchronized fun start() {
        if (_socket != null) return
        // The no-argument constructor already binds an ephemeral port.
        _socket = DatagramSocket(null)
        _socket?.reuseAddress = true
        _socket?.broadcast = true
        try {
            _socket?.bind(InetSocketAddress(if (WeewaLib.mode == WeewaMode.SLAVE1) Const.DISCOVERY_PORT else Const.DISCOVERY_PORT2))
        } catch (e: Exception) {
            _socket?.close()
            _socket = null
            e.localizedMessage?.let { Log.e(tag, it) }
            return
        }
        bindWifiNetwork()
        if (_receivedPacket == null) {
            _receivedPacket = DatagramPacket(_buf, BUFFER_LENGTH)
        }
        startSocketThread()
        startRefreshThread()
        sendInfo()
        AppTrace.d(tag, "discovery service started, port: ${_socket?.localPort}")
    }

    @Throws(IOException::class)
    private fun getBroadcastAddress(): InetAddress? {
        val wifi = _appCtx?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    private fun getWifiIp(): String {
        val wifi = _appCtx?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        return getIpAddress(dhcp.ipAddress)
    }

    private fun getIpAddress(raw: Int): String {
        val ipAddress = StringBuilder()
        for(i in 0..3) {
            ipAddress.append((raw ushr (8 * i)) and 0xFF)
            if (i < 3) {
                ipAddress.append(".")
            }
        }
        return ipAddress.toString()
    }
    private fun isLocalAddress(ip: String): Boolean {
        val wifi = _appCtx?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val localIp = getIpAddress(dhcp.ipAddress)
        return localIp == ip
    }

    private fun startSocketThread() {
        _thread = Thread {
            loop()
        }
        _running = true
        _thread?.start()
    }

    private fun startRefreshThread() {
        _refreshThread = Thread {
            try {
                while (_running) {
                    Thread.sleep(1000)
                    if (_running) refresh()
                }
            } catch (_: InterruptedException) {
                // Normal service shutdown.
            }
            AppTrace.d(tag, "refresh thread exit")
        }
        _refreshThread?.start()
    }

    private fun loop() {
        while (_running) {
            try {
                _socket?.receive(_receivedPacket)
                if (_receivedPacket == null || _receivedPacket?.length == 0)
                    continue

                // if request, send self info back
                val str = String(_receivedPacket!!.data, _receivedPacket!!.offset, _receivedPacket!!.length)
                val packet = protocolJson.decodeFromString<BroadcastPacket>(str)
                if(packet.request) {
                    sendInfo()
                    continue
                }

                // we need all fields
                if(packet.device_name == null ||
                    packet.device_type == null ||
                    packet.port == null ||
                    packet.http_port == null ||
                    packet.ip == null) {
                    continue
                }

                // if new host, save
                val ip = packet.ip

                // if local ip, ignore
                if(isLocalAddress(ip))
                    continue

                // port2 and mode
                val port2 = packet.port2 ?: 0
                val mode = if(packet.mode == null) WeewaMode.SLAVE1 else WeewaMode.values().firstOrNull { it.v == packet.mode } ?: continue

                // check meta
                val key = "${packet.device_name},${packet.ip}"
                if(!_hostMetas.containsKey(key)) {
                    val meta = HostMetadata(
                        ip,
                        packet.device_name,
                        packet.device_type,
                        packet.port,
                        port2,
                        packet.http_port,
                        mode.v
                    )
                    _ipMetas[ip] = meta
                    _hostMetas[key] = meta
                    AppTrace.d(tag, "found new host ${packet.device_name}/$ip, mode: ${mode.name}, port: ${packet.port}, port2: $port2, http port: ${packet.http_port}")
                    _delegate?.onNewHost(packet.device_name, ip, packet.port, packet.http_port)
                } else {
                    val meta = _hostMetas[key]
                    if(meta!!.name != packet.device_name ||
                        meta.port != packet.port ||
                        meta.port2 != port2 ||
                        meta.mode != mode.v ||
                        meta.httpPort != packet.http_port) {
                        meta.name = packet.device_name
                        meta.port = packet.port
                        meta.port2 = port2
                        meta.httpPort = packet.http_port
                        meta.mode = mode.v
                        meta.type = packet.device_type
                        _ipMetas[ip] = meta
                        AppTrace.d(tag, "host info updated => ${packet.device_name}/$ip, mode: ${mode.name}, port: ${packet.port}, port2: $port2, http port: ${packet.http_port}")
                    }
                }

                // reset packet
                _receivedPacket?.length = BUFFER_LENGTH
            } catch (e: IOException) {
                if(_running) {
                    e.localizedMessage?.let { Log.e(tag, it) }
                }
                return
            } catch (e: IllegalArgumentException) {
                // One malformed/extended discovery packet must not kill the app.
                AppTrace.d(tag, "ignore invalid discovery packet: ${e.localizedMessage}")
            } finally {
                // Also reset after request/ignored packets, otherwise the next
                // datagram is truncated to the preceding packet's length.
                _receivedPacket?.length = BUFFER_LENGTH
            }
        }

        AppTrace.d(tag, "socket thread exit")
    }

    @Synchronized fun stop() {
        _running = false
        // Close first to release blocking receive(), then wait for workers.
        _socket?.close()
        _refreshThread?.interrupt()
        if(_thread != Thread.currentThread()) _thread?.join(1500)
        if(_refreshThread != Thread.currentThread()) _refreshThread?.join(1500)
        _socket = null
        _receivedPacket = null
        _thread = null
        _refreshThread = null
        _ipMetas.clear()
        _hostMetas.clear()
        _boundNetwork = null
    }

    fun getMeta(ip: String): HostMetadata? {
        return _ipMetas[ip]
    }

    fun getMetaByName(name: String): HostMetadata? {
        for ((_, meta) in _ipMetas) {
            if (meta.name == name) {
                return meta
            }
        }
        return null
    }

    private fun bindWifiNetwork(): Boolean = synchronized(networkLock) {
        val network = WifiTransport.network(_appCtx!!) ?: return@synchronized false
        if (network != _boundNetwork) {
            try {
                val socket = _socket ?: return@synchronized false
                network.bindSocket(socket)
                _boundNetwork = network
                _ipMetas.clear()
                _hostMetas.clear()
            } catch (e: IOException) {
                Log.e(tag, "Cannot bind camera discovery to Wi-Fi", e)
                return@synchronized false
            }
        }
        true
    }

    private fun refresh() {
        if (!bindWifiNetwork()) return
        val jsonData = Json.encodeToString(
            BroadcastPacket.serializer(), BroadcastPacket(
                true,
                Build.DEVICE,
                Build.MODEL,
                Prefs.defaultPreference(_appCtx!!).serverPort,
                Prefs.defaultPreference(_appCtx!!).serverPort2,
                0,
                WeewaLib.mode.v,
                getWifiIp()
            )
        )
        val bytes = jsonData.toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(bytes, bytes.size, getBroadcastAddress(), Const.DISCOVERY_PORT)
        try {
            _socket?.send(packet)
        } catch (e: Exception) {
        }
        val packet2 = DatagramPacket(bytes, bytes.size, getBroadcastAddress(), Const.DISCOVERY_PORT2)
        try {
            _socket?.send(packet2)
        } catch (e: Exception) {
        }
    }

    private fun sendInfo() {
        _threadPool.execute {
            try {
                if (!bindWifiNetwork()) return@execute
                val jsonData = Json.encodeToString(
                    BroadcastPacket.serializer(), BroadcastPacket(
                        false,
                        Build.MODEL,
                        "Android",
                        Prefs.defaultPreference(_appCtx!!).serverPort,
                        Prefs.defaultPreference(_appCtx!!).serverPort2,
                        0,
                        WeewaLib.mode.v,
                        getWifiIp()
                    )
                )
                val bytes = jsonData.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(bytes, bytes.size, getBroadcastAddress(), Const.DISCOVERY_PORT)
                _socket?.send(packet)
                val packet2 = DatagramPacket(bytes, bytes.size, getBroadcastAddress(), Const.DISCOVERY_PORT2)
                _socket?.send(packet2)
            } catch (e: UnknownHostException) {
                e.localizedMessage?.let { Log.e(tag, it) }
            } catch (e: IOException) {
                e.localizedMessage?.let { Log.e(tag, it) }
            }
        }
    }
}
