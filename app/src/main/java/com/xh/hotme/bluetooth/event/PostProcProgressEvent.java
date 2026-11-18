package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class PostProcProgressEvent {
    public final int progress;

    public PostProcProgressEvent(int info) {
        this.progress = info;
    }
}
