package com.xh.hotme.storage.filetransfer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.databinding.ListItemFileTransferBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.FileSizeUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.ModalDialog;

public class TaskItemViewHolder extends BaseVH<TaskModel, ListItemFileTransferBinding> {
    private final static String TAG = "TaskItemViewHolder";

    Context _context;

    public TaskItemViewHolder(Context context, ListItemFileTransferBinding viewBinding) {
        super(viewBinding);
        _context = context;

        // install listeners
        viewBinding.ivPlay.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                byte status = FileDownloader.getImpl().getStatus(_model.getUrl(), _model.getPath());
                switch(status) {
                    case FileDownloadStatus.paused:
                    case FileDownloadStatus.INVALID_STATUS:
                    case FileDownloadStatus.error:
                    case FileDownloadStatus.retry:
                        uploadToObs();
                        break;
                    default:
                        showConfirmDialog();
                        break;
                }
                return true;
            }
        });
        viewBinding.getRoot().setOnClickListener(v -> {
            TaskItemAdapter adapter = (TaskItemAdapter) getBindingAdapter();
            if(adapter != null && adapter.getOnItemClickListener() != null) {
                adapter.getOnItemClickListener().onItemClick(getBindingAdapterPosition());
            }

            // toggle delete checkbox
            if(viewBinding.cbDelete.getVisibility() == View.VISIBLE) {
                viewBinding.cbDelete.setChecked(!viewBinding.cbDelete.isChecked());
            }
        });
        viewBinding.cbDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(_model != null) {
                _model.setUiMarkDelete(isChecked);
                TaskManager.getImpl().notifyGroupIfNeeded(_model);
            }
        });
    }

    @Override
    public void update(TaskModel model, int position) {
        _model = model;
        if(model == null) return;

        viewBinding.tvTitle.setText(_model.getVideoName());
        viewBinding.tvTitle.setBackgroundColor(Color.TRANSPARENT);
        viewBinding.tvSize.setText(FileSizeUtil.formatFileSize(model.getSize()));
        viewBinding.tvSize.setVisibility(View.GONE);
        viewBinding.tvTransferSize.setText(FileSizeUtil.formatFileSize(model.getSize()));
        viewBinding.tvCompetitionName.setText(model.getCompetitionName());
        viewBinding.progressBar.setProgress(0);
        viewBinding.tvStatus.setText(String.format("等待%s...",
            model.isUpload() ? HotmeApplication.getContext().getString(R.string.upload) : HotmeApplication.getContext().getString(R.string.download)));
        viewBinding.tvProgressPercent.setText(TaskManager.getImpl().getProgress(_model.getId()) + "%");
        viewBinding.tvProgressPercent.setVisibility(View.GONE);
        viewBinding.groupProgress.setVisibility(View.VISIBLE);
        viewBinding.ivPlay.setVisibility(View.VISIBLE);
        viewBinding.cbDelete.setVisibility(View.INVISIBLE);
        viewBinding.cbDelete.setChecked(model.isUiMarkDelete());

        // set thumbnail
        if(model.isUpload()) {
            GlideUtil.loadRoundedCorner(
                HotmeApplication.getContext(),
                VideoUtil.getThumbFilePath(model),
                viewBinding.ivLogo,
                10,
                R.mipmap.video_default_cover
            );
        } else {
            GlideUtil.loadRoundedCorner(
                HotmeApplication.getContext(),
                model.getPath(),
                viewBinding.ivLogo,
                10,
                R.mipmap.video_default_cover
            );
        }

        byte status = FileDownloader.getImpl().getStatus(_model.getUrl(), _model.getPath());
        AppTrace.d(TAG, "url=" + _model.getUrl() +", status=" + status);
        switch(status) {
            case FileDownloadStatus.pending://等待
                viewBinding.tvStatus.setText(String.format("等待%s...",
                    model.isUpload() ? HotmeApplication.getContext().getString(R.string.upload) : HotmeApplication.getContext().getString(R.string.download)));
                break;
            case FileDownloadStatus.INVALID_STATUS://等待
                viewBinding.tvStatus.setText(HotmeApplication.getContext().getString(R.string.wait));
                break;
            case FileDownloadStatus.started://下载中
            case FileDownloadStatus.connected://下载中
            case FileDownloadStatus.progress://下载中
            case FileDownloadStatus.warn://已经在下载或者在队列中了，丢失下载进度和状态记录，进行重新连接
                viewBinding.tvProgressPercent.setVisibility(View.VISIBLE);
                viewBinding.tvStatus.setVisibility(View.GONE);
                viewBinding.progressBar.setProgress(TaskManager.getImpl().getProgress(_model.getId()));
                viewBinding.ivPlay.setImageResource(R.mipmap.upload_pause);
                break;
            case FileDownloadStatus.paused://暂停
                viewBinding.tvStatus.setText(HotmeApplication.getContext().getString(R.string.pause));
                viewBinding.ivPlay.setImageResource(R.mipmap.upload_start);
                break;
            case FileDownloadStatus.completed://安装
            case FileDownloadStatus.blockComplete://安装
                viewBinding.groupProgress.setVisibility(View.GONE);
                viewBinding.ivPlay.setVisibility(View.INVISIBLE);
                viewBinding.cbDelete.setVisibility(View.VISIBLE);
                viewBinding.tvSize.setVisibility(View.VISIBLE);
                break;
            case FileDownloadStatus.retry://重试
            case FileDownloadStatus.error://错误
                viewBinding.tvStatus.setText(HotmeApplication.getContext().getString(R.string.retry));
                viewBinding.ivPlay.setImageResource(R.mipmap.upload_start);
                break;
        }
    }
    private void showConfirmDialog(){
        ModalDialog dialog = new ModalDialog(_context);

        String dialogTitle = _context.getString(R.string.dialog_cancel_upload_title);
        String dialogMessage = _context.getString(R.string.dialog_cancel_upload_message);
        String dialogBtnText = _context.getString(R.string.stop_upload);
        if (!_model.isUpload()) {
             dialogTitle = _context.getString(R.string.dialog_cancel_download_title);
             dialogMessage = _context.getString(R.string.dialog_cancel_download_message);
             dialogBtnText = _context.getString(R.string.stop_download);
        }


        dialog.setTitle(dialogTitle);

        dialog.setMessage(dialogMessage);
        dialog.setNegativeButton(_context.getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        dialog.setPositiveButton(dialogBtnText, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FileDownloader.getImpl().pause(_model.getId()) <= 0) {
                    update(_model, getBindingAdapterPosition());
                }
            }
        });
        dialog.show();
    }

    public void uploadToObs(){
        if(FileDownloadUtils.getObsConfig() == null){
            ApiUtil.getObsInfo(new IApiCallback() {
                @Override
                public void onSuccess() {
                    if(TaskManager.getImpl().restartTask(_model)) {
                        update(_model, getBindingAdapterPosition());
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d("getObsFail: code=" + code +", message=" + message);
                }

                @Override
                public void onFinish() {

                }
            });
        } else {
            if(TaskManager.getImpl().restartTask(_model)) {
                update(_model, getBindingAdapterPosition());
            }
        }
    }
}
