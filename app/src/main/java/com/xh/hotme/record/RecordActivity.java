package com.xh.hotme.record;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.flyco.tablayout.listener.OnTabSelectListener;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BLEAware;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.LiveUrl;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.AutoUploadStatusRequestBean;
import com.xh.hotme.bluetooth.bean.EnergyInfoRequestBean;
import com.xh.hotme.bluetooth.bean.FecRtmpAddUrlRequestBean;
import com.xh.hotme.bluetooth.bean.FecRtmpRemoveAllUrlRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.LiveStatusRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStopRequestBean;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStopRequestBean;
import com.xh.hotme.bluetooth.bean.RoiOpRequestBean;
import com.xh.hotme.bluetooth.bean.SetTrainingModeRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApOpenRequestBean;
import com.xh.hotme.bluetooth.bean.StopAutoUploadRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import com.xh.hotme.bluetooth.event.DeviceEnergyInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceStorageInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceUsageInfoEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveAllUrlEvent;
import com.xh.hotme.bluetooth.event.LiveStatusResultEvent;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.event.RenameEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.event.RkIpcStopEvent;
import com.xh.hotme.bluetooth.event.SetTrainingModeEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.camera.ota.OTAUpdateDialog;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.databinding.ActivityRecordBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.RecordTaskManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.live.dialog.DevicesDialogUtils;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.record.score.ScoreCenterActivity;
import com.xh.hotme.record.setting.CameraInfoActivity;
import com.xh.hotme.record.video.CameraVideoListActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import tv.danmaku.ijk.media.player.IMediaPlayer;

@BLEAware
@IEventBus
@FullscreenLandscape(immersive = true)
public class RecordActivity extends BaseActivity<ActivityRecordBinding> implements IRecordControlListener, IBleSoftApNotifyListener, NetworkManager.OnNetworkStatusChangedListener {
    private static final int ZORDER_POWEROFF_DIALOG = 20000;
    private static final int ZORDER_CONFIRM_STOP_UPLOAD_DIALOG = 10000;
    private static final int ZORDER_OTA_DIALOG = 8000;
    private static final int ZORDER_SOFTAP_DIALOG = 5000;
    private static final int ZORDER_NETWORKTYPE_DIALOG = 4000;
    private final static String TAG = RecordActivity.class.getSimpleName();

    String _url = "rtmp://" + BleConstants.SOFT_AP_IP + ":1935/live/preview";
    final String _play_url = "rtmp://" + BleConstants.SOFT_AP_IP + ":1935/live/preview";
    final String _play_url_avs = "rtmp://" + BleConstants.SOFT_AP_IP + ":1935/live/preview_avs";

    String _url_push;
    String _url_pull;

    private boolean _deviceIsRecord = false;
    private boolean _deviceStatusChecked = false;
    private boolean _deviceStatusBackgroundCheck = false;

    private WifiManager mWifiManager;

    BluetoothLoadingDialog _connectingDialog;

    DeviceUsageInfo _deviceUsageInfo;

    DeviceInfo _deviceInfo;

    WifiInfo _wifiInfo;

    NetworkBean _networkInfo;

    ModalDialog _dialog;
    ModalDialog _backgroundExitDialog;
    ModalDialog _joinSoftApDialog;
    ModalDialog _startSoftApDialog;

    ModalDialog _networkTypeDialog;

    ModalDialog _restartRkipcDialog;
    ModalDialog _noEnoughSpaceDialog;
    private boolean _ignoreLowStorage = false;
    ModalDialog _stopAutoUploadDialog;

    OTAUpdateDialog otaUpdateDialog;

    boolean requestEnergy = false;
    boolean requestStorage = false;

    boolean isPause = false;
    boolean isPlay = false;
    boolean isPlayError = false;

    boolean isRkipcRunning = false;

    boolean isFactoryTest = false;

    int retryCount = 0;
    int timeoutCheckCount = 0;
    int rtmpUrlCheckCount = 0;

    //停止录制
    private boolean stopRequest = false;

    private boolean playError = false;
    private ModalDialog errorDialog;

    boolean isCheckOtaVersion = false;
    private final com.xh.hotme.camera.ota.CameraOtaCheck otaPromptCheck =
            new com.xh.hotme.camera.ota.CameraOtaCheck();

    int _recordTime = 0;
    long _recordStatusSyncTime = 0;
    boolean _lastConnected = false;

    int connectSoftApAction = 0; //1 for video

    RecordInfoBean _recordInfoBean;

    RecordStatusResultBean _recordStatus;
    private double _roi_scale = 1;
    private boolean _pendingSetTrainingMode = false;

    private boolean _showVideoControl = false;

    private int _3rdPlt = Constants.LIVE_PLATFORM_XQM;

    private String[] mTitles;

    private final Runnable _timeOutRunnable = () -> {
        dismissConnectingDialog();
    };


    private final Runnable _openSoftApRunnable = () -> ToastUtil.s(info_timeout_open_soft_ap);

    private final Runnable _energyTimeOutRunnable = () -> {
        requestEnergy = false;
    };

    private final Runnable _playLoadingCountDownRunnable = () -> viewBinding.loading.getRoot().setVisibility(View.GONE);


