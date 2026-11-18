package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.WeewaInfo;

@Keep
public class WeewaInfoEvent {
    public final WeewaInfo weewaInfo;

    public WeewaInfoEvent(WeewaInfo info) {
        this.weewaInfo = info;
    }
}
