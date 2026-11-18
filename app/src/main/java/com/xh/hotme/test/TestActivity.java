package com.xh.hotme.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.ExecRequestBean;
import com.xh.hotme.bluetooth.bean.GetEthernetSettingsResponseBean;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.OTAStatusRequestBean;
import com.xh.hotme.bluetooth.bean.SetApnRequestBean;
import com.xh.hotme.bluetooth.bean.SoftwareInfoRequestBean;
import com.xh.hotme.bluetooth.bean.StartUsbRequestBean;
import com.xh.hotme.bluetooth.bean.StopUsbRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UploadLogRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.DeviceUsageInfoEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import com.xh.hotme.bluetooth.event.SetApnEvent;
import com.xh.hotme.bluetooth.event.SoftwareInfoEvent;
import com.xh.hotme.bluetooth.event.StartUsbEvent;
import com.xh.hotme.bluetooth.event.StopUsbEvent;
import com.xh.hotme.bluetooth.event.UnbindEvent;
import com.xh.hotme.bluetooth.event.UploadLogEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraUpgradeActivity;
import com.xh.hotme.camera.softap.CameraNetworkSettingActivity;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.camera.wifi.WifiListDialog;
import com.xh.hotme.camera.wifi.WifiPasswordDialog;
import com.xh.hotme.databinding.ActivityTestBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.me.camsetting.CameraSettingsActivity;
import com.xh.hotme.record.setting.SetDeviceSNDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.xh.hotme.test.EthernetSettingActivity;

@IEventBus
public class TestActivity extends BaseActivity<ActivityTestBinding> implements IWifiNotifyListener, IBleDeviceUsageInfoNotifyListener, IBleSoftApNotifyListener, IBleNetworkInfoNotifyListener, IRecordControlListener, IBleDeviceInfoNotifyListener, IBleWeewaInfoNotifyListener {

    private final static String TAG = TestActivity.class.getSimpleName();
    private UpdateSettingsExRequestBean _lastUpdateBean;

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    TextView tvStatus;

    WifiListDialog _wifiListDialog;
    WifiPasswordDialog _setupDialog;

    private WifiManager mWifiManager;

    ModalDialog _upgradeDialog;
    ModalDialog _networkDialog;

    DeviceInfo _deviceInfo;
    boolean getDeviceInfoAndPreview = false;

    final List<String> infoList = new ArrayList<>();

    final int maxLines = 10;

    String avs_mode = "0";

    private final Runnable _timeOutRunnable = () -> {
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, TestActivity.class);
        context.startActivity(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        String title = "测试";
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo != null && cameraInfo.deviceInfo != null) {
            title += "-" + cameraInfo.deviceInfo.device_name;
        }

        TitleLayoutHelper.initTitle(viewBinding.titleBar, title);
        if(CameraManager.getNetworkInfo(null) != null) {
            viewBinding.tvIp.setText("wifi ip: " + CameraManager.getNetworkInfo(null).getWifi().ip);
            viewBinding.tvImei.setText("IMEI: " + CameraManager.getNetworkInfo(null).getSim().imei);

            NetworkBean networkBean = CameraManager.getNetworkInfo(null);
            updateNetwork(networkBean);
        }

