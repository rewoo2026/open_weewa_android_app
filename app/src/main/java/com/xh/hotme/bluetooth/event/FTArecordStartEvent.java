package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FTArecordStartEvent {
    public int status;

    public FTArecordStartEvent(int s){
        this.status = s;
    }
}
