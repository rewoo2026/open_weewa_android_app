package com.xh.hotme.base;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xh.hotme.databinding.ActivityWebViewBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;


public class WebViewActivity extends BaseActivity<ActivityWebViewBinding> {

    private static final String TAG = WebViewActivity.class.getSimpleName();

    Dialog _loadingDialog;


    String _url;
    String _title;

    public static void start(Context context, String title, String url) {
        if(null != context) {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(Constants.TITLE_NAME, title);
            intent.putExtra(Constants.URL, url);

            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        Intent intent = getIntent();
        if(intent != null) {
            _title = intent.getStringExtra(Constants.TITLE_NAME);
            _url = intent.getStringExtra(Constants.URL);
        }

        webviewInit();

        // back click
        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(viewBinding.webview.canGoBack()) {
                    viewBinding.webview.goBack();
                } else {
                    finish();
                }
                return true;
            }
        });

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.refresh.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                viewBinding.webview.reload();

                return true;
            }
        });

        viewBinding.service.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

//                FollowUtil.startCommonQuestion(startCommonQuestionwWebViewActivity.this);

                return true;
            }
        });

        if(!TextUtils.isEmpty(_title) && _title.equalsIgnoreCase("常见问题")) {
            viewBinding.service.setVisibility(View.GONE);
        }


        // title
        if(!TextUtils.isEmpty(_title)) {
            viewBinding.tvTitle.setText(_title);
        }

        if(TextUtils.isEmpty(_url)) {
            finish();
            return;
        }

        viewBinding.webview.loadUrl(_url);
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(_loadingDialog != null && _loadingDialog.isShowing()) {
            _loadingDialog.dismiss();
        }
        _loadingDialog = null;
    }

    private void webviewInit() {
        viewBinding.webview.getSettings().setLoadsImagesAutomatically(true);
        viewBinding.webview.getSettings().setDefaultTextEncodingName("UTF-8");
        viewBinding.webview.getSettings().setAllowFileAccess(true);
        viewBinding.webview.getSettings().setAllowFileAccessFromFileURLs(true);
        viewBinding.webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
        viewBinding.webview.getSettings().setBuiltInZoomControls(true);
        viewBinding.webview.getSettings().setDisplayZoomControls(false);
        viewBinding.webview.getSettings().setSupportMultipleWindows(false);
//        viewBinding.webview.getSettings().setAppCacheEnabled(true);
        viewBinding.webview.getSettings().setDomStorageEnabled(true);
        viewBinding.webview.getSettings().setDatabaseEnabled(true);
        viewBinding.webview.getSettings().setJavaScriptEnabled(true);
        viewBinding.webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
//        viewBinding.webview.getSettings().setAppCacheMaxSize(Long.MAX_VALUE);
        viewBinding.webview.getSettings().setGeolocationEnabled(true);
        viewBinding.webview.getSettings().setUseWideViewPort(true);

        viewBinding.webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                AppTrace.e("Webview onPageStarted", "url=" + url);
                if(_loadingDialog != null && _loadingDialog.isShowing()) {
                    _loadingDialog.dismiss();
                    _loadingDialog = null;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                AppTrace.e(TAG, "url=" + url);
                return false;
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                super.onFormResubmission(view, dontResend, resend);
                resend.sendToTarget();
            }
        });
    }

}
