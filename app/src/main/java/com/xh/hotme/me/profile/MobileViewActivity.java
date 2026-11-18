package com.xh.hotme.me.profile;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.xh.hotme.R;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityMobileViewBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;


public class MobileViewActivity extends BaseActivity<ActivityMobileViewBinding> {
    private static final String TAG = MobileViewActivity.class.getSimpleName();

    private final int reqestCode = -1;

    String _mobile;

    public static void start(Context context, String mobile) {
        if(null != context) {
            Intent intent = new Intent(context, MobileViewActivity.class);
            intent.putExtra(Constants.REQUEST_MOBILE, mobile);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        if(getIntent() != null) {
            _mobile = getIntent().getStringExtra(Constants.REQUEST_MOBILE);
        }

        viewBinding.titleBar.tvTitle.setText(R.string.mobile);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        if(!TextUtils.isEmpty(_mobile)) {
            if(_mobile.length() == 11) {
                _mobile = _mobile.substring(0, 3) + "****" + _mobile.substring(7);
            }
            viewBinding.tvMobile.setText(_mobile);
        }
        viewBinding.btnLoginSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                MobileLoginActivity.start(MobileViewActivity.this, Constants.LOGIN_TYPE_CHANGE_MOBILE);
                finish();
                return true;
            }
        });
    }

    @Override
    protected void initData() {

    }
}
