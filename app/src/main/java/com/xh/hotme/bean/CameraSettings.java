package com.xh.hotme.bean;

import androidx.annotation.Keep;

@Keep
public class CameraSettings {
    int auto_upload;
    int auto_upload_avs;
    int auto_upload_fec;
    int auto_upload_hl;
    int record_audio;
    int live_audio;
    int volume;

    public int getAuto_upload_avs() {
        return auto_upload_avs;
    }

    public void setAuto_upload_avs(int auto_upload_avs) {
        this.auto_upload_avs = auto_upload_avs;
    }

    public int getAuto_upload_fec() {
        return auto_upload_fec;
    }

    public void setAuto_upload_fec(int auto_upload_fec) {
        this.auto_upload_fec = auto_upload_fec;
    }

    public int getAuto_upload_hl() {
        return auto_upload_hl;
    }

    public void setAuto_upload_hl(int auto_upload_hl) {
        this.auto_upload_hl = auto_upload_hl;
    }

    public int getAuto_upload() {
        return auto_upload;
    }

    public void setAuto_upload(int auto_upload) {
        this.auto_upload = auto_upload;
    }

    public int getRecord_audio() {
        return record_audio;
    }

    public void setRecord_audio(int record_audio) {
        this.record_audio = record_audio;
    }

    public int getLive_audio() {
        return live_audio;
    }

    public void setLive_audio(int live_audio) {
        this.live_audio = live_audio;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
