package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class RkipcStartRequestBean extends BaseBluetoothBean {

    public RkipcStartRequestBean() {
        this.c = BleConstants.BLE_CMD_RKIPC_START;
    }
}
