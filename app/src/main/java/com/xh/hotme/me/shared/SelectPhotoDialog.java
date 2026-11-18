package com.xh.hotme.me.shared;

import android.content.Context;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogCameraPhotoBinding;

@Keep
@SheetStyle
public class SelectPhotoDialog extends BaseDialog<DialogCameraPhotoBinding> {
    // listener
    private View.OnClickListener cameraBtnClickListener;
    private View.OnClickListener albumBtnClickListener;
    private View.OnClickListener cancelBtnClickListener;

    public SelectPhotoDialog(@NonNull final Context context) {
        super(context);
    }

    @Override
    protected void onInit() {
        // views
        viewBinding.dialogClose.setOnClickListener(v -> {
            if(cancelBtnClickListener != null) {
                cancelBtnClickListener.onClick(v);
            }
            dismiss();
        });

        viewBinding.fromCamera.setOnClickListener(v -> {
            if(cameraBtnClickListener != null) {
                cameraBtnClickListener.onClick(v);
            }
            dismiss();
        });
        viewBinding.fromAlbum.setOnClickListener(v -> {
            if(albumBtnClickListener != null) {
                albumBtnClickListener.onClick(v);
            }
            dismiss();
        });

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void disableCamera() {
        viewBinding.fromCamera.setVisibility(View.GONE);
        viewBinding.cameraAlbumDivider.setVisibility(View.GONE);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


    public void setCameraBtnClickListener(View.OnClickListener listener) {
        cameraBtnClickListener = listener;
    }

    public void setAlbumBtnClickListener(View.OnClickListener listener) {
        albumBtnClickListener = listener;
    }

    public void setCancelBtnClickListener(View.OnClickListener listener) {
        cameraBtnClickListener = listener;
    }

}
