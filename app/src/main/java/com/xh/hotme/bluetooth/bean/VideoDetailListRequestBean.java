package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class VideoDetailListRequestBean extends BaseBluetoothBean {

    String p;
    public VideoDetailListRequestBean(String path) {
        this.c = BleConstants.BLE_CMD_VIDEO_DETAIL_LIST;
        this.p = path;
    }
}
