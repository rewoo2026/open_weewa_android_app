package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.Device;

@Keep
public class ScanDeviceEvent {
    final Device _device;

    public ScanDeviceEvent(Device device) {
        this._device = device;
    }

}
