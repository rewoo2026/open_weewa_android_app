package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.StorageBean;

public interface IBleDeviceUsageInfoNotifyListener {
    void onStorageInfo(StorageBean storageBean);

    void onEnergy(int energy);

    void onDeviceInfo(DeviceUsageInfo data);
}
