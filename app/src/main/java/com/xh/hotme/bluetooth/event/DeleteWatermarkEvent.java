package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeleteWatermarkEvent {
    public final String result;

    public DeleteWatermarkEvent(String result) {
        this.result = result;
    }
}
