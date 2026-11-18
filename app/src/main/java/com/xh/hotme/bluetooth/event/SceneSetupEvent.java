package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.CameraSettings;

@Keep
public class SceneSetupEvent {
    public final int _status;

    public SceneSetupEvent(int status) {
        this._status = status;
    }
}
