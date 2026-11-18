package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class PostProcStatusRequestBean extends BaseBluetoothBean {
    private int type = 2;
    public PostProcStatusRequestBean(int type) {
        this.c = BleConstants.BLE_CMD_POST_PROC_STATE;
        this.type = type;
    }
}
