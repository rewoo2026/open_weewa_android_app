package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class OTADownloadRequestBean extends BaseBluetoothBean {

    public OTADownloadRequestBean() {
        this.c = BleConstants.BLE_CMD_START_OTA_DOWNLOAD;
    }
}
