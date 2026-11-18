package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 添加多个推流url
 */
@Keep
public class FecRtmpAddUrlsRequestBean extends BaseBluetoothBean {
    final String[] urls;
    final String[] keys;

    public FecRtmpAddUrlsRequestBean(String[] urls, String[] keys) {
        this.c = BleConstants.BLE_CMD_FEC_LIVE_URL_ADD;
        this.urls = urls;
        this.keys = keys;
    }
}
