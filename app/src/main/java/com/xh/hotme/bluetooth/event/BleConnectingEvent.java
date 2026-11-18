package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleConnectingEvent {
    public final String address;
    public final String name;

    public BleConnectingEvent(String address, String name) {
        this.address = address;
        this.name = name;
    }
}
