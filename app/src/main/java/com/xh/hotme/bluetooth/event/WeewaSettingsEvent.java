package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.CameraSettings;

@Keep
public class WeewaSettingsEvent {
    public final CameraSettings settings;

    public WeewaSettingsEvent(CameraSettings info) {
        this.settings = info;
    }
}
