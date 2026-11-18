package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetVideoNameEvent {
    public final int state;

    public SetVideoNameEvent(int st) {
        this.state = st;
    }
}
