package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class PostProcProgressRequestBean extends BaseBluetoothBean {

    public PostProcProgressRequestBean() {
        this.c = BleConstants.BLE_CMD_POST_PROC_PROGRESS;
    }
}
