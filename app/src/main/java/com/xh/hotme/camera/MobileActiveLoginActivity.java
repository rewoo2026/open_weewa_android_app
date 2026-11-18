package com.xh.hotme.camera;


import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.BindResultBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeviceActiveBindRequestBean;
import com.xh.hotme.bluetooth.event.DeviceEvent;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.bluetooth.event.WeewaActiveSuccessEvent;
import com.xh.hotme.bluetooth.listener.IBleActiveLoginListener;
import com.xh.hotme.databinding.ActivityMobileActiveBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.me.setting.SimpleWebActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;


public class MobileActiveLoginActivity extends BaseActivity<ActivityMobileActiveBinding> implements IBleActiveLoginListener {
    private static final String TAG = MobileActiveLoginActivity.class.getSimpleName();

    private String _mobile;
    private int reqestCode = -1;

    private int _loginType = 0;

    private boolean _requestInFlight;
    private boolean _waitingForBind;
    private int _requestGeneration;

    final Runnable _timeoutRunnable = () -> showLoginFailure(getString(R.string.request_timeout));

    private void beginRequest(String message, long timeout) {
        _requestInFlight = true;
        viewBinding.btnLoginSubmit.setEnabled(false);
        viewBinding.loginStatus.setVisibility(View.GONE);
        _handler.removeCallbacks(_timeoutRunnable);
        _handler.postDelayed(_timeoutRunnable, timeout);
        DialogUtil.showDialog(message);
    }

    private boolean isCurrentRequest(int generation) {
        return _requestInFlight && generation == _requestGeneration
                && !BaseAppUtil.isDestroy(this);
    }

    private void endRequest() {
        _requestInFlight = false;
        _waitingForBind = false;
        _requestGeneration++;
        _handler.removeCallbacks(_timeoutRunnable);
        DialogUtil.dismissDialog();
        viewBinding.btnLoginSubmit.setEnabled(true);
    }

    private void showLoginFailure(String message) {
        if (BaseAppUtil.isDestroy(this)) return;
        endRequest();
        String error = TextUtils.isEmpty(message) ? getString(R.string.login_fail) : message;
        viewBinding.loginStatus.setText(error);
        viewBinding.loginStatus.setVisibility(View.VISIBLE);
        ToastUtil.s(error);
    }

