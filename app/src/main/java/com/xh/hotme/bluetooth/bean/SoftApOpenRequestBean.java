package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SoftApOpenRequestBean extends BaseBluetoothBean {
    public final String s;
    public final String p;

    public SoftApOpenRequestBean(String ssid, String password) {
        this.c = BleConstants.BLE_CMD_SOFTAP_OPEN;
        this.s = ssid;
        this.p = password;
    }
}
