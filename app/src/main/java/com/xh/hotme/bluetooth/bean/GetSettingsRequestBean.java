package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


/**
 * 获取相机配置接口
 */
@Keep
public class GetSettingsRequestBean extends BaseBluetoothBean {

    public GetSettingsRequestBean() {
        this.c = BleConstants.BLE_CMD_GET_SETTINGS;
    }

}
