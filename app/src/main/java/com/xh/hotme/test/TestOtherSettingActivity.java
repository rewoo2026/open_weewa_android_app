package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UploadCalibRequestBean;
import com.xh.hotme.bluetooth.event.RebootEvent;
import com.xh.hotme.bluetooth.event.UploadCalibEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityTestOtherSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class TestOtherSettingActivity extends BaseActivity<ActivityTestOtherSettingBinding> {

    private final static String TAG = TestOtherSettingActivity.class.getSimpleName();

    private UpdateSettingsExRequestBean _lastUpdateBean;
    GetSettingsExRequestBean _customSearch;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
    };


    public static void start(Context context) {
        Intent intent = new Intent(context, TestOtherSettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {

        TitleLayoutHelper.initTitle(viewBinding.titleBar, "其它设置");
        if (!com.xh.hotme.utils.Constants.shouldShowCameraTest()) {
            viewBinding.btnMqSendFailRebootDevice.setVisibility(View.GONE);
            viewBinding.btnUploadCalib.setVisibility(View.GONE);
            viewBinding.querySettings.setVisibility(View.GONE);
        }

        viewBinding.btnMqSendFailRebootDevice.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    boolean to_off = viewBinding.btnMqSendFailRebootDevice.getText().toString().startsWith("关闭");
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("idle.remote:allow_reboot", to_off ? "0" : "1");
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog("正在设置重启功能");
                        _lastUpdateBean = bean;
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                    ToastUtil.s(getString(R.string.info_value_verify_illegal));
                }
                return true;
            }
        });


        boolean canEditEnvironment = com.xh.hotme.environment.RuntimeEnvironment.canEdit();
        viewBinding.btnDeviceServer.setVisibility(canEditEnvironment ? View.VISIBLE : View.GONE);
        if (canEditEnvironment) {
            viewBinding.btnDeviceServer.setOnClickListener(v ->
                    com.xh.hotme.environment.RuntimeEnvironmentActivity.start(this, true));
        }

        viewBinding.btnSearch.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String searchText = viewBinding.etSearch.getText().toString();
                if(TextUtils.isEmpty(searchText)){
                    ToastUtil.s("请输入查询参数");
                    return true;
                }
                GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
                bean.addKey(searchText);
                DeviceApiManager.sendCmdSimpleObj(bean);
                _customSearch = bean;
                return true;
            }
        });

        viewBinding.btnUploadCalib.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                DeviceApiManager.sendCmdSimpleObj(new UploadCalibRequestBean());
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("idle.remote:allow_reboot");

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
        _handler.removeCallbacks(_timeOutRunnable);
        if(event != null && event._status == 1) {
            ToastUtil.s(getString(R.string.info_update_success));

            // refresh settings
            if(_lastUpdateBean != null && _lastUpdateBean.has("idle.server:test_server")){
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
                if (isOk) {
                    DialogUtil.showDialog("正在重启生效...");
                }
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
        if(event.has("idle.remote:allow_reboot")) {
            boolean on = "1".equals(event.get("idle.remote:allow_reboot"));
            viewBinding.btnMqSendFailRebootDevice.setText(on ? "关闭mq发送失败重启idle" : "打开mq发送失败重启idle");
        }
        if(event.has("idle.server:test_server")) {
            boolean on = "1".equals(event.get("idle.server:test_server"));
            viewBinding.btnDeviceServer.setText("相机运行环境");
        }

        if(_customSearch != null) {
            String searchText = viewBinding.etSearch.getText().toString();
            if(!TextUtils.isEmpty(searchText)) {
                if(event.has(searchText)) {
                    String result = event.get(searchText);
                    viewBinding.searchResult.setVisibility(View.VISIBLE);
                    viewBinding.searchResult.setText(result);
                    _customSearch = null;
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRebootEvent(RebootEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadCalibEvent(UploadCalibEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        if(event!=null && event.status == 1){
            ToastUtil.s("上传成功");
            return;
        }
        ToastUtil.s("上传失败");
        return;
    }
}
