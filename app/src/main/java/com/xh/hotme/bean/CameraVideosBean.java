package com.xh.hotme.bean;

import android.text.TextUtils;

import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.VideoUtil;

import java.io.Serializable;

public class CameraVideosBean implements Serializable {
    public String path;
    public int type;
    public long size;
    public String name;
    public int duration = 0;
    public String link;
    public int video_width;
    public int video_height;
    public String thumb;

    public String deviceId;
    public int hl_type;

    public CameraVideosBean() {
    }

    public CameraVideosBean(CameraVideosBean bean) {
        path = bean.path;
        type = bean.type;
        size = bean.size;
        name = bean.name;
        duration = bean.duration;
        link = bean.link;
        video_width = bean.video_width;
        video_height = bean.video_height;
        thumb = bean.thumb;
        deviceId = bean.deviceId;
    }

    public CameraVideosBean(HmVideo bean) {
        path = VideoUtil.getVideoFilePath(bean);
        type = bean.getVideoClassify();
        size = bean.getVideoSize();
        name = bean.getVideoName();
        duration = bean.getVideoDuration();
        link = bean.getPath() + "/" + StringUtils.lastPathComponent(path);
        video_width = bean.getVideoWidth();
        video_height = bean.getVideoHeight();
        thumb = TextUtils.isEmpty(bean.getVideoImage()) ? "" : (bean.getPath() + "/" + StringUtils.lastPathComponent(bean.getVideoImage()));
        deviceId = bean.getDeviceId();
    }
}
