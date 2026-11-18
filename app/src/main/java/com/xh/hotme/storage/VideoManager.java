package com.xh.hotme.storage;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.bean.CameraVideoDateListBean;
import com.xh.hotme.bean.CameraVideoFilterListBean;
import com.xh.hotme.bean.CameraVideosBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.GsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 按照相机的视频存储接口，保存视频到手机本地
 */
public class VideoManager {
    private static final class HolderClass {
        private static final VideoManager INSTANCE = new VideoManager();
    }

    public static VideoManager getImpl() {
        return HolderClass.INSTANCE;
    }

    private VideoManager() {
    }

    //
    // 新增：存储 List<HmVideo> 的成员变量
    private List<HmVideo> videoList;

    // 新增：设置数据的方法
    public void setVideoList(List<HmVideo> videoList) {
        this.videoList = videoList;
    }

    // 新增：获取数据的方法
    public List<HmVideo> getVideoList() {
        return videoList;
    }

    public static List<CameraVideoDateListBean> convertVideoInfo(List<CameraVideoFilterListBean.CameraVideoGroupBean> videoGroupList) {
        List<CameraVideoDateListBean> videoDateBeanList = new ArrayList<>();

        if(videoGroupList == null || videoGroupList.isEmpty()) {
            return videoDateBeanList;
        }

        for(CameraVideoFilterListBean.CameraVideoGroupBean videoGroupInfo : videoGroupList) {
            if(videoGroupInfo != null) {
                String date = videoGroupInfo.path.split("/")[0];
                CameraVideoDateListBean cameraVideoDateListBean = null;
                boolean isNewDate = true;
                if(videoDateBeanList.isEmpty()) {
                    cameraVideoDateListBean = new CameraVideoDateListBean();
                    cameraVideoDateListBean.date = date;
                    cameraVideoDateListBean.groups = new ArrayList<>();
                } else {
                    for(CameraVideoDateListBean videoDateListBean : videoDateBeanList) {
                        if(videoDateListBean.date.equalsIgnoreCase(date)) {
                            cameraVideoDateListBean = videoDateListBean;
                            isNewDate = false;
                            break;
                        }
                    }
                    if(cameraVideoDateListBean == null) {
                        cameraVideoDateListBean = new CameraVideoDateListBean();
                        cameraVideoDateListBean.date = date;
                        cameraVideoDateListBean.groups = new ArrayList<>();
                    }
                }

                List<CameraVideosBean> videosBeanList = videoGroupInfo.videos;
                if(videosBeanList != null && !videosBeanList.isEmpty()) {
                    for(CameraVideosBean videosBean : videosBeanList) {
                        CameraVideoDateListBean.CameraVideoDateBean dateBean = new CameraVideoDateListBean.CameraVideoDateBean();
                        dateBean.duration = videosBean.duration;
                        dateBean.link = videosBean.link;
                        dateBean.name = videosBean.name;
                        dateBean.size = videosBean.size;
                        dateBean.type = videosBean.type;
                        dateBean.thumb = videosBean.thumb;
                        dateBean.video_width = videosBean.video_width;
                        dateBean.video_height = videosBean.video_height;
                        dateBean.path = videoGroupInfo.path;
                        cameraVideoDateListBean.groups.add(dateBean);

                    }
                }
                if(isNewDate) {
                    videoDateBeanList.add(cameraVideoDateListBean);
                }

            }
        }

        return videoDateBeanList;
    }

    public static void saveVideoFilterInfo(int type, List<CameraVideoFilterListBean.CameraVideoGroupBean> list) {

        if(list == null || list.isEmpty()) {
            return;
        }

        String cacheVideoFileName = AppFileUtil.CACHE_DEVICE_VIDEO + "_" + type;

        String cameraName = BluetoothManager.getConnectDeviceName();

        File cacheFile = new File(AppFileUtil.getCameraDir(cameraName), cacheVideoFileName);
        if(!cacheFile.exists()) {
            AppFileUtil.saveVideoJson(GsonUtils.GsonString(list), cacheFile);
            return;
        }

        String content = FileUtil.readContent(cacheFile);
        if(TextUtils.isEmpty(content)) {
            AppFileUtil.saveVideoJson(GsonUtils.GsonString(list), cacheFile);
            return;
        }
        List<CameraVideoFilterListBean.CameraVideoGroupBean> groupLocalList = new Gson().fromJson(content, new TypeToken<List<CameraVideoFilterListBean.CameraVideoGroupBean>>() {
        }.getType());
        if(groupLocalList == null || groupLocalList.isEmpty()) {
            AppFileUtil.saveVideoJson(GsonUtils.GsonString(list), cacheFile);
            return;
        }

        for(CameraVideoFilterListBean.CameraVideoGroupBean videoGroupBean : list) {
            if(videoGroupBean != null && videoGroupBean.videos != null && !videoGroupBean.videos.isEmpty()) {
                String path = videoGroupBean.path;
                boolean hasGroupDate = false;
                for(CameraVideoFilterListBean.CameraVideoGroupBean localGroupBean : groupLocalList) {
                    if(localGroupBean.path.equalsIgnoreCase(path)) {
                        List<CameraVideosBean> videosList = videoGroupBean.videos;
                        List<CameraVideosBean> localList = localGroupBean.videos;
                        hasGroupDate = true;
                        for(CameraVideosBean cameraVideosBean : videosList) {
                            String link = cameraVideosBean.link;
                            boolean isExitVideo = false;
                            for(CameraVideosBean localVideoBean : localList) {
                                if(localVideoBean.link.equalsIgnoreCase(link)) {
                                    isExitVideo = true;
                                    break;
                                }
                            }
                            if(!isExitVideo) {
                                localGroupBean.videos.add(cameraVideosBean);
                            }
                        }
                    }
                }

                if(!hasGroupDate) {
                    groupLocalList.add(videoGroupBean);
                }
            }
        }

        AppFileUtil.saveVideoJson(GsonUtils.GsonString(groupLocalList), cacheFile);
    }

}
