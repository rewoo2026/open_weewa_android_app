package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class BeepEvent {
    public int state;

    public BeepEvent(int st) {
        this.state = st;
    }
}
