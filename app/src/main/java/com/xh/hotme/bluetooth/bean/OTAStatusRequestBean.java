package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class OTAStatusRequestBean extends BaseBluetoothBean {
    public int refresh;

    public OTAStatusRequestBean() {
        this(false);
    }

    public OTAStatusRequestBean(boolean refresh) {
        this.c = BleConstants.BLE_CMD_GET_OTA_STATUS;
        this.refresh = refresh ? 1 : 0;
    }
}
