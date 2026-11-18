package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class CommonFinishEvent {
    public static final int TYPE_PAY_SUCCESS = 1001;
    public static final int TYPE_LIVE_SUCCESS = 1002;
    public final int status;

    public CommonFinishEvent(int s) {
        this.status = s;
    }
}
