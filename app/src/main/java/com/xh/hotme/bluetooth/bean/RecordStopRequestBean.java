package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class RecordStopRequestBean extends BaseBluetoothBean {

    public RecordStopRequestBean() {
        this.c = BleConstants.BLE_CMD_RECORDER_STOP;
    }
}
