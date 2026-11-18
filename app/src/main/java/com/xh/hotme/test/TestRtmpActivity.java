package com.xh.hotme.test;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.FecRtmpAddUrlRequestBean;
import com.xh.hotme.bluetooth.bean.FecRtmpRemoveUrlRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStopRequestBean;
import com.xh.hotme.bluetooth.bean.SimCloseRequestBean;
import com.xh.hotme.bluetooth.bean.SimOpenRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApOpenRequestBean;
import com.xh.hotme.bluetooth.event.FecRtmpAddUrlEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveUrlEvent;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.event.RkIpcStopEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSimEnableNotifyListener;
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
import com.xh.hotme.databinding.ActivityBlueTestRtmpBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.SwitchButtonO;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


@IEventBus
public class TestRtmpActivity extends BaseActivity<ActivityBlueTestRtmpBinding> implements IWifiNotifyListener, IBleDeviceUsageInfoNotifyListener, IBleSoftApNotifyListener, IBleNetworkInfoNotifyListener, IRecordControlListener, IBleDeviceInfoNotifyListener, IBleSimEnableNotifyListener {

    private final static String TAG = TestRtmpActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    private ImageView backView;

    TextView tvStatus;

    SwitchButtonO _switchButton;

    TextView interfaceType;

    WifiListDialog _wifiListDialog;
    WifiPasswordDialog _setupDialog;

    private WifiManager mWifiManager;

    String uploadId = "1671090275338526721";
    String videoName;

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

    boolean isRkipcStart = false;

    int _network_sim = 0;

    boolean _simCard;


    final static String FILE_RTMP = "test_rtmp_url";

