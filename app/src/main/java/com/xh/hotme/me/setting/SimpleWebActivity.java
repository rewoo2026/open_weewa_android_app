package com.xh.hotme.me.setting;

import android.content.Context;
import android.content.Intent;
import android.webkit.WebSettings;

import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.databinding.ActivitySimpleWebBinding;


public class SimpleWebActivity extends BaseActivity<ActivitySimpleWebBinding> {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_URL = "url";

    private static String _title = "";
    private static String _url = "";

    public static void start(Context context, String title, String url) {
        if(null != context) {
            Intent intent = new Intent(context, SimpleWebActivity.class);
            intent.putExtra(EXTRA_TITLE, title);
            intent.putExtra(EXTRA_URL, url);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {

        if(getIntent() != null) {
            _title = getIntent().getStringExtra(EXTRA_TITLE);
            _url = getIntent().getStringExtra(EXTRA_URL);
        }

        TitleLayoutHelper.initTitle(viewBinding.title, _title, v -> finish());


        WebSettings settings = viewBinding.web.getSettings();
        settings.setDefaultTextEncodingName("utf-8");

        viewBinding.web.loadUrl(_url);
    }

    @Override
    protected void initData() {

    }
}
