package com.xh.hotme.record.setting;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeviceUpdateNameRequestBean;
import com.xh.hotme.bluetooth.listener.IBleDeviceNameNotifyListener;
import com.xh.hotme.databinding.DialogDeviceNameBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IUpdateDevNameListener;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.YTextWatcher;


@Keep
@SheetStyle(heightGap = 20)
public class SetDeviceNameDialog extends BaseDialog<DialogDeviceNameBinding> implements IBleDeviceNameNotifyListener {
    private static final String TAG = "BluetoothListDialog";

    // listener
    private IUpdateDevNameListener _listener;

    private Handler _handler;

    String _name;

    final int maxLen = 10;

    private final Runnable _timeOutRunnable = () -> {
    };


    public SetDeviceNameDialog(@NonNull final Context context, String name) {
        super(context, R.style.hotme_custom_dialog);

        if(!TextUtils.isEmpty(name)) {
            viewBinding.etName.setText(name);
            _name = name;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        BluetoothHandle.addDeviceNameNotifyListener(this);

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        BluetoothHandle.removeDeviceNameNotifyListener(this);

    }

    public void setOnClickListener(IUpdateDevNameListener listener) {
        _listener = listener;
    }


    @Override
    public void onDeviceName() {
        MainHandler.runOnUiThread(() -> {
            try {
                if(_listener != null) {
                    _listener.onUpdate(_name);
                }
                ToastUtil.s(getContext().getString(R.string.info_update_dev_name_suc));
                dismiss();
            } catch(Throwable e) {

            }
        });
    }

    @Override
    public void onDeviceNameFail(String msg) {
        MainHandler.runOnUiThread(() -> ToastUtil.s(getContext().getString(R.string.update_fail) + "：" + msg));
    }

    @Override
    protected void onInit() {
        _handler = new Handler();

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onCancel();
                }
                dismiss();
                return true;
            }
        });

        viewBinding.btnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                String name = viewBinding.etName.getText().toString();
                if(TextUtils.isEmpty(name)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_device_name));
                    return true;
                }

                if(!TextUtils.isEmpty(_name) && _name.equalsIgnoreCase(name)) {
                    ToastUtil.s(getContext().getString(R.string.info_device_name_the_same_as_device));
                    return true;
                }

                _name = name;

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceUpdateNameRequestBean(name));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.etName.addTextChangedListener(new YTextWatcher() {
            public void afterTextChanged(Editable s) {
                String content = viewBinding.etName.getText().toString();
                int count = content.getBytes().length;
                if(count > maxLen) {
                    int length = content.length();
                    String tempStr = content.substring(0, length - 1);
                    viewBinding.etName.setText(tempStr);
                    viewBinding.etName.setSelection(viewBinding.etName.getText().length());
                    ToastUtil.s(getContext().getString(R.string.info_length_limit));
                }
            }
        });

        // set content view
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
