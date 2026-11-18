package com.xh.hotme.test;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogDeviceUnbindBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;

@Keep
public class UnbindDialog extends BaseDialog<DialogDeviceUnbindBinding> {
    IUnbindDeviceListener _listener;

    public final static int UNBIND_DEVICE_ID = 1;
    public final static int UNBIND_DEVICE_NAME = 2;
    public final static int UNBIND_PHONE = 3;


    public UnbindDialog(@NonNull final Context context, int type) {
        super(context, R.style.hotme_custom_dialog);

        if(type == UNBIND_DEVICE_NAME) {
            viewBinding.tvLabel.setText(getContext().getString(R.string.device_name));
        } else if(type == UNBIND_PHONE) {
            viewBinding.tvLabel.setText(getContext().getString(R.string.phone_number));
        }
    }

    @Override
    protected void onInit() {
        viewBinding.tvConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String name = viewBinding.etContent.getText().toString();
                if(TextUtils.isEmpty(name)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_content_is_empty));
                    return true;
                }

                if(_listener != null) {
                    _listener.onConfirm(name);
                }
                return true;
            }
        });

        viewBinding.tvCancel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_listener != null) {
                    _listener.onCancel();
                }
                return true;
            }
        });
    }

    public void setOnClickListener(IUnbindDeviceListener listener) {
        _listener = listener;
    }


    public interface IUnbindDeviceListener {
        void onConfirm(String deviceId);

        void onCancel();
    }

}
