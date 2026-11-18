package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class MatchTimePause extends BaseBluetoothBean {

    public MatchTimePause() {
        this.c = BleConstants.BLE_CMD_MATCH_TIME_PAUSE;
    }
}
