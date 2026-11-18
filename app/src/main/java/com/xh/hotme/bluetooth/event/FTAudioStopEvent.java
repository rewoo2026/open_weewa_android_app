package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FTAudioStopEvent {
    public int status;
    public String path;

    public FTAudioStopEvent(int s, String path){
        this.status = s;
        this.path = path;
    }
}
