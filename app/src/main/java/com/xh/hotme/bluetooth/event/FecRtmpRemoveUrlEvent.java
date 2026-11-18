package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

/**
 * 删除fec推流url
 */
@Keep
public class FecRtmpRemoveUrlEvent {
    public final int status;
    public String message;

    public FecRtmpRemoveUrlEvent(int st) {
        this.status = st;
    }

    public FecRtmpRemoveUrlEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
