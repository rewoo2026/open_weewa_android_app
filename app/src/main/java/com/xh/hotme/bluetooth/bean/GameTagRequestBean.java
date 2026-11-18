package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class GameTagRequestBean extends BaseBluetoothBean {
    String label;

    public GameTagRequestBean(String label) {
        this.c = BleConstants.BLE_CMD_COMPETITION_LABEL;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
