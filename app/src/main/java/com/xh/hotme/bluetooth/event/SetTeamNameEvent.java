package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SetTeamNameEvent {
    public final int state;

    public SetTeamNameEvent(int st) {
        this.state = st;
    }
}
