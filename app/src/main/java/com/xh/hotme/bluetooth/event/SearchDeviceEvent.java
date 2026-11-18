package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SearchDeviceEvent {
    public boolean isBackGround = false;
    public SearchDeviceEvent(boolean isBackGround){
        this.isBackGround = isBackGround;
    }
}
