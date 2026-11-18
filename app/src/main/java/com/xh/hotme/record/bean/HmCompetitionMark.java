package com.xh.hotme.record.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class HmCompetitionMark implements Serializable {

    private String competitionId;
    private String createTime;
    private String createUserId;
    private String deviceId;
    private String markBeginTime;
    private String markEndTime;
    private String markId;
    private String markName;
    private String updateTime;
    private String updateUserId;
    private String videoId;
    private String videoName;
    private int markTimeOffset;

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMarkBeginTime() {
        return markBeginTime;
    }

    public void setMarkBeginTime(String markBeginTime) {
        this.markBeginTime = markBeginTime;
    }

    public String getMarkEndTime() {
        return markEndTime;
    }

    public void setMarkEndTime(String markEndTime) {
        this.markEndTime = markEndTime;
    }

    public String getMarkId() {
        return markId;
    }

    public void setMarkId(String markId) {
        this.markId = markId;
    }

    public String getMarkName() {
        return markName;
    }

    public void setMarkName(String markName) {
        this.markName = markName;
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

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getMarkTimeOffset() {
        return markTimeOffset;
    }

    public void setMarkTimeOffset(int markTimeOffset) {
        this.markTimeOffset = markTimeOffset;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }
}
