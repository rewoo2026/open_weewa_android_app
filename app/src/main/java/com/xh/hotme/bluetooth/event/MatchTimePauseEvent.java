package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class MatchTimePauseEvent {
    public int status;
    public MatchTimePauseEvent(int s){
        this.status = s;
    }
}
