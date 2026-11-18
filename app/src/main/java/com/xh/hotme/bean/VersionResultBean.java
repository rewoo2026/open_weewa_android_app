package com.xh.hotme.bean;


import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class VersionResultBean implements Serializable {
    String versionCode;
    String versionName;
    String pkgUrl;
    String forceUpdateFlag;

    String descr;

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPkgUrl() {
        return pkgUrl;
    }

    public void setPkgUrl(String pkgUrl) {
        this.pkgUrl = pkgUrl;
    }

    public int getForceUpdateFlag() {
        try {
            return Integer.parseInt(forceUpdateFlag);
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setForceUpdateFlag(int forceUpdateFlag) {
        this.forceUpdateFlag = String.valueOf(forceUpdateFlag);
    }

    public String getContent() {
        return descr;
    }

    public void setContent(String content) {
        this.descr = content;
    }
}
