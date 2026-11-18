package com.xh.hotme.bean;

import androidx.annotation.Keep;


@Keep
public class HmVideoUploadProgress {
    private String createTime;
    private String createUserId;
    private String domainId;
    private String updateTime;
    private String updateUserId;
    private long uploadProgress;
    private double uploadSpeed;
    private Integer uploadStatus;
    private Integer uploadType;
    private String videoId;
    private String videoUrl;

    // HmVideoUploadProgress和HmVideo存在套娃结构
    private HmVideo hmVideo;

    public HmVideo getHmVideo() {
        return hmVideo;
    }

    public void setHmVideo(HmVideo hmVideo) {
        this.hmVideo = hmVideo;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Long getUploadProgress() {
        return uploadProgress;
    }

    public void setUploadProgress(Long uploadProgress) {
        this.uploadProgress = uploadProgress;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public void setUploadSpeed(double uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public Integer getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(Integer uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public Integer getUploadType() {
        return uploadType;
    }

    public void setUploadType(Integer uploadType) {
        this.uploadType = uploadType;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
