package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class RecordStatusRequestBean extends BaseBluetoothBean {

    public RecordStatusRequestBean() {
        this.c = BleConstants.BLE_CMD_RECORDER_STATUS;
    }
}
