package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class HighTempEvent {
    public final float temp;

    public HighTempEvent(float t) {
        this.temp = t;
    }
}
