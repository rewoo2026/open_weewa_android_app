package com.xh.hotme.me.setting;

import android.content.Context;
import android.content.Intent;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityAboutMeBinding;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;


public class AboutMeActivity extends BaseActivity<ActivityAboutMeBinding> {
    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, AboutMeActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        String appVersion = BaseAppUtil.getAppVersionName();

        viewBinding.tvAppVersion.setText(String.format("V%s", appVersion));

        // back click
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        // title
        viewBinding.titlebar.tvTitle.setText(getString(R.string.about_me));

        viewBinding.userAgreement.setOnClickListener(v -> SimpleWebActivity.start(AboutMeActivity.this, getString(R.string.user_proxy), SdkApi.user_agreement));

        viewBinding.privacy.setOnClickListener(v -> SimpleWebActivity.start(AboutMeActivity.this, getString(R.string.user_agreement), SdkApi.user_privacy));

        String appName = String.valueOf(getResources().getText(R.string.app_name));
        viewBinding.appName.setText(appName);
    }

    @Override
    protected void initData() {

    }
}
