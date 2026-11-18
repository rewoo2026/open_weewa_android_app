package com.xh.hotme.record.setting;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.event.RenameEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.databinding.ActivityCameraInfoBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IUpdateDevNameListener;
import com.xh.hotme.me.bean.SettingBean;
import com.xh.hotme.me.holder.SettingAdapter;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.NetUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;


public class CameraInfoActivity extends BaseActivity<ActivityCameraInfoBinding> implements IBleDeviceUsageInfoNotifyListener {

    private final static String TAG = CameraInfoActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    DeviceUsageInfo _usageInfo;

    DeviceInfo _deviceInfo;
    Device _device;

    SetDeviceNameDialog _deviceNameDialog;

    String _deviceName;

    SettingAdapter _taskAdapter;
    final List<SettingBean> _taskList = new ArrayList<>();

    RecyclerView _recyclerView;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, CameraInfoActivity.class);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, DeviceUsageInfo usageInfo, DeviceInfo deviceInfo, Device device) {
        Intent intent = new Intent(ctx, CameraInfoActivity.class);
        intent.putExtra(Constants.REQUEST_INTENT_USAGE_INFO, GsonUtils.GsonString(usageInfo));
        intent.putExtra(Constants.REQUEST_INTENT_DEVICE_INFO, GsonUtils.GsonString(deviceInfo));
        intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
        ctx.startActivity(intent);
    }

    @Override
    protected void initView() {
        // set content view
        viewBinding.titlebar.tvTitle.setText(getString(R.string.active_camera_info_title));

        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.titleModify.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(_deviceNameDialog != null && _deviceNameDialog.isShowing()) {
                    _deviceNameDialog.dismiss();
                }
                _deviceNameDialog = null;

                _deviceNameDialog = new SetDeviceNameDialog(CameraInfoActivity.this, _deviceName);

                _deviceNameDialog.setOnClickListener(new IUpdateDevNameListener() {
                    @Override
                    public void onUpdate(String name) {
                        _deviceName = name;
                        viewBinding.cameraTitle.setText(name);

                        if(_deviceInfo != null) {
                            _deviceInfo.device_name = name;
                        }

                        DeviceManager.updateDeviceName(_device.address, name);

                        EventBus.getDefault().post(new RenameEvent(_device.address, name));

                        _deviceNameDialog.dismiss();

                        try {
                            BindDeviceBean bindDevice = DeviceManager.getImpl().getConnectedBindDevice();
                            if(bindDevice != null) {
                                if(NetUtil.isConnectedAvailableNetwork()) {
                                    ApiUtil.updateDeviceName(bindDevice.getDeviceId(), name, null);
                                }
                            }
                        } catch(Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancel() {

                        _deviceNameDialog.dismiss();
                    }
                });

                _deviceNameDialog.show();

                return true;
            }
        });

        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_CAMERA_RUNNING_INFO, 0, this.getString(R.string.camera_running_info_title)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_CAMERA_SOFT_AP_SETTING, 0, this.getString(R.string.camera_soft_ap_setting)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_CAMERA_ABOUT, 0, this.getString(R.string.camera_about)));
//        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_CAMERA_FAN, 0, this.getString(R.string.camera_camera_fan)));

        this._recyclerView = findViewById(R.id.recyclerView);
        _taskAdapter = new SettingAdapter(CameraInfoActivity.this, _taskList);
        // setup views
        _recyclerView.setLayoutManager(new LinearLayoutManager(CameraInfoActivity.this));
        _recyclerView.setAdapter(_taskAdapter);

        _recyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    protected void initData() {
        if(getIntent() != null) {
            String usageInfo = (String) getIntent().getSerializableExtra(Constants.REQUEST_INTENT_USAGE_INFO);
            _usageInfo = GsonUtils.GsonToBean(usageInfo, DeviceUsageInfo.class);

            String deviceInfo = getIntent().getStringExtra(Constants.REQUEST_INTENT_DEVICE_INFO);
            if(!TextUtils.isEmpty(deviceInfo)) {
                _deviceInfo = GsonUtils.GsonToBean(deviceInfo, DeviceInfo.class);
            }
            String deviceStr = getIntent().getStringExtra(Constants.REQUEST_DEVICE);
            if(!TextUtils.isEmpty(deviceStr)) {
                _device = GsonUtils.GsonToBean(deviceStr, Device.class);
            }
        }

        float energy = 5;
        if(_usageInfo != null) {
            viewBinding.usageProgress.setMax(100);
            if(_usageInfo.energy >= 0) {
                viewBinding.usageProgress.setProgress(_usageInfo.energy);
                energy = 5 * _usageInfo.energy / 100;
            } else {
                viewBinding.usageProgress.setProgress(0);
                energy = 5F;
            }
        } else {
            DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
        }

        _deviceName = getString(R.string.weewa_camera);
        if(_device != null) {
            _deviceName = BluetoothManager.getDisplayDeviceName(_device);
        } else if(_deviceInfo != null && !TextUtils.isEmpty(_deviceInfo.device_name)) {
            _deviceName = BluetoothManager.getDisplayDeviceName(_deviceInfo.device_name);
        }

        viewBinding.cameraTitle.setText(_deviceName);

        viewBinding.tvUsageLeft.setText(String.format(" %.1f h", energy));

        BluetoothHandle.addDeviceUsageInfoNotifyListener(this);
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

    @Override
    public void onDeviceInfo(DeviceUsageInfo data) {
        _usageInfo = data;
    }
}
