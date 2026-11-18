package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SoftApInfoRequestBean extends BaseBluetoothBean {

    public SoftApInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_SOFTAP_STATUS;
    }
}
