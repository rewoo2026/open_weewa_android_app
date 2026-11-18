package com.xh.hotme.test;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeviceInfoRequestBean;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.EnergyInfoRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.PoweroffRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStopRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApCloseRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApOpenRequestBean;
import com.xh.hotme.bluetooth.bean.StorageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.WifiListRequestBean;
import com.xh.hotme.bluetooth.bean.WifiSetupRequestBean;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.camera.wifi.WifiListDialog;
import com.xh.hotme.camera.wifi.WifiPasswordDialog;
import com.xh.hotme.databinding.ActivityBlueTestBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IUpdateDevNameListener;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.record.setting.SetDeviceNameDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;


public class BlueTestActivity extends BaseActivity<ActivityBlueTestBinding> implements IWifiNotifyListener, IBleDeviceUsageInfoNotifyListener, IBleSoftApNotifyListener, IBleNetworkInfoNotifyListener, IRecordControlListener, IBleDeviceInfoNotifyListener {

    private final static String TAG = BlueTestActivity.class.getSimpleName();


    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    WifiListDialog _wifiListDialog;
    WifiPasswordDialog _setupDialog;
    SetDeviceNameDialog _deviceNameDialog;

    private WifiManager mWifiManager;

    DeviceInfo _deviceInfo;
    boolean getDeviceInfoAndPreview = false;

    final List<String> infoList = new ArrayList<>();

    RecordInfoBean _recordInfoBean;
    String _recordName;

    NetworkBean _networkInfo;
    boolean _deviceSoftApLaunched = false;

    String _softap_ssid = WifiManager.WEEWA_SOFTAP_NAME;
    String _softap_password = WifiManager.WEEWA_SOFTAP_PWD;
    int _softap_status;

    ModalDialog errorDialog;

    WifiManager.SearchWifiListener _searchWifiListener;

    WifiManager.WifiConnectListener _connectWifiListener;

    boolean wifiConnected = false;

