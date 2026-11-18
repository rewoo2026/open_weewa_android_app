package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class DeviceInfo implements Serializable {
    public String device_name;
    public String device_id;
    public String device_model;
    public String mac;
    public int active_status;
    public String account;
    public String version;
    public String active_time;
    public String ota_version;
    public int ota_version_code;

    public int auto_upload;
    public int volume;
    public String nonce;
    public int adb;
    public long _t;
}
