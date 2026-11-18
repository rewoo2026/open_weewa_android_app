package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class GetOSDSettingsRequestBean extends BaseBluetoothBean {
    public GetOSDSettingsRequestBean() {
        this.c = BleConstants.BLE_CMD_GET_OSD_SETTINGS;
    }
}
