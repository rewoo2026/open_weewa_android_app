package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class PostProcStartRequestBean extends BaseBluetoothBean {
    public final float threshold;
    public final long duration;

    public final int type;

    public PostProcStartRequestBean(int type, float threshold, long duration) {
        this.c = BleConstants.BLE_CMD_START_POST_PROC;
        this.threshold = threshold;
        this.duration = duration;
        this.type = type;
    }


    public PostProcStartRequestBean(int type, long duration) {
        this.c = BleConstants.BLE_CMD_START_POST_PROC;
        this.threshold = 0.95f;
        this.duration = duration;
        this.type = type;
    }
}
