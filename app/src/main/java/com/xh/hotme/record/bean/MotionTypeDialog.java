package com.xh.hotme.record.bean;


import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogMotionTypeBinding;
import com.xh.hotme.utils.ClickGuard;

@Keep
@SheetStyle
public class MotionTypeDialog extends BaseDialog<DialogMotionTypeBinding> {
    private static final String TAG = "MotionTypeDialog";

    String selected;

    int motionType;
    int placeId;


    public interface ICallback {
        void onCancel();

        void onConfirm(MotionBean motionBean, PlaceBean placeBean);
    }

    private MotionTypeDialog.ICallback iCallback;


    public MotionTypeDialog(@NonNull final Context context) {
        super(context);
    }

    public MotionTypeDialog setCallback(MotionTypeDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public MotionTypeDialog setTitle(String content) {
        viewBinding.tvTitle.setText(content);
        return this;
    }

    public MotionTypeDialog setConfirm(String content) {
        viewBinding.btnOk.setText(content);
        return this;
    }

    @Override
    protected void onInit() {

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(iCallback != null) {
                    iCallback.onCancel();
                }

                dismiss();
                return true;
            }
        });

        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                MotionBean motionBean = viewBinding.motionPicker.getMotionType();
                PlaceBean placeBean = viewBinding.motionPicker.getPlaceType();

                if(iCallback != null) {
                    iCallback.onConfirm(motionBean, placeBean);
                }

                dismiss();

                return true;
            }
        });

    }

    public MotionTypeDialog setSelectItem(int motionType, int placeType) {
        this.motionType = motionType;
        this.placeId = placeType;
        viewBinding.motionPicker.setMotionSelectItemById(motionType);
        viewBinding.motionPicker.setPlaceSelectItemById(placeType);
        return this;
    }
}
