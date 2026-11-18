package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/**
 * {
 * "beginTime": "2024-01-03T09:51:28.427Z",
 * "competitionBeginTime": "2024-01-03T09:51:28.427Z",
 * "competitionEndTime": "2024-01-03T09:51:28.427Z",
 * "competitionName": "string",
 * "competitionStatus": 0,
 * "competitionTime": "2024-01-03T09:51:28.427Z",
 * "deviceId": "string",
 * "endTime": "2024-01-03T09:51:28.427Z",
 * "motionType": "string",
 * "pageNum": 0,
 * "pageSize": 0
 * }
 * }
 */
@Keep
public class CompetitionListRequestBean implements Serializable {
    private String beginTime;
    private String competitionBeginTime;
    private String competitionEndTime;
    private String competitionName;
    private Integer competitionStatus;
    private String competitionTime;
    private String deviceId;
    private String endTime;
    private String motionType;
    private Integer pageNum;
    private Integer pageSize;

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getCompetitionBeginTime() {
        return competitionBeginTime;
    }

    public void setCompetitionBeginTime(String competitionBeginTime) {
        this.competitionBeginTime = competitionBeginTime;
    }

    public String getCompetitionEndTime() {
        return competitionEndTime;
    }

    public void setCompetitionEndTime(String competitionEndTime) {
        this.competitionEndTime = competitionEndTime;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public Integer getCompetitionStatus() {
        return competitionStatus;
    }

    public void setCompetitionStatus(Integer competitionStatus) {
        this.competitionStatus = competitionStatus;
    }

    public String getCompetitionTime() {
        return competitionTime;
    }

    public void setCompetitionTime(String competitionTime) {
        this.competitionTime = competitionTime;
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

    public String getMotionType() {
        return motionType;
    }

    public void setMotionType(String motionType) {
        this.motionType = motionType;
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
}
