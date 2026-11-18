package com.xh.hotme.bean;


import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import com.xh.hotme.utils.Constants;

import java.io.Serializable;

@Keep
public class HmVideo implements Serializable {
    private String competitionId;
    private int competitionType = Constants.COMPETITION_TYPE_RECORD;
    private String createTime;
    private String createUserId;
    private String deviceId;
    private String deviceMac;
    private HmCompetition hmCompetition;
    private HmVideoUploadProgress hmVideoUploadProgress;
    private String updateTime;
    private String updateUserId;
    private int uploadType;
    private int videoClassify;
    private int videoDuration;
    private int videoHeight;
    private String videoId;
    private String videoImage;
    private String videoName;
    private long videoSize;
    private int videoStatus;
    private String videoTime;
    private String videoUrl;
    private int videoWidth;

    // 自定义字段, 因为服务器那边没有这个字段, 需要解析时间戳拼出来
    private String path;

    public String getPath() {
        // lazy generate
        if(TextUtils.isEmpty(path)) {
            path = hmCompetition.generatePath();
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVideoImage() {
        return videoImage;
    }

    public void setVideoImage(String videoImage) {
        this.videoImage = videoImage;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }

    public int getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(int competitionType) {
        this.competitionType = competitionType;
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

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public HmCompetition getHmCompetition() {
        return hmCompetition;
    }

    public void setHmCompetition(HmCompetition hmCompetition) {
        this.hmCompetition = hmCompetition;
    }

    public HmVideoUploadProgress getHmVideoUploadProgress() {
        return hmVideoUploadProgress;
    }

    public void setHmVideoUploadProgress(HmVideoUploadProgress hmVideoUploadProgress) {
        this.hmVideoUploadProgress = hmVideoUploadProgress;
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

    public int getUploadType() {
        return uploadType;
    }

    public void setUploadType(int uploadType) {
        this.uploadType = uploadType;
    }

    public int getVideoClassify() {
        return videoClassify;
    }

    public void setVideoClassify(int videoClassify) {
        this.videoClassify = videoClassify;
    }

    public int getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(long videoSize) {
        this.videoSize = videoSize;
    }

    public int getVideoStatus() {
        return videoStatus;
    }

    public void setVideoStatus(int videoStatus) {
        this.videoStatus = videoStatus;
    }

    public String getVideoTime() {
        return videoTime;
    }

    public void setVideoTime(String videoTime) {
        this.videoTime = videoTime;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }
}
