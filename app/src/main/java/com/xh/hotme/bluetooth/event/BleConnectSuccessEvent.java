package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleConnectSuccessEvent {
    public final String address;

    public BleConnectSuccessEvent(String address) {
        this.address = address;
    }
}
