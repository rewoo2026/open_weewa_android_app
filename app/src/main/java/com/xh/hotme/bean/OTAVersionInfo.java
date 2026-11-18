package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class OTAVersionInfo implements Serializable {
    public long size;
    public String apkPath;
    public String version;
    public int versionCode;
    public String descr;
    public String forceUpdateFlag;
    public String updateTitle;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getApkPath() {
        return apkPath;
    }

    public void setApkPath(String apkPath) {
        this.apkPath = apkPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getForceUpdateFlag() {
        return forceUpdateFlag;
    }

    public void setForceUpdateFlag(String forceUpdateFlag) {
        this.forceUpdateFlag = forceUpdateFlag;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }
}