    public static void start(Context context, String mobile, int type) {
        if(null != context) {
            Intent intent = new Intent(context, MobileActiveLoginActivity.class);
            intent.putExtra(Constants.REQUEST_MOBILE, mobile);
            intent.putExtra(Constants.ACTIVE_LOGIN, type);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        if(getIntent() != null) {
            reqestCode = getIntent().getIntExtra(REQUEST_CODE, -1);
            _mobile = getIntent().getStringExtra(Constants.REQUEST_MOBILE);
            _loginType = getIntent().getIntExtra(Constants.ACTIVE_LOGIN, Constants.ACTIVE_TYPE_ACTIVE);
        }

        // find views
        viewBinding.titlebar.tvTitle.setText(getString(R.string.login));

        // back click
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                doFinish(false);
                return true;
            }
        });

        viewBinding.tvMobile.setText(_mobile);

        // agreement label underline & click
        viewBinding.userAgreement.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        viewBinding.userAgreement.getPaint().setAntiAlias(true);
        viewBinding.privacyAgreement.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        viewBinding.privacyAgreement.getPaint().setAntiAlias(true);
        viewBinding.userAgreement.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                SimpleWebActivity.start(MobileActiveLoginActivity.this, getString(R.string.user_proxy), SdkApi.user_agreement);
                return true;
            }
        });
        viewBinding.privacyAgreement.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                SimpleWebActivity.start(MobileActiveLoginActivity.this, getString(R.string.user_agreement), SdkApi.user_privacy);
                return true;
            }
        });

        viewBinding.btnLoginSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (_requestInFlight) return true;

                if(!viewBinding.cbAgree.isChecked()){
                    showLoginFailure(getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                    return true;
                }

                String sms = viewBinding.etSmsCode.getText().toString().trim();
                if(TextUtils.isEmpty(sms)) {
                    showLoginFailure(getString(R.string.login_hint_input_sms));
                    return true;
                }

                if(!NetUtil.isConnectedAvailableNetwork()) {
                    showLoginFailure(getString(R.string.error_network_error));
                    return true;
                }

                final int generation = ++_requestGeneration;
                beginRequest(getString(R.string.loading_code_verify), 45000);
                if(_loginType == Constants.ACTIVE_TYPE_LOGIN) {
                    ApiUtil.submitLogin(_mobile, sms, new IGenericApiCallback<LoginResultBean>() {
                        @Override
                        public void onSuccess(LoginResultBean data) {
                            if (!isCurrentRequest(generation)) return;
                            getUserInfo();
                        }

                        @Override
                        public void onFail(String code, String message) {
                            if (isCurrentRequest(generation)) showLoginFailure(message);
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
                } else {
                    ApiUtil.checkSmsCode(_mobile, sms, new IGenericApiCallback<BindResultBean>() {
                        @Override
                        public void onSuccess(BindResultBean data) {
                            if (!isCurrentRequest(generation)) return;
                            String mac = data == null ? null : data.getMac();
                            if (TextUtils.isEmpty(mac)) {
                                DeviceInfo device = CameraManager.getDeviceInfo();
                                if (device != null) mac = device.mac;
                            }
                            DeviceActiveBindRequestBean requestBean = new DeviceActiveBindRequestBean(_mobile, mac);
                            _waitingForBind = true;
                            boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
                            if(isOk) {
                                beginRequest(getString(R.string.loading), BluetoothManager.RESP_TIMEOUT);
                            } else {
                                showLoginFailure(getString(R.string.login_error_connect_device));
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {
                            if (isCurrentRequest(generation)) showLoginFailure(message);
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
                }
                return true;
            }
        });
        BluetoothHandle.addDeviceActiveLoginNotifyListener(this);
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onDestroy() {
        _handler.removeCallbacks(_timeoutRunnable);
        if (_requestInFlight) DialogUtil.dismissDialog();
        _requestInFlight = false;
        _waitingForBind = false;
        super.onDestroy();

        BluetoothHandle.removeDeviceActiveLoginNotifyListener(this);
    }

    public void doFinish(boolean success) {
        if(reqestCode > -1) {
            Intent i = new Intent();
            i.putExtra("result", 0);
            setResult(success ? 1 : 0, i);
            finish();
        } else {
            if(success) {
                ToastUtil.s(getString(R.string.login_success));
            }
            finish();
        }
    }

    private void getUserInfo() {
        final int generation = _requestGeneration;
        beginRequest(getString(R.string.loading), 45000);
        ApiUtil.getUserInfo(new IGenericApiCallback<UserInfoBean>() {
            @Override
            public void onSuccess(UserInfoBean data) {

                if(isCurrentRequest(generation)) {
                    MainHandler.runOnUiThread(() -> {
                        if (!isCurrentRequest(generation)) return;
                        endRequest();
                        if(_loginType == Constants.ACTIVE_TYPE_LOGIN) {
                            EventBus.getDefault().post(new LoginEvent());
                        } else {
                            EventBus.getDefault().post(new DeviceEvent());
                            EventBus.getDefault().post(new WeewaActiveSuccessEvent());
                        }

                        //登录成功后断开蓝牙连接
                        AppTrace.d(TAG, "disconnect bluetooth");
                        if(BluetoothManager.getImpl() != null) {
                            BluetoothManager.getImpl().disconnectBle();
                        }

                        if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
                            MainActivity.start(MobileActiveLoginActivity.this);
                        }
                        doFinish(true);
                    });
                }
            }

            @Override
            public void onFail(String code, String message) {
                if (isCurrentRequest(generation)) showLoginFailure(message);
            }

            @Override
            public void onFinish() {
                // Completion also runs after failure; only onSuccess may leave this page.
            }
        });
    }

    @Override
    public void onActive(LoginResultBean loginResultBean) {
        MainHandler.runOnUiThread(() -> handleActive(loginResultBean));
    }

    private void handleActive(LoginResultBean loginResultBean) {
        if (!_waitingForBind || !_requestInFlight || BaseAppUtil.isDestroy(this)) return;
        _waitingForBind = false;
        if (loginResultBean == null || TextUtils.isEmpty(loginResultBean.getToken())) {
            showLoginFailure(getString(R.string.login_fail));
            return;
        }
        _handler.removeCallbacks(_timeoutRunnable);
        LoginManager.saveUserToken(loginResultBean.getToken());
        //前移激活成功通知
        EventBus.getDefault().post(new DeviceEvent());
        EventBus.getDefault().post(new WeewaActiveSuccessEvent());
        //激活完断开蓝牙连接
        AppTrace.d(TAG, "disconnect bluetooth");
        if(BluetoothManager.getImpl() != null) {
            BluetoothManager.getImpl().disconnectBle();
        }

        final int generation = _requestGeneration;
        beginRequest(getString(R.string.loading), 45000);
        ApiUtil.getUserInfo(new IGenericApiCallback<UserInfoBean>() {
            @Override
            public void onSuccess(UserInfoBean data) {
                MainHandler.runOnUiThread(() -> {
                    if (!isCurrentRequest(generation)) return;
                    endRequest();
                    ConnectActivity.start(MobileActiveLoginActivity.this, null, Constants.ACTIVE_STEP_BIND_SUCCESS);
                    doFinish(true);
                });
            }

            @Override
            public void onFail(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (isCurrentRequest(generation)) showLoginFailure(message);
                });
            }

            @Override
            public void onFinish() {

            }
        });
    }

    @Override
    public void onActiveFail(String message) {
        AppTrace.i(TAG, "onActiveFail fail: " + message);
        MainHandler.runOnUiThread(() -> {
            if (_waitingForBind && _requestInFlight) showLoginFailure(message);
        });
    }
}
