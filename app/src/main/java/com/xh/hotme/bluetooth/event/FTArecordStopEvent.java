package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FTArecordStopEvent {
    public int status;

    public FTArecordStopEvent(int s){
        this.status = s;
    }
}
