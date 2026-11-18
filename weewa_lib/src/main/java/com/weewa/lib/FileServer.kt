package com.weewa.lib

import android.content.Context
import android.util.Log
import com.weewa.lib.Prefs.serverPort
import com.weewa.lib.Prefs.serverPort2
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

interface IFileServerDelegate {
    fun onNewReceiver(socket: Socket)
}

class FileServer(ctx: Context) {
    private var _serverSocket: ServerSocket? = null
    private var _appCtx: Context? = null
    private lateinit var _thread: Thread
    private var _running = false
    private var _delegate: IFileServerDelegate? = null
    private val tag = javaClass.simpleName

    init {
        _appCtx = ctx.applicationContext
    }

    fun start() {
        val prefs = Prefs.defaultPreference(_appCtx!!)
        _serverSocket = ServerSocket()
        _serverSocket?.reuseAddress = true
        var success = false
        while(!success) {
            try {
                _serverSocket?.bind(InetSocketAddress(if (WeewaLib.mode == WeewaMode.SLAVE1) prefs.serverPort else prefs.serverPort2))
                success = true
            } catch (e: BindException) {
                WeewaLib.shared().rerandServerPort()
            }
        }
        startServerThread()
    }

    fun setDelegate(d: IFileServerDelegate) {
        _delegate = d
    }

    private fun startServerThread() {
        _thread = Thread(Runnable {
            loop()
        })
        _running = true
        _thread.start()
        AppTrace.d(tag, String.format("file server start listening on %d", Prefs.defaultPreference(_appCtx!!).serverPort))
    }

    private fun loop() {
        while (_running) {
            try{
                val socket = _serverSocket?.accept()
                _delegate?.onNewReceiver(socket!!)
            } catch(e:Exception){
                e.printStackTrace()
            }

        }
    }
}