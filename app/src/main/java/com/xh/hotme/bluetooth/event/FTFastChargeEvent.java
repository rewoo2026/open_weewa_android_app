package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class FTFastChargeEvent {
    public int charge;

    public FTFastChargeEvent(int charge){
        this.charge = charge;
    }
}
