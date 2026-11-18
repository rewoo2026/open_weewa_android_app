package com.xh.hotme.utils;


import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.storage.filetransfer.TaskModel;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoUtil {

    public final static String VIDEO_TYPE_STR_ALL_VIDEOS = "全部视频";
    public final static String VIDEO_TYPE_STR_HIGH_LIGHT = "精彩视频";
    public final static String VIDEO_TYPE_STR_PLAY_BACK = "全场回放";

    public final static int INT_VIDEO_FILTER_TYPE_ALL_VIDEOS = -1;
    public final static int INT_VIDEO_FILTER_TYPE_FULL_VIDEOS = 0;
    public final static int INT_VIDEO_FILTER_TYPE_HIGH_LIGHT = 2;
    public final static int INT_VIDEO_FILTER_TYPE_PLAY_BACK = 1;

    public final static int INT_VIDEO_FILTER_TYPE_HOST_GOAL = 3;
    public final static int INT_VIDEO_FILTER_TYPE_GUEST_GOAL = 4;

    public final static int INT_VIDEO_TYPE_FULL_VIDEO = 0;   //全景视频
    public final static int INT_VIDEO_TYPE_PLAY_BACK = 1;   //回放视频
    public final static int INT_VIDEO_TYPE_HIGH_LIGHT = 2;  //精彩视频
    public final static int INT_VIDEO_TYPE_HOME_GOALS = 3;   //主队进球
    public final static int INT_VIDEO_TYPE_AWAY_GOALS = 4;   //客队进球

    public final static int VIDEO_SOURCE_CAMERA = 0;   //相机视频
    public final static int VIDEO_SOURCE_LOCAL = 1;   //手机视频
    public final static int VIDEO_SOURCE_ICLOUD = 2;   //云存储视频


    public final static int DOT_TYPE_GOAL = 0;   // 进球
    public final static int DOT_TYPE_SHOOT = 1;   //射门
    public final static int DOT_TYPE_SAVE = 2;   //扑救
    public final static int DOT_TYPE_PENALTY_KICK = 3;   //点球
    public final static int DOT_TYPE_FREE_KICK = 4;   //任意球
    public final static int DOT_TYPE_CORNER_KICK = 5;   //角球
    public final static int DOT_TYPE_BREAK_AWAY = 6;   //过人
    public final static int DOT_TYPE_FOUL = 7;   //犯规


    public final static int VIDEO_UPLOAD_TYPE_MANUAL = 2;   //手动上传
    public final static int VIDEO_UPLOAD_TYPE_AUTO = 1;   //自动上传

    public static String getVideoTypeName(int type) {
        switch(type) {
            case 0:
                return VIDEO_TYPE_STR_ALL_VIDEOS;
            case 1:
                return VIDEO_TYPE_STR_HIGH_LIGHT;
            case 2:
                return VIDEO_TYPE_STR_PLAY_BACK;
            default:
                return VIDEO_TYPE_STR_HIGH_LIGHT;
        }
    }

    public static String getVideoContent(CameraVideoListBean.VideosBean videosBean) {
        if(videosBean == null) {
            return "";
        }

        return replaceTime(videosBean.time) + " " + videosBean.address + " " + videosBean.competition_name;
    }


    public static String replaceTime(String time) {
        if(time == null) {
            return "";
        }

        return time.replace("T", " ").replace(".000Z", "");
    }

    public static boolean hasAnyVideos(CameraVideoListBean.VideosBean data) {
        String dirPath = AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + "/" + data.path;
        File dir = new File(dirPath);
        File[] videos = dir.listFiles((f, name) -> name.toLowerCase().endsWith(".mp4"));
        return videos != null && videos.length > 0;
    }

    public static String getVideoDirPath(CameraVideoListBean.VideosBean data) {
        String path = AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + "/" + data.path;
        return path.replace("//", "/");
    }

    public static File getVideoDir(CameraVideoListBean.VideosBean data) {
        return new File(getVideoDirPath(data));
    }

    public static boolean isVideoFileExist(CameraVideoDetailListBean.VideosBean data) {
        return new File(getVideoFilePath(data)).exists();
    }

    public static String getVideoFilePath(CameraVideoDetailListBean.VideosBean data) {
        String path = AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + data.link;
        return path.replace("//", "/");
    }

    public static String getVideoFilePath(HmVideo data) {
        String path = data.getPath();
        String videoFileName = StringUtils.lastPathComponent(data.getVideoUrl());
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + data.getDeviceId().hashCode() + ConnectLogic.getImpl().getBasePath() + path + "/" + videoFileName).replace("//", "/");
    }

    public static String getThumbFilePath(HmVideo data) {
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + data.getDeviceId().hashCode() + ConnectLogic.getImpl().getBasePath() +
            data.getPath() + "/" + StringUtils.lastPathComponent(data.getVideoImage())).replace("//", "/");
    }

    public static String getCompetitionThumbFilePath(HmVideo data) {
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + data.getDeviceId().hashCode() + ConnectLogic.getImpl().getBasePath() +
            data.getPath() + "/" + StringUtils.lastPathComponent(data.getHmCompetition().getCompetitionImage())).replace("//", "/");
    }

    public static String getThumbFilePath(CameraVideoDetailListBean.VideosBean data) {
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + data.thumb).replace("//", "/");
    }

    public static String getCompetitionStateFilePath(CameraVideoListBean.VideosBean data) {
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + "/" + data.path + "/mstat.log").replace("//", "/");
    }

    public static String getThumbFilePath(CameraVideoListBean.VideosBean data) {
        return AppFileUtil.getWeewaDownloadRoot() + "/" + data.deviceId.hashCode() + ConnectLogic.getImpl().getBasePath() + data.path + "/" + data.thumb;
    }

    public static String getThumbFilePath(TaskModel model) {
        return (AppFileUtil.getWeewaDownloadRoot() + "/" + model.getDeviceId().hashCode() + ConnectLogic.getImpl().getBasePath() + model.getThumbUrl()).replace("//", "/");
    }

    public static int getMotionTypeInt(String motionType) {
        String[] motionTypeList = HotmeApplication.getContext().getResources().getStringArray(R.array.motion_type_list);
        for(int i = 0; i < motionTypeList.length; i++) {
            if(motionTypeList[i].equalsIgnoreCase(motionType)) {
                return i;
            }
        }
        return 0;
    }


    public static int getPlaceTypeInt(String placeType) {
        String[] placeTypeList = HotmeApplication.getContext().getResources().getStringArray(R.array.place_type_list);
        for(int i = 0; i < placeTypeList.length; i++) {
            if(placeTypeList[i].equalsIgnoreCase(placeType)) {
                return i;
            }
        }
        return 0;
    }


    /**
     * 测试设备 如果是篮球或足球五人场，则设置12288，足球8人场为12288， 11人场为16384
     * 正式设备 如果是篮球或足球五人场，则设置12288，足球8人场为12288， 11人场为16384
     *
     * @param motionType
     * @param placeType
     * @return
     */
    public static int getDefaultAvsMBR(int motionType, int placeType) {
        boolean isTestDevice = CameraManager.isTestDevice();
        if(motionType == 2 || placeType == 1) {//篮球或足球5人场
            return 12288;
        } else if(motionType == 1 && placeType == 2) {  //足球8人场
            return 12288;
        } else if(motionType == 1 && placeType == 3) { //足球11人场
            return 16384;
        }
        return 12288;
    }

    /**
     * 测试设备 如果是篮球或足球五人场，则设置8192，足球8人场为8192， 11人场为16384
     * 正式设备 如果是篮球或足球五人场，则设置8192，足球8人场为8192， 11人场为16384
     *
     * @param motionType
     * @param placeType
     * @return
     */
    public static int getAvsMBR(int motionType, int placeType) {
        int avsMBR = CameraManager.getAvsMBR(motionType, placeType);
        if(avsMBR == -1) {
            return getDefaultAvsMBR(motionType, placeType);
        }
        return avsMBR;
    }


    public static int getLiveMBR(int motionType, int placeType) {
        int liveMBR = CameraManager.getLiveMBR(motionType, placeType);
        if(liveMBR == -1) {
            return getDefaultFecMBR(motionType, placeType);
        }
        return liveMBR;
    }


    /**
     * 如果是篮球或足球五人场，则设置4096，足球8人场4096, 11人场为8192
     *
     * @param motionType
     * @param placeType
     * @return
     */
    public static int getDefaultFecMBR(int motionType, int placeType) {
        if(motionType == 2 || placeType == 1) {//篮球或足球5人场
            return 4096;
        } else if(motionType == 1 && placeType == 2) {  //足球8人场
            return 4096;
        } else if(motionType == 1 && placeType == 3) { //足球11人场
            return 8192;
        }
        return 4096;
    }


    /**
     * 如果是篮球或足球五人场，则设置2048，足球8人场或11人场为4096
     *
     * @param motionType
     * @param placeType
     * @return
     */
    public static int getFecMBR(int motionType, int placeType) {
        int fecMBR = CameraManager.getFecMBR(motionType, placeType);
        if(fecMBR == -1) {
            return getDefaultFecMBR(motionType, placeType);
        }
        return fecMBR;
    }

    public static List<CameraVideoListBean.VideosBean> listSorting(List<CameraVideoListBean.VideosBean> listInAppxList) {
        Comparator<CameraVideoListBean.VideosBean> comparator = (details1, details2) -> {
            //排序规则，按照时间,
            if(TimeUtil.str2Millis(details1.time) < TimeUtil.str2Millis(details2.time))
                return 1;
            else {
                return -1;
            }
        };
        //这里就会自动根据规则进行排序
        Collections.sort(listInAppxList, comparator);
        return listInAppxList;
    }

    public static String getPlaceNameByType(int motionType, int placeType) {
        if(motionType == 1) {
            switch(placeType) {
                case 0:
                    return "训练";
                case 1:
                    return "5人场";
                case 2:
                    return "8人场";
                case 3:
                    return "11人场";
                default:
                    return "8人场";
            }
        } else if(motionType == 2) {
            switch(placeType) {
                case 0:
                    return "半场";
                case 1:
                    return "全场";
                default:
                    return "全场";
            }
        } else {
            return "其它";
        }

    }
}
