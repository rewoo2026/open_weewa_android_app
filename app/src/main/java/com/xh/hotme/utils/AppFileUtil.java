package com.xh.hotme.utils;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.DeviceSettingResultBean;
import com.xh.hotme.bean.UserInfoBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Create by zhaozhihui on 2019/2/22
 **/
@Keep
public class AppFileUtil {


    public static final String CACHE_ROOT = "temp";
    public static final String CACHE_RXVOLLEY = "rxvolley";
    public static final String CACHE_GAME_INFO = "game_info";
    public static final String DEFAULT_USER_INFO = "DEFAULT_USER_INFO";
    public static final String CACHE_APP_ID = "channel_id";

    public static final String CACHE_DEVICE_FILE = "device_list";

    public static final String CACHE_DEVICE_BIND_FILE = "device_bind_list";

    public static final String CACHE_DEVICE_VIDEO = "device_video";

    public static final String ROOT_VIDEO = "videos";

    public static final String COMPETITION_FILE = "competition_name";

    public static final String VERSION_FILE = "version";
    public static final String ALL_DEVICES_JSON = "all_devices.json";
    public static final String DEVICE_VIDEOS_JSON = "v_%d.json";

    public static final String DEVICE_VIDEOS_DOT_JSON = "v_%d_dot.json";

    // 保存device config的文件, 里面的内容不是合并的结果(合并的话,底层的变了上层的值还是老的),
    // 所以查询的时候, 要先查自定义设置, 再查设备专有默认配置, 再查全局默认配置.
    public static final String GLOBAL_TEST_DEVICE_DEFAULT_CONFIG = "g_test_device_def_cfg.json";
    public static final String GLOBAL_NORMAL_DEVICE_DEFAULT_CONFIG = "g_normal_device_def_cfg.json";
    public static final String PROPRIETARY_DEVICE_DEFAULT_CONFIG = "device_def_cfg_%s_%d.json";
    public static final String CUSTOM_DEVICE_CONFIG = "device_cfg_%s_%d.json";

    /**
     * 保存任意json字符串到本地缓存, 不区分用户
     */
    public static void saveJson(String jsonString, String fileName) {
        File cacheFile = getAppDir();
        if(cacheFile == null) {
            return;
        }
        File thirdUser = new File(cacheFile, fileName);
        FileUtil.write(thirdUser, jsonString, "utf-8");
    }

    public static void saveJson(String jsonString, File file) {
        FileUtil.write(file, jsonString, "utf-8");
    }

    public static void saveString(String str, String fileName) {
        File cacheFile = getAppDir();
        if(cacheFile == null) {
            return;
        }
        File thirdUser = new File(cacheFile, fileName);
        FileUtil.write(thirdUser, str, "utf-8");
    }

    public static void saveInt(int i, String fileName) {
        File cacheFile = getAppDir();
        if(cacheFile == null) {
            return;
        }
        File file = new File(cacheFile, fileName);
        FileUtil.write(file, String.valueOf(i), "utf-8");
    }

    public static void saveIntForCurrentUser(int i, String fileName) {
        File userDir = getCurrentUserCacheDir();
        if(userDir == null) {
            return;
        }
        File file = new File(userDir, fileName);
        FileUtil.write(file, String.valueOf(i), "utf-8");
    }

    public static void saveLongForCurrentUser(long i, String fileName) {
        File userDir = getCurrentUserCacheDir();
        if(userDir == null) {
            return;
        }
        File file = new File(userDir, fileName);
        FileUtil.write(file, String.valueOf(i), "utf-8");
    }

    /**
     * 保存任意json字符串到当前用户的本地缓存
     */
    public static void saveJsonForCurrentUser(String fileName, String jsonContent) {
        File userDir = getCurrentUserCacheDir();
        if(userDir == null) {
            return;
        }
        File gameFile = new File(userDir, fileName);

        boolean isSucc = FileUtil.write(gameFile, jsonContent, "utf-8", false);
        if(!isSucc) {
            AppTrace.d("Page", "save json fail for current user. ----fileName = " + fileName);
        }
    }

    public static boolean hasCacheFile(String fileName) {
        try {
            File cacheFile = getAppDir();
            if(cacheFile == null) {
                return false;
            }
            File file = new File(cacheFile, fileName);
            return file.exists();
        } catch(Exception e) {
            return false;
        }
    }

