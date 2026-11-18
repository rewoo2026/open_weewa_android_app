package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class UploadCalibEvent {
    public int status;

    public UploadCalibEvent(int s) {
        this.status = s;
    }
}
