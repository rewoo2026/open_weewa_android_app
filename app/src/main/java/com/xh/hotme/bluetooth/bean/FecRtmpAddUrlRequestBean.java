package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 添加fec推流url
 */
@Keep
public class FecRtmpAddUrlRequestBean extends BaseBluetoothBean {
    final String url;
    final String key;
    final int plt;

    public FecRtmpAddUrlRequestBean(String url) {
        this.c = BleConstants.BLE_CMD_FEC_LIVE_URL_ADD;
        // The caller supplies the complete push URL, including authentication.
        // Never replace or split it using a saved address/key from another live.
        this.url = url;
        this.key = "";
        this.plt = 4;
    }
}
