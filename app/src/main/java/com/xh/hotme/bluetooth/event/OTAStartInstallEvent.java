package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class OTAStartInstallEvent {
    public final int status;
    public String message;

    public OTAStartInstallEvent(int status, String msg) {
        this.status = status;
        this.message = msg;
    }
}
