package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class RemoveDeviceEvent {
    public final String mac;

    public RemoveDeviceEvent(String bleMac) {
        this.mac = bleMac;
    }
}
