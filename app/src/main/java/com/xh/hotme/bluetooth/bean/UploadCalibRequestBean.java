package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 上传标定文件
 */
@Keep
public class UploadCalibRequestBean extends BaseBluetoothBean {

    public UploadCalibRequestBean() {
        this.c = BleConstants.BLE_CMD_UPLOAD_CALIB;
    }
}
