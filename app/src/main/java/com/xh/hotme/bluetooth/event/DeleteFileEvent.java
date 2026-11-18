package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeleteFileEvent {
    public final int state;

    public DeleteFileEvent(int st) {
        this.state = st;
    }
}
