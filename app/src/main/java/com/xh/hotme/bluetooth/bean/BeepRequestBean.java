package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class BeepRequestBean extends BaseBluetoothBean {

    public BeepRequestBean() {
        this.c = BleConstants.BLE_CMD_BEEP;
    }
}
