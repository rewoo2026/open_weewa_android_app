package com.xh.hotme.record.bean;


import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.ScoreCenterDialogDotEditBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DensityUtil;

@Keep
public class DotTimeEditDialog extends BaseDialog<ScoreCenterDialogDotEditBinding> {
    private static final String TAG = "MotionTypeDialog";


    public interface ICallback {
        void onCancel();

        void onConfirm(int minute, int second);
    }

    private DotTimeEditDialog.ICallback iCallback;


    public DotTimeEditDialog(@NonNull final Context context) {
        super(context);
    }

    public DotTimeEditDialog setCallback(DotTimeEditDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public DotTimeEditDialog setTitle(String content) {
        viewBinding.tvTitle.setText(content);
        return this;
    }

    public DotTimeEditDialog setConfirm(String content) {
        viewBinding.tvConfirm.setText(content);
        return this;
    }

    @Override
    protected void onInit() {

        viewBinding.tvCancel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(iCallback != null) {
                    iCallback.onCancel();
                }

                dismiss();
                return true;
            }
        });

        viewBinding.tvConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                int min = viewBinding.dotTimePicker.getMinute();
                int second = viewBinding.dotTimePicker.getSecond();

                if(iCallback != null) {
                    iCallback.onConfirm(min, second);
                }

                dismiss();

                return true;
            }
        });

        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = DensityUtil.dip2px(300);
        windowparams.gravity = Gravity.CENTER;
        window.setAttributes(windowparams);
    }


    public DotTimeEditDialog setDate(long time){
        viewBinding.dotTimePicker.setData(time);
        return this;
    }

    public DotTimeEditDialog setSelectItem(int time) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewBinding.dotTimePicker.setSelectItemById(time/60, time%60);
            }
        }, 200);

        return this;
    }

}
