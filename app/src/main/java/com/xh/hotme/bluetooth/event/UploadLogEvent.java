package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class UploadLogEvent {
    public String status;

    public UploadLogEvent(String s){
        this.status = s;
    }
}
