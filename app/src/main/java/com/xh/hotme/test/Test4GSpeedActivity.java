package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.SpeedTestRequestBean;
import com.xh.hotme.databinding.ActivityEthernetSettingBinding;
import com.xh.hotme.databinding.ActivityTest4gSpeedBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.bluetooth.event.SpeedTestEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class Test4GSpeedActivity extends BaseActivity<ActivityTest4gSpeedBinding> {
    private static final String TAG = "Test4GSpeedActivity";

    public static void start(Context context) {
        Intent intent = new Intent(context, Test4GSpeedActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titlebar, "4G模块测速");
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.btnStartTest.setOnClickListener(v -> {
            startSpeedTest();
        });
    }

    @Override
    protected void initData() {


    }


    private void startSpeedTest() {
        viewBinding.progressBar.setVisibility(View.VISIBLE);
        viewBinding.tvResult.setText("测速中...");
//         发送测速命令到设备
        DeviceApiManager.sendCmdSimpleObj(new SpeedTestRequestBean());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSpeedTestEvent(SpeedTestEvent event) {
        viewBinding.progressBar.setVisibility(View.GONE);
        AppTrace.d(TAG, "onSpeedTestEvent: " + event.toString());
        if (event.status == 1) {
            double downloadMbps = event.downloadSpeed * 1.0 / (1024 * 1024);
            double uploadMbps = event.uploadSpeed * 1.0 / (1024 * 1024);
            viewBinding.tvResult.setText(String.format("下载速度: %.2f MB/s\n上传速度: %.2f MB/s",
            downloadMbps, uploadMbps));
        } else {
            viewBinding.tvResult.setText("测速失败, 请检查相机网络");
        }
    }
}