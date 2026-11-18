package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class WifiTurnOffRequestBean extends BaseBluetoothBean {

    public WifiTurnOffRequestBean() {
        this.c = BleConstants.BLE_CMD_WIFI_TURN_OFF;
    }
}
