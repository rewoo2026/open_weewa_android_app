package com.xh.hotme.record.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.databinding.ActivityCameraRunningInfoBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.widget.RoundProgressBar;


public class CameraRunningInfoActivity extends BaseActivity<ActivityCameraRunningInfoBinding> implements IBleDeviceUsageInfoNotifyListener {

    private final static String TAG = CameraRunningInfoActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    // views
    private TextView _cameraTitleTv, _tempInfoTv, _energyInfoTv, _storageInfoTv, _storageTotalInfoTv;
    ProgressBar _storagePb;
    RoundProgressBar _temperaturePb, _energyPb;


    DeviceUsageInfo _usageInfo;

    Device _device;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, CameraRunningInfoActivity.class);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, DeviceUsageInfo usageInfo, DeviceInfo deviceInfo, Device device) {
        Intent intent = new Intent(ctx, CameraRunningInfoActivity.class);
        intent.putExtra(Constants.REQUEST_INTENT_USAGE_INFO, usageInfo);
        intent.putExtra(Constants.REQUEST_INTENT_DEVICE_INFO, deviceInfo);
        intent.putExtra(Constants.REQUEST_DEVICE, device);
        ctx.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addDeviceUsageInfoNotifyListener(this);
    }

    @Override
    protected void initView() {
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_running_info_title));
        _energyPb = viewBinding.energyProgress;
        _storagePb = viewBinding.storageProgress;
        _temperaturePb = viewBinding.temperatureProgress;

        _storageInfoTv = viewBinding.tvStorageLeft;
        _storageTotalInfoTv = viewBinding.tvStorageTotal;
    }

    @Override
    public void initData() {
        if(getIntent() != null) {
            _usageInfo = (DeviceUsageInfo) getIntent().getSerializableExtra(Constants.REQUEST_INTENT_USAGE_INFO);
        }

        if(_usageInfo != null) {
            updateUsageInfo();
        } else {
            DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        CameraMonitor.getImpl().resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppTrace.d(TAG, "onPause");
        CameraMonitor.getImpl().pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeDeviceUsageInfoNotifyListener(this);
    }

    @Override
    public void onStorageInfo(StorageBean storageBean) {

    }

    @Override
    public void onEnergy(int energy) {

    }


    private void updateUsageInfo() {
        if(_usageInfo != null) {
            _energyPb.setMax(100);
            _energyPb.setAnimProgress(_usageInfo.energy);
            _energyPb.setText(String.format("%d%%", _usageInfo.energy));

            _storagePb.setMax(100);
            long storageP = 100 * _usageInfo.storage.used / _usageInfo.storage.total;
            int storageProcess = (int) (storageP);
            _storagePb.setProgress(storageProcess);

            String totalStr = String.format("%dG", (int) (_usageInfo.storage.total / 1024 / 1024 / 1024));
            String usedStr = "";
            String usedCell = "M";
            if(_usageInfo.storage.used / 1024 / 1024 > 1024) {
                if(_usageInfo.storage.used * 1.0 / 1024 / 1024 / 1024 > 10) {
                    usedStr = String.format("%d", (int) (_usageInfo.storage.used * 1.0 / 1024 / 1024 / 1024));
                } else {
                    usedStr = String.format("%.1f", _usageInfo.storage.used * 1.0 / 1024 / 1024 / 1024);
                }
                usedCell = "G";
            } else {
                usedCell = "M";
                usedStr = String.format("%.2f", _usageInfo.storage.used * 1.0 / 1024 / 1024);
            }
            _storageInfoTv.setText(usedStr);
            _storageTotalInfoTv.setText(String.format("%s/%s", usedCell, totalStr));

            _temperaturePb.setMax(100);

            int temperature = _usageInfo.temperature;
            _temperaturePb.setText(String.format("%d°C", temperature));
            _temperaturePb.setAnimProgress(temperature, false);
        }
    }

    @Override
    public void onDeviceInfo(DeviceUsageInfo data) {
        _handler.post(() -> {
            if(data != null) {
                _usageInfo = data;
                updateUsageInfo();
            }
        });
    }
}
