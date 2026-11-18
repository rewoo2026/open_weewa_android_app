package com.xh.hotme.bean;


import androidx.annotation.Keep;

import java.io.Serializable;

/**
 * createUserId : 157
 * createTime : 2024-03-06 10:12:16
 * updateUserId : null
 * updateTime : null
 * configId : 1765198679429996546
 * deviceType : 1
 * deviceId :
 * configKey : avs_record_disable
 * configValue : 0
 * configEnable : 1
 */

@Keep
public class DeviceSettingResultBean implements Serializable {

    private String createUserId;
    private String createTime;
    private String updateUserId;
    private String updateTime;
    private String configId;
    private int deviceType;
    private String deviceId;
    private String configKey;
    private String configValue;
    private String configEnable;

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigEnable() {
        return configEnable;
    }

    public void setConfigEnable(String configEnable) {
        this.configEnable = configEnable;
    }
}
