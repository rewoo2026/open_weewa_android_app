package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SoftApCloseRequestBean extends BaseBluetoothBean {

    public SoftApCloseRequestBean() {
        this.c = BleConstants.BLE_CMD_SOFTAP_CLOSE;
    }
}
