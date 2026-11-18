package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class RkipcStopRequestBean extends BaseBluetoothBean {

    public RkipcStopRequestBean() {
        this.c = BleConstants.BLE_CMD_RKIPC_STOP;
    }
}