    private final Runnable _timeOutRunnable = () -> {
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, BlueTestActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        initListener();

        viewBinding.apiType.setChecked(true);
        DeviceApiManager.setApiType(true);
        viewBinding.interfaceType.setText("蓝牙");
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.apiType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppTrace.d(TAG, "set api type: " + (isChecked ? "蓝牙" : "http"));
            if(isChecked) {
                viewBinding.interfaceType.setText("蓝牙");
                DeviceApiManager.setApiType(true);
            } else {
                DeviceApiManager.setApiType(false);
                viewBinding.interfaceType.setText("http");
                _networkInfo = CameraManager.getNetworkInfo(null);
                if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
                    _softap_ssid = _networkInfo.getAp().ssid;
                    _softap_password = _networkInfo.getAp().password;
                    _softap_status = _networkInfo.getAp().st;
                    _deviceSoftApLaunched = _softap_status == 1;
                }
                if(!_deviceSoftApLaunched) {
//                      connectSoftApAction = 1;
                    boolean ok = DeviceApiManager.sendCmdSimpleObj(new SoftApOpenRequestBean("", ""));
                    if(ok) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
//                                _requestSoftAp = true;
//                                showConnectingDialog("正在开启热点");
                    } else {
                        showDisconnectToast();
                    }
                } else {
                    if(!isConnectedSoftAp()) {
                        searchWifi();
                    } else {
                        wifiConnected = true;
                        printInfo("wifi已连接");
                    }
                }
            }
        });

        viewBinding.btnPoweron.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                boolean isOk = DeviceApiManager.sendCmdPowerOn();
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnPoweroff.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PoweroffRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnRestart.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnSoftapStart.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SoftApOpenRequestBean(WifiManager.WEEWA_SOFTAP_NAME, ""));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnSoftapStop.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SoftApCloseRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnWifilist.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiListRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnWifiSetup.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String ssid = viewBinding.etSsid.getText().toString();
                String password = viewBinding.etPassword.getText().toString();
                if(TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
                    ToastUtil.s(getString(R.string.info_wifi_ssid_or_password_is_null));
                    return true;
                }

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiSetupRequestBean(ssid, password));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });


        viewBinding.btnEnergeInfo.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj( new EnergyInfoRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnStorageInfo.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new StorageInfoRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnStartRkipc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.btnStopRkipc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStopRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        findViewById(R.id.btn_rkipc_status).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        findViewById(R.id.btn_usage_info).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnNetstatus.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnDeviceInfo.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getDeviceInfoAndPreview = false;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceInfoRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        findViewById(R.id.btn_update_name).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_deviceNameDialog != null && _deviceNameDialog.isShowing()) {
                    _deviceNameDialog.dismiss();
                }
                _deviceNameDialog = null;
                _deviceNameDialog = new SetDeviceNameDialog(BlueTestActivity.this, "weewa_demo");
                _deviceNameDialog.setOnClickListener(new IUpdateDevNameListener() {
                    @Override
                    public void onUpdate(String name) {
                        _handler.post(() -> viewBinding.tvStatus.setText("修改设备名称：" + new Gson().toJson(name)));
                    }

                    @Override
                    public void onCancel() {
                        _handler.post(() -> viewBinding.tvStatus.setText("取消修改设备名称"));
                    }
                });
                _deviceNameDialog.show();
                return false;
            }
        });

        findViewById(R.id.btn_login).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                MobileLoginActivity.start(BlueTestActivity.this);

                return false;
            }
        });
        findViewById(R.id.btn_logout).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ApiUtil.logout(new ApiCallback() {
                    @Override
                    public void onSuccess() {
                        MainHandler.runOnUiThread(() -> ToastUtil.s(getString(R.string.info_logout_success)));
                    }

                    @Override
                    public void onFail(String code, String message) {
                        MainHandler.runOnUiThread(() -> ToastUtil.s(message));
                    }
                });

                return true;
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ApiUtil.deleteUser(new ApiCallback() {
                    @Override
                    public void onSuccess() {
                        MainHandler.runOnUiThread(() -> ToastUtil.s(getString(R.string.info_delete_account_success)));
                    }
                });

                return false;
            }
        });

        findViewById(R.id.btn_rkipc_preview).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getDeviceInfoAndPreview = true;
                if(_deviceInfo == null) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceInfoRequestBean());
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    } else {
                        showDisconnectToast();
                    }
                } else {
                    PlayUtil.startPlay(BlueTestActivity.this, _deviceInfo, null);
                }

                return true;
            }
        });

        BluetoothHandle.addWifiNotifyListener(this);
        BluetoothHandle.addDeviceUsageInfoNotifyListener(this);
        BluetoothHandle.addSoftApNotifyListener(this);
        BluetoothHandle.addNetworkNotifyListener(this);
        BluetoothHandle.addDeviceInfoNotifyListener(this);
        BluetoothHandle.addPlayNotifyListener(this);

        _networkInfo = CameraManager.getNetworkInfo(null);
        if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
            _softap_ssid = _networkInfo.getAp().ssid;
            _softap_password = _networkInfo.getAp().password;
            _softap_status = _networkInfo.getAp().st;
            _deviceSoftApLaunched = _softap_status == 1;
        }
    }

    @Override
    protected void initData() {

    }

    private void initListener() {
        _searchWifiListener = new WifiManager.SearchWifiListener() {
            @Override
            public void onScanStart() {
                Log.d(TAG, "onScanStart...");
//                        mLoadingView.show("正在扫描附近设备");
            }

            @Override
            public void onScanFailed(WifiManager.ErrorType errorType) {
                Log.d(TAG, "onScanFailed..." + errorType);
//                        mLoadingView.dismiss();
                switch(errorType) {
                    case NO_WIFI_FOUND:
                        break;
                    case SEARCH_WIFI_TIMEOUT:
                        break;
                }
            }

            @Override
            public void onScanSuccess(List<WifiInfo> results) {
                Log.d(TAG, "onScanSuccess..." + results.size());
                for(WifiInfo info : results) {
                    Log.d(TAG, info.toString());
                }
                if(results.isEmpty()) {
                    printInfo("未找到WiFi热点");
                } else {
                    showSoftApDialog(results.get(0));
                }
            }
        };

        _connectWifiListener = new WifiManager.WifiConnectListener() {
            @Override
            public void onConnectStart(WifiInfo info) {
                AppTrace.d(TAG, "connect ....");
            }

            @Override
            public void onConnectFailed(WifiInfo info, int error) {
                AppTrace.d(TAG, "connect fail....");
                errorDialog = new ModalDialog(BlueTestActivity.this);
                errorDialog.setMessage(getString(R.string.info_reconnect_for_soft_ap_disconnect));
                errorDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        errorDialog.dismiss();
                        return true;
                    }
                });
                errorDialog.setPositiveButton(getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        searchWifi();
                        return true;
                    }
                });
            }

            @Override
            public void onConnectSuccess(WifiInfo info, int ip) {
                AppTrace.d(TAG, "connect success....");
                _handler.post(() -> {
                    ToastUtil.s(getString(R.string.connected_network));
                    printInfo("热点连接成功！");
                });
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeWifiNotifyListener(this);
        BluetoothHandle.removeDeviceUsageInfoNotifyListener(this);
        BluetoothHandle.removeSoftApNotifyListener(this);
        BluetoothHandle.removeNetworkNotifyListener(this);

        BluetoothHandle.removeDeviceInfoNotifyListener(this);
        BluetoothHandle.removePlayNotifyListener(this);

        autoRun = false;
        if(rkipcHandle != null) {
            rkipcHandle.removeCallbacksAndMessages(null);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);

        if(isConnectedSoftAp()) {
            printInfo(String.format("%s已连接", _softap_ssid));
        }
    }

    @Override
    public void onWifiList(List<WifiInfo> device) {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            String data = device.toString();
            viewBinding.tvStatus.setText(data);

            _wifiListDialog = new WifiListDialog(BlueTestActivity.this, "网络配置", device, new IWifiListAdapterCallback() {
                @Override
                public void onCancel() {

                }

                @Override
                public void onWifiClick(WifiInfo device1, int position) {
                    if(_setupDialog != null && _setupDialog.isShowing()) {
                        _setupDialog.dismiss();
                    }
                    _setupDialog = null;

                    _setupDialog = new WifiPasswordDialog(BlueTestActivity.this, device1.getSsid());
                    _setupDialog.setOnClickListener((dialog, which) -> {
                        if(which == DialogInterface.BUTTON_POSITIVE) {

                            runOnUiThread(() -> {


                            });
                        }
                    });
                    _setupDialog.show();
                }
            });
            _wifiListDialog.show();

        });
    }

    @Override
    public void onSetupStatus(int status, String message) {
        _handler.removeCallbacks(_timeOutRunnable);

        String output = status == 1 ? "成功" : "失败=" + message;

        printInfo("配网情况：" + output);
    }

    @Override
    public void onTurnOff(int status, String message) {

    }

    @Override
    public void onStorageInfo(StorageBean storageBean) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("存储使用情况：" + storageBean.getString());
    }

    @Override
    public void onEnergy(int energy) {
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("电量：" + energy);
    }

    @Override
    public void onDeviceInfo(DeviceUsageInfo info) {
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("设备使用情况：" + info.toString());
    }

    @Override
    public void onSoftApStart(String ssid, String password) {
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("热点启动成功：ssid= " + ssid + " ,password=" + password);
        _handler.post(() -> searchWifi());
    }

    private void searchWifi() {
        if(mWifiManager == null) {
            mWifiManager = WifiManager.getImpl();
        }

        mWifiManager.search(_softap_ssid, _searchWifiListener);
    }

    private void showSoftApDialog(WifiInfo wifiInfo) {
        ModalDialog dialog = new ModalDialog(BlueTestActivity.this);
        dialog.setMessage(String.format(getString(R.string.join_softap), _softap_ssid));
        dialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.tvStatus.setText("正在连接热点：" + wifiInfo.getSsid());
                HotmeWiFiManager.startWifiSettingPage(BlueTestActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
                return true;
            }
        });

        dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dialog.dismiss();
                return true;
            }
        });

        dialog.show();
    }

    @Override
    public void onSoftApStartFail(String message) {
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("热点启动失败：" + message);
    }

    @Override
    public void onSoftApStop() {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("热点关闭");
    }

    @Override
    public void onSoftApStopFail(String message) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("热点关闭失败：" + message);
    }

    @Override
    public void onSoftApStatus(int status) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("热点状态：" + status);
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

    @Override
    public void onNetworkStatus(NetworkBean networkBean) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("网络状态：" + networkBean.toString());
    }

    @Override
    public void onRecordVideoStart() {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("视频开始录制...");
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("录制状态： status=" + recordStatus.getStatus() + ", time= " + recordStatus.getDuration());
    }

    @Override
    public void onRecordVideoStop() {
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("视频已停止录制");
    }

    @Override
    public void onRecordException(String msg) {
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("录制状态： error=" + msg);
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        _deviceInfo = data;
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("设备信息：" + new Gson().toJson(data));

        _handler.post(() -> {
            if(!BaseAppUtil.isDestroy(BlueTestActivity.this) && BaseAppUtil.isForegroundActivity(BlueTestActivity.this, BlueTestActivity.class.getName())) {
                if(getDeviceInfoAndPreview) {
                    PlayUtil.startPlay(BlueTestActivity.this, data, null);
                    getDeviceInfoAndPreview = false;
                }
            }
        });
    }

    @Override
    public void onDeviceInfoFail() {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> viewBinding.tvStatus.setText("获取设备信息异常"));
    }

    boolean autoRun = false;

    long rkipcLoop = 1000;
    long rkipcDuration = 1800;
    long rkipcInterval = 60;
    long rkipcLoopCounter = 0;

    private final Runnable rkipcRunnable = new Runnable() {
        @Override
        public void run() {

            if(autoRun) {
                printInfo(String.format("当前循环第%d 次，开启rkipc, %d秒后执行结束", rkipcLoopCounter, rkipcDuration));
                //
                DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                if(rkipcHandle != null) {
                    rkipcHandle.postDelayed(rkipcStopRunnable, rkipcDuration * 1000);
                }
            } else {
                printInfo("自动化执行停止");
            }
        }
    };


    private final Runnable rkipcStopRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(autoRun) {
                printInfo("停止rkipc");
                DeviceApiManager.sendCmdSimpleObj(new RkipcStopRequestBean());
                rkipcLoopCounter++;
                if(rkipcLoopCounter < rkipcLoop) {
                    printInfo(String.format("等待%d秒后执行下一次", rkipcInterval));
                    rkipcHandle.postDelayed(rkipcRunnable, rkipcInterval * 1000);
                } else {
                    printInfo("执行结束");
                }
            } else {
                printInfo("自动化执行停止");
            }
        }
    };

    Handler rkipcHandle;

    private void showRkipcDialog() {
        AutoRkipcDialog dialog = new AutoRkipcDialog(BlueTestActivity.this);
        dialog.setOnClickListener(new AutoRkipcDialog.IAutoRkipcInfoListener() {
            @Override
            public void onUpdate(long loop, long duration, long interval) {
                autoRun = true;
                String showInfo = "循环次数：" + loop + ", 运行时长：" + duration + ", 间隔：" + interval;
                printInfo(showInfo);

                rkipcLoop = loop;
                rkipcDuration = duration;
                rkipcInterval = interval;

                rkipcLoopCounter = 0;
                if(rkipcHandle == null) {
                    rkipcHandle = new Handler();
                }
                rkipcHandle.post(rkipcRunnable);
            }

            @Override
            public void onCancel() {
                autoRun = false;
            }
        });

        dialog.show();

    }

    private void printInfo(String showInfo) {
        AppTrace.d(showInfo);
        infoList.add(0, showInfo);
        if(infoList.size() > 5) {
            infoList.remove(infoList.size() - 1);
        }
        printToStatusView();
    }

    private void printToStatusView() {

        runOnUiThread(() -> {
            StringBuilder logBuilder = new StringBuilder();
            for(String log : infoList) {
                logBuilder.append(log).append("\n");
            }
            AppTrace.d(TAG, "status output: " + logBuilder);
            viewBinding.tvStatus.setText(logBuilder.toString());
        });
    }


    private boolean isConnectedSoftAp() {
        return NetworkManager.getImpl().isConnectedSoftAp();
    }


    @Subscribe
    public void onEvent(NetWorkForQEvent event) {
        if(event.type == NetWorkForQEvent.AVAILABLE) {
            printInfo("wifi 已连接");
        } else if(event.type == NetWorkForQEvent.UNCONNECT) {
            printInfo("wifi 未连接");
        }
    }
}
