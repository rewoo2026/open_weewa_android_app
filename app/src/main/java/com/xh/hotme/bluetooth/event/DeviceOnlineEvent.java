package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeviceOnlineEvent {
    public String deviceId;
    public boolean isOnline;
    public DeviceOnlineEvent(String deviceId, boolean online) {
        this.deviceId = deviceId;
        this.isOnline = online;
    }
}
