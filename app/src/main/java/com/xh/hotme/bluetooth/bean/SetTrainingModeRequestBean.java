package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetTrainingModeRequestBean extends BaseBluetoothBean {
    public boolean on;

    public SetTrainingModeRequestBean(boolean on) {
        this.c = BleConstants.BLE_CMD_SET_TRAINING_MODE;
        this.on = on;
    }
}
