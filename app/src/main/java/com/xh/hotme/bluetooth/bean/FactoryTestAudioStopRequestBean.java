package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestAudioStopRequestBean extends BaseBluetoothBean {

    public FactoryTestAudioStopRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_AUDIO_STOP;
    }
}
