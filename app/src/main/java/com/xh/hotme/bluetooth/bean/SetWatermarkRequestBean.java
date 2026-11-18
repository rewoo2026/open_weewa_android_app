package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetWatermarkRequestBean extends BaseBluetoothBean {
    // if bmp is empty, means delete watermark
    public final String bmp;

    public SetWatermarkRequestBean(String b64Bmp) {
        this.c = BleConstants.BLE_CMD_SET_WATERMARK;
        this.bmp = b64Bmp;
    }
}
