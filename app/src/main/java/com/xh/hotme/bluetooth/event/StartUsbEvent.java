package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class StartUsbEvent {
    public final int state;

    public StartUsbEvent(int st) {
        this.state = st;
    }
}
