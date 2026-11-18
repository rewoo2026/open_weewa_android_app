package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityTestAudioNoiseSettingBinding;
import com.xh.hotme.databinding.ActivityTestLogSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class TestLogSettingActivity extends BaseActivity<ActivityTestLogSettingBinding> {

    private final static String TAG = TestLogSettingActivity.class.getSimpleName();

    private UpdateSettingsExRequestBean _lastUpdateBean;

    private final Runnable _timeOutRunnable = () -> {
    };


    public static void start(Context context) {
        Intent intent = new Intent(context, TestLogSettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {

        TitleLayoutHelper.initTitle(viewBinding.titleBar, "Log设置");

        viewBinding.btnUpdateKeepDays.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    int keepDays = (int) Float.parseFloat(viewBinding.etKeepDays.getText().toString());
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("log:keep_log_days", keepDays);
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog("正在设置日志天数");
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                    ToastUtil.s(getString(R.string.info_value_verify_illegal));
                }
                return true;
            }
        });

        viewBinding.btnLogToFile.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    boolean to_off = viewBinding.btnLogToFile.getText().toString().startsWith("关闭");
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("log:log_to_file", to_off ? "0" : "1");
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog(String.format("正在%s日志输出到文件", to_off ? "关闭" : "打开"));
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                    ToastUtil.s(getString(R.string.info_value_verify_illegal));
                }
                return true;
            }
        });

        viewBinding.btnSyncToRecordDir.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    boolean to_off = viewBinding.btnSyncToRecordDir.getText().toString().startsWith("关闭");
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("log:sync_to_rec_dir", to_off ? "0" : "1");
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog(String.format("正在%s日志同步到视频目录", to_off ? "关闭" : "打开"));
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                    ToastUtil.s(getString(R.string.info_value_verify_illegal));
                }
                return true;
            }
        });

    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("log:log_to_file");
        bean.addKey("log:keep_log_days");
        bean.addKey("log:sync_to_rec_dir");
        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingsExUpdateEvent(WeewaSettingsExUpdateEvent event) {
        AppTrace.d(TAG, "onWeewaSettingsExUpdateEvent");
        DialogUtil.dismissDialog();
        if(event != null && event._status == 1) {
            ToastUtil.s(getString(R.string.info_update_success));

            // refresh settings
            if(_lastUpdateBean != null && _lastUpdateBean.has("log:log_to_file") ||
                    _lastUpdateBean.has("log:keep_log_days") ||
                    _lastUpdateBean.has("log:sync_to_rec_dir")
            ) {
                GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
                bean.addKey("log:log_to_file");
                bean.addKey("log:keep_log_days");
                bean.addKey("log:sync_to_rec_dir");
                DeviceApiManager.sendCmdSimpleObj(bean);
            }
        } else {
            ToastUtil.s(getString(R.string.info_update_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingExEvent(WeewaSettingsExEvent event) {
        AppTrace.d(TAG, "onWeewaSettingExEvent");
        if(event == null || event.settings == null) {
            return;
        }
        if(event.has("log:log_to_file")) {
            boolean on = "1".equals(event.get("log:log_to_file"));
            viewBinding.btnLogToFile.setText(on ? "关闭输出rkipc日志到文件" : "打开输出rkipc日志到文件");
        }

        if(event.has("log:sync_to_rec_dir")) {
            boolean on = "1".equals(event.get("log:sync_to_rec_dir"));
            viewBinding.btnSyncToRecordDir.setText(on ? "关闭rkipc日志到视频目录" : "同步rkipc日志到视频目录");
        }
        if(event.has("log:keep_log_days")) {
            String days = event.get("log:keep_log_days");
            viewBinding.etKeepDays.setText(days);
        }



    }

}
