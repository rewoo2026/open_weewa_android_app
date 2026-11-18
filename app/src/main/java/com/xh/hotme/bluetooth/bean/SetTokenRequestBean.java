package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetTokenRequestBean extends BaseBluetoothBean {
    final String t;

    public SetTokenRequestBean(String token) {
        this.c = BleConstants.BLE_CMD_SET_TOKEN;
        this.t = token;
    }
}
