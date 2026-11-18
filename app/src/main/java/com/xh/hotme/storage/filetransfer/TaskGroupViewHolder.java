package com.xh.hotme.storage.filetransfer;

import android.annotation.SuppressLint;
import android.view.View;

import com.xh.hotme.base.BaseVH;
import com.xh.hotme.databinding.ListItemFileTransferGroupBinding;
import com.xh.hotme.utils.ClickGuard;

public class TaskGroupViewHolder extends BaseVH<TaskModel, ListItemFileTransferGroupBinding> {
    public TaskGroupViewHolder(ListItemFileTransferGroupBinding viewBinding) {
        super(viewBinding);

        viewBinding.tvDelete.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_model != null) {
                    TaskManager.getImpl().deleteMarkedTask(_model.isUpload());
                }
                return true;
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void update(TaskModel model, int position) {
        _model = model;
        if(model == null) return;

        switch(model.getUiType()) {
            case TaskItemAdapter.ITEM_UPLOADING_GROUP:
                viewBinding.tvTitle.setText(String.format("正在上传(%d)", _model.getSize()));
                viewBinding.tvDelete.setVisibility(View.GONE);
                break;
            case TaskItemAdapter.ITEM_UPLOADED_GROUP:
                viewBinding.tvTitle.setText(String.format("上传完成(%d)", _model.getSize()));
                viewBinding.tvDelete.setVisibility(View.VISIBLE);
                viewBinding.tvDelete.setEnabled(TaskManager.getImpl().hasMarkedDeleteUploadModel());
                break;
            case TaskItemAdapter.ITEM_DOWNLOADING_GROUP:
                viewBinding.tvTitle.setText(String.format("正在下载(%d)", _model.getSize()));
                viewBinding.tvDelete.setVisibility(View.GONE);
                break;
            case TaskItemAdapter.ITEM_DOWNLOADED_GROUP:
                viewBinding.tvTitle.setText(String.format("下载完成(%d)", _model.getSize()));
                viewBinding.tvDelete.setVisibility(View.VISIBLE);
                viewBinding.tvDelete.setEnabled(TaskManager.getImpl().hasMarkedDeleteDownloadModel());
                break;
        }
    }
}
