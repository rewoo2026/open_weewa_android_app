package com.xh.hotme.bean;

import androidx.annotation.Keep;

import com.xh.hotme.live.bean.CommonResponse;

import java.io.Serializable;

@Keep
public class DeviceSerialNumber extends CommonResponse {

    public HmDeviceSerialNumber data;
}
