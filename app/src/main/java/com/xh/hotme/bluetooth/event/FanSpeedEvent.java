package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FanSpeedEvent {
    public final int state;

    public FanSpeedEvent(int st) {
        this.state = st;
    }
}
