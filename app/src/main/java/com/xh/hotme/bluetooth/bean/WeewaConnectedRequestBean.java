package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 当蓝牙连接成功后，需要发送一个connected命令，否则相机3秒后会主动断开连接
 */
@Keep
public class WeewaConnectedRequestBean extends BaseBluetoothBean {

    public WeewaConnectedRequestBean() {
        this.c = BleConstants.BLE_CMD_WEEWA_CONNECTED;
    }
}
