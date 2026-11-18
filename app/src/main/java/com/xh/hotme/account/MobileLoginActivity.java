package com.xh.hotme.account;


import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.databinding.ActivityMobileLoginBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.test.Test4GSpeedActivity;
import com.xh.hotme.test.TestActivity;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;


public class MobileLoginActivity extends BaseActivity<ActivityMobileLoginBinding> {
    private static final String TAG = MobileLoginActivity.class.getSimpleName();

    private int _requestCode = -1;
    private int _loginType = 0;

    public static void start(Context context) {
        start(context, Constants.LOGIN_TYPE_LOGIN);
    }

    public static void start(Context context, int loginType) {
        if(null != context) {
            Intent intent = new Intent(context, MobileLoginActivity.class);
            intent.putExtra(REQUEST_CODE, loginType);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }

    public static void startActivityByRequestCode(Activity context, int requestCode) {
        startActivityByRequestCode(context, requestCode, Constants.LOGIN_TYPE_LOGIN);
    }


    public static void startActivityByRequestCode(Activity context, int requestCode, int loginType) {
        if(null != context) {
            Intent intent = new Intent(context, MobileLoginActivity.class);
            intent.putExtra(REQUEST_CODE, requestCode);
            intent.putExtra(Constants.LOGIN_TYPE, loginType);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    protected void initView() {
        if(getIntent() != null) {
            _requestCode = getIntent().getIntExtra(REQUEST_CODE, -1);
            _loginType = getIntent().getIntExtra(Constants.LOGIN_TYPE, Constants.LOGIN_TYPE_LOGIN);
        }

        // find views
        TextView titleView = viewBinding.titleBar.tvTitle;
        if(_loginType == Constants.LOGIN_TYPE_CHANGE_MOBILE) {
            titleView.setText(getString(R.string.mobile_btn_update));
        } else {
            titleView.setText(getString(R.string.login_title));
        }

        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        // add mobile login view
        MobileLoginView loginView = new MobileLoginView(this, R.layout.login_layout_mobile_view);
        if(_loginType == Constants.LOGIN_TYPE_CHANGE_MOBILE) {
            loginView.setLoginType(Constants.TYPE_UPDATE_PHONE);
        } else {
            loginView.setLoginType(Constants.TYPE_LOGIN);
        }

        viewBinding.content.addView(loginView);
        loginView.setLogListener(new IGenericApiCallback<LoginResultBean>() {
            @Override
            public void onSuccess(LoginResultBean data) {

                getUserInfo();
            }

            @Override
            public void onFail(String code, String message) {
                if (code.equals("asx")) {
                    if (message.equals("register")) {
//                        startActivity(new Intent(MobileLoginActivity.this, RegisterActivity.class));
                        RegisterActivity.start(MobileLoginActivity.this,0);
                    }else if (message.equals("forget")) {
                        RegisterActivity.start(MobileLoginActivity.this,1);
                    }
                }else{
                    if(!BaseAppUtil.isDestroy(MobileLoginActivity.this)) {
                        runOnUiThread(() -> ToastUtil.s(message));
                    }
                }


            }

            @Override
            public void onFinish() {
//                dismissLoading();
            }
        });
    }

    @Override
    protected void initData() {

    }

    public void doFinish(boolean success) {
        if(_requestCode > -1) {
            Intent i = new Intent();
            i.putExtra("result", 0);
            setResult(success ? 1 : 0, i);

        } else {
            if(success) {
                ToastUtil.s(getString(R.string.login_success));
            }

        }
        if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
            MainActivity.start(MobileLoginActivity.this);
        }
        finish();
    }

    private void getUserInfo() {
        ApiUtil.getUserInfo(new IGenericApiCallback<UserInfoBean>() {
            @Override
            public void onSuccess(UserInfoBean data) {
                doFinish(true);
                // Prepare offline recording permissions without blocking normal App access.
                DailyAccessManager.synchronize(true, new IGenericApiCallback<DailyAccessManager.Profile>() {
                    public void onSuccess(DailyAccessManager.Profile profile) { }
                    public void onFail(String code, String message) { }
                    public void onFinish() { }
                });
            }

            @Override
            public void onFail(String code, String message) {
                ToastUtil.s(DailyAccessManager.NETWORK_MESSAGE);
            }

            @Override
            public void onFinish() {
            }
        });
    }
}
