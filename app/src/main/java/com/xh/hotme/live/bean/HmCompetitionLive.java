package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class HmCompetitionLive implements Serializable {
    public String createUserId;
    public String createTime;
    public String updateUserId;
    public String updateTime;
    public String competitionId;
    public String competitionImage;
    public String competitionName;
    public String competitionTime;
    public String competitionOrderPhones;
    public String competitionStatus;
    public Integer pushStreamStatus;
    // 0: manual control; 1: platform starts, recovers and stops the task.
    public Integer autoPushStream;
    public String recordingExecutionStatus;
    public String motionType;
    public String provinceCode;
    public String provinceCodeDisplay;
    public String cityCode;
    public String cityCodeDisplay;
    public String place;
    public String placeType;
    public String homeTeamName;
    public String homeTeamLogo;
    public String awayTeamName;
    public String awayTeamLogo;
    public String deviceId;
    public String liveTime;
    public String liveUrl;
    public String playbackUrl;

    // 这个名字虽然叫3rd但是实际上是用来了统一推流到华为的情况下, 名字算是取的不太对
    public int pushStream3rd;

    public String pushStreamKey;
    public int pushStreamMgc;
    public int pushStreamWxMiniApp;

    // 这两个专门给第三方平台, 即直接推流到第三方而不是统一推流到华为的情况
    public String livePlatformType;
    public String livePlatformId;

    public String deviceMobile;
    public int commentate;
    public String xaxisDistance;
    public String yaxisDistance;
    public int encrypt;
    public String encryptText;
}
