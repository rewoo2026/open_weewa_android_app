package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class PoweroffRequestBean extends BaseBluetoothBean {

    public PoweroffRequestBean() {
        this.c = BleConstants.BLE_CMD_POWER_OFF;
    }
}
