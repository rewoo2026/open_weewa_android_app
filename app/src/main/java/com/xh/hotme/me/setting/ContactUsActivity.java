package com.xh.hotme.me.setting;


import android.content.Context;
import android.content.Intent;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.databinding.ActivityContactUsBinding;
import com.xh.hotme.utils.ClickGuard;

public class ContactUsActivity extends BaseActivity<ActivityContactUsBinding> {


    public static void start(Context context) {
        Intent intent = new Intent(context, ContactUsActivity.class);
        context.startActivity(intent);

    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.title, getString(R.string.me_setting_contact), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

    }

    @Override
    protected void initData() {

    }
}