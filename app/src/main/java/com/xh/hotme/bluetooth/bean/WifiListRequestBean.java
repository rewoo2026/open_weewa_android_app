package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class WifiListRequestBean extends BaseBluetoothBean {

    public WifiListRequestBean() {
        this.c = BleConstants.BLE_CMD_WIFILIST;
    }
}
