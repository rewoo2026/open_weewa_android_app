package com.xh.hotme.live;

import android.content.Intent;
import android.os.Handler;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityConnectWifiOkBinding;

public class ConnectWifiOkActivity extends BaseActivity<ActivityConnectWifiOkBinding> {
    @Override
    protected void initView() {
        viewBinding.titlebar.tvTitle.setText(getString(R.string.player_title));
        findViewById(R.id.iv_back).setVisibility(View.INVISIBLE);
        new Handler().postDelayed(() -> {
            startActivity(new Intent(ConnectWifiOkActivity.this, LiveActivity.class));
            finish();
        }, 3000);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void initData() {

    }
}
