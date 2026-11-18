package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestAudioStartRequestBean extends BaseBluetoothBean {

    public FactoryTestAudioStartRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_AUDIO_START;
    }
}