        viewBinding.btnBluetooth.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                BlueTestActivity.start(TestActivity.this);
                return true;
            }
        });

        viewBinding.btnFactory.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                TestFactoryNewActivity.start(TestActivity.this);
                return true;
            }
        });

        viewBinding.btnAutoRkipc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startActivity(new Intent(TestActivity.this, TestAutoActivity.class));
                return true;
            }
        });
        viewBinding.btnSetDeviceId.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
                if(deviceInfo != null) {
                    String deviceId = deviceInfo.device_id;
                    SetDeviceSNDialog setDeviceSNDialog = new SetDeviceSNDialog(TestActivity.this, deviceId);
                    setDeviceSNDialog.show();
                } else {
                    ToastUtil.s(getString(R.string.please_retry_on_device_info_fail));
                }
                return true;
            }
        });

        viewBinding.btnDeviceInfo.setOnClickListener(
            v -> startActivity(new Intent(TestActivity.this, TestDeviceActivity.class)));

        viewBinding.btnRtmp.setOnClickListener(v -> startActivity(new Intent(TestActivity.this, TestRtmpActivity.class)));

        viewBinding.btnMbrSetting.setOnClickListener(v -> startActivity(new Intent(TestActivity.this, TestMbrActivity.class)));

        viewBinding.btnVideoSetting.setOnClickListener(v -> startActivity(new Intent(TestActivity.this, TestVideoSettingActivity.class)));
        viewBinding.btnAudioNoiseSetting.setOnClickListener(v -> startActivity(new Intent(TestActivity.this, TestAudioNoiseSettingActivity.class)));
        viewBinding.btnIspSetting.setOnClickListener(v -> startActivity(new Intent(TestActivity.this, TestIspSettingActivity.class)));

        viewBinding.btnSetApn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DialogUtil.showDialog("");
                DeviceApiManager.sendCmdSimpleObj(new SetApnRequestBean(viewBinding.etApn.getText().toString().trim()));
                return true;
            }
        });

        viewBinding.btnDeviceUnbind.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startActivity(new Intent(TestActivity.this, TestUnBindActivity.class));
                return true;
            }
        });


        // 添加4G测速按钮
        viewBinding.btn4GSpeedTest.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startActivity(new Intent(TestActivity.this, Test4GSpeedActivity.class));
                return true;
            }
        });

        viewBinding.btnStartUsb.setOnClickListener(v -> DeviceApiManager.sendCmdSimpleObj(new StartUsbRequestBean()));
        viewBinding.btnStopUsb.setOnClickListener(v -> DeviceApiManager.sendCmdSimpleObj(new StopUsbRequestBean()));
        viewBinding.btnOtaUpdate.setOnClickListener(view -> doUpgrade());
        viewBinding.btnUploadLog.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DeviceApiManager.sendCmdSimpleObj(new UploadLogRequestBean());
                return true;
            }
        });
        viewBinding.btnLogSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startActivity(new Intent(TestActivity.this, TestLogSettingActivity.class));
                return true;
            }
        });
        viewBinding.btnOtherSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startActivity(new Intent(TestActivity.this, TestOtherSettingActivity.class));
                return true;
            }
        });
        viewBinding.btnTakeRaw.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                int count = StringUtils.safeParseInt(viewBinding.etRawCount.getText().toString());
                if(count <= 0) {
                    count = 1;
                }
                ExecRequestBean bean1 = new ExecRequestBean("echo 0 >/sys/devices/platform/rkcif-mipi-lvds2/compact_test", false);
                DeviceApiManager.sendCmdSimpleObj(bean1);
                ExecRequestBean bean = new ExecRequestBean(String.format("v4l2-ctl -d /dev/video0 --set-fmt-video width=4000,height=3000,pixelformat=RG10 --stream-mmap=3 --stream-skip=3 --stream-to=/userdata/cif0_%d.raw --stream-count=%d --stream-poll",
                    System.currentTimeMillis(), count), true);
                DeviceApiManager.sendCmdSimpleObj(bean);
                ToastUtil.s("抓raw图命令执行完成");
                return true;
            }
        });


        // 添加有线网络设置按钮
        viewBinding.btnEthernetSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                // 检查是否有有线网络连接
