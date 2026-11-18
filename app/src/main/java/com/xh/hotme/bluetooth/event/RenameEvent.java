package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class RenameEvent {
    public final String name;
    public final String address;

    public RenameEvent(String address, String n) {
        this.name = n;
        this.address = address;
    }
}
