package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.record.bean.HmCompetitionMark;

@Keep
public class AddMarkEvent {
    public final HmCompetitionMark markBean;

    public AddMarkEvent(HmCompetitionMark mark) {
        this.markBean = mark;
    }
}
