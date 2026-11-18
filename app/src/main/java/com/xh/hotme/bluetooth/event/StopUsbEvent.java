package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class StopUsbEvent {
    public final int state;

    public StopUsbEvent(int st) {
        this.state = st;
    }
}
