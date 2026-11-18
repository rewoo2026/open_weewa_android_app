package com.xh.hotme.widget;


import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogPermissonsNewBinding;
import com.xh.hotme.utils.ClickGuard;


@Keep
public class PermissionDialog extends BaseDialog<DialogPermissonsNewBinding> {

    public interface ICallback {
        void onCancel();

        void onConfirm();
    }

    private PermissionDialog.ICallback iCallback;

    public PermissionDialog(@NonNull Context context) {
        super(context);
    }

    public PermissionDialog setCallback(PermissionDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    @Override
    protected void onInit() {
        viewBinding.letoOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(iCallback != null) {
                    iCallback.onConfirm();
                }
                dismiss();
                return true;
            }
        });

        viewBinding.letoCancel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(iCallback != null) {
                    iCallback.onCancel();
                }
                dismiss();
                return true;
            }
        });
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
