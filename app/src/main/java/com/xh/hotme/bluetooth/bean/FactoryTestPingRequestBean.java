package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestPingRequestBean extends BaseBluetoothBean {

    public FactoryTestPingRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_PING;
    }
}
