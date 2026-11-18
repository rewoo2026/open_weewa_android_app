package com.xh.hotme.record.setting;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeviceUpdateSNRequestBean;
import com.xh.hotme.bluetooth.event.UpdateSNEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.DialogDeviceNameBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IUpdateDevNameListener;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


@Keep
@SheetStyle(heightGap = 20)
public class SetDeviceSNDialog extends BaseDialog<DialogDeviceNameBinding> {
    private static final String TAG = "SetDeviceSNDialog";

    // listener
    private IUpdateDevNameListener _listener;


    private Handler _handler;

    String _name;

    private final Runnable _timeOutRunnable = () -> MainHandler.runOnUiThread(DialogUtil::dismissDialog);

    public SetDeviceSNDialog(@NonNull Context context, String name) {
        super(context, R.style.hotme_custom_dialog);

        if(!TextUtils.isEmpty(name)) {
            viewBinding.etName.setText(name);
            _name = name;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

    }

    public void setOnClickListener(IUpdateDevNameListener listener) {
        _listener = listener;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateDevice(UpdateSNEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        MainHandler.runOnUiThread(() -> {
            try {
                DialogUtil.dismissDialog();
                if(event != null && event._status == 1) {
                    if(_listener != null) {
                        _listener.onUpdate(_name);
                    }
                    try {
                        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
                        deviceInfo.device_id = _name;
                        CameraManager.setDeviceInfo(deviceInfo);

                    } catch(Throwable e) {

                    }
                    ToastUtil.s(R.string.info_nickname_update_suc);
                    dismiss();
                } else {
                    ToastUtil.s(R.string.update_fail);
                }
            } catch(Throwable e) {

            }
        });
    }

    @Override
    protected void onInit() {
        _handler = new Handler();

        viewBinding.tvTitle.setText(R.string.update_dev_sn_title);
        viewBinding.etName.setHint(R.string.info_input_device_sn);

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

                String name = viewBinding.etName.getText().toString().trim();
                if(TextUtils.isEmpty(name)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_device_sn));
                    return true;
                }

                _name = name;

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceUpdateSNRequestBean(name));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getContext().getString(R.string.loading));
                }
                return true;
            }
        });

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