    private final Runnable _recordTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateUnattendedStatus();
            if(_deviceIsRecord) {
                _recordTime += 1;
                runOnUiThread(() -> viewBinding.recordTime.setText(TimeUtil.intToTimeMin(_recordTime)));
                if(_recordStatus != null) {
                    _recordStatus.setDuration(_recordTime);
                }
            }
            _handler.postDelayed(_recordTimeRunnable, 1000);
        }
    };

    private final Runnable _startRkipcTimeOutRunnable = () -> {
        dismissConnectingDialog();
        ToastUtil.s(info_timeout_on_lunch_rkipc);
    };

    // 在网络不可用时要刷新状态,因为网络不可用也有可能是相机那边出了啥问题,所以我们需要知道
    // 相机的状态以便能矫正app这边的逻辑
    private boolean _cameraNetMaybeBroken = false;
    private final Runnable _updateStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if(_cameraNetMaybeBroken) {
                DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
            }
            _handler.postDelayed(_updateStatusRunnable, 5000);
        }
    };

    private static String info_timeout_on_lunch_rkipc = "";
    private static String info_timeout_open_soft_ap = "";

    private static String str_lunch_rkipc_fail;



    public static void start(Context context, DeviceInfo deviceInfo, Device device) {
        Intent intent = new Intent(context, RecordActivity.class);
        intent.putExtra(Constants.REQUEST_DEVICE_INFO, deviceInfo);
        intent.putExtra(Constants.REQUEST_DEVICE, device);
        context.startActivity(intent);
    }

    public static void start(Context context, boolean isFactoryTest) {
        Intent intent = new Intent(context, RecordActivity.class);
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        Device device = BluetoothManager.getConnectDevice();
        intent.putExtra(Constants.REQUEST_FACTORY_TEST, isFactoryTest);
        intent.putExtra(Constants.REQUEST_DEVICE_INFO, GsonUtils.GsonString(deviceInfo));
        intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
        context.startActivity(intent);
    }


    public static void start(Context context, DeviceInfo deviceInfo, Device device, NetworkBean network) {
        Intent intent = new Intent(context, RecordActivity.class);
        if(deviceInfo != null) {
            intent.putExtra(Constants.REQUEST_DEVICE_INFO, GsonUtils.GsonString(deviceInfo));
        }
        if(device != null) {
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
        }

        if(network != null) {
            intent.putExtra(Constants.REQUEST_NETWORK_INFO, GsonUtils.GsonString(network));
        }

        context.startActivity(intent);
    }

    public static void start(Context context, DeviceInfo deviceInfo, Device device, String network) {
        Intent intent = new Intent(context, RecordActivity.class);
        if(deviceInfo != null) {
            intent.putExtra(Constants.REQUEST_DEVICE_INFO, GsonUtils.GsonString(deviceInfo));
        }
        if(device != null) {
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
        }
        intent.putExtra(Constants.REQUEST_NETWORK_INFO, network);
        context.startActivity(intent);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppTrace.d(TAG, "onSaveInstanceState...");
        outState.putSerializable(Constants.REQUEST_DEVICE_INFO, _deviceInfo);
        outState.putSerializable(Constants.REQUEST_DEVICE, _device);
        outState.putString(Constants.REQUEST_NETWORK_INFO, GsonUtils.GsonString(_networkInfo));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(savedInstanceState != null) {
            isFactoryTest = savedInstanceState.getBoolean(Constants.REQUEST_FACTORY_TEST);
            _deviceInfo = (DeviceInfo) savedInstanceState.getSerializable(Constants.REQUEST_DEVICE_INFO);
            _device = (Device) savedInstanceState.getSerializable(Constants.REQUEST_DEVICE);
            setDevice(_device);
            String networkInfo = getIntent().getStringExtra(Constants.REQUEST_NETWORK_INFO);
            if(!TextUtils.isEmpty(networkInfo)) {
                _networkInfo = GsonUtils.GsonToBean(networkInfo, NetworkBean.class);
            }
            if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
                NetworkManager.getImpl().updateWithNetworkInfo(_networkInfo);
            }
        }

        if(getIntent() != null) {
            isFactoryTest =  getIntent().getBooleanExtra(Constants.REQUEST_FACTORY_TEST, false);

            String deviceInfo = getIntent().getStringExtra(Constants.REQUEST_DEVICE_INFO);
            if(!TextUtils.isEmpty(deviceInfo)) {
                _deviceInfo = GsonUtils.GsonToBean(deviceInfo, DeviceInfo.class);
            }
            String device = getIntent().getStringExtra(Constants.REQUEST_DEVICE);
            if(!TextUtils.isEmpty(device)) {
                _device = GsonUtils.GsonToBean(device, Device.class);
                setDevice(_device);
            }

            String networkInfo = getIntent().getStringExtra(Constants.REQUEST_NETWORK_INFO);
            if(!TextUtils.isEmpty(networkInfo)) {
                _networkInfo = GsonUtils.GsonToBean(networkInfo, NetworkBean.class);
            }

            if(_networkInfo != null && _networkInfo.getAp() != null && !TextUtils.isEmpty(_networkInfo.getAp().ssid)) {
                NetworkManager.getImpl().updateWithNetworkInfo(_networkInfo);
            }
        }
        info_timeout_on_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_rkipc);
        info_timeout_open_soft_ap = getString(R.string.info_running_timeout_on_lunch_soft_ap);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);

        _handler.postDelayed(_recordTimeRunnable, 1000);
        _handler.postDelayed(_updateStatusRunnable, 2000);

        if(_deviceInfo != null && !TextUtils.isEmpty(_deviceInfo.device_name)) {
            viewBinding.tvTitle.setText(BluetoothManager.getDisplayDeviceName(_deviceInfo.device_name));
        }

        startRkIPC();

        BluetoothHandle.addPlayNotifyListener(this);
        BluetoothHandle.addSoftApNotifyListener(this);

        _handler.postDelayed(() -> CameraMonitor.getImpl().start(), 2000);

        // 监听网络联通情况, 断开时停止ijkplayer, 重连时重新播放预览, 看看能不能解决关闭wifi再打开时的画面卡住问题
        NetworkManager.getImpl().registerNetworkStatusChangeListener(this, false);
    }

    private boolean checkOTAVersion() {
        DeviceInfo device = CameraManager.getDeviceInfo();
        if (device == null || (!CameraManager.hasWifi() && !CameraManager.hasEthernet())) return false;
        if (!Constants.shouldCheckOTAAlways()
                && TimeUtil.isToday(SharedPrefs.getImpl().getLastOTAPromptTime(device.device_id))) return false;
        otaPromptCheck.start(() -> {
            if (_deviceIsRecord) {
                isCheckOtaVersion = false; // Retry when the camera becomes idle again.
                return;
            }
            if (!isFinishing()) showCameraOTAPrompt();
        });
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCameraOtaStatus(com.xh.hotme.bluetooth.event.OTAStatusEvent event) {
        otaPromptCheck.onStatus(event);
    }

    private boolean showCameraOTAPrompt() {
        if(_deviceInfo == null) return false;
        // if no wifi or ethernet, don't check ota
        if(!CameraManager.hasWifi() && !CameraManager.hasEthernet()) {
            return false;
        }

        // 一天只检查一次ota, 今天如果检查过就跳过
        long t = SharedPrefs.getImpl().getLastOTAPromptTime(_deviceInfo.device_id);
        if(!Constants.shouldCheckOTAAlways() && TimeUtil.isToday(t)) {
            return false;
        }

        OTAVersionInfo latestOtaInfo = CameraManager.getOTAVersion();
        if(latestOtaInfo != null) {
            int deviceOtaVersion = _deviceInfo.ota_version_code;
            int latestOtaVersion = latestOtaInfo.getVersionCode();
            boolean needUpgrade = latestOtaVersion > deviceOtaVersion;
            if(needUpgrade) {
                if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
                    otaUpdateDialog.dismiss();
                }
                otaUpdateDialog = null;
                otaUpdateDialog = new OTAUpdateDialog(RecordActivity.this, latestOtaInfo);
                otaUpdateDialog.setZOrder(ZORDER_OTA_DIALOG);
                otaUpdateDialog.setCallback(new OTAUpdateDialog.ICallback() {
                    @Override
                    public void onCancel() {
                        if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
                            otaUpdateDialog.dismiss();
                        }
                    }

                    @Override
                    public void onConfirm() {
                        // 确认升级的话需要清空提示时间, 这样升级完再进来会再提示ota
                        SharedPrefs.getImpl().saveLastOTAPromptTime(_deviceInfo.device_id, 0);
                        CameraManager.clearOTAInfo();
                        if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
                            otaUpdateDialog.dismiss();
                        }
                        onFinish();
                    }
                });
                dialogStack.push(otaUpdateDialog);
                SharedPrefs.getImpl().saveLastOTAPromptTime(_deviceInfo.device_id, System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    private void startRkIPC() {
        AppTrace.d(TAG, "sendCmdStartRkipc");
        boolean ok = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
        if(ok) {
            _handler.postDelayed(_startRkipcTimeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
    }

    private boolean isConnectedSoftAp() {
        _lastConnected = NetworkManager.getImpl().isConnectedSoftAp();
        return _lastConnected;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetTrainingMode(SetTrainingModeEvent e) {
        dismissLoading();
        updateVideoControlUI();
        String switchMode = getString(R.string.switch_mode_to);
        ToastUtil.s(String.format(switchMode, isTrainingMode() ? getString(R.string.camera_mode_type_training) : getString(R.string.camera_mode_type_normal)));
    }

    @SuppressLint("DefaultLocale")
    protected void initView() {
        AppTrace.d(TAG, "initView...");

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
            viewBinding.tvPreviewTips.setText(R.string.record_preview_connect_remote);
        }

        mTitles = getResources().getStringArray(R.array.record_preview_type);
        viewBinding.tabs.setTabData(mTitles);
        viewBinding.layoutEnergy.setEnabled(false);

        viewBinding.btnToggleTrainingMode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showLoading();
                if(_recordStatus == null) {
                    _pendingSetTrainingMode = true;
                    DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                } else {
                    boolean v = !isTrainingMode();
                    _recordStatus.setTrainingMode(v ? 1 : 0);
                    DeviceApiManager.sendCmdSimpleObj(new SetTrainingModeRequestBean(v));
                }
                return true;
            }
        });
        viewBinding.btnShowVideoControl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _showVideoControl = true;
                updateVideoControlUI();
                return true;
            }
        });
        viewBinding.btnHideVideoControl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _showVideoControl = false;
                updateVideoControlUI();
                return true;
            }
        });
        viewBinding.btnFullscreen.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                requestOrientation(isPortrait);

                return true;
            }
        });
        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_deviceIsRecord) {
                    showBackgroundRunDialog(false);
                } else {
                    onFinish();
                }
                return true;
            }
        });

        viewBinding.btnExit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_deviceIsRecord) {
                    showBackgroundRunDialog(true);
                } else {
                    onFinish();
                }
                return true;
            }
        });
        viewBinding.btnExit.setEnabled(false);
        viewBinding.btnExitLabel.setEnabled(false);
        viewBinding.btnExitIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_gray_76)));

        viewBinding.btnIspSettings.setOnClickListener(v -> {
            viewBinding.ispSettingsContainer.setVisibility(viewBinding.ispSettingsContainer.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        });
        viewBinding.ispSettingsContainer.setOnClickListener(v -> {
            viewBinding.ispSettingsContainer.setVisibility(View.GONE);
        });
        viewBinding.btnResetDefaultIspSettings.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:brightness", "50");
                bean.addSetting("isp.0.adjustment:contrast", "50");
                bean.addSetting("isp.0.adjustment:saturation", "50");
                bean.addSetting("isp.0.adjustment:hue", "50");
                bean.addSetting("isp.0.adjustment:sharpness", "50");
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_recovery_default));
                    viewBinding.tvBrightness.setText("50");
                    viewBinding.tvBrightness2.setText("50");
                    viewBinding.tvSaturation.setText("50");
                    viewBinding.tvContrast.setText("50");
                    viewBinding.tvHue.setText("50");
                    viewBinding.tvSharpness.setText("50");
                    viewBinding.sliderBrightness.setProgress(50);
                    viewBinding.sliderBrightness2.setProgress(50);
                    viewBinding.sliderSaturation.setProgress(50);
                    viewBinding.sliderContrast.setProgress(50);
                    viewBinding.sliderHue.setProgress(50);
                    viewBinding.sliderSharpness.setProgress(50);
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

        viewBinding.sliderBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvBrightness.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int brightness = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:brightness", String.valueOf(brightness));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_brightness));
                } else {
                    showDisconnectToast();
                }
            }
        });
        viewBinding.sliderBrightness2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvBrightness2.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int brightness = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:brightness", String.valueOf(brightness));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_brightness));
                } else {
                    showDisconnectToast();
                }
            }
        });
        viewBinding.sliderSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvSaturation.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int v = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:saturation", String.valueOf(v));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_saturation));
                } else {
                    showDisconnectToast();
                }
            }
        });
        viewBinding.sliderContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvContrast.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int v = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:contrast", String.valueOf(v));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_contrast));
                } else {
                    showDisconnectToast();
                }
            }
        });
        viewBinding.sliderHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvHue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int v = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:hue", String.valueOf(v));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_aberration));
                } else {
                    showDisconnectToast();
                }
            }
        });
        viewBinding.sliderSharpness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.tvSharpness.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int v = seekBar.getProgress();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:sharpness", String.valueOf(v));
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showLoading(getString(R.string.info_camere_setting_acutance));
                } else {
                    showDisconnectToast();
                }
            }
        });

        viewBinding.btnScoreCenter.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_recordStatus == null || _recordStatus.getStatus() != 1) {
                    ToastUtil.s(getString(R.string.info_firstly_lunch_record));
                    return true;
                }

                if(_recordInfoBean == null) {
                    ToastUtil.s(getString(R.string.info_firstly_write_record_info));
                    return true;
                }

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                ScoreCenterActivity.start(RecordActivity.this, _recordInfoBean);

                return true;
            }
        });
        viewBinding.btnScoreCenter.setEnabled(false);
        viewBinding.tvScoreSetting.setEnabled(false);

        viewBinding.btnCameraVideo.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (!isConnectedSoftAp()) {
                        connectSoftApAction = 1;
                        startSoftAp();
                    } else {
                        startCameraVideoListActivity();
                    }
                }else{
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        ToastUtil.l(R.string.error_network_error);
                        return true;
                    }
                    startCameraVideoListActivity();
                }

                return true;
            }
        });

        viewBinding.flCameraSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    boolean ok = BluetoothManager.getImpl().isConnected();
                    if (!ok) {
                        showBlueToothDisconnectDialog();
                        return true;
                    }
                } else {
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        ToastUtil.l(R.string.error_network_error);
                        return true;
                    }
                }
                CameraInfoActivity.start(RecordActivity.this, _deviceUsageInfo, _deviceInfo, _device);
                return true;
            }
        });

        viewBinding.btnRecorder.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                // 查询存储空间是否过低
                if(!_deviceIsRecord) {
                    if(!_ignoreLowStorage && !checkStorage()) return true;
                }

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    // Protocol 4 controls run over Bluetooth even while the
                    // phone stays on its Internet connection for authorization.
                    if (_recordStatus != null && _recordStatus.localControlSupported == 1) {
                        if (!BluetoothManager.getImpl().isConnected()) {
                            showBlueToothDisconnectDialog();
                        } else if (_deviceIsRecord) {
                            showStopRecordDialog(getString(R.string.stop_record_title));
                        } else {
                            startRecord();
                        }
                        return true;
                    }

                    if (isConnectedSoftAp()) {
                        if (!NetUtil.isWifi(RecordActivity.this)) {
                            showNetworkTypeDialog();
                            return true;
                        }

                        if (!_deviceStatusChecked) {
                            _deviceStatusBackgroundCheck = false;
                            boolean ok = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                            if (ok) {
                                showConnectingDialog(RecordActivity.this.getString(R.string.record_status));
                                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                            } else {
                                showDisconnectToast();
                                showBlueToothDisconnectDialog();
                            }
                        } else {
                            if (_deviceIsRecord) {
                                showStopRecordDialog(getString(R.string.stop_record_title));
                            } else {
                                startRecord();
                            }
                        }

                    } else {
                        if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                            startSoftAp();
                        } else {
                            showJoinSoftApDialog();
                        }
                    }
                } else{
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        ToastUtil.l(R.string.error_network_error);
                        return true;
                    }

                    if (!_deviceStatusChecked) {
                        _deviceStatusBackgroundCheck = false;
                        boolean ok = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                        if (ok) {
                            showConnectingDialog(RecordActivity.this.getString(R.string.record_status));
                            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                        } else {
                            showDisconnectToast();
                            showBlueToothDisconnectDialog();
                        }
                    } else {
                        if (_deviceIsRecord) {
                            showStopRecordDialog(getString(R.string.stop_record_title));

                        } else {
                            startRecord();
                        }
                    }

                }
                return true;
            }
        });

        viewBinding.layoutEnergy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!requestEnergy) {
                    requestEnergy = true;
                    boolean ok = DeviceApiManager.sendCmdSimpleObj(new EnergyInfoRequestBean());
                    if(ok) {
                        _handler.postDelayed(_energyTimeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    }
                } else {
                    ToastUtil.s(getString(R.string.info_searching_and_dont_repeat_do_it));
                }
                return true;
            }
        });

        viewBinding.player.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!isPlay) {
                    startPlay();
                    retryCount = 0;
                }
                return true;
            }
        });
        viewBinding.tabs.setCurrentTab(1);
        viewBinding.tabs.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                AppTrace.d(TAG, String.format("选择的是第%d个", position));
                if(position == 1) {
                    if(!_url.equalsIgnoreCase(_play_url)) {
                        _url = _play_url;
                        if(!isConnectedSoftAp()) {
                            if(NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                                if(_lastConnected) {
                                    showSoftApDisconnectDialog();
                                } else {
                                    showJoinSoftApDialog();
                                }
                            }
                            return;
                        } else {
                            if(!NetUtil.isWifi(RecordActivity.this)) {
                                showNetworkTypeDialog();
                                return;
                            }
                        }

                        startPlay();
                        retryCount = 0;
                    }
                } else {
                    if(!_url.equalsIgnoreCase(_play_url_avs)) {
                        _url = _play_url_avs;
                        if(!isConnectedSoftAp()) {
                            if(NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                                if(_lastConnected) {
                                    showSoftApDisconnectDialog();
                                } else {
                                    showJoinSoftApDialog();
                                }
                            }
                            return;
                        } else {
                            if(!NetUtil.isWifi(RecordActivity.this)){
                                showNetworkTypeDialog();
                                return;
                            }
                        }
                        startPlay();
                        retryCount = 0;
                    }
                }

                boolean ok = BluetoothManager.getImpl().isConnected();
                if(!ok) {
                    showBlueToothDisconnectDialog();
                }
            }

            @Override
            public void onTabReselect(int position) {

            }
        });

        viewBinding.tabs.setVisibility(Constants.ACCESS_MODEL== Constants.ACCESS_MODEL_REMOTE? View.GONE:View.VISIBLE);

        viewBinding.joystick.setOnMoveListener((angle, strength) -> {
            double rad = angle * Math.PI / 180;
            double x = Math.cos(rad) * strength;
            double y = -Math.sin(rad) * strength;
            DeviceApiManager.sendCmdSimpleObj(new RoiOpRequestBean(RoiOpRequestBean.ROI_OP_FREE_MOVE, x, y));
        }, 333);
        viewBinding.resetRoi.setOnClickListener(view -> {
            _roi_scale = 1;
            viewBinding.tvScaleRatio.setText(String.format("%dX", (int) _roi_scale));
            DeviceApiManager.sendCmdSimpleObj(new RoiOpRequestBean(RoiOpRequestBean.ROI_OP_RESET, 0, 0));
        });
        viewBinding.scaleRoi.setOnClickListener(view -> {
            _roi_scale += 1;
            if(_roi_scale > 3) {
                _roi_scale = 1;
            }
            viewBinding.tvScaleRatio.setText(String.format("%dX", (int) _roi_scale));
            DeviceApiManager.sendCmdSimpleObj(new RoiOpRequestBean(RoiOpRequestBean.ROI_OP_FREE_SCALE, _roi_scale, 0));
        });

        updateVideoControlUI();
    }

    @Override
    protected void initData() {
        // check auto upload status
        DeviceApiManager.sendCmdSimpleObj(new AutoUploadStatusRequestBean());

        // get isp settings
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("isp.0.adjustment:brightness");
        bean.addKey("isp.0.adjustment:contrast");
        bean.addKey("isp.0.adjustment:saturation");
        bean.addKey("isp.0.adjustment:hue");
        bean.addKey("isp.0.adjustment:sharpness");
        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    private boolean checkStorage() {
        if(_deviceUsageInfo == null && CameraMonitor.getImpl().getDeviceUsageInfo() == null) {
            // 如果相机的存储信息还没有获得, 我觉得这里只能是提示一下继续等待
            ToastUtil.s(getString(R.string.cannot_record_because_camera_not_ready));
            return false;
        } else {
            // 如果可用空间小于2g,要提示用户去删除视频
            long free = 0;
            if(_deviceUsageInfo != null) {
                free = _deviceUsageInfo.storage.free;
            } else {
                free = CameraMonitor.getImpl().getDeviceUsageInfo().storage.free;
            }
            if(free <= 2L * 1024L * 1024L * 1024L) {
                showNoEnoughSpaceDialog(true);
                return false;
            } else if(free <= 5L * 1024L * 1024L * 1024L) {
                showNoEnoughSpaceDialog(false);
                return false;
            }
        }
        return true;
    }

    private void updateRecordStatus(boolean isRecord) {
        if(isRecord) {
            viewBinding.btnScoreCenter.setEnabled(true);
            viewBinding.tvScoreSetting.setEnabled(true);
            viewBinding.indicator.setVisibility(View.VISIBLE);
            viewBinding.btnRecorder.setText(getResources().getText(R.string.stop_record));
        } else {
            viewBinding.btnScoreCenter.setEnabled(false);
            viewBinding.tvScoreSetting.setEnabled(false);
            viewBinding.indicator.setVisibility(View.GONE);
            viewBinding.btnRecorder.setText(getResources().getText(R.string.start_record));
            viewBinding.recordTime.setText("");
        }
        updateUnattendedStatus();
    }

    private void updateUnattendedStatus() {
        UnattendedRecordTask task = UnattendedRecordService.current(this);
        long now = System.currentTimeMillis();
        if (task != null && task.isSelectedDevice() && !_deviceIsRecord && now < task.startAt) {
            viewBinding.btnRecorder.setText(R.string.unattended_waiting_button);
            viewBinding.recordTime.setText(UnattendedRecordTask.remainingTime(task.startAt - now));
            return;
        }
        viewBinding.btnRecorder.setText(_deviceIsRecord ? R.string.stop_record : R.string.start_record);
        viewBinding.recordTime.setText(_deviceIsRecord ? TimeUtil.intToTimeMin(_recordTime) : "");
    }


    private void startRecord() {
        //球场管理员版本才会有这个功能
        if(Constants.ROLE.equalsIgnoreCase("greenKeeper")) {
            List<HmCompetitionRecordBean> taskList = RecordTaskManager.loadTaskList();
            if (taskList.isEmpty()) {
                //进入填写录制信息页面
                AddRecordTaskActivity.start(RecordActivity.this);
            } else {
                Intent intent = new Intent(RecordActivity.this, RecordTaskListActivity.class);
                RecordActivity.this.startActivity(intent);
            }
        } else {
            //进入填写录制信息页面
            AddRecordTaskActivity.start(RecordActivity.this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppTrace.d(TAG, "onPause...");

        CameraMonitor.getImpl().pause();

        isPause = true;
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            //停止播放
            if (viewBinding.player.isPlaying()) {
                viewBinding.player.pause();
                viewBinding.player.stopPlayback();
                viewBinding.player.release(false);
                viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
            }
            isPlay = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume...");
        updateUnattendedStatus();
        isPause = false;
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {

            boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(RecordActivity.this);
            if (!hasLocationSer) {
                PermissionsUtil.openLocationServiceForResult(RecordActivity.this, PermissionsUtil.REQUEST_LOCATION_SERVICE);
                return;
            }

            boolean hasPermission = PermissionsUtil.hasBlePermission(RecordActivity.this);
            if (!hasPermission) {
                EasyPermissions.requestPermissions(RecordActivity.this, getString(R.string.bluetooth_permission_title), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                return;
            }

            CameraMonitor.getImpl().resume();

            boolean connected = isConnectedSoftAp();
            AppTrace.d(TAG, String.format("softap connected=%b", connected));
            if (connected) {
                if (NetUtil.isWifi(RecordActivity.this)) {
                    //关闭连接对话框
                    dismissDisconnectDialog();

                    dismissJoinSoftApDialog();

                    if (!isPlay) {
                        startPlay();
                        retryCount = 0;
                    }
                    _deviceStatusBackgroundCheck = true;
                    DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());

                } else {
                    showNetworkTypeDialog();
                }
            } else {
//            viewBinding.previewPlayer.setVisibility(View.VISIBLE);
                if (NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                    if (_lastConnected) {
                        showSoftApDisconnectDialog();
                    } else {
                        showJoinSoftApDialog();
                    }
                }
            }
        } else {
            if(DevicesDialogUtils.openWifiSetting(RecordActivity.this)){
                return;
            }
            //同步一下录制状态
            DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());

            //如果处于直播中，继续播放
            if(!TextUtils.isEmpty(_url_pull)) {
                _url = _url_pull;
                startPreview();
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        AppTrace.d(TAG, "onStop...");
    }

    @Override
    protected void onDestroy() {
        otaPromptCheck.cancel();
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy...");

        CameraMonitor.getImpl().stop();

        if(viewBinding.player != null) {
            viewBinding.player.pause();
            viewBinding.player.release(true);
        }

        BluetoothHandle.removePlayNotifyListener(this);
        BluetoothHandle.removeSoftApNotifyListener(this);
        NetworkManager.getImpl().removeNetworkStatusChangeListener(null, this);

        dismissDisconnectDialog();

        dismissJoinSoftApDialog();
        dismissStopAutoUploadDialog();

        if(_startSoftApDialog != null && _startSoftApDialog.isShowing()) {
            _startSoftApDialog.dismiss();
        }
        if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
            otaUpdateDialog.dismiss();
        }
        if(_noEnoughSpaceDialog != null && _noEnoughSpaceDialog.isShowing()) {
            _noEnoughSpaceDialog.dismiss();
        }
        if(_networkTypeDialog != null && _networkTypeDialog.isShowing()) {
            _networkTypeDialog.dismiss();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(!isPortrait) {
            // 如果当前是横屏，则切换为竖屏
            requestOrientation(false);
        } else {
            showExitPlayerDialog();
        }
    }

    private void startCameraVideoListActivity() {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (isConnectedSoftAp()) {
                ConnectLogic.getImpl().setSsid(NetworkManager.getImpl().getSoftApSsid());
                ConnectLogic.getImpl().setSsidPwd(NetworkManager.getImpl().getSoftApPassword());
                if (_deviceInfo != null) {
                    if (_device != null) {
                        ConnectLogic.getImpl().setDeviceMac(_device.address);
                    }
                    ConnectLogic.getImpl().setDeviceId(_deviceInfo.device_id);
                } else {
                    if (_device != null) {
                        ConnectLogic.getImpl().setDeviceMac(_device.address);
                    }
                    String deviceId = CameraManager.getDeviceId();
                    AppTrace.d(TAG, "set ConnectLogic deviceId = " + deviceId);
                    ConnectLogic.getImpl().setDeviceId(deviceId);
                }
                CameraVideoListActivity.start(RecordActivity.this);
            } else {
                if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                    startSoftAp();
                } else {
                    String title = getString(R.string.join_softap_title);
                    showSoftApDialog(String.format(title, NetworkManager.getImpl().getSoftApSsid()), Constants.REQUEST_CODE_WIFI_CAMERA_VIDEO);
                }
            }
        } else {
            CameraVideoListActivity.start(RecordActivity.this);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
        AppTrace.d(TAG, "onActivityResult: request code=" + requestCode);
        if(isConnectedSoftAp()) {
            dismissJoinSoftApDialog();
            dismissDisconnectDialog();
        }
        if(requestCode == Constants.REQUEST_CODE_WIFI_CAMERA_VIDEO) {
            startCameraVideoListActivity();
        }
    }

    private void stopPlay() {
        if(viewBinding.player.isPlaying()) {
            viewBinding.player.pause();
            viewBinding.player.release(false);
            viewBinding.player.setRender(1);
        }
        isPlay = false;
    }

    private void startPlay() {
        AppTrace.d(TAG, "startPlay...");
        String localIp = NetUtil.getLocalIpAddress();
        AppTrace.d(TAG, "local ip: " + localIp);
        if(Constants.ACCESS_MODEL ==  Constants.ACCESS_MODEL_REMOTE) {
            if (isPause) {
                AppTrace.d(TAG, "skip play...");
                return;
            }
        }

        viewBinding.playerCover.setVisibility(View.GONE);

        if(viewBinding.player.isPlaying()) {
//            viewBinding.playerView.stopPlayback();
            viewBinding.player.release(false);
            viewBinding.player.setRender(1);
        }
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if(TextUtils.isEmpty(_url)){
                AppTrace.d(TAG, "skip play, url is empty");
                return;
            }
            viewBinding.player.setVideoPath(_url);
        } else {
            if(TextUtils.isEmpty(_url_pull)){
                AppTrace.d(TAG, "skip play, pull url is empty");
                return;
            }
            viewBinding.player.setVideoPath(_url_pull);
        }
        viewBinding.player.start();
        AppTrace.d(TAG, "jkplayerView start...");
//        viewBinding.playerView.setVisibility(View.GONE);
        viewBinding.loading.getRoot().setVisibility(View.VISIBLE);

        _handler.removeCallbacks(_playLoadingCountDownRunnable);
        _handler.postDelayed(_playLoadingCountDownRunnable, 20000);

        viewBinding.player.setOnInfoListener((iMediaPlayer, states, i1) -> {
            AppTrace.d(TAG, "onInfo state: " + states);

            if(states == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                viewBinding.loading.getRoot().setVisibility(View.VISIBLE);
            } else if(states == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                viewBinding.loading.getRoot().setVisibility(View.GONE);
                _handler.removeCallbacks(_playLoadingCountDownRunnable);
            } else if(states == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START
                || states == IMediaPlayer.MEDIA_ERROR_TIMED_OUT
            ) {
                viewBinding.loading.getRoot().setVisibility(View.GONE);
                _handler.removeCallbacks(_playLoadingCountDownRunnable);
                isPlayError = false;
            }

            return true;
        });
        viewBinding.player.setOnErrorListener((iMediaPlayer, what, extra) -> {
            AppTrace.d(TAG, "onError state: " + what);
            if(what == CmoIjkVideoView.MEDIA_ERROR_IJK_PLAYER) {
                // 出现-10000错误时,内部状态需要重置,并重新刷一下相机状态
                isPlay = false;
                viewBinding.player.pause();
                viewBinding.player.release(true);
                DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
            } else {
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (!isConnectedSoftAp()) {
                        if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                            startSoftAp();
                        } else {
                            showJoinSoftApDialog();
                        }
                    } else {
                        if (!NetUtil.isWifi(RecordActivity.this)) {
                            showNetworkTypeDialog();
                        }
                    }
                } else {
                    if(DevicesDialogUtils.openWifiSetting(RecordActivity.this)) {
                        return false;
                    }
                    if(!TextUtils.isEmpty(_url_push) && (errorDialog == null || !errorDialog.isShowing())) {
                        playError = true;
                        getNetStatus();
                        getLiveStatus();
                    }
                }
            }

            isPlayError = true;
            return false;
        });
        viewBinding.player.setOnPreparedListener(iMediaPlayer -> {
            AppTrace.d(TAG, "onPrepared");
            viewBinding.loading.getRoot().setVisibility(View.GONE);

            //说明已经连接到相机了
            dismissDisconnectDialog();
            dismissJoinSoftApDialog();
            dismissNetworkTypeDialog();
        });
        isPlay = true;
    }

    //隐藏
    @Override
    public void onRecordVideoStart() {
        _handler.postDelayed(() -> {
            _deviceIsRecord = true;
            updateRecordStatus(true);
            viewBinding.recordTime.setText(TimeUtil.intToTimeMin(_recordTime));

            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                if (isConnectedSoftAp()) {
                    if (!isPlay) {
                        startPlay();
                    }
                }
            } else {
                if(!TextUtils.isEmpty(_url_pull) ) {
                    if(!isPlay) {
                        startPlay();
                    }
                }
            }
            dismissConnectingDialog();

            viewBinding.btnExit.setEnabled(true);
            viewBinding.btnExitLabel.setEnabled(true);
            viewBinding.btnExitIcon.setImageTintList(null);

            if(_deviceIsRecord && isTrainingMode()) {
                if(viewBinding.tabs.getCurrentTab() != 1) {
                    viewBinding.tabs.setCurrentTab(1);
                }
            }
            updateVideoControlUI();

            boolean ok = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());

        }, 1000);

    }

    private void onFinish() {

        //如果播放出错，没有开始录制的话
        //这个操作很危险，先注释掉
//        if (!_deviceIsRecord) {
//            BluetoothManager.getImpl().sendCmdSimpleObj(new RkipcStopRequestBean());
//        }

        if(_wifiInfo != null) {
            mWifiManager.disconnectNetWork();
        }
        _wifiInfo = null;

        //断开蓝牙
        if(!isFactoryTest) {
            DeviceApiManager.disConnect();
        }

        finish();
    }

    private void showBackgroundRunDialog(boolean isMoveTaskBack) {
        if(_backgroundExitDialog != null && _backgroundExitDialog.isShowing()) {
            _backgroundExitDialog.dismiss();
        }
        _backgroundExitDialog = new ModalDialog(RecordActivity.this);
        _backgroundExitDialog.setMessage(getString(R.string.record_background_exit_title));
        _backgroundExitDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(isMoveTaskBack) {
                    stopPlay();
                    moveTaskToBack(true);
                } else {
                    onFinish();
                }
                return true;
            }
        });
        _backgroundExitDialog.hideNegativeButton();
        _backgroundExitDialog.show();

    }


    private void showExitPlayerDialog() {
        showExitRecordDialog(getString(R.string.is_exit_camera));
    }


    private void showExitRecordDialog(String message) {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(RecordActivity.this);
        _dialog.setMessage(message);
        _dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                return true;
            }
        });

        _dialog.setPositiveButton(getString(R.string.exit), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                //断开蓝牙
                if(!isFactoryTest) {
                    DeviceApiManager.disConnect();
                }
                finish();
                return true;
            }
        });

        _dialog.show();
    }


    private void showStopRecordDialog(String message) {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(RecordActivity.this);
        _dialog.setMessage(message);
        _dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                return true;
            }
        });

        _dialog.setPositiveButton(getString(R.string.stop), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                UnattendedRecordService.finishForDevice(RecordActivity.this);
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
                    if (finishCoordinatedRecord()) return true;
                    boolean ok = DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                    if(ok) {
                        DialogUtil.showDialog(getString(R.string.record_saving));
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    } else {
                        showBlueToothDisconnectDialog();
                    }
                } else {
                    stopRtmp();
                }


                return true;
            }
        });

        _dialog.show();
    }

    private volatile String _localStopId, _localStopSession;
    private boolean _localStopTimedOut;
    private final Runnable _localStopPoll = new Runnable() {
        public void run() {
            if (_localStopId == null) return;
            _deviceStatusBackgroundCheck = true;
            DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
            _handler.postDelayed(this, 2000);
        }
    };
    private final Runnable _localStopTimeout = () -> {
        if (_localStopId == null) return;
        _handler.removeCallbacks(_localStopPoll);
        DialogUtil.dismissDialog();
        ToastUtil.s("本地停止录制尚未确认，请检查相机状态");
        _localStopTimedOut = true;
    };
    private boolean finishLocalRecord() {
        if (Constants.ACCESS_MODEL != Constants.ACCESS_MODEL_LOCAL || _recordStatus == null
                || _recordStatus.localControlSupported != 1) return false;
        if (_localStopId != null && !_localStopTimedOut) return true;
        String id = _recordStatus.competitionId;
        if (TextUtils.isEmpty(id)) id = _recordStatus.getLiveCId();
        String session = _recordStatus.sessionId;
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(session)) {
            ToastUtil.s("本地录制会话信息不完整，请刷新相机状态");
            return true;
        }
        if (!DeviceApiManager.sendCmdSimpleObj(new LocalRecordStopRequestBean(id, session))) {
            ToastUtil.s(getString(R.string.info_bluetooth_offline));
            return true;
        }
        _localStopId = id; _localStopSession = session; _localStopTimedOut = false;
        DialogUtil.showDialog(getString(R.string.record_saving));
        _handler.postDelayed(_localStopTimeout, 30000);
        _handler.postDelayed(_localStopPoll, 1000);
        return true;
    }

    private boolean finishCoordinatedRecord() {
        if (finishLocalRecord()) return true;
        if (_recordStatus == null || _recordStatus.recordingProtocolVersion < 3) return false;
        String id = _recordInfoBean == null ? null : _recordInfoBean.live_cid;
        if (TextUtils.isEmpty(id)) {
            id = _recordStatus.getLiveCId();
        }
        if (TextUtils.isEmpty(id)) {
            ToastUtil.s("无法获取当前录制任务，请刷新相机状态后重试");
            DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
            return true;
        }
        DialogUtil.showDialog(getString(R.string.record_saving));
        RecordingApi.finish(id, new IGenericApiCallback<CommonResponse>() {
            public void onSuccess(CommonResponse response) {
                if (isFinishing() || isDestroyed()) return;
                DialogUtil.dismissDialog();
                if (response.code == 200 && TextUtils.isEmpty(response.msg)) onRecordVideoStop();
                else if (!TextUtils.isEmpty(response.msg)) ToastUtil.s(response.msg);
                else ToastUtil.s("停止录制尚未确认，请检查相机状态");
                DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
            }
            public void onFail(String code, String message) {
                if (isFinishing() || isDestroyed()) return;
                DialogUtil.dismissDialog();
                ToastUtil.s(TextUtils.isEmpty(message) ? getString(R.string.stop_record_fail) : message);
            }
            public void onFinish() { }
        });
        return true;
    }

    private void showRecordStoppedOnLowStorageDialog() {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(this);
        _dialog.setMessage(getString(R.string.info_record_stopped_on_low_storage));
        _dialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                return true;
            }
        });

        _dialog.show();
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        _recordStatus = recordStatus;
        if (_localStopId != null && recordStatus != null && "ok".equalsIgnoreCase(recordStatus.getR())
                && recordStatus.getStatus() <= 0 && "idle".equals(recordStatus.deviceState)) {
            final String id = _localStopId, session = _localStopSession;
            _localStopId = null; _localStopSession = null;
            _handler.removeCallbacks(_localStopPoll); _handler.removeCallbacks(_localStopTimeout);
            if(recordStatus.recordingProtocolVersion>=5)OfflineRecordingReports.observe(recordStatus);else LocalRecordingReports.enqueue(id, session, recordStatus.deviceVersion, false);
            if(recordStatus.recordingProtocolVersion<5)runOnUiThread(() -> SetMealTimeManager.getInstance().sendCmdGetPlanUsage(1, this, available -> {
                if (!available) ToastUtil.s("录制已停止，用量同步失败，请联网后重新检查套餐");
            }));
        }
        if(recordStatus == null) {
            _deviceIsRecord = false;
            _recordTime = 0;
        } else {
            _recordInfoBean = recordStatus.convertRecordInfoBean();
            _deviceStatusChecked = true;
            _deviceIsRecord = recordStatus.getStatus() == 1;
            isRkipcRunning = recordStatus.getStatus() != -1;
            AppTrace.d(TAG, String.format("record status: %d, time: %d", recordStatus.getStatus(), recordStatus.getDuration()));

            _handler.post(() -> {
                //移除计时器
                _recordTime = recordStatus.getDuration();
                _recordStatusSyncTime = System.currentTimeMillis();

                if(_deviceStatusBackgroundCheck) {
                    if(_deviceIsRecord) {
                        viewBinding.tvTitle.setText(recordStatus.getVideoName());
                        viewBinding.tvTitle.setSelected(true);

                        viewBinding.btnExit.setEnabled(true);
                        viewBinding.btnExitLabel.setEnabled(true);
                        viewBinding.btnExitIcon.setImageTintList(null);
                    } else {
                        viewBinding.recordTime.setText("");
                        viewBinding.btnExit.setEnabled(false);
                        viewBinding.btnExitLabel.setEnabled(false);
                        viewBinding.btnExitIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_gray_76)));

                        //检查ota升级
                        if(Constants.enableOTA && !isCheckOtaVersion) {
                            AppTrace.d(TAG, "正在检查ota升级");
                            checkOTAVersion();
                            isCheckOtaVersion = true;
                        }
                    }

                    updateRecordStatus(_deviceIsRecord);
                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                        if(isConnectedSoftAp()) {
                            if (!isPlay) {
                                startPlay();
                                retryCount = 0;
                            }
                        }
                    }else {
                        if(!TextUtils.isEmpty(_url_pull)){
                            startPreview();
                        }
                    }

                    return;
                }
                //
                updateRecordStatus(_deviceIsRecord);
                if(!_deviceIsRecord) {
                    showConnectingDialog(RecordActivity.this.getString(R.string.record_launching));
                    startRecord();

                    //检查ota升级
                    if(Constants.enableOTA && !isCheckOtaVersion) {
                        AppTrace.d(TAG, "正在检查ota升级");
                        checkOTAVersion();
                        isCheckOtaVersion = true;
                    }
                } else {
                    if(!isPlay) {
                        startPlay();
                        retryCount = 0;
                    }
                }
            });
        }

        runOnUiThread(() -> {
            if(_deviceIsRecord && isTrainingMode()) {
                if(viewBinding.tabs.getCurrentTab() != 1) {
                    viewBinding.tabs.setCurrentTab(1);
                }
            }
            updateVideoControlUI();

            // set training mode if pending
            if(_pendingSetTrainingMode) {
                _pendingSetTrainingMode = false;
                if(_recordStatus != null) {
                    showLoading();
                    boolean v = !isTrainingMode();
                    _recordStatus.setTrainingMode(v ? 1 : 0);
                    DeviceApiManager.sendCmdSimpleObj(new SetTrainingModeRequestBean(v));
                }
            }
        });

