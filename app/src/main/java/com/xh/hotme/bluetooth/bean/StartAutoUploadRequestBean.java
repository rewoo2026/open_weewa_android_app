package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class StartAutoUploadRequestBean extends BaseBluetoothBean {

    public StartAutoUploadRequestBean() {
        this.c = BleConstants.BLE_CMD_START_AUTO_UPLOAD;
    }
}
