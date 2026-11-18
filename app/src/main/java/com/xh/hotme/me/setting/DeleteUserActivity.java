package com.xh.hotme.me.setting;


import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityDeleteUserBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.ToastUtil;


public class DeleteUserActivity extends BaseActivity<ActivityDeleteUserBinding> {
    private static final String TAG = DeleteUserActivity.class.getSimpleName();

    private final int reqestCode = -1;
    private final int _loginType = 0;

    public static void start(Context context) {
        start(context, Constants.LOGIN_TYPE_LOGIN);
    }

    public static void start(Context context, int loginType) {
        if(null != context) {
            Intent intent = new Intent(context, DeleteUserActivity.class);
            intent.putExtra(REQUEST_CODE, loginType);
            context.startActivity(intent);
        }
    }

    public static void startActivityByRequestCode(Activity context, int requestCode) {
        startActivityByRequestCode(context, requestCode, Constants.LOGIN_TYPE_LOGIN);
    }


    public static void startActivityByRequestCode(Activity context, int requestCode, int loginType) {
        if(null != context) {
            Intent intent = new Intent(context, DeleteUserActivity.class);
            intent.putExtra(REQUEST_CODE, requestCode);
            intent.putExtra(Constants.LOGIN_TYPE, loginType);
            context.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    protected void initView() {

        // find views
        viewBinding.titleBar.tvTitle.setText(getString(R.string.delete_account));

        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        String mobile = LoginManager.getMobile();
        if(mobile.length() == 11) {
            mobile = mobile.substring(0, 3) + "****" + mobile.substring(7);
        }

        // agreement label underline & click
        viewBinding.account.setText(String.format(getString(R.string.account_info), mobile));

        viewBinding.btnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ApiUtil.deleteUser(new ApiCallback() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.s(getString(R.string.delete_account_success));
                        finish();
                    }
                });

                return true;
            }
        });


    }

    @Override
    protected void initData() {

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
}
