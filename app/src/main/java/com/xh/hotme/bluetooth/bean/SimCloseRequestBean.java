package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SimCloseRequestBean extends BaseBluetoothBean {

    public SimCloseRequestBean() {
        this.c = BleConstants.BLE_CMD_SIM_CLOSE;
    }
}
