package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/**
 * {
 * "beginTime": "2024-01-03T13:37:44.408Z",
 * "competitionId": "string",
 * "deviceId": "string",
 * "endTime": "2024-01-03T13:37:44.408Z",
 * "pageNum": 0,
 * "pageSize": 0,
 * "uploadType": 0,
 * "videoClassify": 0,
 * "videoName": "string",
 * "videoStatus": 0
 * }
 * }
 */
@Keep
public class CompetitionVideoRequestBean implements Serializable {
    private String beginTime;
    private String competitionId;
    private String deviceId;
    private String endTime;
    private Integer pageNum;
    private Integer pageSize;
    private Integer uploadType;
    private Integer videoClassify;
    private String videoName;
    private Integer videoStatus;

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getUploadType() {
        return uploadType;
    }

    public void setUploadType(Integer uploadType) {
        this.uploadType = uploadType;
    }

    public Integer getVideoClassify() {
        return videoClassify;
    }

    public void setVideoClassify(Integer videoClassify) {
        this.videoClassify = videoClassify;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public Integer getVideoStatus() {
        return videoStatus;
    }

    public void setVideoStatus(Integer videoStatus) {
        this.videoStatus = videoStatus;
    }
}
