package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeviceUpdateNameRequestBean extends BaseBluetoothBean {
    final String n;

    public DeviceUpdateNameRequestBean(String name) {
        this.c = BleConstants.BLE_CMD_UPDATE_NAME;
        this.n = name;
    }
}
