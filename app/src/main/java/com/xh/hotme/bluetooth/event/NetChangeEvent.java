package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.NetChangeInfo;

/**
 * 网络状态变化事件
 */
@Keep
public class NetChangeEvent {
    public final NetChangeInfo netChangeInfo;

    public NetChangeEvent(NetChangeInfo changeInfo) {
        this.netChangeInfo = changeInfo;
    }
}
