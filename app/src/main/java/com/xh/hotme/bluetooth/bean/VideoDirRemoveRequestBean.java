package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class VideoDirRemoveRequestBean extends BaseBluetoothBean {

    final String path;
    final String request_id;
    final String expected_device_id;
    final String expected_time;

    public VideoDirRemoveRequestBean(String path) {
        this(path, null, null, null);
    }

    public VideoDirRemoveRequestBean(String path, String requestId, String deviceId,String expectedTime) {
        this.c = BleConstants.BLE_CMD_VIDEO_DELETE;
        this.path = path;
        this.request_id = requestId;
        this.expected_device_id = deviceId;
        this.expected_time=expectedTime;
    }
}
