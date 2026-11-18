package com.xh.hotme.bean;


import androidx.annotation.Keep;

import com.xh.hotme.utils.Constants;


@Keep
public class DeviceSettingRequestBean {
    public final String deviceId;
    public final String configVersion;
    public final int deviceType;

    public DeviceSettingRequestBean() {
        deviceId = "";
        configVersion = "";
        deviceType = Constants.DEVICE_TYPE_NORMAL;
    }

    public DeviceSettingRequestBean(String deviceId, String configVersion, int deviceType) {
        this.deviceId = deviceId;
        this.configVersion = configVersion;
        this.deviceType = deviceType;
    }
}
