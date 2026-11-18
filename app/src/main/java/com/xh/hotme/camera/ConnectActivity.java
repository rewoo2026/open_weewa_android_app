package com.xh.hotme.camera;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.SoftApCloseRequestBean;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.bluetooth.event.RefreshBindDeviceEvent;
import com.xh.hotme.bluetooth.event.WeewaActiveSuccessEvent;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.camera.softap.CameraNetworkSettingActivity;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.camera.wifi.WifiListDialog;
import com.xh.hotme.camera.wifi.WifiPasswordDialog;
import com.xh.hotme.databinding.ActivityCameraConnectBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;


@IEventBus
public class ConnectActivity extends BaseActivity<ActivityCameraConnectBinding> implements IWifiNotifyListener {
    private final static String TAG = ConnectActivity.class.getSimpleName();
    public int _type = 0; //0：配网，1:激活， 2:
    public boolean _isLive = false;//进入直播前检查网络

    Device _device;

    String _mobile;

    String _networkInfo;

    NetworkBean _networkBean;

    final Runnable _timeOutRunnable = this::dismissLoadingDialog;

    final Runnable _jumpRunnable = () -> {
        if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
            MainActivity.start(ConnectActivity.this);
        } else {
            AppTrace.d(TAG, "MainActivity running");
        }

