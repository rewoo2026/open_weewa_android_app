package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BleDisConnectSuccessEvent {
    public final String address;

    public boolean isRemote = false;

    public BleDisConnectSuccessEvent(String address) {
        this.address = address;
    }
    public BleDisConnectSuccessEvent(String address, boolean remote) {
        this.address = address;
        this.isRemote = remote;
    }
}
