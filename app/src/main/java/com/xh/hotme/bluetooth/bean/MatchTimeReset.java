package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class MatchTimeReset extends BaseBluetoothBean {

    public MatchTimeReset() {
        this.c = BleConstants.BLE_CMD_MATCH_TIME_RESET;
    }
}