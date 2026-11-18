package com.xh.hotme.storage.filetransfer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.Keep;

import com.liulishuo.filedownloader.upload.backend.obs.ObsConstant;
import com.liulishuo.filedownloader.upload.backend.obs.ObsInfo;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.Serializable;

/**
 * 任务下载模型
 */
@Keep
public class TaskModel implements Serializable {
    public final static int TYPE_VIDEO = 0;
    public final static int TYPE_THUMB = 1;
    public final static int TYPE_COMPETITION_THUMB = 2;

    public final static String ID = "id";//id由FileDownLoader生成
    public final static String SIZE = "size";//视频大小
    public final static String TYPE = "type"; // 文件类型
    public final static String DURATION = "duration";//视频时长
    public final static String URL = "url"; // 上传任务的url和path一样, 下载任务的url就是真正的url
    public final static String PATH = "path";
    public final static String KEY = "key";
    public final static String VIDEO_ID = "videoId";//视频id
    public final static String VIDEO_NAME = "videoName";//视频名字
    public final static String THUMB_URL = "thumb_url";//缩略图url
    public final static String DEVICE_ID = "device_id";// 设备ID
    public final static String COMPETITION_ID = "competition_id";//比赛ID
    public final static String COMPETITION_NAME = "competition_name";//比赛名称
    public final static String DOMAIN_ID = "domainId";//视频id
    public final static String WIDTH = "width";  //width
    public final static String HEIGHT = "height";  //height
    public final static String UPLOAD = "upload"; // true mean is a upload task

    private int id;
    private long size;
    private int duration;
    private String url;
    private String path;
    private String key;
    private int type;
    private String domainId;
    private String videoId;
    private String competitionId;
    private String competitionName;
    private String videoName;
    private String thumbUrl;
    private String deviceId;

    private int video_width;
    private int video_height;
    private boolean upload;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }


    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public int getVideo_width() {
        return video_width;
    }

    public void setVideo_width(int video_width) {
        this.video_width = video_width;
    }

    public int getVideo_height() {
        return video_height;
    }

    public void setVideo_height(int video_height) {
        this.video_height = video_height;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String url) {
        this.thumbUrl = url;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public boolean isUpload() {
        return upload;
    }

    public void setUpload(boolean upload) {
        this.upload = upload;
    }

    // 用于ui, 不保存到数据库
    private int uiType = TaskItemAdapter.ITEM_FILE_TRANSFER_TASK;
    private boolean uiMarkDelete = false;

    public int getUiType() {
        return uiType;
    }

    public void setUiType(int uiType) {
        this.uiType = uiType;
    }

    public boolean isUiMarkDelete() {
        return uiMarkDelete;
    }

    public void setUiMarkDelete(boolean uiMarkDelete) {
        this.uiMarkDelete = uiMarkDelete;
    }

    public String getObsUrl() {
        if(FileDownloadUtils.getObsConfig() != null){
            ObsInfo obsInfo = FileDownloadUtils.getObsConfig();
            return String.format("https://%s.%s:443/%s",
                    obsInfo.bucketName,
                    obsInfo.hostName,
                    key
            );
        } else {
            return String.format("https://%s.%s:443/%s",
                    ObsConstant.OBS_DEFAULT_BUCKET_NAME,
                    ObsConstant.OBS_DEFAULT_HOST_NAME,
                    key
            );
        }
    }

    private String obsUrl;
    private void setObsUrl(String url){
        this.obsUrl = url;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(ID, id);
        cv.put(SIZE, size);
        cv.put(URL, url);
        cv.put(PATH, path);
        cv.put(KEY, key);
        cv.put(TYPE, type);
        cv.put(WIDTH, video_width);
        cv.put(HEIGHT, video_height);
        cv.put(DURATION, duration);
        cv.put(VIDEO_ID, videoId);
        cv.put(VIDEO_NAME, videoName);
        cv.put(COMPETITION_ID, competitionId);
        cv.put(COMPETITION_NAME, competitionName);
        cv.put(THUMB_URL, thumbUrl);
        cv.put(DEVICE_ID, deviceId);
        cv.put(DOMAIN_ID, domainId);
        cv.put(UPLOAD, upload);
        return cv;
    }

    @SuppressLint("Range")
    public static TaskModel fromCursor(Cursor c) {
        try {
            TaskModel model = new TaskModel();
            model.setId(c.getInt(c.getColumnIndex(TaskModel.ID)));
            model.setSize(c.getLong(c.getColumnIndex(TaskModel.SIZE)));
            model.setUrl(c.getString(c.getColumnIndex(TaskModel.URL)));
            model.setType(c.getInt(c.getColumnIndex(TaskModel.TYPE)));
            model.setPath(c.getString(c.getColumnIndex(TaskModel.PATH)));
            model.setVideoId(c.getString(c.getColumnIndex(TaskModel.VIDEO_ID)));
            model.setVideoName(c.getString(c.getColumnIndex(TaskModel.VIDEO_NAME)));
            model.setThumbUrl(c.getString(c.getColumnIndex(TaskModel.THUMB_URL)));
            model.setDuration(c.getInt(c.getColumnIndex(TaskModel.DURATION)));
            model.setCompetitionId(c.getString(c.getColumnIndex(TaskModel.COMPETITION_ID)));
            model.setCompetitionName(c.getString(c.getColumnIndex(TaskModel.COMPETITION_NAME)));
            model.setDeviceId(c.getString(c.getColumnIndex(TaskModel.DEVICE_ID)));
            model.setKey(c.getString(c.getColumnIndex(TaskModel.KEY)));
            model.setVideo_width(c.getInt(c.getColumnIndex(TaskModel.WIDTH)));
            model.setVideo_height(c.getInt(c.getColumnIndex(TaskModel.HEIGHT)));
            model.setDomainId(c.getString(c.getColumnIndex(TaskModel.DOMAIN_ID)));
            model.setUpload(c.getShort(c.getColumnIndex(TaskModel.UPLOAD)) != 0);
            return model;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}