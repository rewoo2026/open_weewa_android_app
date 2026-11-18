package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class WeewaSettingsUpdateEvent {
    public final int _status;

    public WeewaSettingsUpdateEvent(int status) {
        this._status = status;
    }
}
