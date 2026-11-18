package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 删除多个推流地址
 */
@Keep
public class FecRtmpRemoveUrlsRequestBean extends BaseBluetoothBean {
    final String[] urls;
    final String[] keys;

    public FecRtmpRemoveUrlsRequestBean(String[] url, String[] key) {
        this.c = BleConstants.BLE_CMD_FEC_LIVE_URL_REMOVE;
        this.urls = url;
        this.keys = key;
    }
}
