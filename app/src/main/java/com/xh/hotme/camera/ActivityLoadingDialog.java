package com.xh.hotme.camera;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogActiveLoadingBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DeviceUtil;


/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/15 17:19
 */
public class ActivityLoadingDialog extends BaseDialog<DialogActiveLoadingBinding> {
    public interface ICallback {
        void onCancel();

        void onConfirm();
    }

    private ICallback iCallback;

    public ActivityLoadingDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }


    public ActivityLoadingDialog(@NonNull Context context) {
        super(context);
    }

    public ActivityLoadingDialog setContent(String content) {
        viewBinding.tvContent.setText(content);
        return this;
    }

    public ActivityLoadingDialog setStatus(String content) {
        viewBinding.tvStatus.setText(content);
        return this;
    }

    public ActivityLoadingDialog showButton(boolean isVisible) {
        viewBinding.btnLoginSubmit.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    protected void onInit() {
        viewBinding.btnLoginSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(iCallback != null) {
                    iCallback.onConfirm();
                }
                return false;
            }
        });
    }

    public void showFull() {
        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = DeviceUtil.getWidth();
        windowparams.height = DeviceUtil.getHeight();

        show();
    }
}