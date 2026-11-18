package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeviceUpdateSNRequestBean extends BaseBluetoothBean {
    final String sn;

    public DeviceUpdateSNRequestBean(String name) {
        this.c = BleConstants.BLE_CMD_UPDATE_SN;
        this.sn = name;
    }
}
