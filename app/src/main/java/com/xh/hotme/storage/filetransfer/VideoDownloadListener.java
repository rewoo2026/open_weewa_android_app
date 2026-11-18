package com.xh.hotme.storage.filetransfer;

import android.text.TextUtils;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.exception.FileDownloadHttpException;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.storage.TransferListActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.MediaScannerHelper;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class VideoDownloadListener extends FileDownloadListener {

    @Override
    protected void started(BaseDownloadTask task) {
        // 任务开始时执行
        AppTrace.d("VideoDownloadListener", "started");
    }

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

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        // build view
        TaskManager.getImpl().rebuildAndNotifyDownloadViewList();

        AppTrace.d("VideoDownloadListener", "pending");
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

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        try {
            AppTrace.d("VideoDownloadListener", "progress:" + (int)(soFarBytes * 100 / totalBytes));
        }catch (Throwable e){

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

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }

        // build view
        TaskManager.getImpl().rebuildAndNotifyDownloadViewList();

        //同步下载文件到媒体库
        String localPath = task.getTargetFilePath();
        if(!TextUtils.isEmpty(localPath) && localPath.endsWith(".mp4")){
            AppTrace.d("VideoDownloadListener", "文件下载到："+localPath);
            List<String> files = new ArrayList<>();
            files.add(localPath);
            MediaScannerHelper.scanFiles(HotmeApplication.getContext(), files);
        }

        // trigger an event
        EventBus.getDefault().post(new DownloadTaskCompletedEvent(model));

        AppTrace.d("VideoDownloadListener", "completed");
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

        // update ui
        if(holder != null) {
            holder.update(model, holder.getBindingAdapterPosition());
        }
        AppTrace.d("VideoDownloadListener", "paused");
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
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

        // 针对某些错误显示提示
        if(ActivityStackManager.impl.isActivityVisible(TransferListActivity.class)) {
            if(e instanceof FileDownloadHttpException) {
                if(((FileDownloadHttpException) e).getCode() == 404 || ((FileDownloadHttpException) e).getCode() == 403) {
                    MainHandler.runOnUiThread(() -> ToastUtil.s(HotmeApplication.getContext().getString(R.string.info_file_not_exist)));
                } else {
                    MainHandler.runOnUiThread(() -> ToastUtil.s(e.getLocalizedMessage()));
                }
            } else {
                MainHandler.runOnUiThread(() -> ToastUtil.s(e.getLocalizedMessage()));
            }
        }
        AppTrace.d("VideoDownloadListener", "error");
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
        AppTrace.d("VideoDownloadListener", "warn");
    }
}
