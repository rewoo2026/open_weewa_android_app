package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class RkIpcEvent {
    public final int status;
    public String message;

    public RkIpcEvent(int st) {
        this.status = st;
    }

    public RkIpcEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
