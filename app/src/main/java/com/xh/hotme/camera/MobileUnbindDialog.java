package com.xh.hotme.camera;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.DeviceUnbindRequestBean;
import com.xh.hotme.bluetooth.bean.DeviceUnbindSmsRequestBean;
import com.xh.hotme.bluetooth.event.RemoveDeviceEvent;
import com.xh.hotme.bluetooth.listener.IBleUnbindListener;
import com.xh.hotme.databinding.DialogMobileUnbindBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

@Keep
@SheetStyle(heightGap = 20)
public class MobileUnbindDialog extends BaseDialog<DialogMobileUnbindBinding> implements IBleUnbindListener {
    String _mobile;

    // listener
    private OnClickListener _listener;

    final String _bleMac;

    private String _requestTimeOutMsg;

    private Handler _handler;
    private boolean unbindCompleted;

    final Runnable _smsTimeoutRunnable = () -> {

    };

    final Runnable _unbindTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            MainHandler.runOnUiThread(() -> {
                DialogUtil.dismissDialog();
                ToastUtil.s(_requestTimeOutMsg);
            });
        }
    };

    private void startCodeTime(int time) {
        viewBinding.btnSendSmsCode.setTag(time);
        if(time <= 0) {
            viewBinding.btnSendSmsCode.setText(getContext().getString(R.string.login_btn_send_sms));
            viewBinding.btnSendSmsCode.setClickable(true);
            return;
        } else {
            viewBinding.btnSendSmsCode.setClickable(false);
            viewBinding.btnSendSmsCode.setText(time + getContext().getString(R.string.second));
        }
        _handler.postDelayed(() -> {
            int delayTime = (int) viewBinding.btnSendSmsCode.getTag();
            startCodeTime(--delayTime);

        }, 1000);
    }

    public MobileUnbindDialog(@NonNull final Context context, String bleMac) {
        super(context);

        _bleMac = bleMac;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        BluetoothHandle.addDeviceUnbindNotifyListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BluetoothHandle.removeDeviceUnbindNotifyListener(this);
        _handler.removeCallbacksAndMessages(null);
    }


    public void setOnClickListener(OnClickListener listener) {
        _listener = listener;
    }

    public void setNegativeButtonVisible(boolean visible) {

        viewBinding.ivClose.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    public void postSendCodeSuc() {
        startCodeTime(Constants.sms_count_down);
        viewBinding.etSmsCode.requestFocus();
    }

    public void postSendCodeFail(String message) {
        if(TextUtils.isEmpty(message)) {
            showLoginStatus(getContext().getString(R.string.info_send_sms_fail));
        } else {
            showLoginStatus(message);
        }
    }

    public void postLoginFail(String message) {
        showLoginStatus(message != null ? message : getContext().getString(R.string.device_unbind_fail));
    }

    private void showLoginStatus(String message) {
        viewBinding.llStatus.setVisibility(View.VISIBLE);
        viewBinding.loginStatus.setText(message);
    }

    @Override
    public void onSendUnbindSms() {
        AppTrace.d("", "onSendUnbindSms");
        _handler.removeCallbacks(_smsTimeoutRunnable);
        MainHandler.runOnUiThread(() -> {
            if (!isShowing() || unbindCompleted) return;
            DialogUtil.dismissDialog();

            postSendCodeSuc();

        });
    }

    @Override
    public void onSendUnbindSmsFail(String message) {
        AppTrace.d("", "onSendUnbindSmsFail");
        _handler.removeCallbacks(_smsTimeoutRunnable);

        MainHandler.runOnUiThread(() -> {
            if (!isShowing() || unbindCompleted) return;
            DialogUtil.dismissDialog();
            postSendCodeFail(message);
        });
    }

    @Override
    public void onUnbind() {
        AppTrace.d("", "onUnbind");
        _handler.removeCallbacks(_unbindTimeoutRunnable);

        MainHandler.runOnUiThread(() -> {
            if (unbindCompleted) return;
            unbindCompleted = true;
            _handler.removeCallbacksAndMessages(null);
            DialogUtil.dismissDialog();
            // Close the form before disconnecting or notifying device-list listeners.
            dismiss();
            ToastUtil.s(getContext().getString(R.string.device_unbind_ok));
            BluetoothManager.getImpl().disconnectBle();
            CameraManager.release();
            DeviceManager.getImpl().removeBindDeviceData(_bleMac);
            CameraManager.setDeviceId("");
            EventBus.getDefault().post(new RemoveDeviceEvent(_bleMac));
        });

    }

    @Override
    public void onUnbindFail(String message) {
        AppTrace.d("", "onUnbindFail: " + message);
        _handler.removeCallbacks(_unbindTimeoutRunnable);

        MainHandler.runOnUiThread(() -> {
            if (!isShowing() || unbindCompleted) return;
            postLoginFail(message);

            DialogUtil.dismissDialog();
        });
    }

    @Override
    protected void onInit() {
        _requestTimeOutMsg = getContext().getString(R.string.request_timeout);

        _handler = new Handler();

        //获取连接设备的mobile
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo != null && cameraInfo.deviceInfo != null) {
            _mobile = cameraInfo.deviceInfo.account;
        }

        viewBinding.tvMobile.setText(_mobile);

        viewBinding.btnSendSmsCode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.llStatus.setVisibility(View.GONE);

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceUnbindSmsRequestBean(_mobile, _bleMac));
                if(isOk) {
                    _handler.postDelayed(_smsTimeoutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getContext().getString(R.string.loading));
                } else {
                    showDisconnectToast();
                }

                return true;
            }
        });

        viewBinding.btnUnbound.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String code = viewBinding.etSmsCode.getText().toString().trim();
                if(TextUtils.isEmpty(code)) {
                    viewBinding.llStatus.setVisibility(View.VISIBLE);
                    viewBinding.loginStatus.setText(getContext().getString(R.string.info_login_token_empty));
                    return true;
                }
                viewBinding.llStatus.setVisibility(View.GONE);

                String token = LoginManager.getUserToken();
                DeviceUnbindRequestBean requestBean = new DeviceUnbindRequestBean(_mobile, code, _bleMac, token);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
                if(isOk) {
                    DialogUtil.showDialog(getContext().getString(R.string.loading));
                    _handler.postDelayed(_unbindTimeoutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }

                return true;
            }
        });

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onClick(MobileUnbindDialog.this, DialogInterface.BUTTON_NEGATIVE);
                }
                return true;
            }
        });


        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