    public static String loadStringFromFile(String fileName) {
        File cacheFile = getAppDir();
        File file = new File(cacheFile, fileName);
        return FileUtil.readContent(file);
    }

    public static String loadStringFromFile(File file) {
        return FileUtil.readContent(file);
    }

    public static String loadStringForCurrentUser(String fileName) {
        File userDir = getCurrentUserCacheDir();
        if(userDir == null) {
            return "";
        }
        File file = new File(userDir, fileName);
        return FileUtil.readContent(file);
    }

    public static UserInfoBean loadUserInfo() {
        UserInfoBean userInfo = null;

        String content = loadStringFromFile(DEFAULT_USER_INFO);
        if(!TextUtils.isEmpty(content)) {
            try {
                userInfo = new Gson().fromJson(content, new TypeToken<UserInfoBean>() {
                }.getType());
            } catch(JsonSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            AppTrace.d("load local user info is null");
        }
        return userInfo;
    }

    public static void saveUserInfo(String content) {
        if(TextUtils.isEmpty(content)) {
            AppTrace.w("save user info is null");
            return;
        }
        AppFileUtil.saveJson(content, AppFileUtil.DEFAULT_USER_INFO);
    }


    public static void deleteUserInfo(String fileName) {
        File cacheFile = getAppDir();
        if(cacheFile == null) {
            return;
        }
        File thirdUser = new File(cacheFile, fileName);
        if(thirdUser != null && thirdUser.exists()) {
            thirdUser.delete();

        }
    }

    /**
     * 针对于每个用户的临时文件目录
     */
    public static File getUserCacheDir(String userId) {
        File rootDir = getAppDir();
        if(rootDir == null) {
            return null;
        }
        File userDir = new File(rootDir, userId);
        if(userDir != null && (!userDir.exists() || !userDir.isDirectory())) {
            userDir.mkdirs();
        }
        return userDir;
    }


    /**
     * 获取存储框架js代码的根目录
     *
     * @return 框架根目录文件
     */
    public static File getAppDir() {
        File appDir = HotmeApplication.getContext().getFilesDir();
        if(!appDir.exists() || !appDir.isDirectory()) {
            appDir.mkdirs();
        }
        return appDir;
    }

    /**
     * 当前用户的临时文件目录
     */
    public static File getCurrentUserCacheDir() {
        String userId = LoginManager.getUserId();
        return getUserCacheDir(userId);
    }

    public static File getCacheDir() {
        File rootDir = getAppDir();
        if(!rootDir.exists() || !rootDir.isDirectory()) {
            rootDir.mkdirs();
        }

        File cacheFile = new File(rootDir, CACHE_ROOT);
        if(cacheFile != null && !cacheFile.exists()) {
            cacheFile.mkdir();
        }
        return cacheFile;
    }


    public static void deleteFile(String fileName) {
        File cacheFile = HotmeApplication.getContext().getFilesDir();
        if(cacheFile != null) {
            File file = new File(cacheFile, fileName);
            if(file.exists()) {
                file.delete();
            }
        }
    }


    //获取视频根目录
    public static File getVideoDir() {
        File rootDir = getAppDir();
        if(!rootDir.exists() || !rootDir.isDirectory()) {
            rootDir.mkdirs();
        }

        File cacheFile = new File(rootDir, ROOT_VIDEO);
        if(!cacheFile.exists()) {
            cacheFile.mkdirs();
        }
        return cacheFile;
    }


    //获取视频根目录
    public static File getCameraDir(String cameraDir) {
        File videoDir = getVideoDir();
        if(!videoDir.exists() || !videoDir.isDirectory()) {
            videoDir.mkdirs();
        }

        File cacheFile = new File(videoDir, cameraDir);
        if(!cacheFile.exists()) {
            cacheFile.mkdirs();
        }
        return cacheFile;
    }

    /*

     */
    public static void saveVideoJson(String jsonString, File file) {
        File videoDir = getVideoDir();
        if(videoDir == null) {
            videoDir.mkdirs();
            return;
        }
        FileUtil.write(file, jsonString, "utf-8");
    }

    public static String getApkFilePath(String url, String tag) {
        String rootPath = getAppDir() + "/apk_cache";
        File cacheDir = new File(rootPath);
        if(!cacheDir.exists() || !cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }

        if(!TextUtils.isEmpty(tag)) {
            return rootPath + File.separator + MD5.md5(url) + tag + ".apk";
        } else {
            return rootPath + File.separator + MD5.md5(url) + ".apk";
        }

    }

    public static String getApkFileTempPath(String url) {
        String rootPath = getAppDir() + "/apk_cache";
        File cacheDir = new File(rootPath);
        if(!cacheDir.exists() || !cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }
        return rootPath + File.separator + MD5.md5(url) + "_tmp";
    }

    public static void saveBindDeviceInfo(String account, String deviceStr) {
        AppFileUtil.saveJson(deviceStr, account + "_" + com.xh.hotme.environment.RuntimeEnvironment.cacheScope() + "_" + AppFileUtil.CACHE_DEVICE_BIND_FILE);
    }

    public static String getBindDeviceInfo(String account) {
        return AppFileUtil.loadStringFromFile(account + "_" + com.xh.hotme.environment.RuntimeEnvironment.cacheScope() + "_" + AppFileUtil.CACHE_DEVICE_BIND_FILE);
    }

    //云存储目录
    public static String getDownloadDirectory() {
        String directory = "";

        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }

        return directory;
    }

