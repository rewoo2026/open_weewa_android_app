package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetWatermarkEvent {
    public final String result;

    public SetWatermarkEvent(String result) {
        this.result = result;
    }
}
