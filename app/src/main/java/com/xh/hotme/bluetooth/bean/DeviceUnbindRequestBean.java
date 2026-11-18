package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 发送设备解绑
 **/
@Keep
public class DeviceUnbindRequestBean extends BaseBluetoothBean {
    final String mb;
    final String mac;
    final String code;

    final String token;

    public DeviceUnbindRequestBean(String mobile, String code, String mac, String token) {
        this.c = BleConstants.BLE_CMD_DEVICE_UNBIND;
        this.mb = mobile;
        this.code = code;

        this.mac = mac;
        this.token = token;
    }
}
