package com.xh.hotme.bean;


import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.utils.Constants;

import java.io.Serializable;

/**
 * "bindingPhone": "string",
 * "bindingStatus": 0,
 * "bluetoothUuid": "string",
 * "cityCode": "string",
 * "cityCodeDisplay": "string",
 * "createTime": "2024-01-03T08:04:08.381Z",
 * "createUserId": "string",
 * "deviceAdmin": "string",
 * "deviceId": "string",
 * "deviceMac": "string",
 * "deviceModel": "string",
 * "deviceName": "string",
 * "devicePhone": "string",
 * "deviceSn": "string",
 * "deviceStatus": 0,
 * "deviceType": "string",
 * "heartbeatPeriod": 0,
 * "heartbeatType": 0,
 * "lastHeartbeatTime": "2024-08-26T03:22:43.719Z",
 * "liveRemainCount": 0,
 * "onlineStatus": 0,
 * "provinceCode": "string",
 * "provinceCodeDisplay": "string",
 * "pushStreamAddr": "string",
 * "softVersion": "string",
 * "updateTime": "2024-01-03T08:04:08.381Z",
 * "updateUserId": "string"
 */

@Keep
public class BindDeviceBean implements Serializable {

    public Device blueDevice;
    private String bindingPhone;
    private Integer bindingStatus;
    private String bluetoothUuid;
    private String cityCode;
    private String cityCodeDisplay;
    private String createTime;
    private String createUserId;
    private String deviceAdmin;
    private String deviceId;
    private String deviceMac;
    private String deviceModel;
    private String deviceName;
    private String devicePhone;
    private String deviceSn;
    private Integer deviceStatus;
    private String deviceType;
    private String provinceCode;
    private String provinceCodeDisplay;
    private String pushStreamAddr;
    private String softVersion;
    private String updateTime;
    private String updateUserId;

    private Integer onlineStatus;
    private int liveRemainCount;
    private int heartbeatPeriod;
    private int heartbeatType;
    private String lastHeartbeatTime;

    private int status;
    private int liveStatus;

    public void setLiveStatus(int liveStatus) {
        this.liveStatus = liveStatus;
    }

    public int getLiveStatus() {
        return liveStatus;
    }

    public String getBindingPhone() {
        return bindingPhone;
    }

    public void setBindingPhone(String bindingPhone) {
        this.bindingPhone = bindingPhone;
    }

    public Integer getBindingStatus() {
        return bindingStatus;
    }

    public void setBindingStatus(Integer bindingStatus) {
        this.bindingStatus = bindingStatus;
    }

    public String getBluetoothUuid() {
        return bluetoothUuid;
    }

    public void setBluetoothUuid(String bluetoothUuid) {
        this.bluetoothUuid = bluetoothUuid;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityCodeDisplay() {
        return cityCodeDisplay;
    }

    public void setCityCodeDisplay(String cityCodeDisplay) {
        this.cityCodeDisplay = cityCodeDisplay;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getDeviceAdmin() {
        return deviceAdmin;
    }

    public void setDeviceAdmin(String deviceAdmin) {
        this.deviceAdmin = deviceAdmin;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDevicePhone() {
        return devicePhone;
    }

    public void setDevicePhone(String devicePhone) {
        this.devicePhone = devicePhone;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public Integer getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(Integer deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceCodeDisplay() {
        return provinceCodeDisplay;
    }

    public void setProvinceCodeDisplay(String provinceCodeDisplay) {
        this.provinceCodeDisplay = provinceCodeDisplay;
    }

    public String getPushStreamAddr() {
        return pushStreamAddr;
    }

    public void setPushStreamAddr(String pushStreamAddr) {
        this.pushStreamAddr = pushStreamAddr;
    }

    public String getSoftVersion() {
        return softVersion;
    }

    public void setSoftVersion(String softVersion) {
        this.softVersion = softVersion;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Device getBlueDevice() {
        return blueDevice;
    }

    public void setBlueDevice(Device blueDevice) {
        this.blueDevice = blueDevice;
    }

    public int getStatus() {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
            return onlineStatus == null ? 0 : onlineStatus.intValue();
        }
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getOnlineStatus() {
        return onlineStatus == null ? 0 : onlineStatus.intValue();
    }

    public void setOnlineStatus(int onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public int getLiveRemainCount() {
        return liveRemainCount;
    }

    public void setLiveRemainCount(int liveRemainCount) {
        this.liveRemainCount = liveRemainCount;
    }

    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public void setHeartbeatPeriod(int heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
    }

    public int getHeartbeatType() {
        return heartbeatType;
    }

    public void setHeartbeatType(int heartbeatType) {
        this.heartbeatType = heartbeatType;
    }

    public String getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(String lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public BindDeviceBean clone() {
        BindDeviceBean cloneDevice = new BindDeviceBean();
        cloneDevice.bluetoothUuid = this.bluetoothUuid;
        cloneDevice.deviceAdmin = this.deviceAdmin;
        cloneDevice.deviceMac = this.deviceMac;
        cloneDevice.deviceId = this.deviceId;
        cloneDevice.deviceName = this.deviceName;
        cloneDevice.deviceSn = this.deviceSn;
        cloneDevice.deviceModel = this.deviceModel;
        cloneDevice.deviceStatus = this.deviceStatus;
        cloneDevice.devicePhone = this.devicePhone;
        cloneDevice.bindingPhone = this.bindingPhone;
        cloneDevice.bindingStatus = this.bindingStatus;
        cloneDevice.status = this.status;
        cloneDevice.onlineStatus = this.onlineStatus;
        cloneDevice.heartbeatType = this.heartbeatType;

        if(this.blueDevice != null) {
            Device device = this.blueDevice.clone();
            cloneDevice.blueDevice = device;
        }

        return cloneDevice;
    }
}
