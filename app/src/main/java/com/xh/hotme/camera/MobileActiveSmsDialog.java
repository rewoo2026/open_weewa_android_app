package com.xh.hotme.camera;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.DeviceActiveSmsRequestBean;
import com.xh.hotme.bluetooth.listener.IBleActiveSmsListener;
import com.xh.hotme.databinding.DialogMobileSmsBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.RegExpUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

@Keep
@SheetStyle(heightGap = 20)
public class MobileActiveSmsDialog extends BaseDialog<DialogMobileSmsBinding> implements IBleActiveSmsListener {
    private final static String TAG = "MobileActiveSmsDialog";
    // listener
    private IActiveSmsListener _listener;

    private String info_timeout;

    //    Device _device;
    String _mobile;

    ActivityLoadingDialog _loadingDialog;

    ModalDialog _otherLoginDialog;

    BindDeviceBean _bindDeviceBean;

    private Handler _handler;
    private final Device _device;

    final Runnable _smsTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            MainHandler.runOnUiThread(() -> {
                DialogUtil.dismissDialog();

                dismissLoadingMsg();

                ToastUtil.s(info_timeout);
                viewBinding.llStatus.setVisibility(View.VISIBLE);
                viewBinding.loginStatus.setText(info_timeout);
            });
        }
    };

    public MobileActiveSmsDialog(@NonNull final Context context, Device device) {
        super(context);
        _device = device;
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        BluetoothHandle.addDeviceActiveSmsNotifyListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BluetoothHandle.removeDeviceActiveSmsNotifyListener(this);

        dismissLoadingMsg();
    }

    public void setOnClickListener(IActiveSmsListener listener) {
        _listener = listener;
    }

    private void dismissLoadingMsg() {
        if(_loadingDialog != null && _loadingDialog.isShowing()) {
            _loadingDialog.dismiss();
        }
        _loadingDialog = null;
    }

    @Override
    public void onSendActiveSms(String code) {
        AppTrace.d(TAG, "onSendActiveSms");
        _handler.removeCallbacks(_smsTimeoutRunnable);

        MainHandler.runOnUiThread(() -> {
            DialogUtil.dismissDialog();
            if(code.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_DEVICE_BOUND)) {
                ApiUtil.sendLoginSMS(_mobile, new ApiCallback() {
                    @Override
                    public void onSuccess() {
                        dismissLoadingMsg();
                        if(_listener != null) {
                            _listener.onActiveSms(_mobile, BleConstants.BLE_SMS_SUCCESS_LOGIN);
                        }
                    }

                    @Override
                    public void onFail(String code1, String message) {

                        dismissLoadingMsg();
                        postSendSmsFail(message);
                    }
                });
            } else {
                if(_listener != null) {
                    _listener.onActiveSms(_mobile, code);
                }
            }
        });
    }

    private void postSendSmsFail(String message) {
        AppTrace.d(TAG, "postSendSmsFail: " + message);
        MainHandler.runOnUiThread(() -> {

            dismissLoadingMsg();

            DialogUtil.dismissDialog();
            viewBinding.loginStatus.setText(message);
            viewBinding.llStatus.setVisibility(View.VISIBLE);

            if(_listener != null) {
                _listener.onActiveSmsFail(_mobile, message);
            }
        });
    }

    @Override
    public void onSendActiveSmsFail(String message) {
        AppTrace.d(TAG, "onSendActiveSmsFail: " + message);
        _handler.removeCallbacks(_smsTimeoutRunnable);

        // 处理验证码未过期的时候不会跳转输入验证码的问题，手机号验证码就应该放在一个页面啊啊啊 解绑和登录都在一起，看起里没问题
        if (message.contains("验证码未过期")) {
            if(_listener != null) {
                _listener.onActiveSms(_mobile, BleConstants.BLE_SMS_SUCCESS_BIND);
            }
        }else {
            postSendSmsFail(message);
        }


    }

    @Override
    protected void onInit() {
        _handler = new Handler();

        info_timeout = getContext().getString(R.string.request_timeout);


        viewBinding.clear.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.etMobile.getText().clear();

                return true;
            }
        });

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                _handler.removeCallbacksAndMessages(null);

                dismiss();
                return true;
            }
        });

        viewBinding.btnLoginSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                viewBinding.loginStatus.setText("");
                viewBinding.llStatus.setVisibility(View.GONE);
                _mobile = viewBinding.etMobile.getText().toString();
                if(TextUtils.isEmpty(_mobile) || !RegExpUtil.isMobileNumber(_mobile)) {
//                    ToastUtil.s(_context.getString(R.string.login_error_phone_format));
                    viewBinding.loginStatus.setText(getContext().getString(R.string.login_error_phone_format));
                    viewBinding.llStatus.setVisibility(View.VISIBLE);
                    return true;
                }
                String address = "";
                if(_device == null) {
                    address = CameraManager.getDeviceBluetoothId();
                    if(TextUtils.isEmpty(address)) {
                        String error = "";
                        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
                            error = getContext().getString(R.string.info_network_disconnect);
                        }else{
                            error = getContext().getString(R.string.info_bluetooth_offline);
                        }
                        viewBinding.loginStatus.setText(error);
                        viewBinding.llStatus.setVisibility(View.VISIBLE);
                        return true;
                    }
                } else {
                    address = _device.address;
                }
                getActiveStatus(address);

                return true;
            }
        });

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public interface IActiveSmsListener {
        void onActiveSms(String mobile, String code);

        void onActiveSmsFail(String mobile, String message);

        void onCancel();

        void onLogin();
    }

    private void getActiveStatus(String address) {
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo == null || cameraInfo.deviceInfo == null) {
            ToastUtil.s(getContext().getString(R.string.info_disconnect_device_retry_for_device_info_exception));
            return;
        }

        if(cameraInfo.deviceInfo != null && TextUtils.isEmpty(cameraInfo.deviceInfo.device_id)) {
            ToastUtil.s(getContext().getString(R.string.info_active_fail_for_device_id_null));
            return;
        }

        if(_bindDeviceBean != null) {
            checkBindDevice(address);
            return;
        }

        if(!NetUtil.isConnectedAvailableNetwork()) {
            ToastUtil.l(R.string.error_network_error);
            return;
        }

        ApiUtil.getDeviceInfo(cameraInfo.deviceInfo.device_id, new IGenericApiCallback<BindDeviceBean>() {
            @Override
            public void onSuccess(BindDeviceBean data) {
                if(data != null) {
                    _bindDeviceBean = data;
                    MainHandler.runOnUiThread(() -> checkBindDevice(address));
                }
            }

            @Override
            public void onFail(String code, String message) {
                if(code.equalsIgnoreCase("602")) {
                    checkBindDevice(address);
                } else {
                    MainHandler.runOnUiThread(() -> ToastUtil.s(message));
                }
            }

            @Override
            public void onFinish() {

            }
        });
    }

    private void checkBindDevice(String address) {
        if(_bindDeviceBean != null) {
            if(_bindDeviceBean.getBindingStatus() != null && _bindDeviceBean.getBindingStatus() == 1) {
                if(!TextUtils.isEmpty(_bindDeviceBean.getBindingPhone()) && _mobile.equalsIgnoreCase(_bindDeviceBean.getBindingPhone())) {
                    _loadingDialog = new ActivityLoadingDialog(getContext());
                    _loadingDialog.setStatus(getContext().getString(R.string.device_actived));
                    _loadingDialog.setContent(getContext().getString(R.string.right_now_to_login));

                    requestDeviceSms(address, _loadingDialog);
                } else {
                    _loadingDialog = new ActivityLoadingDialog(getContext());
                    _loadingDialog.setStatus(getContext().getString(R.string.device_bound));
                    _loadingDialog.setContent(getContext().getString(R.string.device_try_another_device));
                    _loadingDialog.showButton(true);
                    _loadingDialog.setCallback(new ActivityLoadingDialog.ICallback() {
                        @Override
                        public void onCancel() {
                            _loadingDialog.dismiss();
                        }

                        @Override
                        public void onConfirm() {
                            if(_otherLoginDialog != null && _otherLoginDialog.isShowing()) {
                                _otherLoginDialog.dismiss();
                            }
                            _otherLoginDialog = null;

                            _otherLoginDialog = new ModalDialog(getContext());
                            _otherLoginDialog.setMessage(getContext().getString(R.string.login_another_account));
                            _otherLoginDialog.setNegativeButton(R.string.exit, new ClickGuard.GuardedOnClickListener() {
                                @Override
                                public boolean onClicked() {
                                    _loadingDialog.dismiss();
                                    if(_listener != null) {
                                        _listener.onCancel();
                                    }
                                    return true;
                                }
                            });
                            _otherLoginDialog.setPositiveButton(R.string.go_to_login, new ClickGuard.GuardedOnClickListener() {
                                @Override
                                public boolean onClicked() {

                                    _loadingDialog.dismiss();
                                    if(_listener != null) {
                                        _listener.onLogin();
                                    }
                                    return true;
                                }
                            });
                            _otherLoginDialog.show();
                        }
                    });
                    _loadingDialog.showFull();
                }
            } else {
                _loadingDialog = new ActivityLoadingDialog(getContext());
                _loadingDialog.setStatus(getContext().getString(R.string.device_active_ing));
                _loadingDialog.setContent(getContext().getString(R.string.right_now_to_register));
                requestDeviceSms(address, _loadingDialog);
            }
        } else {
            _loadingDialog = new ActivityLoadingDialog(getContext());
            _loadingDialog.setStatus(getContext().getString(R.string.device_active_ing));
            _loadingDialog.setContent(getContext().getString(R.string.right_now_to_register));
            requestDeviceSms(address, _loadingDialog);
        }
    }

    private void requestDeviceSms(String address, ActivityLoadingDialog _dialog) {
        DeviceActiveSmsRequestBean requestBean = new DeviceActiveSmsRequestBean(_mobile, address);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
        if(isOk) {
            if(_dialog != null) {
                _dialog.showFull();
            }
            _handler.postDelayed(_smsTimeoutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getContext().getString(R.string.info_active_send_sms_code));
        } else {
            if(_dialog != null && _dialog.isShowing()) {
                _dialog.dismiss();
            }
            _loadingDialog = null;
            String error = "";
            if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
                error = getContext().getString(R.string.info_network_disconnect);
            }else{
                error = getContext().getString(R.string.info_bluetooth_offline);
            }

            viewBinding.loginStatus.setText(error);
            viewBinding.llStatus.setVisibility(View.VISIBLE);
        }
    }
}
