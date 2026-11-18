package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FecRtmpAddUrlEvent {
    public final int status;
    public String message;

    public FecRtmpAddUrlEvent(int st) {
        this.status = st;
    }

    public FecRtmpAddUrlEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
