package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeviceInfoRequestBean extends BaseBluetoothBean {

    public DeviceInfoRequestBean() {
        this.c = BleConstants.BLE_CMD_DEVICE_INFO;
    }
}
