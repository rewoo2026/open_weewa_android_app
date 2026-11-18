package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

/**
 * 停止Fec预览
 */
@Keep
public class FecRtmpPreviewStopEvent {
    public final int status;
    public String message;

    public FecRtmpPreviewStopEvent(int st) {
        this.status = st;
    }

    public FecRtmpPreviewStopEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
