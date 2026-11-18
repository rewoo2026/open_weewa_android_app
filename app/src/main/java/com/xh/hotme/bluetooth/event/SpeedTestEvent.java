package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class SpeedTestEvent {
    public final int status;
    public double downloadSpeed;
    public double uploadSpeed;

    public SpeedTestEvent(int st, double downloadSpeed, double uploadSpeed) {
        this.status = st;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
    }

}