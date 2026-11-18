package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class UpdateNameEvent {
    public final String nickName;

    public UpdateNameEvent(String nick) {
        this.nickName = nick;
    }
}
