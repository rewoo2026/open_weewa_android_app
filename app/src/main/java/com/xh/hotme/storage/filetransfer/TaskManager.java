package com.xh.hotme.storage.filetransfer;

import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import androidx.viewbinding.ViewBinding;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadConnectListener;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.VideoUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private static final String TAG = TaskManager.class.getSimpleName();

    private final static class HolderClass {
        private final static TaskManager INSTANCE = new TaskManager();
    }

    public static TaskManager getImpl() {
        return HolderClass.INSTANCE;
    }

    private final SparseArray<BaseDownloadTask> _taskSparseArray;

    private final TaskDBController _dbController;
    private final List<TaskModel> _uploadList;
    private final List<TaskModel> _uploadingList;
    private final List<TaskModel> _uploadedList;
    private final List<TaskModel> _downloadList;
    private final List<TaskModel> _downloadingList;
    private final List<TaskModel> _downloadedList;
    private final List<TaskModel> _uploadViewList;
    private final List<TaskModel> _downloadViewList;
    private final FileDownloadListener _uploadListener;
    private final FileDownloadListener _downloadListener;
    private boolean _taskRestored = false;

    // fake model for view
    private final TaskModel _uploadingGroupModel;
    private final TaskModel _uploadedGroupModel;
    private final TaskModel _downloadingGroupModel;
    private final TaskModel _downloadedGroupModel;

    private TaskManager() {
        _dbController = new TaskDBController(FileDownloadHelper.getAppContext());
        _uploadList = _dbController.getAllUploadTasks();
        _downloadList = _dbController.getAllDownloadTasks();
        _uploadViewList = new ArrayList<>();
        _downloadViewList = new ArrayList<>();
        _downloadedList = new ArrayList<>();
        _downloadingList = new ArrayList<>();
        _uploadedList = new ArrayList<>();
        _uploadingList = new ArrayList<>();
        _taskSparseArray = new SparseArray<>();
        _uploadListener = new VideoUploadListener();
        _downloadListener = new VideoDownloadListener();
        _notifiers = new ArrayList<>();
        _uploadedGroupModel = new TaskModel();
        _uploadedGroupModel.setUiType(TaskItemAdapter.ITEM_UPLOADED_GROUP);
        _uploadedGroupModel.setUpload(true);
        _uploadingGroupModel = new TaskModel();
        _uploadingGroupModel.setUiType(TaskItemAdapter.ITEM_UPLOADING_GROUP);
        _uploadingGroupModel.setUpload(true);
        _downloadingGroupModel = new TaskModel();
        _downloadingGroupModel.setUiType(TaskItemAdapter.ITEM_DOWNLOADING_GROUP);
        _downloadingGroupModel.setUpload(false);
        _downloadedGroupModel = new TaskModel();
        _downloadedGroupModel.setUiType(TaskItemAdapter.ITEM_DOWNLOADED_GROUP);
        _downloadedGroupModel.setUpload(false);
    }

    private FileDownloadConnectListener listener;
    private final List<WeakReference<ITaskDataNotifier>> _notifiers;

    private void registerServiceConnectionListener() {
        if(listener != null) {
            FileDownloader.getImpl().removeServiceConnectListener(listener);
        }

        listener = new FileDownloadConnectListener() {
            @Override
            public void connected() {
                // restore transfer
                if(!_taskRestored) {
                    for(TaskModel model : _uploadList) {
                        byte status = FileDownloader.getImpl().getStatus(model.getId());
                        if(status != FileDownloadStatus.completed) {
                            internalRestartTask(model);
                        }
                    }
                    for(TaskModel model : _downloadList) {
                        byte status = FileDownloader.getImpl().getStatus(model.getId());
                        if(status != FileDownloadStatus.completed) {
                            internalRestartTask(model);
                        }
                    }
                    _taskRestored = true;

                    // update ui
                    buildViewList();
                    notifyDataChanged();
                }
            }

            @Override
            public void disconnected() {
                clearViewList();
                notifyDataChanged();
            }
        };

        FileDownloader.getImpl().addServiceConnectListener(listener);
    }

    public void registerDataNotifier(ITaskDataNotifier n) {
        _notifiers.add(new WeakReference<>(n));
    }

    public void unregisterDataNotifier(ITaskDataNotifier n) {
        int size = _notifiers.size();
        for(int i = size - 1; i >= 0; i--) {
            WeakReference<ITaskDataNotifier> r = _notifiers.get(i);
            if(r.get() == null) {
                _notifiers.remove(i);
            } else if(r.get() == n) {
                _notifiers.remove(i);
            }
        }
    }

    private void notifyDataChanged() {
        for(WeakReference<ITaskDataNotifier> r : _notifiers) {
            if(r.get() != null) {
                r.get().postNotifyDataChanged();
            }
        }
    }

    private void notifyDataChanged(boolean upload) {
        for(WeakReference<ITaskDataNotifier> r : _notifiers) {
            if(r.get() != null && r.get().watchUpload() == upload) {
                r.get().postNotifyDataChanged();
            }
        }
    }

    private void notifyItemChanged(boolean upload, int position) {
        for(WeakReference<ITaskDataNotifier> r : _notifiers) {
            if(r.get() != null && r.get().watchUpload() == upload) {
                r.get().postNotifyItemChanged(position);
            }
        }
    }

    private void clearViewList() {
        _uploadViewList.clear();
        _downloadViewList.clear();
    }

    private void unregisterServiceConnectionListener() {
        FileDownloader.getImpl().removeServiceConnectListener(listener);
        listener = null;
    }

    public void onCreate() {
        if(!FileDownloader.getImpl().isServiceConnected()) {



            new Handler().postDelayed(() -> {
                try {
                    FileDownloader.getImpl().bindService();
                    registerServiceConnectionListener();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }, 500);

        }
    }

    public boolean hasMarkedDeleteUploadModel() {
        for(TaskModel m : _uploadedList) {
            if(m.isUiMarkDelete()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMarkedDeleteDownloadModel() {
        for(TaskModel m : _downloadedList) {
            if(m.isUiMarkDelete()) {
                return true;
            }
        }
        return false;
    }

    private void buildUploadViewList() {
        _uploadViewList.clear();
        _uploadedGroupModel.setSize(0);
        _uploadingGroupModel.setSize(0);
        _uploadingList.clear();
        _uploadedList.clear();
        for(TaskModel m : _uploadList) {
            if(isCompleted(m)) {
                _uploadedList.add(m);
            } else {
                _uploadingList.add(m);
            }
        }
        if(!_uploadingList.isEmpty()) {
            _uploadingGroupModel.setSize(_uploadingList.size());
            _uploadViewList.add(_uploadingGroupModel);
            _uploadViewList.addAll(_uploadingList);
        }
        if(!_uploadedList.isEmpty()) {
            _uploadedGroupModel.setSize(_uploadedList.size());
            _uploadViewList.add(_uploadedGroupModel);
            _uploadViewList.addAll(_uploadedList);
        }
    }

    private void buildDownloadViewList() {
        _downloadViewList.clear();
        _downloadedGroupModel.setSize(0);
        _downloadingGroupModel.setSize(0);
        _downloadedList.clear();
        _downloadingList.clear();
        for(TaskModel m : _downloadList) {
            if(isCompleted(m)) {
                _downloadedList.add(m);
            } else {
                _downloadingList.add(m);
            }
        }
        if(!_downloadingList.isEmpty()) {
            _downloadingGroupModel.setSize(_downloadingList.size());
            _downloadViewList.add(_downloadingGroupModel);
            _downloadViewList.addAll(_downloadingList);
        }
        if(!_downloadedList.isEmpty()) {
            _downloadedGroupModel.setSize(_downloadedList.size());
            _downloadViewList.add(_downloadedGroupModel);
            _downloadViewList.addAll(_downloadedList);
        }
    }

    private void buildViewList() {
        buildDownloadViewList();
        buildUploadViewList();
    }

    public synchronized void rebuildAndNotifyUploadViewList() {
        buildUploadViewList();
        notifyDataChanged(true);
    }

    public synchronized void rebuildAndNotifyDownloadViewList() {
        buildDownloadViewList();
        notifyDataChanged(false);
    }

    public void releaseTasks() {
        _taskSparseArray.clear();
    }

    public void onDestroy() {
        unregisterServiceConnectionListener();
        releaseTasks();
    }

    public boolean isReady() {
        return FileDownloader.getImpl().isServiceConnected();
    }

    public List<TaskModel> getUploadList() {
        return _uploadList;
    }

    public List<TaskModel> getDownloadList() {
        return _downloadList;
    }

    public List<TaskModel> getUploadViewList() {
        return _uploadViewList;
    }

    public List<TaskModel> getDownloadViewList() {
        return _downloadViewList;
    }

    public TaskModel getById(final int id) {
        for(TaskModel model : _uploadList) {
            if(model.getId() == id) {
                return model;
            }
        }
        for(TaskModel model : _downloadList) {
            if(model.getId() == id) {
                return model;
            }
        }
        return null;
    }

    /**
     * @param status Download Status
     * @return has already downloaded
     * @see FileDownloadStatus
     */
    public boolean isCompleted(final int status) {
        return status == FileDownloadStatus.completed;
    }

    public boolean isPaused(final int status) {
        return status == FileDownloadStatus.paused;
    }

    public boolean isCompleted(TaskModel model) {
        return isCompleted(getStatus(model.getId()));
    }

    public boolean isCompleted(final CameraVideoDetailListBean.VideosBean videosBean) {
        int uploadId = FileDownloadUtils.generateId(videosBean.path, videosBean.path);
        return isCompleted(getStatus(uploadId));
    }

    public boolean hasAnyIncompleteTask(final CameraVideoListBean.VideosBean deviceBean) {
        String dirPath = VideoUtil.getVideoDirPath(deviceBean);
        List<TaskModel> models = _dbController.getAllDownloadTasks(dirPath);
        for(TaskModel model : models) {
            if(!isCompleted(model)) {
                return true;
            }
        }
        return false;
    }

    public int getStatus(final int id) {
        return FileDownloader.getImpl().getStatus(id);
    }

    public long getTotal(final int id) {
        return FileDownloader.getImpl().getTotal(id);
    }

    public long getSoFar(final int id) {
        return FileDownloader.getImpl().getSoFar(id);
    }

    public int getProgress(final int id) {
        long total = getTotal(id);
        if(total <= 0) return 0;
        return (int) (getSoFar(id) * 100 / getTotal(id));
    }

    public void updateViewHolder(final int id, final BaseVH<TaskModel, ? extends ViewBinding> holder) {
        final BaseDownloadTask task = _taskSparseArray.get(id);
        if(task == null) {
            return;
        }

        task.setTag(holder);
    }

    public int getUploadTaskCounts() {
        return _uploadList.size();
    }

    public int getDownloadTaskCounts() {
        return _downloadList.size();
    }

    public TaskModel getTaskById(int id) {
        return _dbController.getTaskModelById(id);
    }

    public TaskModel getTaskByPosition(int position, boolean upload) {
        List<TaskModel> list = upload ? _uploadViewList : _downloadViewList;
        if(position < 0 || position >= list.size()) {
            return null;
        }
        return list.get(position);
    }

    public boolean restartTask(final TaskModel model) {
        // if already done, don't restart
        byte status = FileDownloader.getImpl().getStatus(model.getId());
        if(status == FileDownloadStatus.completed) {
            return false;
        }
        return internalRestartTask(model);
    }

    /**
     * 这个方法要求task必须不是完成状态
     */
    private boolean internalRestartTask(final TaskModel model) {
        try {
            if(model != null) {
                // start real task
                BaseDownloadTask task = _taskSparseArray.get(model.getId());
                if(task == null) {

                    task = FileDownloader.getImpl()
                        .create(model.getUrl())
                        .setPath(model.getPath())
                        .setUpload(model.isUpload())
                        .setEtag(model.getKey()) // 用etag字段代表obs的存储路径
                        .setCallbackProgressTimes(100)
                        .setListener(model.isUpload() ? _uploadListener : _downloadListener);
                    _taskSparseArray.put(task.getId(), task);
                } else {
                    if(!task.reuse()) {
                        return false;
                    }
                }
                task.start();


                // clear flags
                model.setUiMarkDelete(false);

                // ok
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized TaskModel addThumbTask(
        final CameraVideoListBean.VideosBean videosBean,
        String thumbPath,
        long thumbSize
    ) {
        // clone bean
        CameraVideoListBean.VideosBean clone = new CameraVideoListBean.VideosBean(videosBean);
        clone.link = clone.path + "/" + clone.thumb;
        clone.thumb = clone.link;
        clone.path = thumbPath;

        // check model exists
        final int id = FileDownloadUtils.generateId(thumbPath, thumbPath);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model == null) {
            model = _dbController.addTask(clone, thumbSize);
            if(model == null) {
                AppTrace.e(TAG, String.format("addThumbTask: failed to add task for thumb path: %s", thumbPath));
                return null;
            }
            _uploadList.add(model);
            buildUploadViewList();
            notifyDataChanged(true);
        }

        // try to add app side task model
        final BaseDownloadTask task = FileDownloader.getImpl()
            .create(thumbPath)
            .setPath(thumbPath)
            .setUpload(true)
            .setEtag(videosBean.deviceId + "/" + clone.link)
            .setCallbackProgressTimes(100)
            .setListener(_uploadListener);
        task.start();
        _taskSparseArray.put(task.getId(), task);
        return model;
    }

    public synchronized TaskModel addThumbTask(
        final CameraVideoDetailListBean.VideosBean videosBean,
        String competitionName,
        String thumbPath,
        long thumbSize
    ) {
        // clone bean
        CameraVideoDetailListBean.VideosBean clone = new CameraVideoDetailListBean.VideosBean(videosBean);
        clone.path = thumbPath;
        clone.size = thumbSize;
        clone.link = clone.thumb;
        clone.name = String.format("%s (%s)", clone.name, FileDownloadHelper.getAppContext().getString(R.string.thumbnail));

        // check model exists
        final int id = FileDownloadUtils.generateId(thumbPath, thumbPath);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model == null) {
            model = _dbController.addTask(clone, competitionName, TaskModel.TYPE_THUMB);
            if(model == null) {
                AppTrace.e(TAG, String.format("addThumbTask: failed to add task for thumb path: %s", thumbPath));
                return null;
            }
            _uploadList.add(model);
            buildUploadViewList();
            notifyDataChanged(true);
        } else {
            // 假如远程的视频后来被删了, 然后这边重新上传后会发现视频已经没了, 这样会重新添加, video id就会变
            if(!videosBean.videoId.equals(model.getVideoId()) || !videosBean.getDomainId().equals(model.getDomainId())) {
                model.setVideoId(videosBean.videoId);
                model.setDomainId(videosBean.getDomainId());
                _dbController.updateTask(model);
            }
        }

        // try to add app side task model
        final BaseDownloadTask task = FileDownloader.getImpl()
            .create(thumbPath)
            .setPath(thumbPath)
            .setUpload(true)
            .setEtag(videosBean.deviceId + "/" + videosBean.thumb)
            .setCallbackProgressTimes(100)
            .setListener(_uploadListener);
        task.start();
        _taskSparseArray.put(task.getId(), task);
        return model;
    }

    public synchronized TaskModel addThumbTask(HmVideo bean) {
        return addThumbTask(bean, TaskModel.TYPE_THUMB);
    }

    public synchronized TaskModel addThumbTask(HmVideo bean, int type) {
        String localPath = type == TaskModel.TYPE_THUMB ? VideoUtil.getThumbFilePath(bean) : VideoUtil.getCompetitionThumbFilePath(bean);
        String url = type == TaskModel.TYPE_THUMB ? bean.getVideoImage() : bean.getHmCompetition().getCompetitionImage();
        final int id = FileDownloadUtils.generateId(url, localPath);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model == null) {
            model = _dbController.addTask(bean, type, null);
            if(model == null) {
                AppTrace.e(TAG, String.format("addThumbTask: failed to add task for type(%d), url: %s", type, url));
                return null;
            }
            _downloadList.add(model);
            buildDownloadViewList();
            notifyDataChanged(false);
        }

        // try to add app side task model
        final BaseDownloadTask task = FileDownloader.getImpl()
            .create(url)
            .setPath(localPath)
            .setCallbackProgressTimes(100)
            .setListener(_downloadListener);
        task.start();
        _taskSparseArray.put(task.getId(), task);
        return model;
    }

    public boolean isVideoUploading(HmVideo bean) {
        TaskModel model = _dbController.getTaskModelByVideoId(bean.getVideoId());
        if(model != null) {
            return model.isUpload() && !isCompleted(model);
        }
        return false;
    }

    public boolean isVideoUploading(CameraVideoDetailListBean.VideosBean bean) {
        final int id = FileDownloadUtils.generateId(bean.path, bean.path);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model != null) {
            return model.isUpload() && !isCompleted(model);
        }
        return false;
    }
    public boolean isVideoUpload(CameraVideoDetailListBean.VideosBean bean) {
        final int id = FileDownloadUtils.generateId(bean.path, bean.path);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model != null) {
            return model.isUpload()  && isCompleted(model);
        }
        return false;
    }

    public synchronized TaskModel addTask(final HmVideo bean, String url) {
        String localPath = VideoUtil.getVideoFilePath(bean);
        final int id = FileDownloadUtils.generateId(url, localPath);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model == null) {
            model = _dbController.addTask(bean, TaskModel.TYPE_VIDEO, url);
            if(model == null) {
                AppTrace.e(TAG, String.format("addTask: failed to add task for video path: %s", localPath));
                return null;
            }
            _downloadList.add(model);
            buildDownloadViewList();
            notifyDataChanged(false);
        }

        // try to add app side task model
        final BaseDownloadTask task = FileDownloader.getImpl()
            .create(url)
            .setPath(localPath)
            .setCallbackProgressTimes(100)
            .setListener(_downloadListener);
        task.start();
        _taskSparseArray.put(task.getId(), task);
        return model;
    }

    public synchronized TaskModel addTask(final CameraVideoDetailListBean.VideosBean videosBean, String competitionName) {
        final int id = FileDownloadUtils.generateId(videosBean.path, videosBean.path);
        TaskModel model = _dbController.getTaskModelById(id);
        if(model == null) {
            model = _dbController.addTask(videosBean, competitionName, TaskModel.TYPE_VIDEO);
            if(model == null) {
                AppTrace.e(TAG, String.format("addTask: failed to add task for video path: %s", videosBean.path));
                return null;
            }
            _uploadList.add(model);
            buildUploadViewList();
            notifyDataChanged(true);
        } else {
            // 假如远程的视频后来被删了, 然后这边重新上传后会发现视频已经没了, 这样会重新添加, video id就会变
            if(!videosBean.videoId.equals(model.getVideoId()) || !videosBean.getDomainId().equals(model.getDomainId())) {
                model.setVideoId(videosBean.videoId);
                model.setDomainId(videosBean.getDomainId());
                _dbController.updateTask(model);
            }
        }

        // try to add app side task model
        final BaseDownloadTask task = FileDownloader.getImpl()
            .create(videosBean.path)
            .setPath(videosBean.path)
            .setUpload(true)
            .setEtag(videosBean.deviceId + "/" + videosBean.link)
            .setCallbackProgressTimes(100)
            .setListener(_uploadListener);
        task.start();
        _taskSparseArray.put(task.getId(), task);
        return model;
    }

    public synchronized boolean deleteUploadTaskAt(int position) {
        // get model, validate
        if(position < 0 || position >= _uploadViewList.size()) {
            return false;
        }
        TaskModel model = _uploadViewList.get(position);
        if(model.getUiType() != TaskItemAdapter.ITEM_FILE_TRANSFER_TASK) {
            return false;
        }

        // get status, some status don't need pause
        byte status = FileDownloader.getImpl().getStatus(model.getId());
        boolean deleted = false;
        switch(status) {
            case FileDownloadStatus.paused:
            case FileDownloadStatus.INVALID_STATUS:
            case FileDownloadStatus.error:
            case FileDownloadStatus.retry:
                deleted = true;
                break;
            default:
                deleted = FileDownloader.getImpl().pause(model.getId()) > 0;
                break;
        }

        // if success
        if(deleted) {
            if(_dbController.deleteTaskById(model.getId())) {
                if(deleteTask(model)) {
                    buildUploadViewList();
                    notifyDataChanged(true);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean deleteTask(TaskModel model) {
        if(model.isUpload()) {
            int size = _uploadList.size();
            for(int i = 0; i < size; i++) {
                if(_uploadList.get(i).getId() == model.getId()) {
                    _uploadList.remove(i);
                    return true;
                }
            }
        } else {
            int size = _downloadList.size();
            for(int i = 0; i < size; i++) {
                if(_downloadList.get(i).getId() == model.getId()) {
                    _downloadList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean deleteDownloadTaskAt(int position) {
        // get model and validate
        if(position < 0 || position >= _downloadViewList.size()) {
            return false;
        }
        TaskModel model = _downloadViewList.get(position);
        if(model.getUiType() != TaskItemAdapter.ITEM_FILE_TRANSFER_TASK) {
            return false;
        }

        // check status, for some status, don't need pause
        byte status = FileDownloader.getImpl().getStatus(model.getId());
        boolean deleted = false;
        switch(status) {
            case FileDownloadStatus.paused:
            case FileDownloadStatus.INVALID_STATUS:
            case FileDownloadStatus.error:
            case FileDownloadStatus.retry:
                deleted = true;
                break;
            default:
                deleted = FileDownloader.getImpl().pause(model.getId()) > 0;
                break;
        }

        // if success
        if(deleted) {
            if(_dbController.deleteTaskById(model.getId())) {
                if(deleteTask(model)) {
                    buildDownloadViewList();
                    notifyDataChanged(false);
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized int deleteMarkedTask(boolean upload) {
        // delete from real list
        int deleted = 0;
        List<TaskModel> list = upload ? _uploadedList : _downloadedList;
        for(TaskModel m : list) {
            if(m.isUiMarkDelete() && _dbController.deleteTaskById(m.getId()) && deleteTask(m)) {
                deleted++;
            }
        }

        // update ui
        if(deleted > 0) {
            if(upload) {
                buildUploadViewList();
            } else {
                buildDownloadViewList();
            }
            notifyDataChanged(upload);
        }

        // return deleted count
        return deleted;
    }

    public void notifyGroupIfNeeded(TaskModel model) {
        if(model.isUpload()) {
            int idx = _uploadViewList.indexOf(_uploadedGroupModel);
            if(idx != -1) {
                notifyItemChanged(true, idx);
            }
        } else {
            int idx = _downloadViewList.indexOf(_downloadedGroupModel);
            if(idx != -1) {
                notifyItemChanged(false, idx);
            }
        }
    }
}