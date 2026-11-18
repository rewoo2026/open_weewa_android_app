package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class HmLivePlatform implements Serializable {
    public String createTime;
    public String createUserId;
    public String deviceId;

    public String domainId;

    public String platformAccount;
    public String platformLoginType;

    public String platformPassword;

    public int platformType;

    public String pullStreamAddress;

    public String pushStreamAddress;

    public String updateTime;

    public String updateUserId;
}
