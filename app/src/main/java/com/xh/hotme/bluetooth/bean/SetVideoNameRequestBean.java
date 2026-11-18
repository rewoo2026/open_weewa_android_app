package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetVideoNameRequestBean extends BaseBluetoothBean {
    final String vn;

    public SetVideoNameRequestBean(String name) {
        this.c = BleConstants.BLE_CMD_SET_VIDEO_NAME;
        this.vn = name;
    }
}
