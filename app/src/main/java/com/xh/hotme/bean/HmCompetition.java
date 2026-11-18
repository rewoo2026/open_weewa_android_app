package com.xh.hotme.bean;

import androidx.annotation.Keep;

import com.xh.hotme.utils.TimeUtil;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {
 * "awayTeamLogo": "string",
 * "awayTeamName": "string",
 * "cityCode": "string",
 * "cityCodeDisplay": "string",
 * "competitionId": "string",
 * "competitionName": "string",
 * "competitionStatus": 0,
 * "competitionTime": "2024-01-02T04:11:29.084Z",
 * "createTime": "2024-01-02T04:11:29.084Z",
 * "createUserId": "string",
 * "deviceId": "string",
 * "homeTeamLogo": "string",
 * "homeTeamName": "string",
 * "motionType": "string",
 * "place": "string",
 * "placeType": "string",
 * "provinceCode": "string",
 * "provinceCodeDisplay": "string",
 * "updateTime": "2024-01-02T04:11:29.084Z",
 * "updateUserId": "string",
 * "videoHighlightCount": 0,
 * "videoReplayCount": 0,
 * "videoTotalCount": 0
 * }
 */
@Keep
public class HmCompetition implements Serializable, ITimeHolder {

    private Integer analyseStatus;  //0 -> 未处理；1 -> 处理中；2 -> 处理结束；3 -> 处理失败； null 为开启分析
    private String analyseTaskHostId;
    private String analyseTaskHostName;
    private String awayTeamLogo;
    private String awayTeamName;
    private int awayTeamScore;
    private String cityCode;
    private String cityCodeDisplay;
    private String competitionId;
    private String competitionImage;
    private String wxShareImage;
    private String competitionName;
    private int competitionType;
    private int competitionStatus;
    private String competitionTime;
    private String createTime;
    private String createUserId;
    private String deviceId;
    private String homeTeamLogo;
    private String homeTeamName;
    private int homeTeamScore;
    private String motionType;
    private String place;
    private String placeType;
    private String provinceCode;
    private String provinceCodeDisplay;
    private String updateTime;
    private String updateUserId;
    private int videoAwayTeamGoalsCount;
    private int videoFullCount;
    private int videoHighlightCount;
    private int videoReplayCount;
    private int videoTotalCount;

    public String generatePath() {
        Pattern regex = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        Matcher m = regex.matcher(competitionTime);
        if(m.find()) {
            String[] parts = competitionTime.split(" ");
            if(parts.length == 2) {
                String path1 = parts[0].replace("-", "");
                String path2 = parts[1].replace(":", "").substring(0, 4);
                return path1 + "/" + path2;
            }
        }
        return null;
    }

    public Integer getAnalyseStatus() {
        return analyseStatus;
    }

    public void setAnalyseStatus(Integer analyseStatus) {
        this.analyseStatus = analyseStatus;
    }

    public String getAnalyseTaskHostId() {
        return analyseTaskHostId;
    }

    public void setAnalyseTaskHostId(String analyseTaskHostId) {
        this.analyseTaskHostId = analyseTaskHostId;
    }

    public String getAnalyseTaskHostName() {
        return analyseTaskHostName;
    }

    public void setAnalyseTaskHostName(String analyseTaskHostName) {
        this.analyseTaskHostName = analyseTaskHostName;
    }

    public String getAwayTeamLogo() {
        return awayTeamLogo;
    }

    public void setAwayTeamLogo(String awayTeamLogo) {
        this.awayTeamLogo = awayTeamLogo;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public String getWxShareImage() {
        return wxShareImage;
    }

    public void setWxShareImage(String wxShareImage) {
        this.wxShareImage = wxShareImage;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityCodeDisplay() {
        return cityCodeDisplay;
    }

    public void setCityCodeDisplay(String cityCodeDisplay) {
        this.cityCodeDisplay = cityCodeDisplay;
    }

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }

    public String getCompetitionImage() {
        return competitionImage;
    }

    public void setCompetitionImage(String competitionImage) {
        this.competitionImage = competitionImage;
    }

    public int getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(int competitionType) {
        this.competitionType = competitionType;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public int getCompetitionStatus() {
        return competitionStatus;
    }

    public void setCompetitionStatus(int competitionStatus) {
        this.competitionStatus = competitionStatus;
    }

    public String getCompetitionTime() {
        return competitionTime;
    }

    public void setCompetitionTime(String competitionTime) {
        this.competitionTime = competitionTime;
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

    public String getHomeTeamLogo() {
        return homeTeamLogo;
    }

    public void setHomeTeamLogo(String homeTeamLogo) {
        this.homeTeamLogo = homeTeamLogo;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public String getMotionType() {
        return motionType;
    }

    public void setMotionType(String motionType) {
        this.motionType = motionType;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(int placeType) {
        this.placeType = String.valueOf(placeType);
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceCodeDisplay() {
        return provinceCodeDisplay;
    }

    public void setProvinceCodeDisplay(String provinceCodeDisplay) {
        this.provinceCodeDisplay = provinceCodeDisplay;
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

    public int getVideoHighlightCount() {
        return videoHighlightCount;
    }

    public void setVideoHighlightCount(int videoHighlightCount) {
        this.videoHighlightCount = videoHighlightCount;
    }

    public int getVideoReplayCount() {
        return videoReplayCount;
    }

    public void setVideoReplayCount(int videoReplayCount) {
        this.videoReplayCount = videoReplayCount;
    }

    public int getVideoTotalCount() {
        return videoTotalCount;
    }

    public void setVideoTotalCount(int videoTotalCount) {
        this.videoTotalCount = videoTotalCount;
    }

    public int getAwayTeamScore() {
        return awayTeamScore;
    }

    public void setAwayTeamScore(int awayTeamScore) {
        this.awayTeamScore = awayTeamScore;
    }

    public int getHomeTeamScore() {
        return homeTeamScore;
    }

    public void setHomeTeamScore(int homeTeamScore) {
        this.homeTeamScore = homeTeamScore;
    }

    public int getVideoAwayTeamGoalsCount() {
        return videoAwayTeamGoalsCount;
    }

    public void setVideoAwayTeamGoalsCount(int videoAwayTeamGoalsCount) {
        this.videoAwayTeamGoalsCount = videoAwayTeamGoalsCount;
    }

    public int getVideoFullCount() {
        return videoFullCount;
    }

    public void setVideoFullCount(int videoFullCount) {
        this.videoFullCount = videoFullCount;
    }

    @Override
    public long getTimeInMillis() {
        return TimeUtil.str2Millis(competitionTime);
    }
}
