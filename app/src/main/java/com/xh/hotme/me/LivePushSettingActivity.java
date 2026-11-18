package com.xh.hotme.me;

import android.content.Intent;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.CameraListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityLivePushSettingBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;

public class LivePushSettingActivity extends BaseActivity<ActivityLivePushSettingBinding> {
    CameraInfo _cameraInfo;

    @Override
    protected void initView() {


        int type = getIntent().getIntExtra("type", 0);

        if(type == 1) {
            viewBinding.photoSelect.setVisibility(View.VISIBLE);
            getCurrentCamera();
        }
        viewBinding.ivBack.setOnClickListener(view -> finish());

        viewBinding.tvOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                savePushSetting();
                ToastUtil.s(getString(R.string.info_save_to_local_success));
                finish();
                return true;
            }
        });
    }

    private void savePushSetting() {
        if(_cameraInfo != null && _cameraInfo.deviceInfo != null) {
            boolean isOtherSelected = viewBinding.cbOther.isChecked();
            boolean isRewooSelected = viewBinding.cbHotmeMiniprogram.isChecked();
            boolean isMgcSelected = viewBinding.cbMgc.isChecked();
            SharedPrefs.getImpl().saveBoolean("push_rewoo_" + _cameraInfo.deviceInfo.device_id, isRewooSelected);
            SharedPrefs.getImpl().saveBoolean("push_other_" + _cameraInfo.deviceInfo.device_id, isOtherSelected);
            SharedPrefs.getImpl().saveBoolean("push_mgc_" + _cameraInfo.deviceInfo.device_id, isMgcSelected);
        }
    }

    private void initPushSetting() {
        if(_cameraInfo != null && _cameraInfo.deviceInfo != null) {
            boolean isRewooSelected = SharedPrefs.getImpl().getBoolean("push_rewoo_" + _cameraInfo.deviceInfo.device_id, true);
            boolean isOtherSelected = SharedPrefs.getImpl().getBoolean("push_other_" + _cameraInfo.deviceInfo.device_id, false);
            boolean isMgcSelected = SharedPrefs.getImpl().getBoolean("push_mgc_" + _cameraInfo.deviceInfo.device_id, false);
            viewBinding.cbOther.setChecked(isOtherSelected);
            viewBinding.cbHotmeMiniprogram.setChecked(isRewooSelected);
            viewBinding.cbMgc.setChecked(isMgcSelected);
        }
    }

    private void getCurrentCamera() {
        _cameraInfo = CameraManager.getCameraInfo("");
        if(_cameraInfo != null) {
            String deviceName = BluetoothManager.getDisplayDeviceName(_cameraInfo.deviceInfo.device_name);
            viewBinding.photoName.setText(deviceName);
            initPushSetting();
        } else {
            if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
                viewBinding.photoName.setText(getString(R.string.info_please_bind_device));
                viewBinding.photoSelect.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        Intent intent = new Intent(LivePushSettingActivity.this, ActiveListActivity.class);
                        startActivityForResult(intent, 0xFE);
                        return true;
                    }
                });
            } else {
                viewBinding.photoName.setText(getString(R.string.info_please_connect_device));
                viewBinding.photoSelect.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        Intent intent = new Intent(LivePushSettingActivity.this, CameraListActivity.class);
                        startActivityForResult(intent, 0xFF);
                        return true;
                    }
                });
            }


        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        getCurrentCamera();
    }

    @Override
    protected void initData() {

    }
}
