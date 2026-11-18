package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FanSpeedRequestBean extends BaseBluetoothBean {
    int rpm;

    public FanSpeedRequestBean(int rpm) {
        this.c = BleConstants.BLE_CMD_SET_FAN_SPEED;
        this.rpm = rpm;
    }

    public int getRpm() {
        return rpm;
    }

    public void setRpm(int rpm) {
        this.rpm = rpm;
    }
}
