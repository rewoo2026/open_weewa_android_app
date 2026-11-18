package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

/**
 * 开始Fec预览
 */
@Keep
public class FecRtmpPreviewStartEvent {
    public final int status;
    public String message;

    public FecRtmpPreviewStartEvent(int st) {
        this.status = st;
    }

    public FecRtmpPreviewStartEvent(int st, String msg) {
        this.status = st;
        this.message = msg;
    }
}
