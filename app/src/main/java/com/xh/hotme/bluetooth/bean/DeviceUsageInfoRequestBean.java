package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeviceUsageInfoRequestBean extends BaseBluetoothBean {

    public DeviceUsageInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_USAGE_INFO;
    }
}
