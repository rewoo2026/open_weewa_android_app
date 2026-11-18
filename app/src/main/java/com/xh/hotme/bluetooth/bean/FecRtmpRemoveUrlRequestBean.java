package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 删除推流地址和密钥
 */
@Keep
public class FecRtmpRemoveUrlRequestBean extends BaseBluetoothBean {
    final String url;
    final String key;

    public FecRtmpRemoveUrlRequestBean(String url) {
        this.c = BleConstants.BLE_CMD_FEC_LIVE_URL_REMOVE;
        this.url = url;
        this.key = "";
    }
}
