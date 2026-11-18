package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

import java.util.ArrayList;
import java.util.List;


/**
 * 获取相机配置接口
 */
@Keep
public class GetSettingsExRequestBean extends BaseBluetoothBean {
    final List<String> keys;

    public GetSettingsExRequestBean() {
        this.c = BleConstants.BLE_CMD_GET_SETTINGS_EX;
        this.keys = new ArrayList<>();
    }

    public void addKey(String key) {
        keys.add(key);
    }
}
