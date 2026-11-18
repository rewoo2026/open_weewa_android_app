package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SoftApOpenEvent {
    private final String ssid;
    private final String password;

    public SoftApOpenEvent(String ssid, String password) {
        this.ssid = ssid;
        this.password = password;
    }
}
