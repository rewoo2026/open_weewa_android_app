package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class GetPlanUsageEvent {
    public final int rtmpUsed;
    public final int recordUsed;

    public GetPlanUsageEvent(int rtmpUsed, int recordUsed) {
        this.rtmpUsed = rtmpUsed;
        this.recordUsed = recordUsed;
    }
}
