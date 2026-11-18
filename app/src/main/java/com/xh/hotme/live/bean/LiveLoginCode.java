package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class LiveLoginCode implements Serializable {
    public String qrcode;
    public String token;
}
