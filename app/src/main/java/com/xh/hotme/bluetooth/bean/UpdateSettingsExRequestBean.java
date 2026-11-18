package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

import java.util.ArrayList;
import java.util.List;

@Keep
public class UpdateSettingsExRequestBean extends BaseBluetoothBean {
    final List<String> keys;
    final List<String> values;
    int restart = 0;

    public UpdateSettingsExRequestBean() {
        this.c = BleConstants.BLE_CMD_UPDATE_SETTINGS_EX;
        keys = new ArrayList<>();
        values = new ArrayList<>();
    }

    public void addSetting(String key, String value) {
        keys.add(key);
        values.add(value);
    }

    public void addSetting(String key, int value) {
        keys.add(key);
        values.add(String.valueOf(value));
    }

    public void addSetting(String key, float value) {
        keys.add(key);
        values.add(String.valueOf(value));
    }

    public boolean has(String key) {
        return keys.contains(key);
    }

    public int getRestart() {
        return restart;
    }

    public void setRestart(int restart) {
        this.restart = restart;
    }
}
