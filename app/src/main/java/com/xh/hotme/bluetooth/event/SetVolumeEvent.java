package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetVolumeEvent {
    public final int status;

    public SetVolumeEvent(int status) {
        this.status = status;
    }
}
