package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class OSDControlEvent {
    public final int state;

    public OSDControlEvent(int st) {
        this.state = st;
    }
}
