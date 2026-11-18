package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class UpdateSNEvent {
    public final String message;
    public final int _status;

    public UpdateSNEvent(int status, String msg) {
        this.message = msg;
        this._status = status;
    }
}
