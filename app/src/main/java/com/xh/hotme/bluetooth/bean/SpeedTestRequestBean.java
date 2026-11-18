package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SpeedTestRequestBean extends BaseBluetoothBean {

    public SpeedTestRequestBean() {
        this.c = BleConstants.BLE_CMD_4G_TEST_SPEED;
    }
}