package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class GetPlanUsageRequestBean extends BaseBluetoothBean {
    public static final int FLAG_CLEAR_ALL = 0;
    public static final int FLAG_RECORD_ONLY = 1;
    public static final int FLAG_RTMP_ONLY = 2;

    public int flag = FLAG_CLEAR_ALL;

    public GetPlanUsageRequestBean() {
        this.c = BleConstants.BLE_CMD_GET_PLAN_USAGE;
    }

    public GetPlanUsageRequestBean(int flag) {
        this.c = BleConstants.BLE_CMD_GET_PLAN_USAGE;
        this.flag = flag;
    }
}
