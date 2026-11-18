package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class WifiBean implements Serializable {
    public String ssid;
    public String password;

    public WifiBean(String s, String p) {
        this.ssid = s;
        this.password = p;
    }
}
