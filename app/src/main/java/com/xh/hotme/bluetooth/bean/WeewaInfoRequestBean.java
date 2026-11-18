package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class WeewaInfoRequestBean extends BaseBluetoothBean {

    public WeewaInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_WEEWA_INFO;
    }
}
