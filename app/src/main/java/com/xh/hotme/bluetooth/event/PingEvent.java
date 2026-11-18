package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class PingEvent {
    public int status;

    public PingEvent(int status){
        this.status = status;
    }
}
