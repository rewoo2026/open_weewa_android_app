package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetApnRequestBean extends BaseBluetoothBean {
    public String apn;

    public SetApnRequestBean(String apn) {
        this.c = BleConstants.BLE_CMD_SET_APN;
        this.apn = apn;
    }
}
