package com.xh.hotme.camera.softap;


import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApCloseRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApOpenRequestBean;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.CameraListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivitySoftApBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.me.camsetting.CameraSettingsActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;


public class CameraSoftApActivity extends BaseActivity<ActivitySoftApBinding> implements IBleSoftApNotifyListener {
    private static final String TAG = CameraSoftApActivity.class.getSimpleName();

    private final int reqestCode = -1;

    private String info_soft_ap_ssid_is_empty = "";
    private String info_soft_ap_password_is_empty = "";
    private String info_soft_ap_password_length_invalid = "";
    public final static int REQUEST_SOFT_AP_TRANSFER = 0;
    public final static int REQUEST_SOFT_AP_PREVIEW = 1;

    public final static int REQUEST_SOFT_AP_CAMERA_INFO = 2;

    public final static int REQUEST_SOFT_AP_FACTORY_TEST = 3;

    String _ssid;
    String _password;
    int _status;
    int _requestCode;

    ModalDialog _dialog;

    boolean _isOpenSoftAp = false;

    CameraInfo _cameraInfo;

    private final Runnable _timeOutRunnable = () -> {
        //
        DialogUtil.dismissDialog();
    };

    public static void start(Context context, String ssid, String password, int status, int requestCode) {
        if (null != context) {
            Intent intent = new Intent(context, CameraSoftApActivity.class);
            intent.putExtra(BleConstants.BLE_KEY_SOFTAP_SSID, ssid);
            intent.putExtra(BleConstants.BLE_KEY_SOFTAP_PASSWORD, password);
            intent.putExtra(BleConstants.BLE_KEY_ST, status);
            intent.putExtra(REQUEST_CODE, requestCode);
            context.startActivity(intent);
        }
    }

    public static void start(Activity context, int requestCode) {
        if (null != context) {
            Intent intent = new Intent(context, CameraSoftApActivity.class);
            intent.putExtra(REQUEST_CODE, requestCode);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityForResult(intent, requestCode);
        }
    }

