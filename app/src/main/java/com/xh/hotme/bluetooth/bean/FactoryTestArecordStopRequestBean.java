package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestArecordStopRequestBean extends BaseBluetoothBean {

    public FactoryTestArecordStopRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_ARECORD_STOP;
    }
}
