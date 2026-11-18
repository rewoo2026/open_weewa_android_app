package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

import java.util.List;

@Keep
public class GameStatListRequestBean extends BaseBluetoothBean {
    final List<Integer> types;

    public List<Integer> getTypes() {
        return types;
    }

    public GameStatListRequestBean(List<Integer> types) {
        this.c = BleConstants.BLE_CMD_STAT_LIST;
        this.types = types;
    }
}
