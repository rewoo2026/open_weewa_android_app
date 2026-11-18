package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;


@Keep
public class HmCompetitionPoint implements Serializable {

    public String createUserId;
    public String createTime;
    public String updateUserId;
    public String updateTime;
    public String pointId;
    public String deviceId;
    public String competitionId;
    public String videoId;
    public String videoName;
    public String pointTeam;
    public String pointType;
    public String subPointType;
    public int pointTimeOffset;
    public int pointSource;
    public String markId;
}
