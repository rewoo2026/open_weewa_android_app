package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class StorageInfoRequestBean extends BaseBluetoothBean {

    public StorageInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_STORAGE;
    }
}
