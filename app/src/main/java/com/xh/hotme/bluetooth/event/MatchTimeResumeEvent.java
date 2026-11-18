package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.bean.MatchTimeResume;

@Keep
public class MatchTimeResumeEvent {
    public int status;
    public MatchTimeResumeEvent(int s){
        this.status = s;
    }
}
