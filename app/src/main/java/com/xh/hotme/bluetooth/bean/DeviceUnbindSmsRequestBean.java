package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeviceUnbindSmsRequestBean extends BaseBluetoothBean {
    final String mb;
    final String mac;

    public DeviceUnbindSmsRequestBean(String mobile, String mac) {
        this.c = BleConstants.BLE_CMD_DEVICE_UNBIND_SMS;
        this.mb = mobile;
        this.mac = mac;
    }
}
