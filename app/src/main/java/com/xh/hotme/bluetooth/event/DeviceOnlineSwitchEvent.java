package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeviceOnlineSwitchEvent extends DeviceOnlineEvent {

    public  DeviceOnlineSwitchEvent(String deviceId, boolean online) {
        super(deviceId, online);
    }
}
