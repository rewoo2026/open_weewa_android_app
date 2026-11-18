package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.DeviceUsageInfo;

@Keep
public class DeviceUsageInfoEvent {
    public final DeviceUsageInfo deviceUsageInfo;

    public DeviceUsageInfoEvent(DeviceUsageInfo data) {
        deviceUsageInfo = data;
    }
}
