package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 发送绑定
 */
@Keep
public class DeviceActiveBindRequestBean extends BaseBluetoothBean {
    final String mb;
    final String mac;

    public DeviceActiveBindRequestBean(String mobile, String mac) {
        this.c = BleConstants.BLE_CMD_DEVICE_ACTIVE_BIND;
        this.mb = mobile;
        // Fixed-code verification can return data={}. The camera protocol still
        // requires a string; the server identifies the camera by its deviceId.
        this.mac = mac == null ? "" : mac;
    }
}
