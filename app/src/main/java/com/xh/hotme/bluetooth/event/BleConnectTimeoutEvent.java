package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleConnectTimeoutEvent {
    public final String address;

    public BleConnectTimeoutEvent(String address) {
        this.address = address;
    }
}
