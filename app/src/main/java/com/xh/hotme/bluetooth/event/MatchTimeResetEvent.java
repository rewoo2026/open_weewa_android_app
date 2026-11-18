package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class MatchTimeResetEvent {
    public int status;
    public MatchTimeResetEvent(int s){        
        this.status = s;
    }
}