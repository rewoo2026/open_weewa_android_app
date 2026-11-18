package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.StorageBean;

@Keep
public class DeviceStorageInfoEvent {
    public final StorageBean storageBean;

    public DeviceStorageInfoEvent(StorageBean data) {
        storageBean = data;
    }
}
