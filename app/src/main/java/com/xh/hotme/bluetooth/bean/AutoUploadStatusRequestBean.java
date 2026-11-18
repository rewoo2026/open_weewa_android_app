package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class AutoUploadStatusRequestBean extends BaseBluetoothBean {

    public AutoUploadStatusRequestBean() {
        this.c = BleConstants.BLE_CMD_AUTO_UPLOAD_STATUS;
    }
}