    public static void startActivityForResult(Activity context, String ssid, String password, int status, int requestCode) {
        if (null != context) {
            Intent intent = new Intent(context, CameraSoftApActivity.class);
            intent.putExtra(BleConstants.BLE_KEY_SOFTAP_SSID, ssid);
            intent.putExtra(BleConstants.BLE_KEY_SOFTAP_PASSWORD, password);
            intent.putExtra(BleConstants.BLE_KEY_ST, status);
            intent.putExtra(REQUEST_CODE, requestCode);
            context.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addSoftApNotifyListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeSoftApNotifyListener(this);

        //如果没有打开热点退出，则断开热点
        if (_requestCode == CameraSoftApActivity.REQUEST_SOFT_AP_TRANSFER) {
            if (_status == 0) {
                DeviceApiManager.disConnect();
            }
        }
    }

    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_video_transfer));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                finish();
                return true;
            }
        });
        viewBinding.btnSetup.requestFocus();
        viewBinding.btnSetup.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (!_isOpenSoftAp) {

                    String ssid = viewBinding.etSsid.getText().toString();
                    String password = viewBinding.etPassword.getText().toString();
                    if (TextUtils.isEmpty(ssid)) {
                        ToastUtil.s(info_soft_ap_ssid_is_empty);
                        return true;
                    }
                    if (!TextUtils.isEmpty(password) && (password.length() < 8 || password.length() > 16)) {
                        ToastUtil.s(info_soft_ap_password_length_invalid);
                        return true;
                    }

                    _ssid = ssid;
                    _password = password;

                    doSoftApSetup();
                } else {
                    CameraSoftApInfoActivity.startActivity(CameraSoftApActivity.this, _ssid, _password);
                }

                return true;
            }
        });
        viewBinding.softApSwitch.setChecked(true);

        viewBinding.photoSelect.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startSelectCamera();
                return true;
            }
        });
    }

    @Override
    protected void initData() {

        if (getIntent() != null) {
            _ssid = getIntent().getStringExtra(BleConstants.BLE_KEY_SOFTAP_SSID);
            _password = getIntent().getStringExtra(BleConstants.BLE_KEY_SOFTAP_PASSWORD);
            _status = getIntent().getIntExtra(BleConstants.BLE_KEY_ST, 0);
            _requestCode = getIntent().getIntExtra(REQUEST_CODE, REQUEST_SOFT_AP_PREVIEW);
        }
        if (_requestCode == CameraSoftApActivity.REQUEST_SOFT_AP_TRANSFER) {
            viewBinding.photoSelect.setVisibility(View.VISIBLE);
        }

        if (_requestCode == REQUEST_SOFT_AP_TRANSFER) {
            viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_video_transfer));
            viewBinding.btnSetup.setVisibility(View.VISIBLE);
        } else {
            viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_wifi_soft_ap_setting));
        }

        viewBinding.softApSwitch.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (_isOpenSoftAp) {
                    showApCloseDialog();
                } else {
                    viewBinding.btnSetup.setVisibility(View.VISIBLE);
                }

                return true;
            }
        });

        info_soft_ap_ssid_is_empty = getString(R.string.info_soft_ap_ssid_is_empty);
        info_soft_ap_password_is_empty = getString(R.string.info_soft_ap_password_is_empty);
        info_soft_ap_password_length_invalid = getString(R.string.info_soft_ap_password_length_invalid);
    }

    private void updateUI() {
        NetworkBean networkBean = CameraManager.getNetworkInfo(null);
        if (networkBean != null && networkBean.getAp() != null) {
            _ssid = networkBean.getAp().ssid;
            _password = networkBean.getAp().password;
            _status = networkBean.getAp().st;
        }

        if (_status != 0) {
            viewBinding.softApSwitch.setChecked(true);
            _isOpenSoftAp = true;
            viewBinding.btnSetup.setVisibility(View.GONE);
            viewBinding.etSsid.setEnabled(false);
            viewBinding.etPassword.setEnabled(false);
        } else {
            _isOpenSoftAp = false;
            viewBinding.softApSwitch.setChecked(true);
            viewBinding.btnSetup.setVisibility(View.VISIBLE);
            viewBinding.etSsid.setEnabled(true);
            viewBinding.etPassword.setEnabled(true);
        }

        //如果相机还没设置热点信息，则使用相机名称作为热点名称
        if (!_isOpenSoftAp && !TextUtils.isEmpty(_ssid) && _ssid.equalsIgnoreCase(WifiManager.WEEWA_SOFTAP_NAME)) {
            CameraInfo cameraInfo = CameraManager.getCameraInfo("");

            if (cameraInfo != null && cameraInfo.deviceInfo != null && !TextUtils.isEmpty(cameraInfo.deviceInfo.device_name)) {
                String deviceName = BluetoothManager.getDisplayDeviceName(cameraInfo.deviceInfo.device_name);
                if (!TextUtils.isEmpty(deviceName)) {
                    _ssid = deviceName;
                    _password = WifiManager.WEEWA_SOFTAP_PWD;
                }
            }
        }

        viewBinding.etPassword.setText(_password);
        viewBinding.etSsid.setText(_ssid);

        if (_requestCode == REQUEST_SOFT_AP_TRANSFER) {
            if (_status == 1) {
                CameraSoftApInfoActivity.startActivity(CameraSoftApActivity.this, _ssid, _password);
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        getCurrentCamera();
    }

    private void startSelectCamera() {
        if (DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            Intent intent = new Intent(CameraSoftApActivity.this, ActiveListActivity.class);
            startActivityForResult(intent, 0xFE);
        } else {
            Intent intent = new Intent(CameraSoftApActivity.this, CameraListActivity.class);
            startActivityForResult(intent, 0xFF);
        }
    }

    private void getCurrentCamera() {
        _cameraInfo = CameraManager.getCameraInfo("");
        if (_cameraInfo != null && _cameraInfo.deviceInfo != null) {
            String deviceName = BluetoothManager.getDisplayDeviceName(_cameraInfo.deviceInfo.device_name);
            viewBinding.photoName.setText(deviceName);

            updateUI();
        } else {
            if (DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
                viewBinding.photoName.setText(getString(R.string.info_please_bind_device));
            } else {
                viewBinding.photoName.setText(getString(R.string.info_please_connect_device));
            }
        }
    }

    private void showApCloseDialog() {
        if (_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }

        _dialog = new ModalDialog(CameraSoftApActivity.this);
        _dialog.setMessage(getString(R.string.soft_ap_close));
        _dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.softApSwitch.setChecked(true);
                return true;
            }
        });
        _dialog.setPositiveButton(getString(R.string.close), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                doSoftApClose();
                return true;
            }
        });
        _dialog.show();
    }

    private void doSoftApSetup() {
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }
        boolean isOK = DeviceApiManager.sendCmdSimpleObj(new SoftApOpenRequestBean(_ssid, _password));
        if (isOK) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(R.string.info_soft_ap_opening));
        } else {
            showDisconnectToast();
        }
    }


    private void doSoftApClose() {
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SoftApCloseRequestBean());
        if (isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(R.string.info_soft_ap_closing));
        } else {
            showDisconnectToast();
        }
    }

    @Override
    public void onSoftApStart(String ssid, String password) {
        DialogUtil.dismissDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        _isOpenSoftAp = true;
        _status = 1;
        _ssid = ssid;
        _password = password;
        if (!BaseAppUtil.isDestroy(CameraSoftApActivity.this)) {

            runOnUiThread(() -> {
                viewBinding.btnSetup.setVisibility(View.VISIBLE);

                if (_requestCode == REQUEST_SOFT_AP_TRANSFER) {
                    CameraSoftApInfoActivity.startActivity(CameraSoftApActivity.this, _ssid, _password);
                }
                finish();
            });
        }
    }

    @Override
    public void onSoftApStartFail(String message) {
        DialogUtil.dismissDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        _isOpenSoftAp = false;
        if (!BaseAppUtil.isDestroy(CameraSoftApActivity.this)) {

            runOnUiThread(() -> {
                if (_dialog != null && _dialog.isShowing()) {
                    _dialog.dismiss();
                }

                _dialog = new ModalDialog(CameraSoftApActivity.this);
                _dialog.setMessage(getString(R.string.info_soft_ap_setup_fail));
                _dialog.setPositiveButton(getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        return true;
                    }
                });
                _dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        return true;
                    }
                });
                _dialog.show();
            });
        }
    }

    @Override
    public void onSoftApStop() {
        _status = 0;
        _isOpenSoftAp = false;
        _handler.removeCallbacks(_timeOutRunnable);

        NetworkBean networkBean = CameraManager.getNetworkInfo(null);
        if (networkBean.getAp() != null) {
            NetworkBean.ApInfo apInfo = networkBean.getAp();
            apInfo.st = 0;
            networkBean.setAp(apInfo);
        }
        if (!BaseAppUtil.isDestroy(CameraSoftApActivity.this)) {

            runOnUiThread(() -> {
                viewBinding.softApSwitch.setChecked(false);
                viewBinding.btnSetup.setVisibility(View.VISIBLE);
                viewBinding.etSsid.setEnabled(true);
                viewBinding.etPassword.setEnabled(true);
                DialogUtil.dismissDialog();
                ToastUtil.s(getString(R.string.info_soft_ap_close_success));
            });
        }
    }

    @Override
    public void onSoftApStopFail(String message) {
        _handler.removeCallbacks(_timeOutRunnable);
        if (!BaseAppUtil.isDestroy(CameraSoftApActivity.this)) {

            runOnUiThread(() -> {
                DialogUtil.dismissDialog();

                ToastUtil.s(getString(R.string.info_soft_ap_close_fail));
            });
        }
    }

    @Override
    public void onSoftApStatus(int status) {

    }

    @Override
    public void onSoftApConnectStart() {

    }

    @Override
    public void onSoftApConnectSuccess() {

    }

    @Override
    public void onSoftApConnectFail(String message) {

    }
}