    private final Runnable _timeOutRunnable = () -> {
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, TestRtmpActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        initListener();

        backView = findViewById(R.id.iv_back);
        tvStatus = findViewById(R.id.tv_status);

        interfaceType = findViewById(R.id.interface_type);
        _switchButton = findViewById(R.id.api_type);
        _switchButton.setChecked(true);
        DeviceApiManager.setApiType(true);
        interfaceType.setText("蓝牙");
        backView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        _switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppTrace.d(TAG, "set api type: " + (isChecked ? "蓝牙" : "http"));
            if(isChecked) {
                interfaceType.setText("蓝牙");
                DeviceApiManager.setApiType(true);
            } else {
                DeviceApiManager.setApiType(false);
                interfaceType.setText("http");
                _networkInfo = CameraManager.getNetworkInfo(null);
                if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
                    _softap_ssid = _networkInfo.getAp().ssid;
                    _softap_password = _networkInfo.getAp().password;
                    _softap_status = _networkInfo.getAp().st;
                    _deviceSoftApLaunched = _softap_status == 1;
                }

                if(_networkInfo != null && _networkInfo.getSim() != null) {
                    _simCard = _networkInfo.getSim().has_card == 1;
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

        viewBinding.btnRkipc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!isRkipcStart) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                    if(isOk) {
                        printInfo("启动RKIPC命令已发送");
                    }
                } else {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStopRequestBean());
                    if(isOk) {
                        printInfo("结束RKIPC命令已发送");
                    }
                }

                return true;
            }
        });
        viewBinding.btnRkipc.setEnabled(true);

        String rtmpUrl = AppFileUtil.loadStringFromFile(FILE_RTMP);

        viewBinding.etFecUrl.setText(rtmpUrl);

        viewBinding.btnAddFecLiveUrl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!isRkipcStart) {
                    ToastUtil.s(getString(R.string.info_rkipc_not_lunched));
                    return true;
                }

                String url = viewBinding.etFecUrl.getText().toString();
                if(TextUtils.isEmpty(url)) {
                    ToastUtil.s(getString(R.string.info_live_url_empty));
                    return true;
                }

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FecRtmpAddUrlRequestBean(url));
                if(isOk) {

                }
                return true;
            }
        });

        viewBinding.btnRemoveFecLiveUrl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String url = viewBinding.etFecUrl.getText().toString();
                if(TextUtils.isEmpty(url)) {
                    ToastUtil.s(getString(R.string.info_live_url_empty));
                    return true;
                }

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FecRtmpRemoveUrlRequestBean(url));
                if(isOk) {

                }
                return true;
            }
        });


        viewBinding.btnSaveFecLiveUrl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String url = viewBinding.etFecUrl.getText().toString();
                if(TextUtils.isEmpty(url)) {
                    ToastUtil.s(getString(R.string.info_live_url_empty));
                    return true;
                }
                AppFileUtil.saveString(url, FILE_RTMP);
                AppTrace.d(TAG, "");
                return true;
            }
        });

        _networkInfo = CameraManager.getNetworkInfo(null);
        if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
            _softap_ssid = _networkInfo.getAp().ssid;
            _softap_password = _networkInfo.getAp().password;
            _softap_status = _networkInfo.getAp().st;
            _deviceSoftApLaunched = _softap_status == 1;
        }

        if(_networkInfo != null && _networkInfo.getSim() != null) {
            _simCard = _networkInfo.getSim().has_card == 1;
        }

        if(!_simCard) {
            viewBinding.sbFourG.setChecked(false);
            viewBinding.sbFourG.setOnClickListener(v -> {
                ToastUtil.s(getString(R.string.info_no_sim_card));
                viewBinding.sbFourG.setChecked(false);
            });
        } else {
            _network_sim = 1;
            viewBinding.sbFourG.setChecked(true);
            viewBinding.sbFourG.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(!isChecked) {
                    doSimCard(isChecked);
                }

            });
        }


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
                errorDialog = new ModalDialog(TestRtmpActivity.this);
                errorDialog.setMessage(getString(R.string.wifi_disconnect));
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

        BluetoothHandle.removeDeviceInfoNotifyListener(this);
        BluetoothHandle.removePlayNotifyListener(this);
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

        if(!BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            runOnUiThread(() -> {
                String data = device.toString();
                tvStatus.setText(data);

                _wifiListDialog = new WifiListDialog(TestRtmpActivity.this, "网络配置", device, new IWifiListAdapterCallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onWifiClick(WifiInfo device1, int position) {
                        if(_setupDialog != null && _setupDialog.isShowing()) {
                            _setupDialog.dismiss();
                        }
                        _setupDialog = null;

                        _setupDialog = new WifiPasswordDialog(TestRtmpActivity.this, device1.getSsid());
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
    }

    @Override
    public void onSetupStatus(int status, String message) {

    }

    @Override
    public void onTurnOff(int status, String message) {

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
            tvStatus.setText(logBuilder.toString());
        });
    }


    private void searchWifi() {
        if(mWifiManager == null) {
            mWifiManager = WifiManager.getImpl();
        }

        mWifiManager.search(_softap_ssid, _searchWifiListener);
    }

    private void showSoftApDialog(WifiInfo wifiInfo) {
        ModalDialog dialog = new ModalDialog(TestRtmpActivity.this);
        dialog.setMessage(String.format(getString(R.string.join_softap), _softap_ssid));
        dialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                tvStatus.setText("正在连接热点：" + wifiInfo.getSsid());
                HotmeWiFiManager.startWifiSettingPage(TestRtmpActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
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


    private boolean isConnectedSoftAp() {
        return NetworkManager.getImpl().isConnectedSoftAp();
    }


    @Subscribe
    public void onEvent(NetWorkForQEvent event) {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        if(event.type == NetWorkForQEvent.AVAILABLE) {
            printInfo("wifi 已连接");
        } else if(event.type == NetWorkForQEvent.UNCONNECT) {
            printInfo("wifi 未连接");
        }
    }

    @Subscribe
    public void onFecRtmpAddUrl(FecRtmpAddUrlEvent event) {
        AppTrace.d(TAG, "onFecRtmpAddUrl");
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        if(event != null) {
            AppTrace.d(TAG, "onFecRtmpAddUrl, status=" + event.status);
            if(event.status == 1) {
                printInfo("添加推流地址成功");
            } else {
                printInfo("添加推流地址失败");
            }
        }
    }

    @Subscribe
    public void onFecRtmpRemoveUrl(FecRtmpRemoveUrlEvent event) {
        AppTrace.d(TAG, "onFecRtmpRemoveUrl");
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        if(event != null) {
            AppTrace.d(TAG, "onFecRtmpRemoveUrl, status=" + event.status);
            if(event.status == 1) {
                printInfo("删除推流地址成功");
            } else {
                printInfo("删除推流地址失败");
            }
        }
    }

    @Override
    public void onStorageInfo(StorageBean storageBean) {

    }

    @Override
    public void onEnergy(int energy) {

    }

    @Override
    public void onDeviceInfo(DeviceUsageInfo info) {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("设备使用情况：" + info.toString());
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        _deviceInfo = data;
        _handler.removeCallbacks(_timeOutRunnable);
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }

        printInfo("设备信息：" + new Gson().toJson(data));

        _handler.post(() -> {
            if(!BaseAppUtil.isDestroy(TestRtmpActivity.this) && BaseAppUtil.isForegroundActivity(TestRtmpActivity.this, TestDeviceActivity.class.getName())) {
                if(getDeviceInfoAndPreview) {
                    PlayUtil.startPlay(TestRtmpActivity.this, data, null);
                    getDeviceInfoAndPreview = false;
                }
            }
        });
    }

    @Override
    public void onDeviceInfoFail() {

    }

    @Override
    public void onRecordVideoStart() {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("视频开始录制...");
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("录制状态： status=" + recordStatus.getStatus() + ", time= " + recordStatus.getDuration());

    }

    @Override
    public void onRecordVideoStop() {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        _handler.removeCallbacks(_timeOutRunnable);
        printInfo("视频已停止录制");
    }

    @Override
    public void onRecordException(String msg) {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        _handler.removeCallbacks(_timeOutRunnable);

        printInfo("录制状态： error=" + msg);
    }

    @Override
    public void onNetworkStatus(NetworkBean networkBean) {

    }

    @Override
    public void onSoftApStart(String ssid, String password) {

    }

    @Override
    public void onSoftApStartFail(String message) {

    }

    @Override
    public void onSoftApStop() {

    }

    @Override
    public void onSoftApStopFail(String message) {

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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        AppTrace.d(TAG, "onRkipcStart");
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        if(event != null && event.status == 1) {
            isRkipcStart = true;
            printInfo("启动RKIPC成功");
            viewBinding.btnRkipc.setText("停止RkIpc");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStop(RkIpcStopEvent event) {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        if(event != null && event.status == 1) {
            isRkipcStart = false;
            printInfo("RKIPC已停止");
            viewBinding.btnRkipc.setText("开启RkIpc");
        }
    }

    private void doSimCard(boolean isOpen) {
        if(isOpen) {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SimOpenRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                printInfo(getString(R.string.loading_open_4g_network));
            } else {
                String error = getString(R.string.info_bluetooth_offline);
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                    error = getString(R.string.info_network_disconnect);
                }
                printInfo(error);
            }

        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SimCloseRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                printInfo(getString(R.string.loading_close_4g_network));
            } else {
                String error = getString(R.string.info_bluetooth_offline);
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                    error = getString(R.string.info_network_disconnect);
                }
                printInfo(error);
            }
        }
    }

    @Override
    public void onSimOpen() {
        if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
            return;
        }
        MainHandler.runOnUiThread(() -> {
            try {
                if(!BaseAppUtil.isDestroy(TestRtmpActivity.this) && BaseAppUtil.isForegroundActivity(TestRtmpActivity.this, TestDeviceActivity.class.getName())) {
                    DialogUtil.dismissDialog();
                    _network_sim = 1;
                    viewBinding.sbFourG.setChecked(true);

                    //同步
                    DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
                    printInfo("4G打开成功");
                }
            } catch(Throwable e) {

            }
        });
    }

    @Override
    public void onSimOpenFail(String msg) {
        try {
            if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(() -> {
                try {
                    if(!BaseAppUtil.isDestroy(TestRtmpActivity.this) && BaseAppUtil.isForegroundActivity(TestRtmpActivity.this, TestDeviceActivity.class.getName())) {
                        printInfo("4G打开失败: " + msg);
                    }
                } catch(Throwable e) {

                }
            });

        } catch(Throwable e) {

        }
    }

    @Override
    public void onSimClose() {
        try {
            if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(() -> {
                try {
                    if(!BaseAppUtil.isDestroy(TestRtmpActivity.this) && BaseAppUtil.isForegroundActivity(TestRtmpActivity.this, TestDeviceActivity.class.getName())) {
                        printInfo("4G 已关闭");
                        viewBinding.sbFourG.setChecked(false);
                    }
                } catch(Throwable e) {

                }
            });

        } catch(Throwable e) {

        }
    }

    @Override
    public void onSimCloseFail(String msg) {
        try {
            if(BaseAppUtil.isDestroy(TestRtmpActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(() -> {
                try {
                    if(!BaseAppUtil.isDestroy(TestRtmpActivity.this) && BaseAppUtil.isForegroundActivity(TestRtmpActivity.this, TestDeviceActivity.class.getName())) {
                        printInfo("4G关闭失败");
                    }
                } catch(Throwable e) {

                }
            });

        } catch(Throwable e) {

        }
    }
}
