package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class AvsRtmpSetUrlEvent {
    public final int status;
    public String message;

    public AvsRtmpSetUrlEvent(int st) {
        this.status = st;
    }

    public AvsRtmpSetUrlEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
