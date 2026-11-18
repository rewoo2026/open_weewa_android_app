package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class PostProcStartEvent {
    public final int state;

    public PostProcStartEvent(int state) {
        this.state = state;
    }
}
