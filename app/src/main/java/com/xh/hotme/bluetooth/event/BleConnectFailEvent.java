package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleConnectFailEvent {
    public final String address;

    public BleConnectFailEvent(String address) {
        this.address = address;
    }
}