//                NetworkBean networkBean = CameraManager.getNetworkInfo(null);
//                if (networkBean != null && CameraManager.hasEthernet()) {
//                    startActivity(new Intent(TestActivity.this, EthernetSettingActivity.class));
//                } else {
//                    ToastUtil.s("未检测到有线网络连接");
//                }
                startActivity(new Intent(TestActivity.this, EthernetSettingActivity.class));
                return true;
            }
        });

        viewBinding.btnSettingAvsMode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {

                    String values = avs_mode.equals("0") ? "2" : "0";
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("avs:avs_mode", values);
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog("正在设置模式");
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {

                }
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("avs:ground_distance");
        bean.addKey("avs:stitch_distance");
        bean.addKey("avs:avs_mode");
        bean.addKey("video.1:zoom_width");
        bean.addKey("video.1:zoom_height");
        bean.addKey("video.1:zoom_width_5p");
        bean.addKey("video.1:zoom_height_5p");
        bean.addKey("video.1:width");
        bean.addKey("video.1:height");
        bean.addKey("video.2:width");
        bean.addKey("video.2:height");
        bean.addKey("audio.0:card_name");
        bean.addKey("audio:noise_reduction");
        bean.addKey("audio:noise_reduction_agc");
        bean.addKey("audio:noise_reduction_hpf");
        bean.addKey("audio.anr:noiseFactor");
        bean.addKey("audio.anr:swU");
        bean.addKey("audio.anr:PsiMin");
        bean.addKey("audio.anr:PsiMax");
        bean.addKey("audio.anr:fGmin");
        bean.addKey("audio.agc:attack_time");
        bean.addKey("audio.agc:release_time");
        bean.addKey("audio.agc:max_gain");
        bean.addKey("audio.agc:max_peak");
        bean.addKey("audio.agc:fRk0");
        bean.addKey("audio.agc:fRth0");
        bean.addKey("audio.agc:fRth1");
        bean.addKey("audio.agc:fRth2");
        bean.addKey("audio.hpf:Filter_type");
        bean.addKey("audio.hpf:Filter_mode");
        bean.addKey("audio.hpf:fc");
        bean.addKey("audio.hpf:bw");
        bean.addKey("isp.0.exposure:exposure_gain");
        bean.addKey("isp.0.exposure:exposure_time");
        bean.addKey("isp.0.adjustment:brightness");
        bean.addKey("isp.0.adjustment:contrast");
        bean.addKey("isp.0.adjustment:saturation");
        bean.addKey("isp.0.adjustment:hue");
        bean.addKey("isp.0.adjustment:sharpness");
        bean.addKey("rknn:track8model_version");
        bean.addKey("rknn:track11model_version");
        bean.addKey("rknn:bbmodel_version");
        bean.addKey("rknn:npmodel_version");
        bean.addKey("rknn:bsmodelPart1_version");
        bean.addKey("rknn:bsmodelPart2_version");
        bean.addKey("rknn:bs_batch_version");
        bean.addKey("rknn:bbnp_version");
        bean.addKey("rknn:bbbs_batch_version");
        DeviceApiManager.sendCmdSimpleObj(bean);
        DeviceApiManager.sendCmdSimpleObj(new GetSettingsRequestBean());
        DeviceApiManager.sendCmdSimpleObj(new SoftwareInfoRequestBean());
        DeviceApiManager.sendCmdSimpleObj(new OTAStatusRequestBean());
        DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());

        getDeviceTypeInfo();

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
            }
        },200);

    }

    @Override
    public void onWeewaInfo(WeewaInfo data) {
        AppTrace.d("asx","onWeewaInfo" + data.ethbean.ip + data.d.device_name);
    }

    @Override
    public void onWeewaInfoFail() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        DeviceApiManager.disConnect();
    }

    private void updateNetwork(NetworkBean networkBean){
        boolean hasSim = CameraManager.isSimValid();
        boolean hasWifi = CameraManager.hasWifi();
        boolean hasEth = CameraManager.hasEthernet();
        if(hasEth) {
            viewBinding.tvNetwork.setText("网络状态：有线网络" + getString(R.string.connected));
        } else if(hasWifi) {
            viewBinding.tvNetwork.setText("网络状态：WIFI, 信号强度" + networkBean.getSim().rssi_lv + " (0是最强，3是无信号)");
        } else if(hasSim) {
            viewBinding.tvNetwork.setText("网络状态：4G, 信号强度" + networkBean.getSim().rssi_lv + " (0是最强，3是无信号)");
        } else {
            viewBinding.tvNetwork.setText("网络状态：" + getString(R.string.without_network));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsageInfoEvent(DeviceUsageInfoEvent event) {
        viewBinding.tvUsageInfo.setText(event.deviceUsageInfo.toFormatString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkStatusEvent(NetworkStatusEvent event) {
        AppTrace.d(TAG, "onNetworkStatus");
        NetworkBean networkBean = event.n;
        updateNetwork(networkBean);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOTAStatusEvent(OTAStatusEvent event) {
        viewBinding.firmwareVersion.setText(String.format("固件版本: %s(%d)", event.versionName, event.versionCode));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadLogEvent(UploadLogEvent event) {
        if(!BleConstants.BLE_RESULT_SUCCESS.equals(event.status)) {
            ToastUtil.s(event.status);
        } else {
            ToastUtil.s("已请求上传log,请到obs端查看");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onWifiList(List<WifiInfo> device) {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            String data = device.toString();
            tvStatus.setText(data);

            _wifiListDialog = new WifiListDialog(TestActivity.this, "网络配置", device, new IWifiListAdapterCallback() {
                @Override
                public void onCancel() {

                }

                @Override
                public void onWifiClick(WifiInfo device1, int position) {
                    if(_setupDialog != null && _setupDialog.isShowing()) {
                        _setupDialog.dismiss();
                    }
                    _setupDialog = null;

                    _setupDialog = new WifiPasswordDialog(TestActivity.this, device1.getSsid());
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

        printInfo("配网情况：" + message);
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
        _handler.post(() -> {

            if(mWifiManager == null) {
                mWifiManager = WifiManager.getImpl();
            }

            mWifiManager.search(Constants.SOFTAP_REGEX, new WifiManager.SearchWifiListener() {
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
                        tvStatus.setText("未找到热点");
//                            showState(STATE.Layout_NoDevice);
                    } else {
                        showSoftApDialog(results.get(0));
                    }
                }
            });
        });
    }

    private void showSoftApDialog(WifiInfo wifiInfo) {
        ModalDialog dialog = new ModalDialog(TestActivity.this);
        dialog.setMessage(getString(R.string.wifi_soft_ap_join_title));
        dialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                tvStatus.setText("正在连接热点：" + wifiInfo.getSsid());
                if(mWifiManager != null) {
                    mWifiManager.connect(wifiInfo, "", new WifiManager.WifiConnectListener() {
                        @Override
                        public void onConnectStart(WifiInfo info) {
                            AppTrace.d(TAG, "connect ....");
                        }

                        @Override
                        public void onConnectFailed(WifiInfo info, int error) {
                            AppTrace.d(TAG, "connect fail....");
                        }

                        @Override
                        public void onConnectSuccess(WifiInfo info, int ip) {
                            AppTrace.d(TAG, "connect success....");
                            _handler.post(() -> {
                                ToastUtil.s(getString(R.string.connected_network));
                                tvStatus.setText("热点连接成功！");
                            });
                        }
                    });
                }
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

        viewBinding.tvIp.setText("wifi ip: " + networkBean.getWifi().ip);
        printInfo("网络状态：" + networkBean);
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
            if(!BaseAppUtil.isDestroy(TestActivity.this) && BaseAppUtil.isForegroundActivity(TestActivity.this, TestActivity.class.getName())) {
                if(getDeviceInfoAndPreview) {
                    PlayUtil.startPlay(TestActivity.this, data, null);
                    getDeviceInfoAndPreview = false;
                }
            }
        });
    }

    @Override
    public void onDeviceInfoFail() {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> tvStatus.setText("获取设备信息异常"));
    }

    private void printInfo(String showInfo) {
        AppTrace.d(showInfo);
        String time = new Date().toString();
        infoList.add(0, time + ": " + showInfo);
        if(infoList.size() > maxLines) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartUsbEvent(StartUsbEvent event) {
        ToastUtil.s(event.state == 1 ? "usb已打开" : "认证失败");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStopUsbEvent(StopUsbEvent event) {
        ToastUtil.s(event.state == 1 ? "usb已关闭" : "认证失败");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSoftwareInfoEvent(SoftwareInfoEvent event) {
        viewBinding.softwareVersion.setText(String.format("idle版本: %s\ndaemon版本: %s\nrkipc版本: %s",
            event.idle_v, event.daemon_v, TextUtils.isEmpty(event.rkipc_v) ? "至少启动一次rkipc才能显示版本" : event.rkipc_v));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingExEvent(WeewaSettingsExEvent event) {
        AppTrace.d(TAG, "onWeewaSettingExEvent");
        if(event == null || event.settings == null) {
            return;
        }
        if(event.has("rknn:track8model_version")) {
            try {

                viewBinding.modelVersion.setText(String.format("8model: %s\n11model: %s\nbbmodel: %s\nnpmodel: %s\nbsmodelPart1: %s\nbsmodelPart2: %s\nbs_batch_version: %s\nbbnp_version: %s\nbbbs_batch_version: %s",
                       event.get("rknn:track8model_version"), event.get("rknn:track11model_version"), event.get("rknn:bbmodel_version"), event.get("rknn:npmodel_version"), event.get("rknn:bsmodelPart1_version"),event.get("rknn:bsmodelPart2_version"),event.get("rknn:bs_batch_version"),event.get("rknn:bbnp_version"),event.get("rknn:bbbs_batch_version")));

            } catch(NumberFormatException e) {
            }
        }

        if (event.has("avs:avs_mode")) {
            try {
                String str = event.get("avs:avs_mode");
                if (str.equals("0")) {
                    viewBinding.btnSettingAvsMode.setText("设置标定模式");
                }else{
                    viewBinding.btnSettingAvsMode.setText("设置普通模式");
                }
                avs_mode = str;

            } catch(NumberFormatException e) {
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingsExUpdateEvent(WeewaSettingsExUpdateEvent event) {
        AppTrace.d(TAG, "onWeewaSettingsExUpdateEvent");
        DialogUtil.dismissDialog();
        if(event != null && event._status == 1) {
            ToastUtil.s(getString(R.string.info_update_success));

            // refresh settings
            if(_lastUpdateBean != null && _lastUpdateBean.has("avs:avs_mode")) {
                GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
                bean.addKey("avs:avs_mode");
                DeviceApiManager.sendCmdSimpleObj(bean);
            }
        } else {
            ToastUtil.s(getString(R.string.info_update_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnbindEvent(UnbindEvent event) {

        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetApn(SetApnEvent event) {
        AppTrace.d(TAG, "onSetApnEvent");
        AppTrace.d("asx","onSetApnEvent");
        DialogUtil.dismissDialog();
        ToastUtil.s(getString(R.string.setup_successful));
    }

    private void doUpgrade() {
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        OTAVersionInfo latestOtaInfo = CameraManager.getOTAVersion();
        if (latestOtaInfo != null) {
            int deviceOtaVersion = deviceInfo.ota_version_code;
            int latestOtaVersion = latestOtaInfo.getVersionCode();
            boolean needUpgrade = latestOtaVersion > deviceOtaVersion;
            if (!needUpgrade) {
                if (_upgradeDialog != null && _upgradeDialog.isShowing()) {
                    _upgradeDialog.dismiss();
                }
                _upgradeDialog = null;
                _upgradeDialog = new ModalDialog(TestActivity.this);
                _upgradeDialog.setTitle(getString(R.string.framework_is_latest));
                _upgradeDialog.hideMessage();
                _upgradeDialog.hideNegativeButton();
                _upgradeDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        return false;
                    }
                });

                _upgradeDialog.show();
                return;
            }
            if (!CameraManager.isNetworkValid()) {
                if (_networkDialog != null && _networkDialog.isShowing()) {
                    _networkDialog.dismiss();
                }
                _networkDialog = null;
                _networkDialog = new ModalDialog(TestActivity.this);
                _networkDialog.setTitle(getString(R.string.dialog_camera_network_title));
                _networkDialog.setMessage(getString(R.string.dialog_camera_network_message));
                _networkDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        return true;
                    }
                });
                _networkDialog.setPositiveButton(getString(R.string.network_config), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        Device connectDevice = BluetoothManager.getConnectDevice();
                        CameraNetworkSettingActivity.start(TestActivity.this, connectDevice, CameraNetworkSettingActivity.REQUEST_NETWORK_CONFIG);
                        return true;
                    }
                });
                _networkDialog.show();
            } else {
                if (!CameraManager.hasEthernet() && !CameraManager.hasWifi()) {
                    if (_networkDialog != null && _networkDialog.isShowing()) {
                        _networkDialog.dismiss();
                    }
                    _networkDialog = null;
                    _networkDialog = new ModalDialog(TestActivity.this);

                    _networkDialog.setTitle(getString(R.string.tip));
                    _networkDialog.setMessage(getString(R.string.setting_ota_camera_network_sim_tip));
                    _networkDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            return true;
                        }
                    });
                    _networkDialog.setPositiveButton(getString(R.string.upgrade), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            CameraUpgradeActivity.start(TestActivity.this, latestOtaInfo);
                            return true;
                        }
                    });
                    _networkDialog.show();
                    return;
                }
                CameraUpgradeActivity.start(TestActivity.this, latestOtaInfo);
            }
        } else {
            if (_upgradeDialog != null && _upgradeDialog.isShowing()) {
                _upgradeDialog.dismiss();
            }
            _upgradeDialog = null;
            _upgradeDialog = new ModalDialog(TestActivity.this);
            _upgradeDialog.setTitle(getString(R.string.framework_is_latest));
            _upgradeDialog.hideMessage();
            _upgradeDialog.hideNegativeButton();
            _upgradeDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    return false;
                }
            });

            _upgradeDialog.showOnCenter(40);
        }
    }

    public void getDeviceTypeInfo(){
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        if(deviceInfo == null || TextUtils.isEmpty(deviceInfo.device_id)){
            viewBinding.deviceType.setText("设备类型：未知");
            return;
        }
        // deviceTypeNo describes hardware; deviceType is the operational DT_* type.
        ApiUtil.getDeviceInfo(deviceInfo.device_id, new IGenericApiCallback<BindDeviceBean>() {
            @Override
            public void onSuccess(BindDeviceBean data) {
                viewBinding.deviceType.setText("设备类型：" +
                        DeviceTypeLabel.format(data == null ? null : data.getDeviceType()));
            }

            @Override
            public void onFail(String code, String message) {
                viewBinding.deviceType.setText("设备类型：获取失败");
            }

            @Override
            public void onFinish() {

            }
        });
    }
}
