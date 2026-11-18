package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class WifiSetupEvent {
    public final String ssid;
    public final String password;

    public WifiSetupEvent(String ssid, String pwd) {
        this.ssid = ssid;
        this.password = pwd;
    }
}
