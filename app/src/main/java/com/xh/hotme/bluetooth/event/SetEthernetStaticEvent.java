package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetEthernetStaticEvent {
    public final int status;
    public SetEthernetStaticEvent(int st) {
        this.status = st;
    }
}