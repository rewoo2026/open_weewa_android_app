package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestFastChargeRequestBean extends BaseBluetoothBean {

    public FactoryTestFastChargeRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_FAST_CHARGE;
    }
}
