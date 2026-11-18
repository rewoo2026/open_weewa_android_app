package com.xh.hotme.storage.filetransfer;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.storage.TransferListActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;

public class VideoUploadListener extends FileDownloadListener {
    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        // get holder and model
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update status
        if(model.getType() == TaskModel.TYPE_VIDEO) {
            ApiUtil.updateVideoUploadStatus(
                model.getVideoId(),
                model.getDomainId(),
                Constants.STATUS_STARTING,
                0,
                0,
                model.getObsUrl(),
                null
            );
        }

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        // build view
        TaskManager.getImpl().rebuildAndNotifyUploadViewList();
    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        // get holder and model
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update status
        if(model.getType() == TaskModel.TYPE_VIDEO) {
            ApiUtil.updateVideoUploadStatus(
                model.getVideoId(),
                model.getDomainId(),
                Constants.STATUS_PROCESSING,
                task.getSpeed() * 1024,
                soFarBytes,
                model.getObsUrl(),
                null
            );
        }

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        // get holder and model
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update status
        switch(model.getType()) {
            case TaskModel.TYPE_VIDEO:
                ApiUtil.updateVideoUploadStatus(
                    model.getVideoId(),
                    model.getDomainId(),
                    Constants.STATUS_COMPLETE,
                    0,
                    model.getSize(),
                    model.getObsUrl(),
                    null
                );
                break;
            case TaskModel.TYPE_THUMB:
                ApiUtil.updateVideoThumbUrl(
                    model.getCompetitionId(),
                    model.getVideoId(),
                    model.getObsUrl(),
                    null
                );
                break;
            case TaskModel.TYPE_COMPETITION_THUMB:
                ApiUtil.updateCompetitionImage(
                    model.getCompetitionId(),
                    model.getObsUrl()
                );
                break;
        }

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        // build view
        TaskManager.getImpl().rebuildAndNotifyUploadViewList();

        // trigger an event
        EventBus.getDefault().post(new UploadTaskCompletedEvent(model));
    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        // get holder and model
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update status
        if(model.getType() == TaskModel.TYPE_VIDEO) {
            ApiUtil.updateVideoUploadStatus(
                model.getVideoId(),
                model.getDomainId(),
                Constants.STATUS_STOP,
                0,
                soFarBytes,
                model.getObsUrl(),
                null
            );
        }

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
        // get holder and model
        AppTrace.e("VideoUploadListener", e.getLocalizedMessage());
        e.printStackTrace();
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update status
        if(model.getType() == TaskModel.TYPE_VIDEO) {
            ApiUtil.updateVideoUploadStatus(
                model.getVideoId(),
                model.getDomainId(),
                Constants.STATUS_FAIL,
                0,
                0,
                model.getObsUrl(),
                null
            );
        }

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        // 针对某些错误显示提示
        if(ActivityStackManager.impl.isActivityVisible(TransferListActivity.class)) {
            if(e instanceof FileNotFoundException) {
                MainHandler.runOnUiThread(() -> ToastUtil.s(HotmeApplication.getContext().getString(R.string.info_file_not_exist)));
            } else {
                MainHandler.runOnUiThread(() -> ToastUtil.s(e.getLocalizedMessage()));
            }
        }
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        // get holder and model
        TaskModel model = null;
        final TaskItemViewHolder holder = (TaskItemViewHolder) task.getTag();
        if(holder != null) {
            model = holder.getModel();
        } else {
            model = TaskManager.getImpl().getTaskById(task.getId());
        }

        // check model
        if(model == null) return;
        if(model.getId() != task.getId()) return;

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }
    }
}