//        _handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
        getLiveStatus();
//            }
//        }, 1000);
    }

    @Override
    public void onRecordVideoStop() {
        AppTrace.d(TAG, "onRecordVideoStop");
        _recordTime = 0;
        _recordStatusSyncTime = 0;
        _recordInfoBean = null;
        _deviceIsRecord = false;
        _url_pull = null;
        _url_push = null;

        ScoreCenterUtil.removeCustomDotRecord();

        _handler.post(() -> {
            DialogUtil.dismissDialog();
            dismissConnectingDialog();

            ToastUtil.s(getString(R.string.record_video_save_to_camera));

            updateRecordStatus(false);

            viewBinding.btnExit.setEnabled(false);
            viewBinding.btnExitLabel.setEnabled(false);
            viewBinding.btnExitIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_gray_76)));

            if(_deviceIsRecord && isTrainingMode()) {
                if(viewBinding.tabs.getCurrentTab() != 1) {
                    viewBinding.tabs.setCurrentTab(1);
                }
            }
            updateVideoControlUI();

            if(_deviceInfo != null && !TextUtils.isEmpty(_deviceInfo.device_name)) {
                viewBinding.tvTitle.setText(BluetoothManager.getDisplayDeviceName(_deviceInfo.device_name));
            }
        });
    }

    @Override
    public void onRecordException(String msg) {
        AppTrace.d(TAG, "onRecordException: " + msg);
        if(!TextUtils.isEmpty(msg) && msg.equalsIgnoreCase("rkipc is not launched")) {
            _handler.post(() -> {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                if(isOk) {
                    _handler.postDelayed(_startRkipcTimeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    showConnectingDialog(getString(R.string.lunching_recorder));
                }
            });

        }

    }

    private void updateStorageInfo(StorageBean storageBean) {
        String totalStr = String.format("%dG", (int) (storageBean.total / 1024 / 1024 / 1024));
        String usedStr = "";
        if(storageBean.used / 1024 / 1024 > 1024) {
            if(storageBean.used * 1.0 / 1024 / 1024 / 1024 > 10) {
                usedStr = String.format("%dG", (int) (storageBean.used * 1.0 / 1024 / 1024 / 1024));
            } else {
                usedStr = String.format("%.1fG", storageBean.used * 1.0 / 1024 / 1024 / 1024);
            }
        } else {
            usedStr = String.format("%.2fM", storageBean.used * 1.0 / 1024 / 1024);
        }

        viewBinding.tvStorageInfo.setText(String.format("%s/%s", usedStr, totalStr));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutoUploadStatusEvent(AutoUploadStatusEvent event) {
        if(event.uploading) {
            showStopAutoUploadDialog();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettingsGot(WeewaSettingsExEvent event) {
        if(event.has("isp.0.adjustment:brightness")) {
            try {
                int brightness = Integer.parseInt(event.get("isp.0.adjustment:brightness"));
                viewBinding.tvBrightness.setText(String.valueOf(brightness));
                viewBinding.sliderBrightness.setProgress(brightness);
                viewBinding.tvBrightness2.setText(String.valueOf(brightness));
                viewBinding.sliderBrightness2.setProgress(brightness);
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:saturation")) {
            try {
                int v = Integer.parseInt(event.get("isp.0.adjustment:saturation"));
                viewBinding.tvSaturation.setText(String.valueOf(v));
                viewBinding.sliderSaturation.setProgress(v);
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:contrast")) {
            try {
                int v = Integer.parseInt(event.get("isp.0.adjustment:contrast"));
                viewBinding.tvContrast.setText(String.valueOf(v));
                viewBinding.sliderContrast.setProgress(v);
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:hue")) {
            try {
                int v = Integer.parseInt(event.get("isp.0.adjustment:hue"));
                viewBinding.tvHue.setText(String.valueOf(v));
                viewBinding.sliderHue.setProgress(v);
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:sharpness")) {
            try {
                int v = Integer.parseInt(event.get("isp.0.adjustment:sharpness"));
                viewBinding.tvSharpness.setText(String.valueOf(v));
                viewBinding.sliderSharpness.setProgress(v);
            } catch(NumberFormatException e) {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettingsUpdated(WeewaSettingsExUpdateEvent event) {
        dismissLoading();
        _handler.removeCallbacks(_timeOutRunnable);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppEnterBackgroundEvent(HotmeApplication.AppEnterBackgroundEvent event) {
        stopPlay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEnergyInfoEvent(DeviceEnergyInfoEvent event) {
        AppTrace.d(TAG, "onEnergyInfoEvent");
        requestEnergy = false;
        _handler.post(() -> {
            if(event != null) {
                if(event.percent < 0) {
                    viewBinding.tvEnergy.setText("-");
                } else {
                    viewBinding.tvEnergy.setText(event.percent + "%");
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordStoppedOnLowStorage(RecordStoppedOnLowStorageEvent event) {
        onRecordVideoStop();
        showRecordStoppedOnLowStorageDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStorageInfoEvent(DeviceStorageInfoEvent event) {
        AppTrace.d(TAG, "onStorageInfoEvent");
        requestStorage = false;
        _handler.post(() -> {
            if(event != null && event.storageBean != null) {
                updateStorageInfo(event.storageBean);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceUsageInfoEvent(DeviceUsageInfoEvent data) {
        AppTrace.d(TAG, "onDeviceUsageInfoEvent");
        _handler.post(() -> {
            if(data != null && data.deviceUsageInfo != null) {
                updateUsageInfo(data.deviceUsageInfo);
            }
        });
    }

    public void updateUsageInfo(DeviceUsageInfo data) {
        _deviceUsageInfo = data;
        if(_deviceUsageInfo != null) {
            if(_deviceUsageInfo.energy >= 0) {
                viewBinding.tvEnergy.setText(_deviceUsageInfo.energy + "%");
            } else {
                viewBinding.tvEnergy.setText("-");
            }

            updateStorageInfo(_deviceUsageInfo.storage);
        }
    }

    /* 退出全屏 */
    @Override
    protected void exitLandscape() {
        // 将JJPlayerView从Activity的R.id.content移除，加入RecyclerView的ItemView下
        viewBinding.portraitLayout.setVisibility(View.VISIBLE);

        viewBinding.landscapePlayerContainer.removeView(viewBinding.groupPlayer);
        viewBinding.landscapeLayout.setVisibility(View.GONE);
        ViewGroup parentView = (ViewGroup) viewBinding.groupPlayer.getParent();
        if(parentView!=null){
            parentView.removeView(viewBinding.groupPlayer);
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.portraitPlayerContainer.addView(viewBinding.groupPlayer, params);

        viewBinding.btnFullscreen.setImageResource(R.mipmap.fullscreen_expand);
        if(_deviceIsRecord && isTrainingMode()) {
            if(viewBinding.tabs.getCurrentTab() != 1) {
                viewBinding.tabs.setCurrentTab(1);
            }
        }
        _showVideoControl = false;
        updateVideoControlUI();
    }

    /* 进入全屏 */
    @Override
    protected void enterLandscape() {
        // 将JJPlayerView从Activity的R.id.content移除，加入RecyclerView的ItemView下
        viewBinding.portraitLayout.setVisibility(View.GONE);

        viewBinding.portraitPlayerContainer.removeView(viewBinding.groupPlayer);
        ViewGroup parentView = (ViewGroup) viewBinding.groupPlayer.getParent();
        if(parentView != null){
            parentView.removeView(viewBinding.groupPlayer);
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.landscapePlayerContainer.addView(viewBinding.groupPlayer, params);
        viewBinding.landscapeLayout.setVisibility(View.VISIBLE);

        viewBinding.btnFullscreen.setImageResource(R.mipmap.fullscreen_skrink);

        // 训练模式的时候调整某些界面
        if(_deviceIsRecord && isTrainingMode()) {
            if(viewBinding.tabs.getCurrentTab() != 1) {
                viewBinding.tabs.setCurrentTab(1);
            }
        }
        updateVideoControlUI();
    }

    private void updateVideoControlUI() {
        if(isPortrait) {
            viewBinding.btnShowVideoControl.setVisibility(View.GONE);
            viewBinding.btnHideVideoControl.setVisibility(View.GONE);
            viewBinding.joystick.setVisibility(View.GONE);
            viewBinding.btnIspSettings.setVisibility(View.GONE);
            viewBinding.btnToggleTrainingMode.setVisibility(View.GONE);
            viewBinding.scaleRoi.setVisibility(View.GONE);
            viewBinding.resetRoi.setVisibility(View.GONE);

            viewBinding.tabs.setVisibility(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE? View.GONE:View.VISIBLE);
        } else {
            viewBinding.btnShowVideoControl.setVisibility(_showVideoControl ? View.GONE : View.VISIBLE);
            viewBinding.btnHideVideoControl.setVisibility(_showVideoControl ? View.VISIBLE : View.GONE);
            if(_showVideoControl) {
                boolean trainingMode = isTrainingMode();
                viewBinding.btnToggleTrainingMode.setVisibility(View.VISIBLE);
                viewBinding.labelVideoMode.setText(trainingMode ? "训练" : "正常");
                viewBinding.scaleRoi.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.resetRoi.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.joystick.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.btnIspSettings.setVisibility(View.VISIBLE);
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
                    viewBinding.tabs.setVisibility(trainingMode ? View.INVISIBLE : View.VISIBLE);
                }
                viewBinding.ispSettingsContainer.setVisibility(View.GONE);
            } else {
                viewBinding.btnToggleTrainingMode.setVisibility(View.GONE);
                viewBinding.scaleRoi.setVisibility(View.GONE);
                viewBinding.resetRoi.setVisibility(View.GONE);
                viewBinding.joystick.setVisibility(View.GONE);
                viewBinding.btnIspSettings.setVisibility(View.GONE);
                viewBinding.tabs.setVisibility(View.GONE);
                viewBinding.ispSettingsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void startSoftAp() {
        _handler.postDelayed(() -> {

            boolean ok = DeviceApiManager.sendCmdSimpleObj(new SoftApOpenRequestBean("", ""));
            if(ok) {
                _handler.postDelayed(_openSoftApRunnable, BluetoothManager.RESP_TIMEOUT);
                showConnectingDialog(getString(R.string.opening_soft_ap));
            } else {
                showDisconnectToast();
                showBlueToothDisconnectDialog();
            }

        }, 2000);
    }

    @Override
    public void onSoftApStart(String ssid, String password) {
        AppTrace.d(TAG, "onSoftApStart: ssid=" + ssid + ", password=" + password);
        _handler.removeCallbacks(_openSoftApRunnable);
        _handler.post(() -> {
            AppTrace.d(TAG, "热点启动成功, ip = " + BleConstants.SOFT_AP_IP);
            showJoinSoftApDialog();
        });
    }

    @Override
    public void onSoftApStartFail(String message) {
        _handler.removeCallbacks(_openSoftApRunnable);
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
    private void showNetworkTypeDialog() {
        if(_networkTypeDialog != null && _networkTypeDialog.isShowing()) {
            return;
        }

        _networkTypeDialog = new ModalDialog(RecordActivity.this);
        _networkTypeDialog.setZOrder(ZORDER_NETWORKTYPE_DIALOG);
        _networkTypeDialog.setMessage(getString(R.string.camera_network_type_error));
        _networkTypeDialog.setPositiveButton(getString(R.string.go_to_setup), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_networkTypeDialog != null && _networkTypeDialog.isShowing()) {
                    _networkTypeDialog.dismiss();
                }
                return true;
            }
        });

        _networkTypeDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_networkTypeDialog != null && _networkTypeDialog.isShowing()) {
                    _networkTypeDialog.dismiss();
                }
                return true;
            }
        });
        dialogStack.push(_networkTypeDialog);
    }

    private void dismissNetworkTypeDialog() {
        if(_networkTypeDialog != null && _networkTypeDialog.isShowing()) {
            _networkTypeDialog.dismiss();
        }
        _networkTypeDialog = null;
    }


    private void showStartSoftApDialog() {
        if(_startSoftApDialog != null && _startSoftApDialog.isShowing()) {
            return;
        }

        _startSoftApDialog = new ModalDialog(RecordActivity.this);
        _startSoftApDialog.setZOrder(ZORDER_SOFTAP_DIALOG);
        _startSoftApDialog.setMessage(getString(R.string.soft_ap_set_up));
        _startSoftApDialog.setPositiveButton(getString(R.string.wifi_open), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.startActivityForResult(
                    RecordActivity.this,
                    NetworkManager.getImpl().getSoftApSsid(),
                    NetworkManager.getImpl().getSoftApPassword(),
                    NetworkManager.getImpl().getSoftApStatus(),
                    CameraSoftApActivity.REQUEST_SOFT_AP_PREVIEW
                );
                return true;
            }
        });

        _startSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_startSoftApDialog != null && _startSoftApDialog.isShowing()) {
                    _startSoftApDialog.dismiss();
                }
                return true;
            }
        });
        dialogStack.push(_startSoftApDialog);
    }

    private void showNoEnoughSpaceDialog(boolean force) {
        if(_noEnoughSpaceDialog != null && _noEnoughSpaceDialog.isShowing()) {
            return;
        }
        _noEnoughSpaceDialog = new ModalDialog(RecordActivity.this);
        _noEnoughSpaceDialog.setPositiveButton(getString(R.string.storage_management), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                startCameraVideoListActivity();
                return true;
            }
        });

        if(force) {
            _noEnoughSpaceDialog.setMessage(getString(R.string.cannot_record_because_no_storage));
            _noEnoughSpaceDialog.setCancelable(false);
            _noEnoughSpaceDialog.hideNegativeButton();
        } else {
            _noEnoughSpaceDialog.setMessage(getString(R.string.cannot_record_because_low_storage));
            _noEnoughSpaceDialog.setCancelable(true);
            _noEnoughSpaceDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(_noEnoughSpaceDialog != null && _noEnoughSpaceDialog.isShowing()) {
                        _noEnoughSpaceDialog.dismiss();
                    }
                    _ignoreLowStorage = true;
                    return true;
                }
            });
        }

        _noEnoughSpaceDialog.show();
    }

    private void dismissJoinSoftApDialog() {
        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            _joinSoftApDialog.dismiss();
        }
        _joinSoftApDialog = null;
    }

    private void dismissStopAutoUploadDialog() {
        if(_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            _stopAutoUploadDialog.dismiss();
        }
        _stopAutoUploadDialog = null;
    }

    private void showJoinSoftApDialog() {
        AppTrace.d(TAG, "showJoinSoftApDialog");
        String title = getString(R.string.join_softap_title);
        showSoftApDialog(String.format(title, NetworkManager.getImpl().getSoftApSsid()), Constants.REQUEST_CODE_WIFI_PREVIEW);
    }

    private void showSoftApDialog(String message, int requestCode) {
        AppTrace.d(TAG, "showSoftApDialog");
        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            return;
        }

        _joinSoftApDialog = new ModalDialog(RecordActivity.this);
        _joinSoftApDialog.setZOrder(ZORDER_SOFTAP_DIALOG);
        _joinSoftApDialog.setMessage(message);
        _joinSoftApDialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!PermissionsUtil.isLocServiceEnable(RecordActivity.this)) {
                    PermissionsUtil.openLocationService(RecordActivity.this);
                    return true;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(RecordActivity.this);
                if(!hasPermission) {
                    EasyPermissions.requestPermissions(RecordActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return true;
                }

                HotmeWiFiManager.startWifiSettingPage(RecordActivity.this, requestCode);
                return true;
            }
        });

        _joinSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
                    _joinSoftApDialog.dismiss();
                }
                return true;
            }
        });
        dialogStack.push(_joinSoftApDialog);
    }

    private void showStopAutoUploadDialog() {
        if(_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            return;
        }

        _stopAutoUploadDialog = new ModalDialog(RecordActivity.this);
        _stopAutoUploadDialog.setZOrder(ZORDER_CONFIRM_STOP_UPLOAD_DIALOG);
        _stopAutoUploadDialog.setMessage(getString(R.string.stop_auto_upload_during_recording));
        _stopAutoUploadDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DeviceApiManager.sendCmdSimpleObj(new StopAutoUploadRequestBean());
                return true;
            }
        });

        _stopAutoUploadDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
                    _stopAutoUploadDialog.dismiss();
                }
                finish();
                return true;
            }
        });
        dialogStack.push(_stopAutoUploadDialog);
    }


    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(RecordActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    private void updateConnectMessage(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.setMessage(message);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void renameDevice(RenameEvent event) {
        AppTrace.d(TAG, "renameDevice");
        if(_deviceInfo != null && event != null && _device != null && _device.address.equalsIgnoreCase(event.address)) {
            _deviceInfo.device_name = event.name;
            if(viewBinding.tvTitle != null) {
                viewBinding.tvTitle.setText(event.name);
            }
            if(_device != null) {
                _device.setName(event.name);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        AppTrace.d(TAG, "onRkipcStart");
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        DialogUtil.dismissDialog();
        dismissConnectingDialog();
        if(event != null) {
            if(event.status == 1) {
                isRkipcRunning = true;
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                        showStartSoftApDialog();
                    } else {
                        if (isConnectedSoftAp()) {
                            if (!isPlay) {
                                startPlay();
                                retryCount = 0;
                            }
                        } else {
                            showJoinSoftApDialog();
                        }
                    }
                }

                _deviceStatusBackgroundCheck = true;
                boolean ok = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
            } else {
                isRkipcRunning = false;
                ToastUtil.s(str_lunch_rkipc_fail);
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPowerOff(PowerOffEvent event) {
        AppTrace.d(TAG, "renameDevice");
        ModalDialog dialog = new ModalDialog(RecordActivity.this);
        dialog.setZOrder(ZORDER_POWEROFF_DIALOG);
        dialog.setTitle(RecordActivity.this.getString(R.string.info_device_exit));
        dialog.setMessage(RecordActivity.this.getString(R.string.info_device_power_off));
        dialog.setPositiveButton(RecordActivity.this.getString(R.string.confirm), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                RecordActivity.this.finish();
                return true;
            }
        });
        dialog.setCancelable(false);
        dialog.hideNegativeButton();
        dialogStack.push(dialog);
    }

    ModalDialog _disconnectDialog;

    private void dismissDisconnectDialog() {
        if(_disconnectDialog != null && _disconnectDialog.isShowing()) {
            _disconnectDialog.dismiss();
        }
        _disconnectDialog = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkStatus(NetworkStatusEvent event) {
        if(_cameraNetMaybeBroken) {
            _cameraNetMaybeBroken = false;
            startRkIPC();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NetWorkForQEvent event) {
        AppTrace.d(TAG, "NetWorkForQEvent: " + event.type);
        if(event.type == NetWorkForQEvent.AVAILABLE) {
            AppTrace.d(TAG, "已连接");
            if(isConnectedSoftAp()) {
                dismissDisconnectDialog();
                dismissJoinSoftApDialog();
                dismissNetworkTypeDialog();
            }

        } else if(event.type == NetWorkForQEvent.UNCONNECT) {
            AppTrace.d(TAG, "网络已断开");
            if(!_cameraNetMaybeBroken) {
                _cameraNetMaybeBroken = true;
            }
            if(!_lastConnected) {
                return;
            }
            if(NetworkManager.getImpl().isDeviceSoftApLaunched() && !isConnectedSoftAp()) {
                showSoftApDisconnectDialog();
            }
        }
    }

    @Subscribe
    public void onRecordInfo(RecordStartEvent event) {
        if(event != null) {
            _recordInfoBean = event.getRecordInfoBean();
//            sendRecord(recordInfoBean);
        }
    }

    private boolean isTrainingMode() {
        if(_recordStatus != null) {
            return _recordStatus.isTrainingMode() != 0;
        } else if(_recordInfoBean != null) {
            return _recordInfoBean.player_num == 0;
        } else {
            return false;
        }
    }

    private void showSoftApDisconnectDialog() {
        if(_disconnectDialog != null && _disconnectDialog.isShowing()) {
            return;
        }

        _disconnectDialog = new ModalDialog(RecordActivity.this);
        _disconnectDialog.setZOrder(ZORDER_SOFTAP_DIALOG);
        _disconnectDialog.setMessage(getString(R.string.info_reconnect_for_soft_ap_disconnect));
        _disconnectDialog.setNegativeButton(RecordActivity.this.getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_disconnectDialog != null && _disconnectDialog.isShowing()) {
                    _disconnectDialog.dismiss();
                }
                return true;
            }
        });
        _disconnectDialog.setPositiveButton(RecordActivity.this.getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_disconnectDialog != null && _disconnectDialog.isShowing()) {
                    _disconnectDialog.dismiss();
                }

                // 如果热点关闭，则去打开热点
                if(!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                    showStartSoftApDialog();
                    return true;
                }

                if(!PermissionsUtil.isLocServiceEnable(RecordActivity.this)) {
                    PermissionsUtil.openLocationService(RecordActivity.this);
                    return true;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(RecordActivity.this);
                if(!hasPermission) {
                    EasyPermissions.requestPermissions(RecordActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return true;
                }

                HotmeWiFiManager.startWifiSettingPage(RecordActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
                return true;
            }
        });
        dialogStack.push(_disconnectDialog);
    }

    private void getNetStatus() {
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
    }

    private void getLiveStatus() {
        DeviceApiManager.sendCmdSimpleObj(new LiveStatusRequestBean());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStatusResultEvent(LiveStatusResultEvent data) {
        AppTrace.d(TAG, "onLiveStatusResultEvent ");
        //检查直播状态回调
        if(data != null) {
            if(data.status == 1 && data.bean != null) {
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
                    checkLiveOnLocalModel(data);
                } else {
                    checkLiveOnRemoteModel(data);
                }

            } else {
                AppTrace.d(TAG, "直播状态返回 :未开启");
                //isPlay = false;
            }
        }
    }
    private void checkLiveOnLocalModel(LiveStatusResultEvent data){
        try {
            AppTrace.e(String.format("avs推流状态异常检查，avs_timeout=%d,  fec_timeout=%d", data.bean.avs_timeout, data.bean.fec_timeout));
            if(data.bean.avs_timeout == 1) {
                AppTrace.e("avs推流状态异常");
                timeoutCheckCount++;
                if(timeoutCheckCount >= 2) {
                    _handler.post(() -> {
                        showRestartDialog();
                    });
                    timeoutCheckCount = 0;
                } else {
                    _handler.postDelayed(() -> {
                        getLiveStatus();
                    }, 2000);
                }
            } else if(data.bean.fec_timeout == 1) {
                AppTrace.e("fec推流状态异常");
                timeoutCheckCount++;
                if(timeoutCheckCount >= 2) {
                    _handler.post(() -> {
                        showRestartDialog();
                    });
                    timeoutCheckCount = 0;
                } else {
                    _handler.postDelayed(() -> {
                        getLiveStatus();
                    }, 2000);
                }
            } else {
                timeoutCheckCount = 0;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void checkLiveOnRemoteModel(LiveStatusResultEvent data){
        // 判断是否有fec直播进行中, 这里我们不管他是否连上了, 只要有外部推流地址就行
        boolean isLiving = false;
        int errCode = 0;
        List<LiveUrl> fecUrls = new ArrayList<>();
        try {
            for(LiveUrl info : data.bean.fec_live_urls) {
                boolean isLocal = info.url.contains("127.0.0.1") || info.url.contains("localhost");
                if(!isLocal) {
                    fecUrls.add(info);
                    break;
                }
            }

            // 在播的fec地址有可能不止一个(虽然不止一个在目前的设计中是一个不正确的状态),
            // 但是代码上还是对这种情况做一种保险的处理, 不然_url_push有可能会被改变, 这会导致
            // 后续停止推流后出现状态停止不了的问题(因为以前在遍历时取了第一个非本地地址, 而这个地址却不是真正的那个)
            if(!fecUrls.isEmpty()) {
                boolean found = false;
                for(LiveUrl info : fecUrls) {
                    if(info.url.equals(_url_push)) {
                        found = true;
                        isLiving = true;
                        errCode = info.err_code;
                        break;
                    }
                }
                if(!found) {
                    _url_push = fecUrls.get(0).url;
                    isLiving = true;
                    errCode = fecUrls.get(0).err_code;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        AppTrace.e(String.format("直播状态，avs_timeout=%d, fec_timeout=%d, isLiving=%b, err code=%d, url=%s",
                data.bean.avs_timeout, data.bean.fec_timeout, isLiving, errCode, _url_push == null ? "null" : _url_push));
        if(data.bean.avs_timeout == 1) {
            AppTrace.e("avs推流状态异");
            timeoutCheckCount++;
            if(timeoutCheckCount >= 2) {
                _handler.post(() -> showRestartDialog());
                timeoutCheckCount = 0;
            } else {
                _handler.postDelayed(() -> getLiveStatus(), 2000);
            }
            return;
        } else if(data.bean.fec_timeout == 1) {
            AppTrace.e("fec推流状态异");
            timeoutCheckCount++;
            if(timeoutCheckCount >= 2) {
                _handler.post(() -> showRestartDialog());
                timeoutCheckCount = 0;
            } else {
                _handler.postDelayed(() -> getLiveStatus(), 2000);
            }
            return;
        } else {
            timeoutCheckCount = 0;
            if(TextUtils.isEmpty(_url_push)) {
                if(_deviceIsRecord) {
                    rtmpUrlCheckCount++;
                    if(rtmpUrlCheckCount < 2) {
                        _handler.postDelayed(() -> getLiveStatus(), 2000);
                    }
                }
            } else {
                rtmpUrlCheckCount = 0;
            }
        }

        //播放失败后检查相机直播状态
        if(playError) {
            playError = false;
            if(!CameraManager.isCameraReachable()) {
                errorDialog = DevicesDialogUtils.liveCameraNoNetworkError(RecordActivity.this,
                        (code, objects) -> DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean()));
            } else if(isLiving) {
                switch(errCode) {
                    case LiveUrl.ERROR_SOCKET_CONNECT:
                    case LiveUrl.ERROR_SOCKET_TIMEOUT:
                    case LiveUrl.ERROR_SOCKET_WAIT:
                    case LiveUrl.ERROR_SOCKET_WRITE:
                        _handler.post(() -> {
                            //直播中
                            errorDialog = DevicesDialogUtils.liveSocketError(RecordActivity.this,
                                    (code, objects) -> {
                                        stopRtmp();
                                    });
                        });
                        break;
                    case LiveUrl.ERROR_H264_DROP_BEFORE_SPS_PPS:
                    case LiveUrl.ERROR_H264_DUPLICATED_PPS:
                    case LiveUrl.ERROR_H264_DUPLICATED_SPS:
                    case LiveUrl.ERROR_OK:
                        // 这些都是正常错误, 不需要管, 重新启动播放
                        startPreview();
                        break;
                    default:
                        _handler.post(() -> {
                            //直播中
                            errorDialog = DevicesDialogUtils.livePhoneError(RecordActivity.this,
                                    (code, objects) -> {
                                        startPreview();
                                    });
                        });
                        break;
                }
            } else {
                _handler.post(() -> {
                    //直播失败
                    errorDialog = DevicesDialogUtils.liveError(RecordActivity.this,
                            (code, objects) -> {
                                boolean isOK = DeviceApiManager.sendCmdSimpleObj(new
                                        FecRtmpAddUrlRequestBean(_url_push));
                                if(isOK) {
                                    showConnectingDialog("画面就绪中......");
                                } else {
                                    showDisconnectToast();
                                }
                            });
                });
            }
            return;
        }

        AppTrace.d(TAG, "推流地址为 " + _url_push);
        boolean finalIsLiving = isLiving;
        _handler.post(() -> {
            if(!finalIsLiving) {
                AppTrace.d(TAG, "当前不在直播中");
                isPlay = false;
//                updateUI();
            } else {
                rtmpUrlCheckCount = 0;
                if(!isPlay ||
                        viewBinding.playerCover.getVisibility() == View.VISIBLE) {
                    startPreview();
                }
            }
        });

    }

    private void stopRtmp() {

        if (finishCoordinatedRecord()) return;

        //停止直播推流
        if(!TextUtils.isEmpty(_url_push)) {
            AppTrace.d(TAG, "停止直播推流");

            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FecRtmpRemoveAllUrlRequestBean());
            if(isOk) {
                showConnectingDialog(getString(R.string.loading_stop_record_and_save_record_video));
                stopRequest = true;
            } else {
                showDisconnectToast();
            }
        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
            if(isOk) {
                showConnectingDialog(getString(R.string.loading_stop_record_and_save_record_video));
                stopRequest = true;
            } else {
                showDisconnectToast();
            }
        }

    }

    private void showRestartDialog() {
        if(_restartRkipcDialog == null) {
            _restartRkipcDialog = new ModalDialog(RecordActivity.this);
            _restartRkipcDialog.setMessage(getString(R.string.restart_camera_on_live_stream_exception));
            _restartRkipcDialog.setNegativeButton(RecordActivity.this.getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(_restartRkipcDialog != null && _restartRkipcDialog.isShowing()) {
                        _restartRkipcDialog.dismiss();
                    }
                    return true;
                }
            });
            _restartRkipcDialog.setPositiveButton(RecordActivity.this.getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(_restartRkipcDialog != null && _restartRkipcDialog.isShowing()) {
                        _restartRkipcDialog.dismiss();
                    }

                    if(!DeviceApiManager.isConnected()){
                        showDisconnectDialog();
                        return true;
                    }

                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStopRequestBean());
                    if(isOk) {
                        DialogUtil.showDialog(getString(R.string.loading_restart_rkipc));
                    }
                    return true;
                }
            });
            _restartRkipcDialog.show();
        } else {
            if(!_restartRkipcDialog.isShowing()) {
                _restartRkipcDialog.show();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStopRkIpcResultEvent(RkIpcStopEvent event) {
        AppTrace.d(TAG, "onStopRkIpcResultEvent");

        if(_handler != null) {
            //检查直播状态回调
            _handler.post(() -> {
                if(event != null && event.status == 1) {
                    setUnRecordStatus();
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                    if(isOk) {
                        AppTrace.d(TAG, "启动rkipc命令发送成功");
                    } else {
                        DialogUtil.dismissDialog();
                        showDisconnectToast();
                        showDisconnectDialog();
                    }
                } else {
                    DialogUtil.dismissDialog();
                    ToastUtil.s(getString(R.string.stop_record_fail));
                    AppTrace.d(TAG, "rkipc 停止异常状态返回");
                }
            });
        }
    }

    private void setUnRecordStatus() {
        _recordTime = 0;
        _recordStatusSyncTime = 0;
        _recordInfoBean = null;
        _deviceIsRecord = false;

        updateRecordStatus(false);

        viewBinding.btnExit.setEnabled(false);
        viewBinding.btnExitLabel.setEnabled(false);
        viewBinding.btnExitIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_gray_76)));

        if(_deviceInfo != null && !TextUtils.isEmpty(_deviceInfo.device_name)) {
            viewBinding.tvTitle.setText(BluetoothManager.getDisplayDeviceName(_deviceInfo.device_name));
        }

        isPlay = false;
    }

    @Override
    public void onStatusChanged(int newStatus, int oldStatus) {
        AppTrace.d(TAG, String.format("onStatusChanged, old status: %d, new status: %d", oldStatus, newStatus));
        MainHandler.runOnUiThread(() -> {
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                if (isConnectedSoftAp()) {
                    dismissDisconnectDialog();
                    dismissJoinSoftApDialog();

                    dismissNetworkTypeDialog();

                    AppTrace.d(TAG, "onStatusChanged start play");
                    startPlay();
                    retryCount = 0;
                } else if (viewBinding.player != null) {
                    AppTrace.d(TAG, "soft ap not connect");
                    if (isPlay || viewBinding.player.isPlaying()) {
                        viewBinding.player.pause();
                        viewBinding.player.release(false);
                        viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
                        viewBinding.playerCover.setVisibility(View.VISIBLE);
                        _handler.removeCallbacks(_playLoadingCountDownRunnable);
                        isPlay = false;
                    }
                }
            }
        });
    }

    @Override
    protected int getStatusBarColor() {
        return 0xff000000;
    }

    @Override
    protected boolean isDarkStatusBar() {
        return true;
    }

    @Override
    protected int getNavigationBarColor() {
        return 0xff000000;
    }

    private void stopPlayer(){
        viewBinding.player.pause();
        viewBinding.player.release(false);
        viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        viewBinding.playerCover.setVisibility(View.VISIBLE);
        _handler.removeCallbacks(_playLoadingCountDownRunnable);
        isPlay = false;
    }

    @Subscribe
    public void onFecRtmpRemoveAllUrl(FecRtmpRemoveAllUrlEvent event) {
        AppTrace.d(TAG, "onFecRtmpRemoveAllUrl");
        runOnUiThread(() -> {
            if(event != null) {
                AppTrace.t(TAG, "onFecRtmpRemoveAllUrl 直播已关闭 ");
                if(event.status == 1) {
                    EventBus.getDefault().post(new LiveStatusResultEvent(0, null));
                    stopPlayer();
                } else {
                    // 删除fec直播url不成功,那么我们需要刷一下状态,看看相机那边到底咋了
                    DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                }

                if(stopRequest) {
                    AppTrace.t(TAG, "停止录制");
                    //停止录制
                    DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                    stopRequest = false;
                } else {
                    dismissConnectingDialog();
                }
            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveUrlEvent(LiveUrlEvent event) {
        if(event != null) {
            _url_push = event.url_push;
            _url_pull = event.url_pull;
            _3rdPlt = event.third_plt;

            _url = _url_pull;
        }
    }

    //获取url开始播放
    public void startPreview() {
        AppTrace.d(TAG, "startPreview");

        if(!TextUtils.isEmpty(_url_pull) ) {
            startPlay();
        } else {
            getPullStreamUrl();
        }
    }

    private void getPullStreamUrl(){
//        ApiUtil.getPullStreamUrlWithoutCommentary(_recordInfoBean.live_cid, new IGenericApiCallback<PullStreamAddrBean>() {
        ApiUtil.getPullStreamUrlWithDeviceIdV2(CameraManager.getDeviceId(),_recordInfoBean.live_cid, new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {

                runOnUiThread(() -> {
                    _url_pull = data.data;
                    startPlay();
                });
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getPullStreamUrlWithDeviceId onFail: code=" + code+", messsage=" + message);
                runOnUiThread(() -> {
                    ToastUtil.s(message);
                });
            }

            @Override
            public void onFinish() {

            }
        });
    }
}
