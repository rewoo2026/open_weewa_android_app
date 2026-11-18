package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

import java.util.List;

@Keep
public class VideoRemoveRequestBean extends BaseBluetoothBean {
    final List<String> sub;

    final String path;

    public VideoRemoveRequestBean(String path, List<String> types) {
        this.c = BleConstants.BLE_CMD_VIDEO_DELETE_SUB;
        this.path = path;
        this.sub = types;
    }
}
