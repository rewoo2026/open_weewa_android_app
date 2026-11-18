package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import java.util.List;

@Keep
public class AutoUploadStatusEvent {
    public boolean uploading = false;
    public int progress = 0;
    public boolean isForceEnd = false;
    public boolean hasUpload = false;

    public List<String> failed ;
}
