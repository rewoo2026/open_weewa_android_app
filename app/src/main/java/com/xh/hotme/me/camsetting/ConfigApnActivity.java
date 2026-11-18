package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.SetApnRequestBean;
import com.xh.hotme.bluetooth.event.SetApnEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityConfigApnBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class ConfigApnActivity extends BaseActivity<ActivityConfigApnBinding> {

    String imei = "";
    String apn = "";

    String str_request_timeout;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
        ToastUtil.s(str_request_timeout);
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        str_request_timeout = getString(R.string.request_timeout);

        viewBinding.titleBar.tvTitle.setText(R.string.apn_setting);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if (cameraInfo != null && cameraInfo.deviceNetworkInfo != null && cameraInfo.deviceNetworkInfo.getSim() != null) {
            imei = cameraInfo.deviceNetworkInfo.getSim().imei;
            apn = cameraInfo.deviceNetworkInfo.getSim().apn;
        }
        viewBinding.tvImeiNumber.setText(imei);
        viewBinding.etApnName.setText(apn);

        viewBinding.btnApnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String apnName = viewBinding.etApnName.getText().toString();
                if(TextUtils.isEmpty(apnName)){
                    ToastUtil.s(ConfigApnActivity.this.getString(R.string.apn_setting_info_apn_name_empty));
                    return true;
                }

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SetApnRequestBean(apnName.trim()));
                if(isOk){
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog("");
                }

                return true;
            }
        });

        viewBinding.tvImeiCopy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("label", imei));
                ToastUtil.s(getString(R.string.copied));
                return true;
            }
        });
    }

    @Override
    protected void initData() {

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(_handler!=null){
            _handler.removeCallbacksAndMessages(null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetApn(SetApnEvent event) {
        AppTrace.d(TAG, "onSetApnEvent");
        _handler.removeCallbacks(_timeOutRunnable);
        apn = viewBinding.etApnName.getText().toString();
        DialogUtil.dismissDialog();
        ToastUtil.s(getString(R.string.setup_successful));
    }
}
