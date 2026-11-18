package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

/**
 * 删除fec推流url
 */
@Keep
public class FecRtmpRemoveAllUrlEvent {
    public final int status;
    public String message;

    public FecRtmpRemoveAllUrlEvent(int st) {
        this.status = st;
    }

    public FecRtmpRemoveAllUrlEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
