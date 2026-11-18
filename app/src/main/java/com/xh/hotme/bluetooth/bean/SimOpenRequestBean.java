package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SimOpenRequestBean extends BaseBluetoothBean {

    public SimOpenRequestBean() {
        this.c = BleConstants.BLE_CMD_SIM_OPEN;
    }
}
