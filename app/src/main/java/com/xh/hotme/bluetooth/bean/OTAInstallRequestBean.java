package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class OTAInstallRequestBean extends BaseBluetoothBean {
    public final int restart;

    public OTAInstallRequestBean(int restart) {
        this.c = BleConstants.BLE_CMD_START_OTA;
        this.restart = restart;
    }
}
