package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class WifiSetupRequestBean extends BaseBluetoothBean {
    public final String s;
    public final String p;

    public WifiSetupRequestBean(String ssid, String password) {
        this.c = BleConstants.BLE_CMD_WIFI_SETUP;
        this.s = ssid;
        this.p = password;
    }
}
