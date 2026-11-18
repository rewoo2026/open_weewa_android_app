package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class NetStatusRequestBean extends BaseBluetoothBean {
    public NetStatusRequestBean() {
        this.c = BleConstants.BLE_CMD_NETWORK_STATUS;
    }
}
