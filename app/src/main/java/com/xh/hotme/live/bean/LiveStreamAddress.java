package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class LiveStreamAddress implements Serializable {
    public String liveObjectId;
    public String pullStreamAddress;
    public String pushStreamAddress;
    public String pushStreamKey;
}
