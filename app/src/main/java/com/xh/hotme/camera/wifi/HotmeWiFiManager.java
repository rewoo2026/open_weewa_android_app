package com.xh.hotme.camera.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;

import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

public class HotmeWiFiManager {

    public static void startWifiSettingPage(Context context) {
        context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    public static void startWifiSettingPage(Activity context, int requestCode) {
        context.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), requestCode);
    }
}
