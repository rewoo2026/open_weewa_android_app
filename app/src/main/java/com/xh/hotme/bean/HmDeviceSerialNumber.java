package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class HmDeviceSerialNumber implements Serializable {

    public String calibrationFileUrl;
    public String createTime;
    public String createUserId;
    public String deviceBluetooth;
    public String deviceId;
    public String deviceMac;
    public String deviceName;
    public String deviceStage;
    public String deviceStatus;
    public DeviceTypeBean deviceType;
    public String deviceTypeNo;
    public String updateTime;
    public String updateUserId;
    
    public static class DeviceTypeBean {
        public String createTime;
        public String createUserId;
        public String lensModule;
        public String microphone;
        public String motherboard;
        public String typeNo;
        public String updateTime;
        public String updateUserId;
    }
}
