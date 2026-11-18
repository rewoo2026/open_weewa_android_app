package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetVolumeRequestBean extends BaseBluetoothBean {
    public final int volume;

    public SetVolumeRequestBean(int v) {
        this.c = BleConstants.BLE_CMD_SET_VOLUME;
        volume = v;
    }
}
