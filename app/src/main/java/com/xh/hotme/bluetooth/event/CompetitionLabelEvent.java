package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class CompetitionLabelEvent {
    public final int state;

    public CompetitionLabelEvent(int st) {
        this.state = st;
    }
}
