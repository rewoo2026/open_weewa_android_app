package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class WeewaSettingsExUpdateEvent {
    public final int _status;

    public WeewaSettingsExUpdateEvent(int status) {
        this._status = status;
    }
}