    public static String getWeewaDownloadRoot() {
        File download = new File(AppFileUtil.getDownloadDirectory(), "weewa");
        if(!download.exists()) {
            download.mkdirs();
        }
        return download.getAbsolutePath();
    }

    public static File getWeewaDataRoot() {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStoragePublicDirectory(HotmeApplication.getContext().getPackageName());
        } else {
            return HotmeApplication.getContext().getFilesDir();
        }
    }

    public static File getWeewaJsonRoot() {
        return new File(getWeewaDataRoot(), "json");
    }

    public static File getAllDevicesJsonFile() {
        return new File(getWeewaJsonRoot(), ALL_DEVICES_JSON);
    }

    @SuppressLint("DefaultLocale")
    public static File getDeviceVideoJsonFile(CameraVideoListBean.VideosBean bean) {
        return new File(getWeewaJsonRoot(), String.format(DEVICE_VIDEOS_JSON, (bean.deviceId + bean.path).hashCode()));
    }

    @SuppressLint("DefaultLocale")
    public static File getDeviceVideoDotJsonFile(CameraVideoListBean.VideosBean bean) {
        return new File(getWeewaJsonRoot(), String.format(DEVICE_VIDEOS_DOT_JSON, (bean.deviceId + bean.path).hashCode()));
    }

    public static File getGlobalTestDeviceDefaultConfigJsonFile() {
        return new File(getWeewaJsonRoot(), GLOBAL_TEST_DEVICE_DEFAULT_CONFIG);
    }

    public static File getGlobalNormalDeviceDefaultConfigJsonFile() {
        return new File(getWeewaJsonRoot(), GLOBAL_NORMAL_DEVICE_DEFAULT_CONFIG);
    }

    public static File getDeviceDefaultConfigJsonFile(String deviceId) {
        return new File(getWeewaJsonRoot(), String.format(PROPRIETARY_DEVICE_DEFAULT_CONFIG, Constants.ROLE, deviceId.hashCode()));
    }

    public static File getDeviceConfigJsonFile(String deviceId) {
        return new File(getWeewaJsonRoot(), String.format(CUSTOM_DEVICE_CONFIG, Constants.ROLE, deviceId.hashCode()));
    }

    public static synchronized List<CameraVideoListBean.VideosBean> loadAllDevicesJson() {
        String content = FileUtil.readContent(getAllDevicesJsonFile());
        try {
            return GsonUtils.fromJsonList(content, CameraVideoListBean.VideosBean.class);
        } catch(Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 从本地缓存中的device视频一级列表中删除一项
     */
    public static void removeDeviceBean(CameraVideoListBean.VideosBean bean) {
        List<CameraVideoListBean.VideosBean> allDevices = loadAllDevicesJson();
        int size = allDevices.size();
        for(int i = size - 1; i >= 0; i--) {
            CameraVideoListBean.VideosBean device = allDevices.get(i);
            if(device.deviceId.equals(bean.deviceId) && device.path.equals(bean.path)) {
                allDevices.remove(i);
                break;
            }
        }
        saveAllDevicesJson(allDevices);
    }

    /**
     * 从本地缓存中的device视频一级列表中替换一项
     */
    public static void replaceDeviceBean(CameraVideoListBean.VideosBean bean) {
        List<CameraVideoListBean.VideosBean> allDevices = loadAllDevicesJson();
        int size = allDevices.size();
        for(int i = size - 1; i >= 0; i--) {
            CameraVideoListBean.VideosBean device = allDevices.get(i);
            if(device.deviceId.equals(bean.deviceId) && device.path.equals(bean.path)) {
                allDevices.remove(i);
                allDevices.add(i, bean);
                break;
            }
        }
        saveAllDevicesJson(allDevices);
    }

    public static synchronized List<CameraVideoDetailListBean.VideosBean> loadDeviceVideoJson(CameraVideoListBean.VideosBean bean) {
        String content = FileUtil.readContent(getDeviceVideoJsonFile(bean));
        try {
            return GsonUtils.fromJsonList(content, CameraVideoDetailListBean.VideosBean.class);
        } catch(Exception e) {
            return new ArrayList<>();
        }
    }

    public static synchronized void saveAllDevicesJson(List<CameraVideoListBean.VideosBean> list) {
        FileUtil.write(getAllDevicesJsonFile(), GsonUtils.GsonString(list), "utf-8");
    }

    public static synchronized void saveDeviceVideoJson(CameraVideoListBean.VideosBean bean, List<CameraVideoDetailListBean.VideosBean> list) {
        FileUtil.write(getDeviceVideoJsonFile(bean), GsonUtils.GsonString(list), "utf-8");
    }

    public static synchronized void saveDeviceVideoDotJson(CameraVideoListBean.VideosBean bean, List<CameraVideoDetailListBean.VideosBean> list) {
        FileUtil.write(getDeviceVideoDotJsonFile(bean), GsonUtils.GsonString(list), "utf-8");
    }

    public static Map<String, DeviceSettingResultBean> loadGlobalTestDeviceDefaultConfig() {
        File f = getGlobalTestDeviceDefaultConfigJsonFile();
        if(!f.exists()) {
            return null;
        }
        String content = FileUtil.readContent(f);
        try {
            return GsonUtils.GsonToMaps(content, DeviceSettingResultBean.class);
        } catch(Exception e) {
            return null;
        }
    }

    public static Map<String, DeviceSettingResultBean> loadGlobalNormalDeviceDefaultConfig() {
        File f = getGlobalNormalDeviceDefaultConfigJsonFile();
        if(!f.exists()) {
            return null;
        }
        String content = FileUtil.readContent(f);
        try {
            return GsonUtils.GsonToMaps(content, DeviceSettingResultBean.class);
        } catch(Exception e) {
            return null;
        }
    }

    public static void saveGlobalTestDeviceDefaultConfig(Map<String, DeviceSettingResultBean> map) {
        FileUtil.write(getGlobalTestDeviceDefaultConfigJsonFile(), GsonUtils.GsonString(map), "utf-8");
    }

    public static void saveGlobalNormalDeviceDefaultConfig(Map<String, DeviceSettingResultBean> map) {
        FileUtil.write(getGlobalNormalDeviceDefaultConfigJsonFile(), GsonUtils.GsonString(map), "utf-8");
    }

    public static Map<String, DeviceSettingResultBean> loadProprietaryDeviceDefaultConfig(String deviceId) {
        File f = getDeviceDefaultConfigJsonFile(deviceId);
        if(!f.exists()) {
            return null;
        }
        String content = FileUtil.readContent(f);
        try {
            return GsonUtils.GsonToMaps(content, DeviceSettingResultBean.class);
        } catch(Exception e) {
            return null;
        }
    }

    public static Map<String, DeviceSettingResultBean> loadProprietaryDeviceConfig(String deviceId) {
        File f = getDeviceConfigJsonFile(deviceId);
        if(!f.exists()) {
            return null;
        }
        String content = FileUtil.readContent(f);
        try {
            return GsonUtils.GsonToMaps(content, DeviceSettingResultBean.class);
        } catch(Exception e) {
            return null;
        }
    }

    public static void saveProprietaryDeviceDefaultConfig(String deviceId, Map<String, DeviceSettingResultBean> map) {
        FileUtil.write(getDeviceDefaultConfigJsonFile(deviceId), GsonUtils.GsonString(map), "utf-8");
    }

    public static void saveProprietaryDeviceConfig(String deviceId, Map<String, DeviceSettingResultBean> map) {
        FileUtil.write(getDeviceConfigJsonFile(deviceId), GsonUtils.GsonString(map), "utf-8");
    }
}
