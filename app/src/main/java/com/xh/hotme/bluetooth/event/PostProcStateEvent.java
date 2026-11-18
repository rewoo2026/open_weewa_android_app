package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.PostProc;

@Keep
public class PostProcStateEvent {
    public final PostProc postProcInfo;

    public PostProcStateEvent(PostProc info) {
        this.postProcInfo = info;
    }
}
