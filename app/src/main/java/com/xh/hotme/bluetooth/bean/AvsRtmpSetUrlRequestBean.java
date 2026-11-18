package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 设置avs推流url，仅支持一个url
 */
@Keep
public class AvsRtmpSetUrlRequestBean extends BaseBluetoothBean {
    final String url;

    public AvsRtmpSetUrlRequestBean(String url) {
        this.c = BleConstants.BLE_CMD_AVS_RTMP_URL_SET;
        this.url = url;
    }
}
