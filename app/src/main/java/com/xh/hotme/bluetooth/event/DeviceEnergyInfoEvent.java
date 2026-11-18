package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class DeviceEnergyInfoEvent {
    public final int percent;

    public DeviceEnergyInfoEvent(int data) {
        percent = data;
    }
}
