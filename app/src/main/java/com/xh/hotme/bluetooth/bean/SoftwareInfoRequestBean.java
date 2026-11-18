package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SoftwareInfoRequestBean extends BaseBluetoothBean {

    public SoftwareInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_SOFTWARE_INFO;
    }
}
