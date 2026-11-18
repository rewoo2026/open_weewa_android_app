package com.xh.hotme.bean;

public class CameraInfo {
    public DeviceUsageInfo deviceUsageInfo;
    public DeviceInfo deviceInfo;
    public NetworkBean deviceNetworkInfo;
    public final String bluetoothId;

    public CameraInfo(String uuid) {
        this.bluetoothId = uuid;
    }
}
