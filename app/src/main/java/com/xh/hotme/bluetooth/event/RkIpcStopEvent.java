package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class RkIpcStopEvent {
    public final int status;
    public String message;

    public RkIpcStopEvent(int st) {
        this.status = st;
    }

    public RkIpcStopEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
