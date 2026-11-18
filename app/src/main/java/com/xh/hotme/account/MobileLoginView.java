package com.xh.hotme.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.me.setting.SimpleWebActivity;
import com.xh.hotme.test.Test4GSpeedActivity;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.RegExpUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.LoadingDialog;

/**
 * Created by Liuhongliangsdk on 2016/11/11.
 */
public class MobileLoginView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = MobileLoginView.class.getSimpleName();
    //登陆
    private Activity loginActivity;
    private EditText mgc_et_loginAccount;
    private EditText mgc_et_sms_code;
    private TextView mgc_btn_loginSubmit;
    private Button mgc_btn_send_sms_code;

    private TextView login_status;
    private ImageView clear_view;

    TextView tv_user_agreement;
    TextView tv_privacy_agreement;
    CheckBox cb_userProxy;

    Button btn_sms_login,btn_account_login,btn_register_account,btn_forgot_password;
    View underline_sms,underline_pwd;
    ImageView show_password;
    EditText etPassword;

    private LinearLayout layout_status;

    final Handler handler = new Handler();

    private IGenericApiCallback<LoginResultBean> onLoginListener;


    IApiCallback onSmsListener;

    private String _loginType = Constants.TYPE_LOGIN;
    private String _mac = "";
    private String _sn = "";
    private String _deviceModel = "";
    private String _bluetoothUuid = "";
    private String _deviceName = "";
    private String _deviceType = "";
    private String _currentLogin = "sms"; // sms 验证码 pwd 密码
    private boolean _smsRequestInFlight;

    PermitCallback _callback;

    private void startCodeTime(int time) {
        mgc_btn_send_sms_code.setTag(time);
        if(time <= 0) {
            mgc_btn_send_sms_code.setText(getContext().getString(R.string.login_btn_send_sms));
            mgc_btn_send_sms_code.setEnabled(true);
            mgc_btn_send_sms_code.setClickable(true);
            return;
        } else {
            mgc_btn_send_sms_code.setEnabled(false);
            mgc_btn_send_sms_code.setClickable(false);
            mgc_btn_send_sms_code.setText(time + getContext().getString(R.string.second));
        }
        handler.postDelayed(() -> {
            int delayTime = (int) mgc_btn_send_sms_code.getTag();
            startCodeTime(--delayTime);

        }, 1000);
    }

    public MobileLoginView(Context context) {
        super(context);
        setupUI(0);
    }

    public MobileLoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupUI(0);
    }

    public MobileLoginView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupUI(0);
    }

    public MobileLoginView(Context context, int layoutId) {
        super(context);
        setupUI(layoutId);
    }

    private void setupUI(int layoutId) {
        loginActivity = (Activity) getContext();
        if(layoutId == 0) {
            layoutId = R.layout.login_include_login_merge;
        }
        LayoutInflater.from(getContext()).inflate(layoutId, this);

        layout_status = findViewById(R.id.ll_status);
        login_status = findViewById(R.id.login_status);
        clear_view = findViewById(R.id.clear);

        mgc_et_loginAccount = findViewById(R.id.et_loginAccount);
        mgc_et_sms_code = findViewById(R.id.et_sms_code);
        mgc_btn_loginSubmit = findViewById(R.id.btn_loginSubmit);
        mgc_btn_send_sms_code = findViewById(R.id.btn_send_sms_code);
        mgc_btn_loginSubmit.setOnClickListener(this);
        mgc_btn_send_sms_code.setOnClickListener(this);

        // 默认选中第一个
        btn_sms_login = findViewById(R.id.btn_sms_login);
        btn_account_login = findViewById(R.id.btn_account_login);
        underline_sms = findViewById(R.id.underline_sms);
        underline_pwd = findViewById(R.id.underline_pwd);
        show_password = findViewById(R.id.iv_show_password);
        etPassword = findViewById(R.id.et_register_password);
        btn_register_account = findViewById(R.id.btn_register_account);
        btn_forgot_password = findViewById(R.id.btn_forgot_password);

        btn_sms_login.setTextColor(getResources().getColor(R.color.orange));
        underline_sms.setVisibility(View.VISIBLE);
        btn_account_login.setTextColor(getResources().getColor(R.color.black));
        underline_pwd.setVisibility(View.GONE);
        findViewById(R.id.pwd_view).setVisibility(View.GONE);
        mgc_et_loginAccount.setHint( getContext().getString(R.string.login_hint_input_phone));
        mgc_et_loginAccount.setInputType(InputType.TYPE_CLASS_PHONE);

        //
        btn_sms_login.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                btn_sms_login.setTextColor(getResources().getColor(R.color.orange));
                underline_sms.setVisibility(View.VISIBLE);
                btn_account_login.setTextColor(getResources().getColor(R.color.black));
                underline_pwd.setVisibility(View.GONE);

                findViewById(R.id.sms_view).setVisibility(View.VISIBLE);
                findViewById(R.id.pwd_view).setVisibility(View.GONE);
                mgc_et_loginAccount.setHint( getContext().getString(R.string.login_hint_input_phone));
                mgc_et_loginAccount.setInputType(InputType.TYPE_CLASS_PHONE);
                _currentLogin = "sms";
                return true;
            }
        });

        btn_account_login.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                btn_sms_login.setTextColor(getResources().getColor(R.color.black));
                underline_sms.setVisibility(View.GONE);
                btn_account_login.setTextColor(getResources().getColor(R.color.orange));
                underline_pwd.setVisibility(View.VISIBLE);

                findViewById(R.id.sms_view).setVisibility(View.GONE);
                findViewById(R.id.pwd_view).setVisibility(View.VISIBLE);

                mgc_et_loginAccount.setHint( getContext().getString(R.string.input_phone_or_email));
                mgc_et_loginAccount.setInputType(InputType.TYPE_CLASS_TEXT);
                _currentLogin = "pwd";
                return true;
            }
        });

        show_password.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                togglePasswordVisibility();
                return true;
            }
        });


        btn_register_account.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(onLoginListener != null) {
                    onLoginListener.onFail("asx", "register");
                 }
                return true;
            }
        });

        btn_forgot_password.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(onLoginListener != null) {
                    onLoginListener.onFail("asx", "forget");
                }
                return true;
            }
        });



        onLoginListener = new IGenericApiCallback<LoginResultBean>() {
            @Override
            public void onSuccess(LoginResultBean data) {
                if(TextUtils.isEmpty(mLoginMessage)) {
                    ToastUtil.s(getContext().getString(R.string.login_success));
                } else {
                    ToastUtil.s(mLoginMessage);
                }
                loginActivity.finish();
            }

            @Override
            public void onFail(String code, String message) {

                postLoginFail(message);
            }

            @Override
            public void onFinish() {
            }
        };

        clear_view.setOnClickListener(v -> mgc_et_loginAccount.setText(""));

        // 不是登录的时候隐藏一下
        if(_loginType.equalsIgnoreCase(Constants.TYPE_LOGIN)) {
            findViewById(R.id.login_tab_view).setVisibility(VISIBLE);
            findViewById(R.id.account_view).setVisibility(VISIBLE);
        }else{
            findViewById(R.id.login_tab_view).setVisibility(GONE);
            findViewById(R.id.account_view).setVisibility(GONE);
        }

        cb_userProxy =  findViewById(R.id.cb_agree);
        tv_user_agreement =  findViewById(R.id.user_agreement);
        tv_privacy_agreement =  findViewById(R.id.privacy_agreement);
        if (cb_userProxy != null) {
            tv_user_agreement.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            tv_user_agreement.getPaint().setAntiAlias(true);
            tv_privacy_agreement.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            tv_privacy_agreement.getPaint().setAntiAlias(true);
            tv_user_agreement.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    SimpleWebActivity.start(getContext(), getContext().getString(R.string.user_proxy), SdkApi.user_agreement);
                    return true;
                }
            });
            tv_privacy_agreement.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    SimpleWebActivity.start(getContext(), getContext().getString(R.string.user_agreement), SdkApi.user_privacy);
                    return true;
                }
            });
        }

    }

    private boolean isPasswordVisible = false;
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // 隐藏密码
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            show_password.setImageResource(R.mipmap.ic_eye_close);
        } else {
            // 显示密码
            etPassword.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            show_password.setImageResource(R.mipmap.ic_eye_open);
        }
        // 移动光标到文本末尾
        etPassword.setSelection(etPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //自动设置相应的布局尺寸

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mgc_btn_loginSubmit.getId()) {

            // 借用这个做跳转
//            if(onLoginListener != null) {
//                onLoginListener.onFail("asx", "register");
////                forget
////                register
//            }
//
//            if (true) {
//                return;
//            }

            if (cb_userProxy != null && !cb_userProxy.isChecked()) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                ToastUtil.s(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                return;
            } else if (_callback != null && !_callback.isPermitLogin()) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                ToastUtil.s(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                return;
            }

            layout_status.setVisibility(View.GONE);
            String mobile = mgc_et_loginAccount.getText().toString().trim();
            String code = mgc_et_sms_code.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();
            if(!RegExpUtil.isMobileNumber(mobile) && _currentLogin.equals("sms")) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.login_error_phone_format));
                return;
            }
            if(TextUtils.isEmpty(code) && _currentLogin.equals("sms")) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.login_error_verify_code_null));
                return;
            }
            if(TextUtils.isEmpty(pwd) && _currentLogin.equals("pwd")) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.login_input_password));
                return;
            }
            if(!NetUtil.isConnectedAvailableNetwork()) {
                ToastUtil.l(loginActivity.getString(R.string.error_network_error));
                return;
            }

            // 这里区分哪个登录 邮箱还是手机号 验证码还是密码


            if(_loginType.equalsIgnoreCase(Constants.TYPE_LOGIN)) {
                if (_currentLogin.equals("sms")) {
                    ApiUtil.submitLogin(mobile, code, onLoginListener);
                }else if (_currentLogin.equals("pwd")){
                    ApiUtil.submitLogin(mobile,pwd,"pwd",onLoginListener);
                }

            } else if(_loginType.equalsIgnoreCase(Constants.TYPE_UNBIND)) {
                ApiUtil.submitUnbind(mobile, code, _mac, onLoginListener);
            } else if(_loginType.equalsIgnoreCase(Constants.TYPE_UPDATE_PHONE)) {
                ApiUtil.submitUpdateMobile(mobile, code, new ApiCallback() {
                    @Override
                    public void onSuccess() {
                        if(onLoginListener != null) {
                            onLoginListener.onSuccess(null);
                        }
                    }

                    @Override
                    public void onFail(String code, String message) {
                        if(onLoginListener != null) {
                            onLoginListener.onFail(code, message);
                        }
                    }
                });
            }
        } else if(view.getId() == mgc_btn_send_sms_code.getId()) {
            if (_smsRequestInFlight || !mgc_btn_send_sms_code.isClickable()) {
                return;
            }
            layout_status.setVisibility(View.GONE);
            String mobile = mgc_et_loginAccount.getText().toString().trim();
            if(!RegExpUtil.isMobileNumber(mobile)) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.login_error_phone_format));
                return;
            }

            if (cb_userProxy != null && !cb_userProxy.isChecked()) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                ToastUtil.s(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                return;

            } else if (_callback != null && !_callback.isPermitLogin()) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                ToastUtil.s(getContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
                return;
            }

            if (!_loginType.equalsIgnoreCase(Constants.TYPE_LOGIN)
                    && !_loginType.equalsIgnoreCase(Constants.TYPE_UPDATE_PHONE)) {
                return;
            }
            if(!NetUtil.isConnectedAvailableNetwork()) {
                layout_status.setVisibility(VISIBLE);
                login_status.setText(loginActivity.getString(R.string.error_network_error));
                ToastUtil.l(loginActivity.getString(R.string.error_network_error));
                return;
            }

            _smsRequestInFlight = true;
            mgc_btn_send_sms_code.setEnabled(false);
            mgc_btn_send_sms_code.setClickable(false);
            ApiCallback callback = new ApiCallback() {
                private boolean completed;

                @Override
                public void onSuccess() {
                    if (completed) return;
                    completed = true;
                    postSendCodeSuc();
                }

                @Override
                public void onFail(String code, String message) {
                    if (completed) return;
                    completed = true;
                    postSendCodeFail(message);
                }

                @Override
                public void onFinish() {
                    if (!completed) {
                        onFail("-1", null);
                    }
                }
            };
            try {
                sendSmsRequest(mobile, callback);
            } catch (RuntimeException e) {
                callback.onFail("-1", null);
            }
        }
    }

    protected void sendSmsRequest(String mobile, IApiCallback callback) {
        if (_loginType.equalsIgnoreCase(Constants.TYPE_LOGIN)) {
            ApiUtil.sendLoginSMS(mobile, callback);
        } else {
            ApiUtil.sendUpdateMobileSMS(mobile, callback);
        }
    }

    public void postSendCodeSuc() {
        _smsRequestInFlight = false;
        layout_status.setVisibility(View.GONE);
        login_status.setText("");
        startCodeTime(Constants.sms_count_down);
        mgc_et_loginAccount.clearFocus();
        mgc_et_sms_code.requestFocus();
    }


    public void postSendCodeFail(String message) {
        _smsRequestInFlight = false;
        mgc_btn_send_sms_code.setEnabled(true);
        mgc_btn_send_sms_code.setClickable(true);
        layout_status.setVisibility(VISIBLE);
        login_status.setText(TextUtils.isEmpty(message)
                ? getContext().getString(R.string.info_send_sms_fail) : message);
    }

    public void postLoginSuccess() {
        if(TextUtils.isEmpty(mLoginMessage)) {
            ToastUtil.s(getContext().getString(R.string.login_success));
        } else {
            ToastUtil.s(mLoginMessage);
        }
    }

    public void postLoginFail(String message) {
        layout_status.setVisibility(VISIBLE);
        login_status.setText(message != null ? message : getContext().getString(R.string.login_fail));
    }


    private String mLoginMessage;

    public void setLoginMessage(String message) {
        this.mLoginMessage = message;
    }

    public void setLogListener(IGenericApiCallback<LoginResultBean> l) {
        onLoginListener = l;
    }


    public void setSmsListener(IApiCallback l) {
        onSmsListener = l;
    }

    public void setButtonText(String message) {

        mgc_btn_loginSubmit.setText(message);
    }

    public void setLoginType(String loginType) {
        _loginType = loginType;
    }

    public void setMac(String mac) {
        _mac = mac;
    }

    public void setSn(String sn) {
        _sn = sn;
    }

    public void setDeviceModel(String model) {
        _deviceModel = model;
    }

    public void setBluetoothUuid(String uuid) {
        _bluetoothUuid = uuid;
    }

    public void setDeviceName(String name) {
        _deviceName = name;
    }

    public void setDeviceType(String type) {
        _deviceType = type;
    }

    public void setCallback(PermitCallback callback) {
        this._callback = callback;
    }

    public interface PermitCallback {
        public boolean isPermitLogin();
    }
}
