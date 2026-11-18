package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.NetworkBean;

@Keep
public class NetworkStatusEvent {
    public final NetworkBean n;

    public NetworkStatusEvent(NetworkBean n) {
        this.n = n;
    }
}
