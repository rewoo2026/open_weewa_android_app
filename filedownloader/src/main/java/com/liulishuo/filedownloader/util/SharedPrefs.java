package com.liulishuo.filedownloader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.liulishuo.filedownloader.upload.backend.obs.ObsInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Create by zhaozhihui on 2019-11-30
 **/
public class SharedPrefs {

    public static final String PRFS_FIRST_LAUNCH = "prfs_first_launch";
    public static final String PRFS_FIRST_LAUNCH_TIME = "prfs_first_launch_time";  //计算次留

    public static final String PRFS_SHOW_VERSION_UPGRADE = "prfs_show_version_upgrade";  //显示版本升级
    public static final String PRFS_SHOW_VERSION_UPGRADE_TIME = "prfs_version_upgrade_show_time";  //首页显示版本升级

    public static final String PRFS_DAY_INSTALL_DATE = "prfs_day_install_date";  //安装日期
    public static final String PRFS_DAY_LAUNCH_DATE = "prfs_day_launch_date";  //启动日期
    public static final String PRFS_DAY_LAUNCH_TIME = "prfs_day_launch_time";  //启动次数

    public static String HOST_NAME = "HOST_NAME";
    public static String BUCKET_NAME = "BUCKET_NAME";
    public static String ACCESS_KEY_ID = "ACCESS_KEY_ID";
    public static String SECRET_ACCESS_KEY = "SECRET_ACCESS_KEY";
    public static String END_POINT = "END_POINT";



    private static final class HolderClass {
        private static final SharedPrefs INSTANCE = new SharedPrefs();
        private static final SharedPreferences PREFS =FileDownloadHelper.getAppContext().getSharedPreferences(
                FileDownloadHelper.getAppContext().getPackageName(), Context.MODE_PRIVATE);
    }

    public static SharedPrefs getImpl() {
        return HolderClass.INSTANCE;
    }

    private static final SimpleDateFormat dayFormat = new SimpleDateFormat("yyMMdd", Locale.getDefault());

    private static String getDay() {
        return dayFormat.format(new Date());
    }

    public Boolean contain(String key) {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.contains(key);
        }
        return false;
    }


    public void saveString(String key, String value) {
        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putString(key, value).apply();
        }
    }

    public String getString(String key) {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getString(key, "");
        }
        return "";
    }

    public void saveInt(String key, int value) {
        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putInt(key, value).apply();
        }
    }

    public int getInt(String key) {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getInt(key, 0);
        }
        return 0;
    }

    public void saveBoolean(String key, boolean value) {
        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putBoolean(key, value).apply();
        }
    }

    public Boolean getBoolean(String key) {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getBoolean(key, false);
        }
        return false;
    }

    public void putLong(String key, long value) {
        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putLong(key, value).apply();
        }
    }

    public long getLong(String key) {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getLong(key, 0);
        }
        return 0;
    }


    public SharedPreferences.Editor edit() {
        return HolderClass.PREFS.edit();
    }


    public String getString(String key, String defaultValue) {
        return HolderClass.PREFS.getString(key, defaultValue);
    }

    public void putString(String key, String value) {
        this.edit().putString(key, value).apply();
    }

    public void putInt(String key, int value) {
        this.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return HolderClass.PREFS.getInt(key, defaultValue);
    }

    public void putFloat(String key, float value) {
        this.edit().putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return HolderClass.PREFS.getFloat(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        this.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return HolderClass.PREFS.getBoolean(key, defaultValue);
    }

    public void saveObsInfo(ObsInfo obsInfo){
        this.edit().putString(HOST_NAME, obsInfo.hostName)
                .putString(BUCKET_NAME, obsInfo.bucketName)
                .putString(ACCESS_KEY_ID, obsInfo.accessKeyId)
                .putString(SECRET_ACCESS_KEY, obsInfo.secretAccessKey)
                .putString(END_POINT, obsInfo.endPoint).apply();
    }

    public ObsInfo getObsInfo(){
        String hostName = getString(HOST_NAME);
        String bucketName = getString(BUCKET_NAME);
        String ak = getString(ACCESS_KEY_ID);
        String sk = getString(SECRET_ACCESS_KEY);
        String endPoint = getString(END_POINT);
        if(TextUtils.isEmpty(hostName) || TextUtils.isEmpty(bucketName) || TextUtils.isEmpty(ak) || TextUtils.isEmpty(sk)){
            Log.e("[weewa]SharedPrefs", "getObsInfo null");
            return null;
        }

        ObsInfo obsInfo = new ObsInfo();
        obsInfo.hostName = hostName;
        obsInfo.bucketName = bucketName;
        obsInfo.accessKeyId = ak;
        obsInfo.secretAccessKey = sk;
        obsInfo.endPoint = endPoint;
        return obsInfo;
    }
}
