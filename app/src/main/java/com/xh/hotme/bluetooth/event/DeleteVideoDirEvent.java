package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeleteVideoDirEvent {
    public final int state;

    public DeleteVideoDirEvent(int st) {
        this.state = st;
    }
}
