package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class StopAutoUploadRequestBean extends BaseBluetoothBean {

    public StopAutoUploadRequestBean() {
        this.c = BleConstants.BLE_CMD_STOP_AUTO_UPLOAD;
    }
}
