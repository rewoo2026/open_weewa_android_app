package com.xh.hotme.storage;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.VideoEditDialogRenameVideoBinding;
import com.xh.hotme.utils.ToastUtil;


@Keep
public class VideoNameEditDialog extends BaseDialog<VideoEditDialogRenameVideoBinding> {
    private static final String TAG = "VideoNameEditDialog";

    public interface ICallback {
        void onCancel();

        void onConfirm(String name);
    }

    private VideoNameEditDialog.ICallback iCallback;

    public VideoNameEditDialog(@NonNull Context context) {
        super(context);
    }

    public VideoNameEditDialog setCallback(VideoNameEditDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public VideoNameEditDialog setContent(String content) {
        viewBinding.editContent.setText(content);
        viewBinding.editContent.setSelection(viewBinding.editContent.getText().length());
        return this;
    }

    public VideoNameEditDialog setTitle(String content) {
        viewBinding.tvTitle.setText(content);
        return this;
    }

    public VideoNameEditDialog setConfirm(String content) {
        viewBinding.tvConfirm.setText(content);
        return this;
    }

    @Override
    protected void onInit() {
        viewBinding.ivClose.setOnClickListener(v -> {
            cancel();
            if(iCallback != null) {
                iCallback.onCancel();
            }
        });
        viewBinding.tvConfirm.setOnClickListener(v -> {
            cancel();
            String name = viewBinding.editContent.getText().toString();
            if(TextUtils.isEmpty(name)) {
                ToastUtil.s(getContext().getString(R.string.game_name_is_null));
                return;
            }
            if(iCallback != null) {
                iCallback.onConfirm(name);
            }
        });
    }

}
