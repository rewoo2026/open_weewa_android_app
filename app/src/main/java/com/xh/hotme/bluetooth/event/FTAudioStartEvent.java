package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FTAudioStartEvent {
    public int status;
    public String path;

    public FTAudioStartEvent(int s, String path){
        this.status = s;
        this.path = path;
    }
}