        finish();
    };


    MobileActiveSmsDialog _smsDialog;

    BluetoothLoadingDialog _bluetoothLoadingDialog;

    WifiListDialog _wifiListDialog;
    WifiPasswordDialog _setupDialog;


    public static void start(Context context, Device device, int step) {
        start(context, device, step, false);
    }

    public static void start(Context context, Device device, int step, boolean isLive) {
        if(null != context) {
            Intent intent = new Intent(context, ConnectActivity.class);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_ACTIVE_STEP, step);
            intent.putExtra(Constants.REQUEST_ACTIVE_LIVE, isLive);
            context.startActivity(intent);
        }
    }


    public static void start(Context context, Device device, String netConfig) {
        if(null != context) {
            Intent intent = new Intent(context, ConnectActivity.class);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_ACTIVE_STEP, Constants.ACTIVE_STEP_SETUP_WIFI);
            intent.putExtra(Constants.REQUEST_NETWORK_INFO, netConfig);
            context.startActivity(intent);
        }
    }

    public static void start(Context context, Device device, int step, String mobile) {
        if(null != context) {
            Intent intent = new Intent(context, ConnectActivity.class);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_ACTIVE_STEP, step);
            intent.putExtra(Constants.REQUEST_MOBILE, mobile);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        if(!TextUtils.isEmpty(_networkInfo)) {
            _networkBean = GsonUtils.GsonToBean(_networkInfo, NetworkBean.class);
        } else {
            _networkBean = CameraManager.getNetworkInfo("");
        }

        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.titleBar.tvTitle.setText(getString(R.string.player_title));

        viewBinding.tvSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_type == Constants.ACTIVE_STEP_SETUP_WIFI) {
                    setupWifi();
                } else if(_type == Constants.ACTIVE_STEP_ACTIVE) {
                    showActiveSmsDialog();
                } else if(_type == Constants.ACTIVE_STEP_BIND_FAIL) {
                    finish();
                } else if(_type == Constants.ACTIVE_STEP_ACTIVE_FAIL) {
                    finish();
                }
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        if(getIntent() != null) {
            String deviceInfo = getIntent().getStringExtra(Constants.REQUEST_DEVICE);
            if(!TextUtils.isEmpty(deviceInfo)) {
                _device = GsonUtils.GsonToBean(deviceInfo, Device.class);
            }
            _type = getIntent().getIntExtra(Constants.REQUEST_ACTIVE_STEP, Constants.ACTIVE_STEP_SETUP_WIFI);
            _mobile = getIntent().getStringExtra(Constants.REQUEST_MOBILE);
            _networkInfo = getIntent().getStringExtra(Constants.REQUEST_NETWORK_INFO);
            _isLive = getIntent().getBooleanExtra(Constants.REQUEST_ACTIVE_LIVE, false);
        }

        setup(_type);
    }

    private void dismissActiveSmsDialog() {
        if(_smsDialog != null && _smsDialog.isShowing()) {
            _smsDialog.dismiss();
        }
        _smsDialog = null;
    }

    private void showActiveSmsDialog() {
        dismissActiveSmsDialog();

        _smsDialog = new MobileActiveSmsDialog(ConnectActivity.this, _device);
        _smsDialog.setOnClickListener(new MobileActiveSmsDialog.IActiveSmsListener() {
            @Override
            public void onActiveSms(String mobile, String code) {
                _mobile = mobile;
                if(!TextUtils.isEmpty(code)) {
                    if(code.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_DEVICE_BOUND)) {
                        _type = Constants.ACTIVE_STEP_BOUND;
                        setup(_type);
                        _smsDialog.dismiss();
                    } else if(code.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_BIND)) {
                        _smsDialog.dismiss();
                        MobileActiveLoginActivity.start(ConnectActivity.this, mobile, Constants.ACTIVE_TYPE_ACTIVE);
//                        finish();
                    } else if(code.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_LOGIN)) {
                        _smsDialog.dismiss();
                        MobileActiveLoginActivity.start(ConnectActivity.this, mobile, Constants.ACTIVE_TYPE_LOGIN);
//                        finish();
                    }

                }
            }

            @Override
            public void onActiveSmsFail(String mobile, String message) {
                ToastUtil.s(message);
            }

            @Override
            public void onCancel() {
                _smsDialog.dismiss();
                finish();
            }

            @Override
            public void onLogin() {
                _smsDialog.dismiss();

                MobileLoginActivity.start(ConnectActivity.this);
                _smsDialog.dismiss();
                finish();
            }
        });

        _smsDialog.show();
    }


    public void setup(int status) {
        if(status == Constants.ACTIVE_STEP_SETUP_WIFI) {
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_connect_success));
            viewBinding.tvContent.setText(getString(R.string.active_device_setup_wifi));
            viewBinding.tvSetting.setText(getString(R.string.setup_wifi));
        } else if(status == Constants.ACTIVE_STEP_ACTIVE) {
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_network_setup_success));
            viewBinding.tvContent.setText(getString(R.string.device_active_nextstep));
            viewBinding.tvSetting.setText(getString(R.string.device_active));
        } else if(status == Constants.ACTIVE_STEP_BIND_SUCCESS) {
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_bind_success));
            viewBinding.tvContent.setText(getString(R.string.right_now_to_home));
            viewBinding.tvSetting.setText("");
            viewBinding.tvSetting.setVisibility(View.GONE);

            //获取绑定列表
            getDeviceList();

        } else if(status == Constants.ACTIVE_STEP_ACTIVE_SUCCESS) {
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_active_success));
            viewBinding.tvContent.setText(getString(R.string.right_now_to_home));
            viewBinding.tvSetting.setText("");
            viewBinding.tvSetting.setVisibility(View.GONE);

            //获取绑定列表
            getDeviceList();

        } else if(status == Constants.ACTIVE_STEP_BOUND) {
            viewBinding.ivStatus.setImageResource(R.mipmap.device_status);
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_bound));
            viewBinding.tvContent.setText(getString(R.string.right_now_to_login));
            viewBinding.tvSetting.setVisibility(View.GONE);

            _handler.postDelayed(() -> {
                MobileActiveLoginActivity.start(ConnectActivity.this, _mobile, Constants.ACTIVE_TYPE_LOGIN);
                finish();
            }, 2000);
        } else if(status == Constants.ACTIVE_STEP_BIND_FAIL) {
            viewBinding.ivStatus.setImageResource(R.mipmap.device_status_error);
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_bind_fail));
            viewBinding.tvContent.setText(getString(R.string.device_try_another_device));
            viewBinding.tvSetting.setText(getString(R.string.ok));
            viewBinding.tvSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    ModalDialog dialog = new ModalDialog(ConnectActivity.this);
                    dialog.setMessage(getString(R.string.login_another_account));
                    dialog.setNegativeButton(getString(R.string.exit), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            finish();
                            return true;
                        }
                    });
                    dialog.setPositiveButton(getString(R.string.go_to_login), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            MobileActiveLoginActivity.start(ConnectActivity.this, _mobile, Constants.ACTIVE_TYPE_LOGIN);
                            finish();
                            return true;
                        }
                    });

                    return true;
                }
            });

        } else if(status == Constants.ACTIVE_STEP_ACTIVE_FAIL) {
            viewBinding.ivStatus.setImageResource(R.mipmap.device_status_error);
            viewBinding.ivStatus.setVisibility(View.VISIBLE);
            viewBinding.tvStatus.setText(getString(R.string.device_bind_fail));
            viewBinding.tvContent.setText(getString(R.string.right_now_to_home));
            viewBinding.tvSetting.setText(getString(R.string.device_bind));

            _handler.postDelayed(() -> {
                if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
                    MainActivity.start(ConnectActivity.this);
                }
                finish();
            }, 2000);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy");

        dismissActiveSmsDialog();

        if(_setupDialog != null && _setupDialog.isShowing()) {
            _setupDialog.dismiss();
        }
        _setupDialog = null;
    }

    private void showLoadingDialog(String message) {
        if(_bluetoothLoadingDialog != null && _bluetoothLoadingDialog.isShowing()) {
            _bluetoothLoadingDialog.dismiss();
            _bluetoothLoadingDialog = null;
        }
        _bluetoothLoadingDialog = new BluetoothLoadingDialog(ConnectActivity.this);
        _bluetoothLoadingDialog.setMessage(message);
        _bluetoothLoadingDialog.show();
    }


    private void dismissLoadingDialog() {
        if(_bluetoothLoadingDialog != null && _bluetoothLoadingDialog.isShowing()) {
            _bluetoothLoadingDialog.dismiss();
        }
        _bluetoothLoadingDialog = null;
    }


    private void setupWifi() {
        CameraNetworkSettingActivity.start(ConnectActivity.this, _device, _isLive);
        finish();
    }

    @Override
    public void onWifiList(List<WifiInfo> device) {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            dismissLoadingDialog();
            if(_wifiListDialog != null && _wifiListDialog.isShowing()) {
                _wifiListDialog.dismiss();
            }
            _wifiListDialog = new WifiListDialog(ConnectActivity.this, "网络配置", device, new IWifiListAdapterCallback() {
                @Override
                public void onCancel() {

                }

                @Override
                public void onWifiClick(WifiInfo device1, int position) {
                    if(_setupDialog != null && _setupDialog.isShowing()) {
                        _setupDialog.dismiss();
                    }
                    _setupDialog = null;

                    _setupDialog = new WifiPasswordDialog(ConnectActivity.this, device1.getSsid());
                    _setupDialog.setOnClickListener((dialog, which) -> {
                        if(which == DialogInterface.BUTTON_POSITIVE) {
                            _type = Constants.ACTIVE_STEP_ACTIVE;
                            runOnUiThread(() -> {
                                setup(_type);
                                if(_wifiListDialog != null && _wifiListDialog.isShowing()) {
                                    _wifiListDialog.dismiss();
                                }

                                _setupDialog.dismiss();
                            });
                        }
                    });
                    _setupDialog.show();
                }
            });
            //关闭热点
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SoftApCloseRequestBean());
            _wifiListDialog.show();

        });
    }

    @Override
    public void onSetupStatus(int status, String message) {

    }

    @Override
    public void onTurnOff(int status, String message) {

    }

    private void getDeviceList() {
        UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
        if(userInfoBean == null) {
            AppTrace.e(TAG, "not login");
            return;
        }

        DeviceManager.getImpl().onRefreshList(false);

        _handler.postDelayed(_jumpRunnable, 5000);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActiveSuccess(WeewaActiveSuccessEvent event) {
        AppTrace.d(TAG, "onActiveSuccess, type = " + _type);

        dismissActiveSmsDialog();

        if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
            MainActivity.start(ConnectActivity.this);
        } else {
            AppTrace.d(TAG, "MainActivity running");
        }

        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshBindDevice(RefreshBindDeviceEvent event) {
        AppTrace.d(TAG, "onRefreshBindDevice, type = " + _type);
        try {
            _handler.removeCallbacks(_jumpRunnable);

            if(!ActivityStackManager.impl.hasActivity(MainActivity.class)) {
                MainActivity.start(ConnectActivity.this);
            } else {
                AppTrace.d(TAG, "MainActivity running");
            }
            finish();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginEvent(LoginEvent event) {
        AppTrace.i(TAG, "onLoginEvent, type = " + _type);
        try {
            finish();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }
}
