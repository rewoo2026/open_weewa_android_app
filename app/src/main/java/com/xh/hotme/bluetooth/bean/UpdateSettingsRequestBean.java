package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

/**
 * 更新相机配置接口，是否自动上传视频
 */
@Keep
public class UpdateSettingsRequestBean extends BaseBluetoothBean {
    int auto_upload = -1;
    int auto_upload_avs = -1;
    int auto_upload_fec = -1;
    int auto_upload_hl = -1;
    int factory_test = -1;
    int auto_upload_log = -1;
    int auto_upload_crop_buffer = -1;

    public UpdateSettingsRequestBean() {
        this.c = BleConstants.BLE_CMD_UPDATE_SETTINGS;
    }

    public int getAutoUploadAvs() {
        return auto_upload_avs;
    }

    public void setAutoUploadAvs(int auto_upload_avs) {
        this.auto_upload_avs = auto_upload_avs;
    }

    public int getAutoUploadFec() {
        return auto_upload_fec;
    }

    public void setAutoUploadFec(int auto_upload_fec) {
        this.auto_upload_fec = auto_upload_fec;
    }

    public int getAutoUploadHl() {
        return auto_upload_hl;
    }

    public void setAutoUploadHl(int auto_upload_hl) {
        this.auto_upload_hl = auto_upload_hl;
    }

    public int getAutoUpload() {
        return auto_upload;
    }

    public void setAutoUpload(int auto_upload) {
        this.auto_upload = auto_upload;
    }

    public int getFactoryTest() {
        return factory_test;
    }

    public void setFactoryTest(int factory_test) {
        this.factory_test = factory_test;
    }

    public int getAuto_upload_log() {
        return auto_upload_log;
    }

    public void setAutoUploadLog(int auto_upload_log) {
        this.auto_upload_log = auto_upload_log;
    }

    public int getAutoUploadCropBuffer() {
        return auto_upload_crop_buffer;
    }

    public void setAutoUploadCropBuffer(int auto_upload_crop_buffer) {
        this.auto_upload_crop_buffer = auto_upload_crop_buffer;
    }
}
