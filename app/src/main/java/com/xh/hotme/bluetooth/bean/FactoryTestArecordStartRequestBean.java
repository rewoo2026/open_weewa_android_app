package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class FactoryTestArecordStartRequestBean extends BaseBluetoothBean {

    int channel = 0;
    int duration = 20;
    String card_name = "hw:1,0";
    String p = "/data/weewa/debug/arecord.wav";

    public FactoryTestArecordStartRequestBean() {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_ARECORD_START;
    }

    public FactoryTestArecordStartRequestBean(String cardName, int channel, int duration, String path) {
        this.c = BleConstants.BLE_CMD_FACTORY_TEST_ARECORD_START;
        this.card_name = cardName;
        this.channel = channel;
        this.duration = duration;
        this.p = path;
    }
}
