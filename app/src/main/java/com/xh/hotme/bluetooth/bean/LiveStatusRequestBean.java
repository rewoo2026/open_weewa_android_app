package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 直播状态查询
 */
@Keep
public class LiveStatusRequestBean extends BaseBluetoothBean {

    public LiveStatusRequestBean() {
        this.c = BleConstants.BLE_CMD_LIVE_STATUS;
    }
}
