package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class UploadLogRequestBean extends BaseBluetoothBean {
    public UploadLogRequestBean() {
        this.c = BleConstants.BLE_CMD_UPLOAD_LOG;
    }
}
