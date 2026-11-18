package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class OTAInstallSucEvent {
    public final int status;

    public OTAInstallSucEvent(int status) {
        this.status = status;
    }
}
