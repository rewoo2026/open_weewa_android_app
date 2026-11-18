package com.weewa.lib

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WeewaLibService: Service() {
    private val _binder = Binder()

    class Binder: android.os.Binder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WeewaLib.shared().launch(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder {
        return _binder
    }
}