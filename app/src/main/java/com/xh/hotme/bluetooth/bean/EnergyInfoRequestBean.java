package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class EnergyInfoRequestBean extends BaseBluetoothBean {

    public EnergyInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_ENERGY;
    }
}
