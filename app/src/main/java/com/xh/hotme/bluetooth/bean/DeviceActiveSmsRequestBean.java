package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 发送激活码
 */
@Keep
public class DeviceActiveSmsRequestBean extends BaseBluetoothBean {
    final String mb;
    final String bluetoothUuid;

    public DeviceActiveSmsRequestBean(String mobile, String mac) {
        this.c = BleConstants.BLE_CMD_DEVICE_ACTIVE_SMS;
        this.mb = mobile;
        this.bluetoothUuid = mac;
    }
}
