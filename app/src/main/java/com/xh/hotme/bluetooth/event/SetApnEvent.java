package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetApnEvent {
    public final int status;

    public SetApnEvent(int st) {
        this.status = st;
    }
}
