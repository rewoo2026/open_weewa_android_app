package com.xh.hotme.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.xh.hotme.HotmeApplication;

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


    public static final String PRFS_LOBOX_INIT_TIMES = "prfs_lebox_init_times";

    public static final String PRFS_SHOW_PRIVACY_CONTENT = "prfs_privacy_content";
    public static final String PRFS_SHOW_PERMISSION_CONTENT = "prfs_permission_content";


    public static final String PRFS_TAOBAO_ACCCESS_TOKEN = "prfs_taobao_access_token";  //安装日期
    public static final String PRFS_TAOBAO_ACCCESS_EXPIRE_TIME = "prfs_taobao_access_expire_time";  //安装日期

    public static final String PRFS_VIDEO_LIST = "prfs_video_list";  //安装日期

    public static final String PREF_DEFAULT_MATCH_NAME = "pref_default_match_name"; // 水印设置界面填的那个赛事名称

    // 上一次提示升级ota的时间,因为现在改成一天就弹一次
    public static final String PREF_LAST_OTA_PROMPT_TIME = "pref_last_ota_prompt_time_%d";
    public static final String PREF_ENABLE_LIVE_PASSWORD = "pref_enable_live_password";
    public static final String PREF_LIVE_PASSWORD = "pref_live_password";
    public static final String PREF_HIDE_OSD = "pref_hide_osd";
    public static final String PREF_AVS_RESOLUTION = "pref_avs_resolution";


    private static final class HolderClass {
        private static final SharedPrefs INSTANCE = new SharedPrefs();
        private static final SharedPreferences PREFS = HotmeApplication.getContext().getSharedPreferences(
            HotmeApplication.getContext().getPackageName(), Context.MODE_PRIVATE);
    }

    public static SharedPrefs getImpl() {
        return HolderClass.INSTANCE;
    }

    public void setFirstLaunch(boolean isFirst) {
        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putBoolean(PRFS_FIRST_LAUNCH, isFirst).apply();
        }
    }

    public boolean isFirstLaunch() {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getBoolean(PRFS_FIRST_LAUNCH, true);
        }
        return true;
    }

    public void setFirstLaunchTimeStamp(long time) {

        if(HolderClass.PREFS != null) {
            HolderClass.PREFS.edit().putLong(PRFS_FIRST_LAUNCH_TIME, time).apply();
        }
    }

    public long getFirstLaunchTimeStamp() {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getLong(PRFS_FIRST_LAUNCH_TIME, 0);
        }
        return 0;
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

    public long getLastOTAPromptTime(String deviceId) {
        return getLong(String.format(PREF_LAST_OTA_PROMPT_TIME, deviceId.hashCode()));
    }

    public void saveLastOTAPromptTime(String deviceId, long t) {
        putLong(String.format(PREF_LAST_OTA_PROMPT_TIME, deviceId.hashCode()), t);
    }

    public boolean isLivePasswordEnabled() {
        return getBoolean(PREF_ENABLE_LIVE_PASSWORD);
    }

    public void setEnableLivePassword(boolean enable) {
        saveBoolean(PREF_ENABLE_LIVE_PASSWORD, enable);
    }

    public String getLivePassword() {
        return getString(PREF_LIVE_PASSWORD);
    }

    public void setLivePassword(String pwd) {
        saveString(PREF_LIVE_PASSWORD, pwd);
    }

    public void setPrivateShowStatus(Context context, boolean isShowed) {
        SharedPreferences.Editor e = HolderClass.PREFS.edit();
        e.putBoolean(PRFS_SHOW_PRIVACY_CONTENT, isShowed);
        e.apply();
    }

    public boolean getPrivateShowStatus() {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getBoolean(PRFS_SHOW_PRIVACY_CONTENT, false);
        }
        return true;
    }

    public void setDefaultMatchName(String name) {
        if(HolderClass.PREFS != null) {
            SharedPreferences.Editor e = HolderClass.PREFS.edit();
            e.putString(PREF_DEFAULT_MATCH_NAME, name);
            e.apply();
        }
    }

    public String getDefaultMatchName() {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getString(PREF_DEFAULT_MATCH_NAME, "");
        }
        return "";
    }

    public void setPermissionShowStatus(boolean isShowed) {
        if(HolderClass.PREFS != null) {
            SharedPreferences.Editor e = HolderClass.PREFS.edit();
            e.putBoolean(PRFS_SHOW_PERMISSION_CONTENT, isShowed);
            e.apply();
        }
    }

    public boolean getPermissionShowStatus() {
        if(HolderClass.PREFS != null) {
            return HolderClass.PREFS.getBoolean(PRFS_SHOW_PERMISSION_CONTENT, false);
        }
        return true;
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


    public boolean remove(String key) {
        return this.edit().remove(key).commit();
    }


    public boolean shownVersionToday() {
        long lastShowTime = getLong(PRFS_SHOW_VERSION_UPGRADE_TIME);
        if(lastShowTime == 0) {
            return false;
        }

        return TimeUtil.isToday(lastShowTime);
    }

    public void setShowVersionToday(long timeStamp) {
        putLong(PRFS_SHOW_VERSION_UPGRADE_TIME, timeStamp);
    }
}
