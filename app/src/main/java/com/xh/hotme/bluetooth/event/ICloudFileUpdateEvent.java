package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class ICloudFileUpdateEvent {
    public final String id;
    public final int allVideoNumber;
    public final int highlightNumber;
    public final int playbackNumber;

    public ICloudFileUpdateEvent(String id, int allVideoNumber, int highlightNumber, int playbackNumber) {
        this.id = id;
        this.allVideoNumber = allVideoNumber;
        this.highlightNumber = highlightNumber;
        this.playbackNumber = playbackNumber;
    }
}
