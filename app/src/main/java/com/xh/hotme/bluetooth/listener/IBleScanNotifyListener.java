package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bluetooth.Device;

public interface IBleScanNotifyListener {
    void onScanning(boolean isBackground);

    void onLeScan(Device device);

    void onTimeout();

    void onScanEnd();
}
