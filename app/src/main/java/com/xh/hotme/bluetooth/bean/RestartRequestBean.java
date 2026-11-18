package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 发送重启命令
 */
@Keep
public class RestartRequestBean extends BaseBluetoothBean {

    public RestartRequestBean() {
        this.c = BleConstants.BLE_CMD_RESTART;
    }
}
