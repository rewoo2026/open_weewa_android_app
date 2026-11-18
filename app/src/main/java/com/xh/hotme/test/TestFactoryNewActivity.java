package com.xh.hotme.test;

import static com.xh.hotme.camera.DeviceListFragment.type_test;
import static com.xh.hotme.utils.PermissionsUtil.REQUEST_PERMISSION_LOCATION;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.weewa.lib.WeewaLib;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.WifiBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.BeepRequestBean;
import com.xh.hotme.bluetooth.bean.DeviceInfoRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestArecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestArecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestAudioStartRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestAudioStopRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestFanRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestFastChargeRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestLightRequestBean;
import com.xh.hotme.bluetooth.bean.FactoryTestPingRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.StartUsbRequestBean;
import com.xh.hotme.bluetooth.bean.WifiTurnOffRequestBean;
import com.xh.hotme.bluetooth.event.BeepEvent;
import com.xh.hotme.bluetooth.event.CameraFilesDownloadedEvent;
import com.xh.hotme.bluetooth.event.DeviceWeewaStartEvent;
import com.xh.hotme.bluetooth.event.FTArecordStartEvent;
import com.xh.hotme.bluetooth.event.FTArecordStopEvent;
import com.xh.hotme.bluetooth.event.FTAudioStartEvent;
import com.xh.hotme.bluetooth.event.FTAudioStopEvent;
import com.xh.hotme.bluetooth.event.FTFastChargeEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.PingEvent;
import com.xh.hotme.bluetooth.event.RebootEvent;
import com.xh.hotme.bluetooth.event.StartUsbEvent;
import com.xh.hotme.bluetooth.event.StopUsbEvent;
import com.xh.hotme.bluetooth.event.WifiSetupEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiAddDialog;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.databinding.ActivityTestFactoryNewBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.RecordActivity;
import com.xh.hotme.test.factory.ArecordInfoDialog;
import com.xh.hotme.test.factory.ITestCommandListener;
import com.xh.hotme.test.factory.ScanEvent;
import com.xh.hotme.test.factory.ScanHelper;
import com.xh.hotme.test.factory.TestAdapter;
import com.xh.hotme.test.factory.TestModelBean;
import com.xh.hotme.test.factory.TestResultDialog;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.RecycleViewDivider;
import com.xh.hotme.widget.WavPlayer;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public class TestFactoryNewActivity extends BaseActivity<ActivityTestFactoryNewBinding> implements IBleDeviceInfoNotifyListener, IWifiNotifyListener {

    private final static String TAG = TestFactoryNewActivity.class.getSimpleName();

    public final static String ft_wifi_file = "ft_wifi_info";

    TestAdapter _adapter;

    List<TestModelBean> _dataList = new ArrayList<TestModelBean>();

    TestResultDialog resultDialog;

    ArecordInfoDialog _arecordInfoDialog;

    CameraInfo _cameraInfo;

    DeviceInfo _deviceInfo;

    NetworkBean _networkBean;

    TestModelBean _pendingTest;

    WifiAddDialog _wifiAddDialog;

    WavPlayer _audioPlayer;

    FactoryTestArecordStartRequestBean _recordRequest;

    String _audioPath = "/data/weewa/debug";
    String _dumpAnrIn = "anr_in.wav";
    String _dumpAnrOut = "anr_out.wav";
    String _dumpAgcOut = "agc_out.wav";
    String _dumpHpfOut = "hpf_out.wav";

    String _arecordFile = "arecord.wav";

    WifiBean _defaultWifiBean;

    long time1; //保存第一次的时间
    long time2; //保存重启后的时间

    boolean isReboot = false;
    List<String> _dumpFiles = new ArrayList<>();

    private final Runnable _timeOutRunnable = () -> {

    };

    public static void start(Context context) {
        Intent intent = new Intent(context, TestFactoryNewActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        String title = "测试";
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if (cameraInfo != null) {
            title += "-" + cameraInfo.deviceInfo.device_name;
        }

        TitleLayoutHelper.initTitle(viewBinding.titleBar, title);

        viewBinding.titleBar.ivRight.setImageResource(R.mipmap.ic_scanicon);
        viewBinding.titleBar.ivRight.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                return true;
            }
        });
        viewBinding.btnReset.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                time1 = 0;
                time2 = 0;
                isReboot = false;
                //删除之前的文件
                deleteAudioFile();
                for (TestModelBean modelBean : _dataList) {
                    modelBean.setStatus(TestModelBean.STATUS_UNTEST);
                }
                _adapter.notifyDataSetChanged();
                return false;
            }
        });
        viewBinding.photoSelect.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startSelectAllCamera();
                return false;
            }
        });

        viewBinding.btnSoftapSetup.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.start(TestFactoryNewActivity.this, CameraSoftApActivity.REQUEST_SOFT_AP_FACTORY_TEST);
                return false;
            }
        });

        viewBinding.btnReboot.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                doReboot(null, true);
                return false;
            }
        });

        viewBinding.btnStartUsb.setOnClickListener(v ->{
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new StartUsbRequestBean());
            if (!isOk) {
                showDisconnectToast();
            }
        });
    }

    private void startSelectAllCamera() {
        ActiveListActivity.start(TestFactoryNewActivity.this, type_test);
    }

    @Override
    protected void initData() {
        int index = 0;
        _dataList.add(new TestModelBean(TestModelBean.TYPE_BLUETOOTH, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_BEEP, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_FAN, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_LIGHT, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_NETWORK_4G, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_NETWORK_WIFI, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_NETWORK_ETH, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_CAMERA, TestModelBean.STATUS_UNTEST, index++));
//        _dataList.add(new TestModelBean(TestModelBean.TYPE_AUDIO, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_RTC_TIME, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_FAST_CHARGE, TestModelBean.STATUS_UNTEST, index++));
        _dataList.add(new TestModelBean(TestModelBean.TYPE_ARECORD, TestModelBean.STATUS_UNTEST, index++));

        viewBinding.recyclerView.addItemDecoration(new RecycleViewDivider(TestFactoryNewActivity.this, LinearLayoutManager.VERTICAL, getResources().getColor(R.color.bg_gray)));
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(TestFactoryNewActivity.this));

        _adapter = new TestAdapter(TestFactoryNewActivity.this, _dataList);
        viewBinding.recyclerView.setAdapter(_adapter);

        _adapter.setListener(new ITestCommandListener() {
            @Override
            public void onStart(TestModelBean modelBean) {
                if (_cameraInfo == null) {
                    ToastUtil.s(getString(R.string.info_please_connect_device));
                    return;
                }

                doTest(modelBean, true);
            }
        });


        BluetoothHandle.addDeviceInfoNotifyListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                doScan(TestFactoryNewActivity.this);
                break;
            default:
                break;

        }
    }

    private void doTest(TestModelBean modelBean, boolean isNewTest) {
        _pendingTest = null;
        switch (modelBean.getType()) {
            case TestModelBean.TYPE_BEEP:
                doBeep(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_RTC_TIME:
                doDeviceInfo(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_LIGHT:
                doLight(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_FAN:
                doFan(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_ARECORD:
                _pendingTest = modelBean;
                if (_arecordInfoDialog == null) {
                    _arecordInfoDialog = new ArecordInfoDialog(TestFactoryNewActivity.this);
                }
                _arecordInfoDialog.initValue();
                _arecordInfoDialog.setCallback(new ArecordInfoDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onRecord(String cardName, int channels, int duration, String path) {
                        _recordRequest = new FactoryTestArecordStartRequestBean(cardName, channels, duration, path);
                        doArecord(modelBean, isNewTest);
                    }
                });
                _arecordInfoDialog.show();

                ;
                break;
            case TestModelBean.TYPE_AUDIO:
                _pendingTest = modelBean;
                doAudio(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_FAST_CHARGE:
                _pendingTest = modelBean;
                doFastCharge(modelBean, isNewTest);
                break;
            case TestModelBean.TYPE_NETWORK_WIFI:
            case TestModelBean.TYPE_NETWORK_ETH:
            case TestModelBean.TYPE_NETWORK_4G:
                _pendingTest = modelBean;
                getNetworkInfo();
                break;
            case TestModelBean.TYPE_CAMERA:
                showConfirmDialog(modelBean, true);
                RecordActivity.start(TestFactoryNewActivity.this, true);
                break;
            default:
                ToastUtil.s("unsupport");

        }
    }

    public void doScan(Activity activity) {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
        } else {
            ScanHelper.getInstance().scan(TestFactoryNewActivity.this, null);
        }
    }

    private void getCurrentCamera() {
        _cameraInfo = CameraManager.getCameraInfo("");
        if (_cameraInfo != null && _cameraInfo.deviceInfo != null) {

            NetworkManager.getImpl().updateNetworkStatus();

            String deviceName = BluetoothManager.getDisplayDeviceName(_cameraInfo.deviceInfo.device_name);
            viewBinding.photoName.setText(deviceName);

            Device device = BluetoothManager.getConnectDevice();
            if (device != null) {
                AppTrace.d(TAG, "rssi = " + device.rssi);
                if (device.rssi > -52) {
                    _dataList.get(0).setStatus(TestModelBean.STATUS_PASS);
                } else {
                    _dataList.get(0).setStatus(TestModelBean.STATUS_FAIL);
                    _dataList.get(0).setErrMessage(String.format("信号强度%d,<-52", device.rssi));
                }
            }

        } else {
            viewBinding.photoName.setText(getString(R.string.info_please_connect_device));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        getCurrentCamera();

        if (_pendingTest != null && _pendingTest.getType() == TestModelBean.TYPE_AUDIO) {
            doAudio(_pendingTest, false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        WeewaLib.Companion.shared().setRemoteDirectory(ConnectLogic.getImpl().getBasePath());

        BluetoothHandle.removeDeviceInfoNotifyListener(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.REQUEST_CODE_WIFI_FACTORY_TEST) {
            if(isConnectedSoftAp()) {
                ConnectLogic.getImpl().setSsid(NetworkManager.getImpl().getSoftApSsid());
                ConnectLogic.getImpl().setSsidPwd(NetworkManager.getImpl().getSoftApPassword());
                if(_deviceInfo != null) {
                    if(_device != null) {
                        ConnectLogic.getImpl().setDeviceMac(_device.address);
                    }
                    ConnectLogic.getImpl().setDeviceId(_deviceInfo.device_id);
                } else {
                    if(_device != null) {
                        ConnectLogic.getImpl().setDeviceMac(_device.address);
                    }
                    String deviceId = CameraManager.getDeviceId();
                    AppTrace.d(TAG, "set ConnectLogic deviceId = " + deviceId);
                    ConnectLogic.getImpl().setDeviceId(deviceId);
                }
                if(_pendingTest!=null){
                    doTest(_pendingTest, false);
                }

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanEvent(ScanEvent event) {
        AppTrace.d(TAG, "onScanEvent: " + event.info);
    }

    private void doDeviceInfo(TestModelBean modelBean, boolean isNew) {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceInfoRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private void doReboot(TestModelBean modelBean, boolean isNew) {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
        if (isOk) {
            isReboot = true;
            if (modelBean != null) {
                showConfirmDialog(modelBean, isNew);
            }
        } else {
            showDisconnectToast();
        }
    }

    private void doBeep(TestModelBean modelBean, boolean isNew) {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new BeepRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private void doLight(TestModelBean modelBean, boolean isNew) {

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestLightRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private void doFan(TestModelBean modelBean, boolean isNew) {

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestFanRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private boolean isConnectedSoftAp() {
        return NetworkManager.getImpl().isConnectedSoftAp();
    }

    private void doArecord(TestModelBean modelBean, boolean isNew) {

        if (!isConnectedSoftAp()) {
            if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                CameraSoftApActivity.start(TestFactoryNewActivity.this, CameraSoftApActivity.REQUEST_SOFT_AP_FACTORY_TEST);
            } else {
                if (!PermissionsUtil.isLocServiceEnable(TestFactoryNewActivity.this)) {
                    PermissionsUtil.openLocationService(TestFactoryNewActivity.this);
                    return;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(TestFactoryNewActivity.this);
                if (!hasPermission) {
                    EasyPermissions.requestPermissions(TestFactoryNewActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return;
                }
                HotmeWiFiManager.startWifiSettingPage(TestFactoryNewActivity.this, Constants.REQUEST_CODE_WIFI_FACTORY_TEST);
            }
            return;
        }

        showConfirmDialog(modelBean, isNew);
    }

    private void doAudio(TestModelBean modelBean, boolean isNew) {

        if (!isConnectedSoftAp()) {
            if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                CameraSoftApActivity.start(TestFactoryNewActivity.this, CameraSoftApActivity.REQUEST_SOFT_AP_FACTORY_TEST);
            } else {
                if(!PermissionsUtil.isLocServiceEnable(TestFactoryNewActivity.this)) {
                    PermissionsUtil.openLocationService(TestFactoryNewActivity.this);
                    return ;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(TestFactoryNewActivity.this);
                if(!hasPermission) {
                    EasyPermissions.requestPermissions(TestFactoryNewActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return ;
                }
                HotmeWiFiManager.startWifiSettingPage(TestFactoryNewActivity.this, Constants.REQUEST_CODE_WIFI_FACTORY_TEST);
            }
            return;
        }

        showConfirmDialog(modelBean, isNew);
    }

    private void doARecord(boolean isRecord) {

        if (isRecord) {
            //录制前清除本地文件
            deleteAudioFile();

            boolean isOk = DeviceApiManager.sendCmdSimpleObj(_recordRequest);
            if (isOk) {
                AppTrace.d("开始录制");
                DialogUtil.showDialog("等待录制开始");
            } else {
                showDisconnectToast();
            }
        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestArecordStopRequestBean());
            if (isOk) {
                AppTrace.d("请求结束录制");
                DialogUtil.showDialog("等待录制结束");
            } else {
                showDisconnectToast();
            }

        }

    }

    private void doAudioRecord(boolean isRecord) {

        if (isRecord) {
            //录制前清除本地文件
            deleteAudioFile();

            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestAudioStartRequestBean());
            if (isOk) {
                AppTrace.d("开始录制");
                DialogUtil.showDialog("等待录制开始");
            } else {
                showDisconnectToast();
            }
        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestAudioStopRequestBean());
            if (isOk) {
                AppTrace.d("请求结束录制");
                DialogUtil.showDialog("等待录制结束");
            } else {
                showDisconnectToast();
            }

        }

    }

    private void doPing(TestModelBean modelBean, boolean isNew) {

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestPingRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private void doFastCharge(TestModelBean modelBean, boolean isNew) {

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FactoryTestFastChargeRequestBean());
        if (isOk) {
            showConfirmDialog(modelBean, isNew);
        } else {
            showDisconnectToast();
        }
    }

    private void getNetworkInfo() {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
        if (isOk) {
            DialogUtil.showDialog("正在获取网络信息");
            _handler.postDelayed(_timeOutRunnable, 5000);
        } else {
            showDisconnectToast();
        }
    }

    private void closeWiFi() {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiTurnOffRequestBean());
        if (!isOk) {
            showDisconnectToast();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBeepEvent(BeepEvent event) {
        AppTrace.d(TAG, "onBeepEvent");

        DialogUtil.dismissDialog();
        if (event != null && event.state == 1) {
//            ToastUtil.s("相机请求执行成功");
        } else {
            ToastUtil.s(getString(R.string.setup_successful));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRebootEvent(RebootEvent event) {
        AppTrace.d(TAG, "onBeepEvent");

        DialogUtil.dismissDialog();
        if (event != null) {
            ToastUtil.s(getString(R.string.weewa_reboot_success));
        } else {
            ToastUtil.s(getString(R.string.weewa_reboot_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadAudioEvent(CameraFilesDownloadedEvent event) {
        AppTrace.d(TAG, "onDownloadAudioEvent");

        //播放声音
        if (TextUtils.isEmpty(_audioPath)) {
            return;
        }
        _dumpFiles.clear();
        if (_pendingTest != null && _pendingTest.getType() == TestModelBean.TYPE_AUDIO) {

            _dumpFiles.add(getAudioFile(_dumpAnrIn));
            _dumpFiles.add(getAudioFile(_dumpAnrOut));
            _dumpFiles.add(getAudioFile(_dumpHpfOut));
            _dumpFiles.add(getAudioFile(_dumpAgcOut));
        } else {
            _dumpFiles.add(getAudioFile(_arecordFile));
        }

        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.addExtendInfo(String.format("录音文件已下载"));
        }

        resultDialog.showDownloadView(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioStopEvent(FTAudioStopEvent event) {
        AppTrace.d(TAG, "onAudioStopEvent");

        DialogUtil.dismissDialog();
        if (event != null) {
            _audioPath = event.path;
            ToastUtil.s(getString(R.string.info_audio_record_end));
            if (resultDialog != null && resultDialog.isShowing()) {
                resultDialog.setRecordStatus(false);
                if (!TextUtils.isEmpty(_audioPath)) {
                    resultDialog.addExtendInfo(String.format("录音已结束,开始下载..."));
                    downloadAudioFile();
                } else {
                    ToastUtil.s(getString(R.string.info_download_path_null));
                }
            }

        } else {
            ToastUtil.s(getString(R.string.info_excute_exception));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onArecordStartEvent(FTArecordStartEvent event) {
        AppTrace.d(TAG, "onArecordStartEvent");
        DialogUtil.dismissDialog();
        if (event != null) {
            AppTrace.d(TAG, "录音开始");
            if (resultDialog != null && resultDialog.isShowing()) {
                resultDialog.setRecordStatus(true);
            }

        } else {
            ToastUtil.s(getString(R.string.info_excute_exception));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onArecordStopEvent(FTArecordStopEvent event) {
        AppTrace.d(TAG, "onArecordStopEvent");

        DialogUtil.dismissDialog();
        if (event != null) {
            ToastUtil.s(getString(R.string.info_audio_record_end));
            if (resultDialog != null && resultDialog.isShowing()) {
                resultDialog.setRecordStatus(false);
                if (!TextUtils.isEmpty(_audioPath)) {
                    resultDialog.addExtendInfo(String.format("录音已结束,开始下载..."));
                    downloadARecordFile();
                } else {
                    ToastUtil.s(getString(R.string.info_download_path_null));
                }
            }

        } else {
            ToastUtil.s(getString(R.string.info_excute_exception));
        }
    }

    private void  downloadARecordFile() {
        if (TextUtils.isEmpty(_audioPath)) {
            ToastUtil.s(getString(R.string.info_download_path_null));
            return;
        }
        WeewaLib.Companion.shared().setRemoteDirectory(_audioPath);
        List<String> downloadFiles = new ArrayList<>();
        downloadFiles.add(_arecordFile);

        ConnectLogic.getImpl().addCustomFiles(downloadFiles);
        ConnectLogic.getImpl().toReceive();
    }

    private void downloadAudioFile() {
        if (TextUtils.isEmpty(_audioPath)) {
            ToastUtil.s(getString(R.string.info_download_path_null));
            return;
        }
        WeewaLib.Companion.shared().setRemoteDirectory(_audioPath);
        List<String> downloadFiles = new ArrayList<>();
        downloadFiles.add(_dumpAnrIn);
        downloadFiles.add(_dumpAnrOut);
        downloadFiles.add(_dumpAgcOut);
        downloadFiles.add(_dumpHpfOut);

        ConnectLogic.getImpl().addCustomFiles(downloadFiles);
        ConnectLogic.getImpl().toReceive();
    }

    private void deleteAudioFile() {
        AppTrace.d(TAG, "deleteAudioFile");
        for (String path : _dumpFiles) {
            AppTrace.d(TAG, "delete file=" + path);
            FileUtil.deleteFile(path);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioStartEvent(FTAudioStartEvent event) {
        AppTrace.d(TAG, "onAudioStartEvent");

        DialogUtil.dismissDialog();
        if (event != null) {
            AppTrace.d(TAG, "录音开始");
            if (resultDialog != null && resultDialog.isShowing()) {
                resultDialog.setRecordStatus(true);
            }

        } else {
            ToastUtil.s(getString(R.string.info_excute_exception));
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkStatusEvent(NetworkStatusEvent event) {
        AppTrace.d(TAG, "onNetworkStatusEvent");
        DialogUtil.dismissDialog();
        if (event != null) {
            _networkBean = event.n;
            if (_pendingTest != null) {
                switch (_pendingTest.getType()) {
                    case TestModelBean.TYPE_NETWORK_WIFI:
                        if (CameraManager.hasWifi()) {
                            doPing(_pendingTest, true);
                        } else {
                            showWifiSetupDialog();
                        }
                        break;
                    case TestModelBean.TYPE_NETWORK_ETH:
                        if (CameraManager.hasWifi()) {
                            closeWiFi();
                        }
                        if (CameraManager.hasEthernet()) {
                            doPing(_pendingTest, true);
                        } else {
                            ToastUtil.s(getString(R.string.info_insert_cable_line_retry_it));
                        }

                        break;
                    case TestModelBean.TYPE_NETWORK_4G:
                        if (CameraManager.hasWifi()) {
                            closeWiFi();
                        }

                        if (CameraManager.isSimValid()) {
                            doPing(_pendingTest, true);
                        } else {
                            ToastUtil.l(getString(R.string.info_insert_card_reboot_device));
                        }
                        break;
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPingEvent(PingEvent event) {
        AppTrace.d(TAG, "onPingEvent");

        DialogUtil.dismissDialog();
        if (_pendingTest != null) {
            int position = _pendingTest.getPosition();
            if (event != null && event.status == 1) {
                _dataList.get(position).setStatus(TestModelBean.STATUS_PASS);
            } else {
                _dataList.get(position).setStatus(TestModelBean.STATUS_FAIL);
            }
            _adapter.notifyDataSetChanged();
            checkTime();
            dismissConfirmDialog();
            _pendingTest = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiSetupEvent(WifiSetupEvent event) {
        AppTrace.d(TAG, "onWifiSetupEvent");

        DialogUtil.dismissDialog();
        if (_pendingTest != null) {
            doPing(_pendingTest, false);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFastChargeEvent(FTFastChargeEvent event) {
        AppTrace.d(TAG, "onFastChargeEvent");
        DialogUtil.dismissDialog();
        if (event != null) {
            if (event.charge > 4500) {
                if (_pendingTest != null) {
                    _dataList.get(_pendingTest.getPosition()).setStatus(TestModelBean.STATUS_PASS);
                }
                _pendingTest = null;
                _adapter.notifyDataSetChanged();
                if (resultDialog != null && resultDialog.isShowing()) {
                    resultDialog.addExtendInfo(String.format("当前充电电流= %d, 大于快充限定最低电流4500mA", event.charge));
                }
                _handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (resultDialog != null && resultDialog.isShowing()) {
                            resultDialog.dismiss();
                        }
                    }
                }, 2000);

            } else {
                if (_pendingTest != null) {
                    _dataList.get(_pendingTest.getPosition()).setStatus(TestModelBean.STATUS_PASS);
                }
                _adapter.notifyDataSetChanged();
                if (resultDialog != null && resultDialog.isShowing()) {
                    resultDialog.addExtendInfo(String.format("当前充电电流= %d, 小于快充限定最低电流4500mA", event.charge));
                }
                _pendingTest = null;
            }
        }
    }

    public void checkTime() {
        NetworkBean networkBean = CameraManager.getNetworkInfo("");
        if (CameraManager.isNetworkValid(networkBean)) {
            if (!isReboot) {
                time1 = _cameraInfo.deviceInfo._t;
            }
            if (time1 == 0) {
                time1 = _cameraInfo.deviceInfo._t;
                time2 = 0;
            } else {
                time2 = _cameraInfo.deviceInfo._t;
            }
            if (time1 != 0 && time2 != 0) {
                if (TimeUtil.isToday(time1) && TimeUtil.isToday(time2)) {
                    setTypeStatus(TestModelBean.TYPE_RTC_TIME, true);
                } else {
                    setTypeStatus(TestModelBean.TYPE_RTC_TIME, false);
                }
            }
        }
    }

    private void dismissConfirmDialog() {
        AppTrace.d(TAG, "dismissConfirmDialog");
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
        resultDialog = null;
    }

    private void showConfirmDialog(TestModelBean modelBean, boolean isNew) {
        AppTrace.d(TAG, "showConfirmDialog");
        if (isNew) {
            if (resultDialog != null && resultDialog.isShowing()) {
                resultDialog.dismiss();
                AppTrace.d(TAG, "hide dialog");
            }
            resultDialog = null;
        }

        if (resultDialog == null) {
            newTestResultDialog(modelBean, isNew);
        }

        resultDialog.show();
    }

    private void newTestResultDialog(TestModelBean modelBean, boolean isNew) {
        AppTrace.d(TAG, "newTestResultDialog");
        resultDialog = new TestResultDialog(TestFactoryNewActivity.this);
        resultDialog.setTestModelBean(modelBean);
        resultDialog.setCallback(new TestResultDialog.ICallback() {
            @Override
            public void onCancel(TestModelBean testModelBean) {

            }

            @Override
            public void onReboot(TestModelBean testModelBean) {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
                if (isOk) {
                    showConfirmDialog(modelBean, false);
                } else {
                    showDisconnectToast();
                }
            }


            @Override
            public void onTryAgain(TestModelBean testModelBean) {
                doTest(testModelBean, false);
            }

            @Override
            public void onPass(TestModelBean testModelBean) {
                int position = testModelBean.getPosition();
                _dataList.get(position).setStatus(TestModelBean.STATUS_PASS);

                _adapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(TestModelBean testModelBean) {
                int position = testModelBean.getPosition();
                _dataList.get(position).setStatus(TestModelBean.STATUS_FAIL);

                _adapter.notifyDataSetChanged();
            }
        });

        resultDialog.setMediaCallback(new TestResultDialog.IMediaCallback() {
            @Override
            public void onPlayAnrIn(TestModelBean testModelBean) {

                onPlayVideoFile(getAudioFile(_dumpAnrIn));
            }

            @Override
            public void onPlayAnrOut(TestModelBean testModelBean) {

                onPlayVideoFile(getAudioFile(_dumpAnrOut));
            }

            @Override
            public void onPlayAgcOut(TestModelBean testModelBean) {

                onPlayVideoFile(getAudioFile(_dumpAgcOut));
            }

            @Override
            public void onPlayHpfOut(TestModelBean testModelBean) {

                onPlayVideoFile(getAudioFile(_dumpHpfOut));
            }

            @Override
            public void onPlayArecord(TestModelBean testModelBean) {
                onPlayVideoFile(getAudioFile(_arecordFile));
            }
        });

        resultDialog.setRecordCallback(new TestResultDialog.IRecordCallback() {
            @Override
            public void onRecordStart(TestModelBean testModelBean) {
                if (testModelBean.getType() == TestModelBean.TYPE_ARECORD) {
                    if(_recordRequest==null){

                        return;
                    }
                    doARecord(true);
                } else {
                    doAudioRecord(true);
                }
            }

            @Override
            public void onRecordStop(TestModelBean testModelBean) {
                if (testModelBean.getType() == TestModelBean.TYPE_ARECORD) {
                    doARecord(false);
                } else {
                    doAudioRecord(false);
                }
            }

            @Override
            public void onDownload(TestModelBean testModelBean) {

                //主动下载文件
                if (TextUtils.isEmpty(_audioPath)) {
                    ToastUtil.s(getString(R.string.info_record_audio_file));
                    return;
                }
                if (testModelBean.getType() == TestModelBean.TYPE_ARECORD) {
                    //如果之前已经有录制文件，则删除重新下载
                    if (!_dumpFiles.isEmpty()) {
                        //删除之前的文件
                        deleteAudioFile();
                    }

                    downloadARecordFile();
                } else {
                    //如果之前已经有录制文件，则删除重新下载
                    if (!_dumpFiles.isEmpty()) {
                        //删除之前的文件
                        deleteAudioFile();
                    }

                    downloadAudioFile();
                }
            }
        });
    }

    public String getAudioFile(String fileName) {
        return AppFileUtil.getWeewaDownloadRoot() + "/" + ConnectLogic.getImpl().getDeviceId().hashCode() + "/" +
                fileName;
    }

    public void onPlayVideoFile(String filePath) {
        AppTrace.d(TAG, "onPlayVideoFile: " + filePath);

        wavPlay(filePath);
    }

    private void wavPlay(String filePath) {
        AppTrace.d(TAG, "wavPlay: " + filePath);
        if (_audioPlayer == null) {
            _audioPlayer = new WavPlayer();
            _audioPlayer.setOnCompletionListener(new WavPlayer.WavOnCompletionListener() {
                @Override
                public void onCompletion(boolean success) {

                }
            });
        } else {
            _audioPlayer.stop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        _audioPlayer.play(filePath, true);
    }

    public void setTypeStatus(int type, boolean pass) {
        for (TestModelBean testBean : _dataList) {
            if (testBean.getType() == type) {
                testBean.setStatus(pass ? TestModelBean.STATUS_PASS : TestModelBean.STATUS_FAIL);
                _adapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        _deviceInfo = data;
        _handler.removeCallbacks(_timeOutRunnable);

        if (!BaseAppUtil.isDestroy(this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DialogUtil.dismissDialog();
                    if (resultDialog != null && resultDialog.isShowing()) {
                        String extendInfo = resultDialog.getExtendInfo();
                        if (TextUtils.isEmpty(extendInfo)) {
                            resultDialog.addExtendInfo(String.format("相机时间：%s", TimeUtil.millis2YMDHMS(data._t)));
                        } else {
                            resultDialog.addExtendInfo(String.format("相机时间：%s", TimeUtil.millis2YMDHMS(data._t)));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceInfoFail() {

    }


    @Override
    public void onWifiList(List<WifiInfo> device) {

    }

    @Override
    public void onSetupStatus(int status, String message) {

    }

    @Override
    public void onTurnOff(int status, String message) {

    }

    private void showWifiSetupDialog() {
        String wifiInfo = AppFileUtil.loadStringFromFile(ft_wifi_file);
        if (!TextUtils.isEmpty(wifiInfo)) {
            try {
                _defaultWifiBean = new Gson().fromJson(wifiInfo, WifiBean.class);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (_wifiAddDialog != null && _wifiAddDialog.isShowing()) {
            _wifiAddDialog.dismiss();
        }
        _wifiAddDialog = null;

        _wifiAddDialog = new WifiAddDialog(TestFactoryNewActivity.this);
        _wifiAddDialog.setCallback(new WifiAddDialog.ICallback() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onConfirm(String name, String password) {

                WifiBean wifiBean = new WifiBean(name, password);

                AppFileUtil.saveString(GsonUtils.GsonString(wifiBean), ft_wifi_file);

                _wifiAddDialog.dismiss();
                //同步
                //DeviceApiManager.sendCmdSimpleObj(new NetInfoRequestBean());
            }
        });
        if (_defaultWifiBean != null) {
            _wifiAddDialog.setDefaultSsidAndPassword(_defaultWifiBean.ssid, _defaultWifiBean.password);
        }
        _wifiAddDialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaStart(DeviceWeewaStartEvent event) {
        AppTrace.d(TAG, "onWeewaStart");
        if (_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartUsbEvent(StartUsbEvent event) {
        ToastUtil.s(event.state == 1 ? "usb已打开" : "认证失败");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStopUsbEvent(StopUsbEvent event) {
        ToastUtil.s(event.state == 1 ? "usb已关闭" : "认证失败");
    }
}
