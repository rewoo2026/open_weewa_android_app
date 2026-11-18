package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class CommonResponse implements Serializable {
    public int code;
    public String msg;
}
