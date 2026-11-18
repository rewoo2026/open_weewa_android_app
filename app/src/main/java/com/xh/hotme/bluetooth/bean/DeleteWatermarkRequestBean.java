package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class DeleteWatermarkRequestBean extends BaseBluetoothBean {

    public DeleteWatermarkRequestBean() {
        this.c = BleConstants.BLE_CMD_DELETE_WATERMARK;
    }
}
