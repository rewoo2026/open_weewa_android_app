package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleDisConnectingEvent {
    public final String address;

    public BleDisConnectingEvent(String address) {
        this.address = address;
    }
}
