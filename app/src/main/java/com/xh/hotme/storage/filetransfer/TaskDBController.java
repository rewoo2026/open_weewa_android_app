package com.xh.hotme.storage.filetransfer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.VideoUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDBController {
    public final static String TABLE_NAME = "tasks";
    private final SQLiteDatabase db;

    public TaskDBController(Context context) {
        TaskDBOpenHelper openHelper = new TaskDBOpenHelper(context.getApplicationContext());
        db = openHelper.getWritableDatabase();
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllDownloadTasks() {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE upload=0", null);
        try {
            while(c.moveToNext()) {
                TaskModel model = TaskModel.fromCursor(c);
                list.add(model);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllDownloadTasks(String pathPrefix) {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE upload=0", null);
        try {
            while(c.moveToNext()) {
                try {
                    String path = c.getString(c.getColumnIndex(TaskModel.PATH));
                    if(path.startsWith(pathPrefix)) {
                        TaskModel model = TaskModel.fromCursor(c);
                        list.add(model);
                    }
                } catch (Exception e) {
                }
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllUploadTasks() {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE upload=1", null);
        try {
            while(c.moveToNext()) {
                TaskModel model = TaskModel.fromCursor(c);
                list.add(model);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllUploadTasks(String orderBy, boolean ascending) {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " +
            TABLE_NAME +
            " WHERE upload=1" +
            " ORDER BY " + orderBy +
            (ascending ? " ASC" : " DESC"), null);
        try {
            while(c.moveToNext()) {
                TaskModel model = TaskModel.fromCursor(c);
                list.add(model);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllDownloadTasks(String orderBy, boolean ascending) {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " +
            TABLE_NAME +
            " WHERE upload=0" +
            " ORDER BY " + orderBy +
            (ascending ? " ASC" : " DESC"), null);
        try {
            while(c.moveToNext()) {
                TaskModel model = TaskModel.fromCursor(c);
                list.add(model);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public List<TaskModel> getAllTasks(String key, String value, String orderBy, boolean ascending) {
        if(db == null) {
            return new ArrayList<>();
        }
        final List<TaskModel> list = new ArrayList<>();
        final Cursor c = db.rawQuery("SELECT * FROM " +
            TABLE_NAME +
            " WHERE " + key + "=?" +
            " ORDER BY " + orderBy +
            (ascending ? " ASC" : " DESC"), new String[] { value });
        try {
            while(c.moveToNext()) {
                TaskModel model = TaskModel.fromCursor(c);
                list.add(model);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public TaskModel getTaskModelByVideoId(String videoId) {
        Map<String, String> params = new HashMap<>();
        params.put("videoId", videoId);
        params.put("type", String.valueOf(TaskModel.TYPE_VIDEO));
        return findTaskModel(params);
    }

    @SuppressLint("Range")
    public TaskModel getTaskModelById(int id) {
        return findTaskModel("id", String.valueOf(id));
    }

    @SuppressLint("Range")
    public TaskModel findTaskModel(Map<String, String> params) {
        Cursor c = null;
        try {
            if(db == null) {
                return null;
            }
            if(params.isEmpty()) return null;
            List<String> values = new ArrayList<>();
            boolean first = true;
            StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME + " WHERE ");
            for(Map.Entry<String, String> entry : params.entrySet()) {
                if(!first) {
                    sql.append(" AND ");
                }
                first = false;
                sql.append(entry.getKey()).append("=?");
                values.add(entry.getValue());
            }
            c = db.rawQuery(sql.toString(), values.toArray(new String[0]));
            TaskModel model = null;
            if(c.moveToNext()) {
                model = TaskModel.fromCursor(c);
            }
            return model;
        } catch(Throwable e) {
            e.printStackTrace();
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return null;
    }

    @SuppressLint("Range")
    public TaskModel findTaskModel(String key, String value) {
        Cursor c = null;
        try {
            if(db == null) {
                return null;
            }
            if(TextUtils.isEmpty(value)) return null;
            String sql = "select * from " +
                TABLE_NAME +
                " where " + key + "=?";
            c = db.rawQuery(sql, new String[] { value });
            TaskModel model = null;
            if(c.moveToNext()) {
                model = TaskModel.fromCursor(c);
            }
            return model;
        } catch(Throwable e) {
            e.printStackTrace();
        } finally {
            if(c != null) {
                c.close();
            }
        }
        return null;
    }

    public TaskModel addTask(CameraVideoListBean.VideosBean videoBean, long thumbSize) {
        try {
            if(db == null) {
                return null;
            }
            if(videoBean == null || TextUtils.isEmpty(videoBean.path)) {
                return null;
            }

            final int id = FileDownloadUtils.generateId(videoBean.path, videoBean.path);
            TaskModel model = new TaskModel();
            model.setId(id);
            model.setUrl(videoBean.path);
            model.setPath(videoBean.path);
            model.setType(TaskModel.TYPE_COMPETITION_THUMB);
            model.setVideoId("");
            model.setDuration(0);
            model.setVideoName(String.format("%s (%s)", videoBean.competition_name, FileDownloadHelper.getAppContext().getString(R.string.thumbnail)));
            model.setSize(thumbSize);
            model.setThumbUrl(videoBean.thumb);
            model.setCompetitionName(videoBean.competition_name);
            model.setCompetitionId(videoBean.competition_id);
            model.setDeviceId(videoBean.deviceId);
            model.setKey(videoBean.deviceId + "/" + videoBean.link);
            model.setVideo_width(0);
            model.setVideo_height(0);
            model.setDomainId("");
            model.setUpload(true);

            final boolean succeed = db.insert(TABLE_NAME, null, model.toContentValues()) != -1;
            return succeed ? model : null;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public TaskModel addTask(CameraVideoDetailListBean.VideosBean videoBean, String competitionName, int type) {
        try {
            if(db == null) {
                return null;
            }
            if(videoBean == null || TextUtils.isEmpty(videoBean.path)) {
                return null;
            }

            final int id = FileDownloadUtils.generateId(videoBean.path, videoBean.path);
            TaskModel model = new TaskModel();
            model.setId(id);
            model.setUrl(videoBean.path);
            model.setPath(videoBean.path);
            model.setType(type);
            model.setVideoId(videoBean.videoId);
            model.setDuration(videoBean.duration);
            model.setVideoName(videoBean.name);
            model.setSize(videoBean.size);
            model.setThumbUrl(videoBean.thumb);
            model.setCompetitionName(competitionName);
            model.setCompetitionId(videoBean.competitionId);
            model.setDeviceId(videoBean.deviceId);
            model.setKey(videoBean.deviceId + "/" + videoBean.link);
            model.setVideo_width(videoBean.video_width);
            model.setVideo_height(videoBean.video_height);
            model.setDomainId(videoBean.domainId);
            model.setUpload(true);

            final boolean succeed = db.insert(TABLE_NAME, null, model.toContentValues()) != -1;
            return succeed ? model : null;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public TaskModel addTask(HmVideo bean, int type, String videoUrl) {
        try {
            if(db == null) {
                return null;
            }
            if(bean == null || TextUtils.isEmpty(bean.getVideoUrl())) {
                return null;
            }

            String localPath = null;
            String url = null;
            String name = bean.getVideoName();
            switch(type) {
                case TaskModel.TYPE_VIDEO:
                    localPath = VideoUtil.getVideoFilePath(bean);
                    if (videoUrl == null) {
                        url = bean.getVideoUrl();
                    }else{
                        url = videoUrl;
                    }

                    break;
                case TaskModel.TYPE_THUMB:
                    localPath = VideoUtil.getThumbFilePath(bean);
                    url = bean.getVideoImage();
                    name = String.format("%s (%s)", name, FileDownloadHelper.getAppContext().getString(R.string.thumbnail));
                    break;
                case TaskModel.TYPE_COMPETITION_THUMB:
                    localPath = VideoUtil.getCompetitionThumbFilePath(bean);
                    url = bean.getHmCompetition().getCompetitionImage();
                    name = String.format("%s (%s)", name, FileDownloadHelper.getAppContext().getString(R.string.thumbnail));
                    break;
            }
            String link = bean.getPath() + "/" + StringUtils.lastPathComponent(localPath);
            final int id = FileDownloadUtils.generateId(url, localPath);
            TaskModel model = new TaskModel();
            model.setId(id);
            model.setUrl(url);
            model.setPath(localPath);
            model.setType(type);
            model.setVideoId(bean.getVideoId());
            model.setDuration(bean.getVideoDuration());
            model.setVideoName(name);
            model.setSize(bean.getVideoSize()); // TODO 如果是thumb需要不同的size
            model.setThumbUrl(link);
            model.setCompetitionName(bean.getHmCompetition().getCompetitionName());
            model.setCompetitionId(bean.getCompetitionId());
            model.setDeviceId(bean.getDeviceId());
            model.setKey(bean.getDeviceId() + "/" + link);
            model.setVideo_width(bean.getVideoWidth());
            model.setVideo_height(bean.getVideoHeight());
            model.setDomainId("");
            model.setUpload(false);

            final boolean succeed = db.insert(TABLE_NAME, null, model.toContentValues()) != -1;
            return succeed ? model : null;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateTask(TaskModel model) {
        try {
            if(db == null) {
                return false;
            }
            return db.update(TABLE_NAME, model.toContentValues(), "id=?",
                new String[] { String.valueOf(model.getId()) }) > 0;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTaskById(int id) {
        try {
            if(db == null) {
                return false;
            }
            return db.delete(TABLE_NAME, "id=?", new String[] { String.valueOf(id) }) > 0;
        } catch(Throwable e) {

        }
        return false;
    }

    public boolean deleteTaskByVideoId(String videoId) {
        try {
            if(db == null) {
                return false;
            }
            return db.delete(TABLE_NAME, "videoId=?", new String[] { videoId }) > 0;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return false;
    }
}