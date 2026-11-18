package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

import java.util.List;

@Keep
public class VideoListRequestBean extends BaseBluetoothBean {
    public String request_id;
    public VideoListRequestBean() {
        this.c = BleConstants.BLE_CMD_VIDEO_LIST;
    }
    public VideoListRequestBean(String requestId) { this(); request_id=requestId; }
}
