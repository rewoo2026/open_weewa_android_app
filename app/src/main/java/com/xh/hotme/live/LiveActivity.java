package com.xh.hotme.live;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.dou361.ijkplayer.widget.PlayStateParams;
import com.xh.hotme.R;
import com.xh.hotme.base.BLEAware;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.LiveUrl;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
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
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStopRequestBean;
import com.xh.hotme.bluetooth.bean.RoiOpRequestBean;
import com.xh.hotme.bluetooth.bean.SetTrainingModeRequestBean;
import com.xh.hotme.bluetooth.bean.StopAutoUploadRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import com.xh.hotme.bluetooth.event.DeviceEnergyInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceStorageInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceUsageInfoEvent;
import com.xh.hotme.bluetooth.event.FecRtmpAddUrlEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveAllUrlEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveUrlEvent;
import com.xh.hotme.bluetooth.event.LiveStatusResultEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.event.RkIpcStopEvent;
import com.xh.hotme.bluetooth.event.SetTrainingModeEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.camera.ota.OTAUpdateDialog;
import com.xh.hotme.camera.softap.NetworkConnectManager;
import com.xh.hotme.databinding.ActivityLiveBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.HmLivePlatform;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.live.dialog.DevicesDialogUtils;
import com.xh.hotme.live.dialog.ResolutionPopUtils;
import com.xh.hotme.live.dialog.ShareDialog;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.record.score.ScoreCenterActivity;
import com.xh.hotme.record.video.CameraVideoListActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.NetUtil;
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

import tv.danmaku.ijk.media.player.IMediaPlayer;

@BLEAware
@IEventBus
@FullscreenLandscape(immersive = true)
public class LiveActivity extends BaseActivity<ActivityLiveBinding> implements IRecordControlListener, NetworkConnectManager.OnNetworkStatusChangedListener {
    private final static String TAG = LiveActivity.class.getSimpleName();

    private static final int ZORDER_POWEROFF_DIALOG = 20000;
    private static final int ZORDER_CONFIRM_STOP_UPLOAD_DIALOG = 10000;
    private static final int ZORDER_OTA_DIALOG = 8000;

    //推流 -- 设备
    private boolean _push_to_3rd = false;
    private int _3rdPlt = Constants.LIVE_PLATFORM_XQM;
    private String _url_push = "";
    //拉流 -- 播放器
    private String _url_pull = "";
    private String _url_pull_commentary = null;
    private String _url_push_commentary = null;
    private boolean _commentary = false;

    boolean requestEnergy = false;

    ModalDialog _dialog;

    int _recordTime = 0;
    RecordInfoBean _recordInfoBean;
    private RecordStatusResultBean _recordStatus;
    private String _recordFecVideoId;
    private boolean _pendingSetTrainingMode = false;

    private boolean _showVideoControl = false;
    DeviceUsageInfo _deviceUsageInfo;
    ModalDialog _stopAutoUploadDialog;
    private double _roi_scale = 1;
    //停止录制
    private boolean stopRequest = false;

    int timeoutCheckCount = 0;
    int rtmpUrlCheckCount = 0;

    private boolean _deviceIsRecord = false;
    ModalDialog _noEnoughSpaceDialog;
    private boolean _ignoreLowStorage = false;

    OTAUpdateDialog otaUpdateDialog;
    boolean isCheckOtaVersion = false;
    private final com.xh.hotme.camera.ota.CameraOtaCheck otaPromptCheck =
            new com.xh.hotme.camera.ota.CameraOtaCheck();


    private String str_timeout_lunch_rkipc;
    private String str_lunch_rkipc_fail;
    private String str_request_timeout;

    private boolean isPause = false;

    private static final class FetchResult {
        HmCompetitionLive bean;
        String errMsg;
    }

    private FetchResult _fetchResult = null;

    private static final class RestoreResult {
        HmCompetitionLive bean;
        String errMsg;
    }

    private RestoreResult _restoreResult = null;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
        ToastUtil.s(str_request_timeout);
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetTrainingMode(SetTrainingModeEvent e) {
        dismissLoading();
        updateVideoControlUI();
        String switchMode = getString(R.string.switch_mode_to);
        ToastUtil.s(String.format(switchMode, isTrainingMode() ? getString(R.string.camera_mode_type_training) : getString(R.string.camera_mode_type_normal)));
    }

    @Override
    protected void initView() {

        str_timeout_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_live);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);
        str_request_timeout = getString(R.string.request_timeout);

        BluetoothHandle.addPlayNotifyListener(this);

        //检查网络
        NetworkConnectManager.getInstance().registerNetworkStatusChangeListener(this, false);

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

        viewBinding.btnCopyPullStreamWithCommentary.setOnClickListener(v -> {
//            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//            clip.setPrimaryClip(ClipData.newPlainText("url", (_push_to_3rd || !_commentary) ? _url_pull : _url_pull_commentary));
//            ToastUtil.s(getString(R.string.copied));
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull));
            ToastUtil.s(getString(R.string.copied));
        });
        viewBinding.tvCopyPullStreamWithoutCommentary.setOnClickListener(v -> {
//            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//            clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull));
//            ToastUtil.s(getString(R.string.copied));
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("url", (_push_to_3rd || !_commentary) ? _url_pull : _url_pull_commentary));
            ToastUtil.s(getString(R.string.copied));
        });
        viewBinding.tvCopyPushStreamWithCommentary.setOnClickListener(v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("url", _url_push_commentary));
            ToastUtil.s(getString(R.string.copied));
        });
        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                onBackPressed();
                return true;
            }
        });
        viewBinding.ivBack2.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                onBackPressed();
                return true;
            }
        });
        viewBinding.tvTitle.setText(CameraManager.getDeviceName());
        viewBinding.tvRight.setVisibility(View.VISIBLE);
        viewBinding.tvRight.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(isPlay) {
                    return true;
                }
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                //检查直播任务
                ApiUtil.getLiveTaskList((code, objects) -> {
                    if(code > 0) {
                        startActivity(new Intent(LiveActivity.this,
                            LiveTaskListActivity.class));
                    } else {
                        startActivity(new Intent(LiveActivity.this,
                            AddLiveTaskActivity.class));
                    }
                });

                return true;
            }
        });

        //分享
        viewBinding.ivShare.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                ShareDialog shareDialog = new ShareDialog(LiveActivity.this);

                try {
                    shareDialog.setUrl(SdkApi.share_url() + _recordInfoBean.live_cid);
                    shareDialog.setName(_recordInfoBean.name);
                    shareDialog.setSubName("我正在直播，快来围观！");
                } catch(Exception e) {
                    e.printStackTrace();
                }
                shareDialog.show();

                return false;
            }
        });
        //分辨率
        viewBinding.tvResolution.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                ResolutionPopUtils.showPop(LiveActivity.this,
                    viewBinding.tvResolution, (code, objects) ->
                        viewBinding.tvResolution.setText(code + ""));

                return false;
            }
        });
        //查询电量
        viewBinding.layoutEnergy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }
                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                if(!requestEnergy) {
                    requestEnergy = true;
                    boolean ok = DeviceApiManager.sendCmdSimpleObj(new EnergyInfoRequestBean());
                    if(ok) {
                        _handler.postDelayed(() -> requestEnergy = false, BluetoothManager.RESP_TIMEOUT);
                    }
                } else {
                    ToastUtil.s(getString(R.string.info_searching_and_dont_repeat_do_it));
                }
                return true;
            }
        });

        viewBinding.btnBackgroundRun.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!_deviceIsRecord) return true;

                moveTaskToBack(true);
                return true;
            }
        });
        //比分控制
        viewBinding.btnScoreControl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!_deviceIsRecord) return true;

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                if(_recordInfoBean == null) {
                    ToastUtil.s(getString(R.string.info_firstly_lunch_live));
                    return true;
                }

                ScoreCenterActivity.start(LiveActivity.this, _recordInfoBean);

                return true;
            }
        });
        viewBinding.btnPauseResume.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!_deviceIsRecord) return true;

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }
                if(isPlay) {
                    DevicesDialogUtils.pauseLive(LiveActivity.this, (code, objects) -> {
                        stopRequest = false;
                        boolean isOK = DeviceApiManager.sendCmdSimpleObj(new
                                FecRtmpRemoveAllUrlRequestBean());
                        if(isOK) {
                            showConnectingDialog(getString(R.string.loading_live_pause));
                        } else {
                           showDisconnectToast();
                        }
                    });
                } else {
                    if(TextUtils.isEmpty(_url_push)) {
                        if(_recordInfoBean == null) {
                            return true;
                        }

                        if(TextUtils.isEmpty(_recordInfoBean.live_cid)) {
                            AppTrace.d("当前不在直播中, 不需恢复");
                        } else {
                            restoreLiveStep1_queryCompetition();
                        }
                    } else {
                        boolean isOK = DeviceApiManager.sendCmdSimpleObj(new
                            FecRtmpAddUrlRequestBean(_url_push));
                        if(isOK) {
                            showConnectingDialog(getString(R.string.loading_live_ready));
                        } else {
                           showDisconnectToast();
                        }
                    }
                }

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

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return true;
                }

                if(!_deviceIsRecord) {
                    //检查直播任务
                    ApiUtil.getLiveTaskList(
                        (code2, objects2) -> {
                            if(code2 == 0) {
                                startActivity(new Intent(LiveActivity.this,
                                    AddLiveTaskActivity.class));
                            } else {
                                startActivity(new Intent(LiveActivity.this,
                                    LiveTaskListActivity.class));
                            }
                        });
                } else {
                    DevicesDialogUtils.stopLive(LiveActivity.this, (code, objects) -> {
                        stopRtmp();
                    });
                }

                return false;
            }
        });
        viewBinding.btnFullscreen.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                requestOrientation(isPortrait);
                return false;
            }
        });

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

        viewBinding.loading.loadingMsg.setText(getString(R.string.loading_dialog_load_live_image));

        updateUI();

        //开启Rkipc
        AppTrace.d(TAG, "开启Rkipc");
        DialogUtil.showDialog(getString(R.string.check_live_status));
        DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);

        _handler.postDelayed(() -> {
            //查询电池存储
            CameraMonitor.getImpl().start();
        }, 2000);
        _handler.postDelayed(_recordTimeRunnable, 1000);
        _handler.postDelayed(_netStatusRunnable, 1000);
    }

    private void restoreLiveStep1_queryCompetition() {
        DialogUtil.showDialog("正在恢复直播");
        _restoreResult = new RestoreResult();
        if(TextUtils.isEmpty(_recordInfoBean.live_cid)) {
            _restoreResult.errMsg = "没有比赛id";
            restoreLiveStep6_wrapUp();
            return;
        }

        // get competition
        ApiUtil.getCompetitionLive(_recordInfoBean.live_cid, new IGenericApiCallback<HmCompetitionLive>() {
            @Override
            public void onSuccess(HmCompetitionLive data) {
                _restoreResult.bean = data;
                _commentary = data.commentate == 1;
                _push_to_3rd = !TextUtils.isEmpty(data.livePlatformId);
                try{
                    _3rdPlt = Integer.parseInt(data.livePlatformType);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                toggleCopyGroupVisibility();
                if(_push_to_3rd) {
                    restoreLiveStep2_get3rdPushUrl();
                } else {
                    restoreLiveStep3_getPushUrl();
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getCompetitionLive fail: code=" + code + ", message=" + message);
                _restoreResult.errMsg = message;
                restoreLiveStep6_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void restoreLiveStep2_get3rdPushUrl() {
        ApiUtil.getLivePlatform(_fetchResult.bean.livePlatformId, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _url_push = data.pushStreamAddress;
                restoreLiveStep5_startRkipc();
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getLivePlatform fail: code=" + code + ", message=" + message);
                _fetchResult.errMsg = message;
                restoreLiveStep6_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void restoreLiveStep3_getPushUrl() {
        ApiUtil.getPushStreamWithoutCommentary(_recordInfoBean.live_cid, new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {
                _url_push = data.data;
                restoreLiveStep4_getCommentaryPushUrl();
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getPushStreamWithoutCommentary fail: code=" + code + ", message=" + message);
                _restoreResult.errMsg = "获取推流地址失败, 无法恢复直播";
                restoreLiveStep6_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void restoreLiveStep4_getCommentaryPushUrl() {
        if(_restoreResult.bean.commentate == 1) {
            ApiUtil.getPushStreamWithCommentary(_restoreResult.bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _url_push_commentary = data.data;
                    restoreLiveStep5_startRkipc();
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "getPushStreamWithCommentary fail: code=" + code + ", message=" + message);
                    _restoreResult.errMsg = message;
                    restoreLiveStep6_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        } else {
            restoreLiveStep5_startRkipc();
        }
    }

    private void restoreLiveStep5_startRkipc() {
        boolean isOK = DeviceApiManager.sendCmdSimpleObj(new
            FecRtmpAddUrlRequestBean(_url_push));
        if(isOK) {
            showConnectingDialog(getString(R.string.loading_live_ready));
        } else {
            String error = getString(R.string.info_bluetooth_offline);
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                error = getString(R.string.info_network_disconnect);
            }
            _restoreResult.errMsg = error;
        }
        restoreLiveStep6_wrapUp();
    }

    private void restoreLiveStep6_wrapUp() {
        DialogUtil.dismissDialog();
        if(!TextUtils.isEmpty(_restoreResult.errMsg)) {
            ToastUtil.s(_restoreResult.errMsg);
        }
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
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        if(deviceInfo == null) return false;

        // if no wifi or ethernet, don't check ota
        if(!CameraManager.hasWifi() && !CameraManager.hasEthernet()) {
            return false;
        }

        // 一天只检查一次ota, 今天如果检查过就跳过
        long t = SharedPrefs.getImpl().getLastOTAPromptTime(deviceInfo.device_id);
        if(!Constants.shouldCheckOTAAlways() && TimeUtil.isToday(t)) {
            return false;
        }

        OTAVersionInfo latestOtaInfo = CameraManager.getOTAVersion();
        if(latestOtaInfo != null) {
            int deviceOtaVersion = deviceInfo.ota_version_code;
            int latestOtaVersion = latestOtaInfo.getVersionCode();
            boolean needUpgrade = latestOtaVersion > deviceOtaVersion;
            if(needUpgrade) {
                if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
                    otaUpdateDialog.dismiss();
                }
                otaUpdateDialog = null;
                otaUpdateDialog = new OTAUpdateDialog(LiveActivity.this, latestOtaInfo);
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
                        SharedPrefs.getImpl().saveLastOTAPromptTime(deviceInfo.device_id, 0);
                        CameraManager.clearOTAInfo();
                        if(otaUpdateDialog != null && otaUpdateDialog.isShowing()) {
                            otaUpdateDialog.dismiss();
                        }
                        //释放连接
                        DeviceApiManager.disConnect();
                        finish();
                    }
                });
                dialogStack.push(otaUpdateDialog);
                SharedPrefs.getImpl().saveLastOTAPromptTime(deviceInfo.device_id, System.currentTimeMillis());
                return true;
            }
        }
        return false;
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

    private void showNoEnoughSpaceDialog(boolean force) {
        if(_noEnoughSpaceDialog != null && _noEnoughSpaceDialog.isShowing()) {
            return;
        }
        _noEnoughSpaceDialog = new ModalDialog(LiveActivity.this);
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
            _noEnoughSpaceDialog.setNegativeButton(getString(R.string.manage_later), new ClickGuard.GuardedOnClickListener() {
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

    private void startCameraVideoListActivity() {
        // 因为直播界面需要联通外网, 所以这里不需要检查热点
        CameraVideoListActivity.start(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutoUploadStatusEvent(AutoUploadStatusEvent event) {
        if(event.uploading) {
            showStopAutoUploadDialog();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPowerOffEvent(PowerOffEvent event) {
        if (_recordInfoBean !=null) {
            ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_STOP);
        }

        ModalDialog dialog = new ModalDialog(this);
        dialog.setZOrder(ZORDER_POWEROFF_DIALOG);
        dialog.setTitle(getString(R.string.info_device_exit));
        dialog.setMessage(getString(R.string.info_device_power_off));
        dialog.setPositiveButton(getString(R.string.confirm), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                LiveActivity.this.finish();
                return true;
            }
        });
        dialog.setCancelable(false);
        dialog.hideNegativeButton();
        dialogStack.push(dialog);
    }

    private void dismissStopAutoUploadDialog() {
        if(_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            _stopAutoUploadDialog.dismiss();
        }
        _stopAutoUploadDialog = null;
    }

    private void showStopAutoUploadDialog() {
        if(_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            return;
        }

        _stopAutoUploadDialog = new ModalDialog(LiveActivity.this);
        _stopAutoUploadDialog.setZOrder(ZORDER_CONFIRM_STOP_UPLOAD_DIALOG);
        _stopAutoUploadDialog.setMessage(getString(R.string.stop_auto_upload_during_live));
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
                _stopAutoUploadDialog.dismiss();
                finish();
                return true;
            }
        });
        dialogStack.push(_stopAutoUploadDialog);
    }

    private void updateUI() {
        if(!_deviceIsRecord) {
            dismissConnectingDialog();

            viewBinding.groupCommentary.setVisibility(View.GONE);
            viewBinding.btnCopyPullStreamWithCommentary.setEnabled(false);
            viewBinding.ivCopyPullStream.setEnabled(false);
            viewBinding.tvCopyPullStream.setEnabled(false);
            viewBinding.btnPauseResume.setEnabled(false);
            viewBinding.tvPauseResume.setEnabled(false);
            viewBinding.ivPauseResume.setEnabled(false);
            viewBinding.ivPauseResume.setImageResource(R.drawable.ic_live_pause);
            viewBinding.btnScoreControl.setEnabled(false);
            viewBinding.tvScoreControl.setEnabled(false);
            viewBinding.ivScoreControl.setEnabled(false);
            viewBinding.btnBackgroundRun.setEnabled(false);
            viewBinding.tvBackgroundRun.setEnabled(false);
            viewBinding.icBackgroundRun.setEnabled(false);

            viewBinding.btnRecorder.setText(getString(R.string.live_start));
            viewBinding.indicator.setVisibility(View.GONE);
            viewBinding.recordTime.setText("");
            viewBinding.btnFullscreen.setVisibility(View.GONE);
            viewBinding.tvResolution.setVisibility(View.GONE);
        } else {
            toggleCopyGroupVisibility();

            viewBinding.btnCopyPullStreamWithCommentary.setEnabled(true);
            viewBinding.ivCopyPullStream.setEnabled(true);
            viewBinding.tvCopyPullStream.setEnabled(true);
            viewBinding.btnPauseResume.setEnabled(true);
            viewBinding.tvPauseResume.setEnabled(true);
            viewBinding.ivPauseResume.setEnabled(true);
            viewBinding.btnScoreControl.setEnabled(true);
            viewBinding.tvScoreControl.setEnabled(true);
            viewBinding.ivScoreControl.setEnabled(true);
            viewBinding.btnBackgroundRun.setEnabled(true);
            viewBinding.tvBackgroundRun.setEnabled(true);
            viewBinding.icBackgroundRun.setEnabled(true);

            viewBinding.indicator.setVisibility(View.VISIBLE);

            viewBinding.btnFullscreen.setVisibility(View.VISIBLE);

            if(isPlay) {
//            handler.postDelayed(runnable, 8000);
                viewBinding.playerCover.setVisibility(View.GONE);
                viewBinding.btnRecorder.setText(getString(R.string.live_end));
                viewBinding.tvPauseResume.setText(getString(R.string.live_pause));
                viewBinding.ivPauseResume.setImageResource(R.drawable.ic_live_pause);
//                viewBinding.tvReslution.setVisibility(View.VISIBLE);
            } else {
                viewBinding.playerCover.setVisibility(View.VISIBLE);
                viewBinding.btnRecorder.setText(getString(R.string.stop_record));
                viewBinding.tvPauseResume.setText(getString(R.string.live_continue));
                viewBinding.ivPauseResume.setImageResource(R.mipmap.ic_live_resume_active);
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
            viewBinding.ivShare.setVisibility(View.GONE);
        } else {
            viewBinding.btnShowVideoControl.setVisibility(_showVideoControl ? View.GONE : View.VISIBLE);
            viewBinding.btnHideVideoControl.setVisibility(_showVideoControl ? View.VISIBLE : View.GONE);
            if(_showVideoControl) {
                boolean trainingMode = isTrainingMode();
                viewBinding.btnToggleTrainingMode.setVisibility(View.VISIBLE);
                viewBinding.labelVideoMode.setText(trainingMode ? getString(R.string.camera_mode_type_training) : getString(R.string.camera_mode_type_normal));
                viewBinding.scaleRoi.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.resetRoi.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.joystick.setVisibility(trainingMode ? View.VISIBLE : View.GONE);
                viewBinding.btnIspSettings.setVisibility(View.VISIBLE);
                viewBinding.ispSettingsContainer.setVisibility(View.GONE);
                viewBinding.ivShare.setVisibility(View.VISIBLE);
            } else {
                viewBinding.btnToggleTrainingMode.setVisibility(View.GONE);
                viewBinding.scaleRoi.setVisibility(View.GONE);
                viewBinding.resetRoi.setVisibility(View.GONE);
                viewBinding.joystick.setVisibility(View.GONE);
                viewBinding.btnIspSettings.setVisibility(View.GONE);
                viewBinding.ispSettingsContainer.setVisibility(View.GONE);
                viewBinding.ivShare.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void initData() {
        // check auto upload status
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
        }, 2000);

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

    //停止直播
    private void stopRtmp() {

        //停止直播推流
        if(!TextUtils.isEmpty(_url_push)) {
            AppTrace.d(TAG, "停止直播推流");

            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new FecRtmpRemoveAllUrlRequestBean());
            if(isOk) {
                showConnectingDialog(getString(R.string.loading_stop_live_and_save_live_video));
                stopRequest = true;
            } else {
               showDisconnectToast();
            }
        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
            if(isOk) {
                showConnectingDialog(getString(R.string.loading_stop_record_and_save_live_video));
                stopRequest = true;
            } else {
               showDisconnectToast();
            }
        }

    }

    protected int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /* 进入全屏 */
    @Override
    protected void enterLandscape() {
        viewBinding.portraitLayout.setVisibility(View.GONE);

        viewBinding.portraitPlayerContainer.removeView(viewBinding.groupPlayer);
        ViewGroup parentView = (ViewGroup) viewBinding.groupPlayer.getParent();
        if(parentView != null){
            parentView.removeView(viewBinding.groupPlayer);
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.landscapePlayerContainer.addView(viewBinding.groupPlayer, params);
        viewBinding.landscapeLayout.setVisibility(View.VISIBLE);

        viewBinding.tvResolution.setPadding(dp2px(90), 0, 0, 0);
        viewBinding.rlEye.setVisibility(View.VISIBLE);
        viewBinding.tvNumber.setText((int) (200 + Math.random() * 52000) + "");
        viewBinding.btnFullscreen.setImageResource(R.mipmap.fullscreen_skrink);

        // 训练模式的时候调整某些界面
        updateVideoControlUI();
    }

    @Override
    protected void exitLandscape() {
        viewBinding.portraitLayout.setVisibility(View.VISIBLE);

        viewBinding.landscapePlayerContainer.removeView(viewBinding.groupPlayer);
        viewBinding.landscapeLayout.setVisibility(View.GONE);

        ViewGroup parentView = (ViewGroup) viewBinding.groupPlayer.getParent();
        if(parentView!=null){
            parentView.removeView(viewBinding.groupPlayer);
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.portraitPlayerContainer.addView(viewBinding.groupPlayer, params);

        viewBinding.tvResolution.setPadding(dp2px(0), 0, 0, 0);

        viewBinding.rlEye.setVisibility(View.GONE);
        viewBinding.btnFullscreen.setImageResource(R.mipmap.fullscreen_expand);

        _showVideoControl = false;
        updateVideoControlUI();
    }

    @Override
    public void onBackPressed() {
        if(!isPortrait) {
            requestOrientation(false);
        } else if(_deviceIsRecord) {
            showExitPlayerDialog();
        } else {
            showExitRecordDialog(getString(R.string.is_exit_live_stream_page));
        }
    }

    private void showExitPlayerDialog() {
        showExitRecordDialog(getString(R.string.is_exit_live_stream_page_background_run));
    }

    private void showExitRecordDialog(String message) {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(LiveActivity.this);
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
                //释放连接
                DeviceApiManager.disConnect();
                finish();
                return true;
            }
        });

        _dialog.show();
    }

    private void showRecordStoppedOnLowStorageDialog() {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(LiveActivity.this);
        _dialog.setMessage(getString(R.string.info_record_stopped_on_low_storage));
        _dialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                return true;
            }
        });

        _dialog.show();
    }


    private boolean isPlay = false;
    private boolean playError = false;
    private ModalDialog errorDialog;

    private final Runnable _detectCommentaryStreamRunnable = () -> {
        viewBinding.commentaryPlayer.release(false);
        viewBinding.commentaryPlayer.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        if(_commentary && !TextUtils.isEmpty(_url_pull)) {
            viewBinding.commentaryPlayer.setVideoPath(_url_pull);
            viewBinding.commentaryPlayer.start();
        }
    };

    //开始播放
    private void startPlay() {
        AppTrace.d(TAG, "startPlay...");
        if(isPause){
            AppTrace.d(TAG, "skip play...");
            return;
        }
        viewBinding.player.setVisibility(View.VISIBLE);
        if(viewBinding.player.isPlaying()) {
            viewBinding.player.release(false);
            viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }
        if(viewBinding.commentaryPlayer.isPlaying()) {
            viewBinding.commentaryPlayer.release(false);
            viewBinding.commentaryPlayer.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }

        AppTrace.d(TAG, "开始播放拉流 播放地址:" + _url_pull);
        viewBinding.player.setVideoPath(_url_pull);
        viewBinding.player.start();
        viewBinding.playerCover.setVisibility(View.GONE);
        viewBinding.loading.getRoot().setVisibility(View.VISIBLE);

        viewBinding.player.setOnInfoListener((iMediaPlayer, state, i1) -> {
            AppTrace.t(TAG, "直播状态 onInfo - what: " + state + ", extra: " + i1);
            if(state == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                viewBinding.loading.getRoot().setVisibility(View.GONE);
            } else if(state == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){
                viewBinding.loading.getRoot().setVisibility(View.GONE);
                //说明已经连接到相机了
                if(errorDialog != null && errorDialog.isShowing()){
                    errorDialog.dismiss();
                }
                //说明已经连接到相机了
                if(_restartRkipcDialog != null && _restartRkipcDialog.isShowing()){
                    _restartRkipcDialog.dismiss();
                }
            } else if(state == IMediaPlayer.MEDIA_ERROR_TIMED_OUT){
                viewBinding.loading.getRoot().setVisibility(View.GONE);
            }

            return true;
        });
        viewBinding.player.setOnErrorListener((iMediaPlayer, i, i1) -> {
            AppTrace.t(TAG, "直播错误 onError - framework error: " + i + ", impl error: " + i1);
            if(viewBinding.player.getVisibility() == View.VISIBLE) {
                viewBinding.loading.getRoot().setVisibility(View.GONE);
                if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                    return false;
                }
                if(!TextUtils.isEmpty(_url_push) && (errorDialog == null || !errorDialog.isShowing())) {
                    playError = true;
                    getNetStatus();
                    getLiveStatus();
                }
            }
            return false;
        });
        viewBinding.player.setOnPreparedListener(iMediaPlayer -> {
            AppTrace.d(TAG, "onPrepared");
            viewBinding.loading.getRoot().setVisibility(View.GONE);

            //说明已经连接到相机了
            if(errorDialog != null && errorDialog.isShowing()){
                errorDialog.dismiss();
            }
            //说明已经连接到相机了
            if(_restartRkipcDialog != null && _restartRkipcDialog.isShowing()){
                _restartRkipcDialog.dismiss();
            }

        });

        // for commentary player
        if(_commentary && !TextUtils.isEmpty(_url_pull)) {
            // play
            AppTrace.d(TAG, "开始尝试播放带解说拉流 播放地址:" + _url_pull);
            viewBinding.commentaryPlayer.setVideoPath(_url_pull);
            viewBinding.commentaryPlayer.start();

            // 监听带解说拉流的状态, 如果处于播放态, 暂停无解说拉流, 显示带解决拉流
            viewBinding.commentaryPlayer.setOnInfoListener((iMediaPlayer, state, i1) -> {
                AppTrace.t(TAG, "带解说直播状态 onInfo - what: " + state + ", extra: " + i1);
                switch(state) {
                    case PlayStateParams.STATE_PLAYING:
                        if(viewBinding.player.getVisibility() == View.VISIBLE) {
                            // stop and hide
                            viewBinding.player.release(false);
                            viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
                            viewBinding.player.setVisibility(View.GONE);
                        }
                        break;
                }
                return true;
            });
            viewBinding.commentaryPlayer.setOnErrorListener((iMediaPlayer, i, i1) -> {
                AppTrace.t(TAG, "带解说直播错误 onError - framework error: " + i + ", impl error: " + i1);
                if(viewBinding.player.getVisibility() == View.VISIBLE) {
                    AppTrace.t(TAG, "5秒后重新开始探测解说流");
                    _handler.removeCallbacks(_detectCommentaryStreamRunnable);
                    _handler.postDelayed(_detectCommentaryStreamRunnable, 5000);
                } else {
                    viewBinding.loading.getRoot().setVisibility(View.GONE);
                    if(DevicesDialogUtils.openWifiSetting(LiveActivity.this)) {
                        return false;
                    }
                    if(!TextUtils.isEmpty(_url_push) && (errorDialog == null || !errorDialog.isShowing())) {
                        playError = true;
                        getNetStatus();
                        getLiveStatus();
                    }
                }
                return false;
            });
            viewBinding.commentaryPlayer.setOnPreparedListener(iMediaPlayer -> {
                AppTrace.d(TAG, "带解说直播 onPrepared");
                viewBinding.loading.getRoot().setVisibility(View.GONE);
            });
        }

        // set flag
        isPlay = true;

        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppTrace.d(TAG, "onPause");
        CameraMonitor.getImpl().pause();
        isPause = true;
        //停止播放
        if(viewBinding.player.isPlaying()) {
            viewBinding.player.pause();
            viewBinding.player.stopPlayback();
            viewBinding.player.release(false);
            viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }
        if(viewBinding.commentaryPlayer.isPlaying()) {
            viewBinding.commentaryPlayer.pause();
            viewBinding.commentaryPlayer.stopPlayback();
            viewBinding.commentaryPlayer.release(false);
            viewBinding.commentaryPlayer.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }
        isPlay = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        CameraMonitor.getImpl().resume();
        DevicesDialogUtils.openWifiSetting(LiveActivity.this);
        isPause = false;
        //如果处于直播中，继续播放
        if(!TextUtils.isEmpty(_url_pull) && (!_commentary || !TextUtils.isEmpty(_url_pull_commentary))) {
            startPlay();
        }

        DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
    }

    @Override
    protected void onDestroy() {
        otaPromptCheck.cancel();
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy");
        dismissStopAutoUploadDialog();

        try {
            NetworkConnectManager.getInstance().removeNetworkStatusChangeListener(this, this);
            viewBinding.player.stopPlayback();
            viewBinding.commentaryPlayer.stopPlayback();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(_noEnoughSpaceDialog != null && _noEnoughSpaceDialog.isShowing()) {
            _noEnoughSpaceDialog.dismiss();
        }
        _noEnoughSpaceDialog = null;

        CameraMonitor.getImpl().stop();

        BluetoothHandle.removePlayNotifyListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        runOnUiThread(() -> {
            if(event != null) {
                if(event.status == 1) {
                    //检查录制状态
                    AppTrace.d(TAG, "onRkipc服务已开启，检查录制状态");
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    } else {
                        DialogUtil.dismissDialog();
                       showDisconnectToast();
                    }
                } else {
                    ToastUtil.s(str_lunch_rkipc_fail);
                }
            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFecRtmpAddUrl(FecRtmpAddUrlEvent event) {
        runOnUiThread(() -> {
            dismissConnectingDialog();
            if(event != null && _recordInfoBean !=null && !TextUtils.isEmpty(_recordInfoBean.live_cid)) {
                AppTrace.d(TAG, "FecRtmpAddUrl   开启直播 " + event.status);
                if(event.status == 1) {
                    if (isActive()) {
                        // 目前观测是有两个请求，所以先去掉一个
                         ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_START);
                    }
                    startPreview();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFecRtmpRemoveAllUrl(FecRtmpRemoveAllUrlEvent event) {
        AppTrace.d(TAG, "onFecRtmpRemoveAllUrl");
        runOnUiThread(() -> {
            if(event != null) {
                AppTrace.t(TAG, "onFecRtmpRemoveAllUrl 直播已关闭 ");
                if(event.status == 1) {
                    EventBus.getDefault().post(new LiveStatusResultEvent(0, null));
                } else {
                    // 删除fec直播url不成功,那么我们需要刷一下状态,看看相机那边到底咋了
                    DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                }
                if (_recordInfoBean != null) {
                    if (stopRequest) {
                        AppTrace.t(TAG, "停止录制");
                        //停止录制
                        DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                        stopRequest = false;
                        ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_STOP);
                    } else {
                        dismissConnectingDialog();
                        ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_PAUSE);
                    }
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveUrlEvent(LiveUrlEvent event) {
        if(event != null) {
            _push_to_3rd = event.third_party;
            _3rdPlt = event.third_plt;
            _url_push = event.url_push;
            _url_pull = event.url_pull;
            _url_push_commentary = event.url_push_commentary;
            _url_pull_commentary = event.url_pull_commentary;
            _commentary = event.commentary;
            toggleCopyGroupVisibility();
        }
    }

    private void toggleCopyGroupVisibility() {
        boolean visible = !_push_to_3rd && _commentary && !TextUtils.isEmpty(_url_pull_commentary) && !TextUtils.isEmpty(_url_push_commentary);
        viewBinding.groupCommentary.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void fetchLiveUrlStep1_queryCompetition() {

        if (_recordInfoBean == null) {
            return;
        }

        // check competition id
        _fetchResult = new FetchResult();
        if(TextUtils.isEmpty(_recordInfoBean.live_cid)) {
            _fetchResult.errMsg = "没有比赛id";
            fetchLiveUrlStep6_wrapUp();
            return;
        }

        // check network
        if(!NetUtil.isConnectedAvailableNetwork()) {
            _fetchResult.errMsg = getString(R.string.error_network_error);
            fetchLiveUrlStep6_wrapUp();
            return;
        }

        // get competition
        ApiUtil.getCompetitionLive(_recordInfoBean.live_cid, new IGenericApiCallback<HmCompetitionLive>() {
            @Override
            public void onSuccess(HmCompetitionLive data) {
                _fetchResult.bean = data;
                _commentary = data.commentate == 1;
                _push_to_3rd = !TextUtils.isEmpty(data.livePlatformId);
                try {
                    _3rdPlt = Integer.parseInt(data.livePlatformType);
                } catch (Throwable e){
                    e.printStackTrace();
                }
                toggleCopyGroupVisibility();
                if(_push_to_3rd) {
                    fetchLiveUrlStep2_get3rdPullUrl();
                } else {
                    fetchLiveUrlStep3_getPullUrl();
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getCompetitionLive fail: code=" + code + ", message=" + message);
                _fetchResult.errMsg = message;
                fetchLiveUrlStep6_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void fetchLiveUrlStep2_get3rdPullUrl() {
        ApiUtil.getLivePlatform(_fetchResult.bean.livePlatformId, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _url_pull = data.pullStreamAddress;
                _url_push = data.pushStreamAddress;
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getLivePlatform fail: code=" + code + ", message=" + message);
                _fetchResult.errMsg = message;
            }

            @Override
            public void onFinish() {
                fetchLiveUrlStep6_wrapUp();
            }
        });
    }

    private void fetchLiveUrlStep3_getPullUrl() {
        if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
            ApiUtil.getPullStreamUrlWithCommentaryPassword(_fetchResult.bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _url_pull = data.data;
                    fetchLiveUrlStep4_getCommentaryPushUrl();
                }

                @Override
                public void onFail(String code, String message) {
                    _fetchResult.errMsg = message;
                    AppTrace.d(TAG, "getPullStreamUrlWithCommentaryPassword fail: code=" + code + ", message=" + message);
                }

                @Override
                public void onFinish() {
                    fetchLiveUrlStep6_wrapUp();
                }
            });
        } else {
            ApiUtil.getPullStreamUrlWithoutCommentary(_recordInfoBean.live_cid, new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _url_pull = data.data;
                    fetchLiveUrlStep4_getCommentaryPushUrl();
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "getPullStreamUrlWithoutCommentary fail: code=" + code + ", message=" + message);
                    _fetchResult.errMsg = message;
                    fetchLiveUrlStep6_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        }
    }

    private void fetchLiveUrlStep4_getCommentaryPushUrl() {
        if(_fetchResult.bean != null && _fetchResult.bean.commentate == 1 && TextUtils.isEmpty(_url_push_commentary)) {
            ApiUtil.getPushStreamWithCommentary(_fetchResult.bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _url_push_commentary = data.data;
                    fetchLiveUrlStep5_getCommentaryPullUrl();
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "fetchLiveUrlStep4_getCommentaryPushUrl fail: code=" + code + ", message=" + message);
                    _fetchResult.errMsg = message;
                    fetchLiveUrlStep6_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        } else {
            fetchLiveUrlStep5_getCommentaryPullUrl();
        }
    }

    private void fetchLiveUrlStep5_getCommentaryPullUrl() {
        if(_fetchResult.bean != null && _fetchResult.bean.commentate == 1 && TextUtils.isEmpty(_url_pull_commentary)) {
//            if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
//                ApiUtil.getPullStreamUrlWithCommentaryPassword(_fetchResult.bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
//                    @Override
//                    public void onSuccess(PullStreamAddrBean data) {
//                        _url_pull_commentary = data.data;
//                    }
//
//                    @Override
//                    public void onFail(String code, String message) {
//                        _fetchResult.errMsg = message;
//                        AppTrace.d(TAG, "getPullStreamUrlWithCommentaryPassword fail: code=" + code + ", message=" + message);
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        fetchLiveUrlStep6_wrapUp();
//                    }
//                });
//            } else {
                ApiUtil.getPullStreamUrlWithCommentary(_fetchResult.bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                    @Override
                    public void onSuccess(PullStreamAddrBean data) {
                        _url_pull_commentary = data.data;
                    }

                    @Override
                    public void onFail(String code, String message) {
                        AppTrace.d(TAG, "getPullStreamUrlWithCommentary fail: code=" + code + ", message=" + message);
                        _fetchResult.errMsg = message;
                    }

                    @Override
                    public void onFinish() {
                        fetchLiveUrlStep6_wrapUp();
                    }
                });
//            }
        } else {
            fetchLiveUrlStep6_wrapUp();
        }
    }

    private void fetchLiveUrlStep6_wrapUp() {
        if(!TextUtils.isEmpty(_fetchResult.errMsg)) {
            ToastUtil.s(_fetchResult.errMsg);
        } else {
            if (!TextUtils.isEmpty(_url_pull) && (!_commentary || !TextUtils.isEmpty(_url_pull_commentary))) {
                playError = false;
                startPlay();
            }
        }
    }

    //获取url开始播放
    public void startPreview() {
        if(isPause){
            return;
        }
        if(!TextUtils.isEmpty(_url_pull) && (!_commentary || !TextUtils.isEmpty(_url_pull_commentary))) {
            startPlay();
        } else {
            fetchLiveUrlStep1_queryCompetition();
        }
    }


    @Override
    public void onRecordVideoStart() {
        runOnUiThread(() -> {
            //录制开始
            AppTrace.t(TAG, "onRecordVideoStart 录制开始 检查录制状态" + Thread.currentThread().getName());
            //        获取拉流并播放
            //检查录制状态
            //
            DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());

            _deviceIsRecord = true;
        });
    }


    private final Runnable _recordTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if(_deviceIsRecord) {
                _recordTime += 1;
                runOnUiThread(() -> viewBinding.recordTime.setText(TimeUtil.intToTimeHour(_recordTime)));
            }
            _handler.postDelayed(_recordTimeRunnable, 1000);
        }
    };

    private final Runnable _netStatusRunnable = new Runnable() {
        @Override
        public void run() {
            DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
            _handler.postDelayed(_netStatusRunnable, 30000);
        }
    };

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordInfo) {
        _handler.removeCallbacks(_timeOutRunnable);
        _recordStatus = recordInfo;

        //检查录制状态
        AppTrace.t(TAG, "录制状态为 " + recordInfo.getStatus());
        runOnUiThread(() -> {
            _recordInfoBean = recordInfo.convertRecordInfoBean();
            AppTrace.d(TAG, "onRecordVideoStatusUpdate, recordInfo转换为RecordInfoBean" + _recordInfoBean);
            if (_recordInfoBean != null) {
                AppTrace.d(TAG, "onRecordVideoStatusUpdate, recordInfo转换为RecordInfoBean, competition id: " + _recordInfoBean.live_cid);
            }
            if(recordInfo.getStatus() == 1) {
                _deviceIsRecord = true;
                _recordTime = recordInfo.getDuration();
                updateUI();
                //检查直播状态
                AppTrace.t(TAG, "检查直播状态");
                getLiveStatus();
            } else {
                DialogUtil.dismissDialog();
                _deviceIsRecord = false;
                _recordTime = 0;
                updateUI();

                //检查ota升级
                if(Constants.enableOTA && !isCheckOtaVersion) {
                    AppTrace.d(TAG, "正在检查ota升级");
                    checkOTAVersion();
                    isCheckOtaVersion = true;
                }
            }

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
    }


//    Handler handler = new Handler(Looper.getMainLooper());
//    Runnable runnable = () -> {
//        if (!isDestroyed() && !isFinishing()) {
//            BluetoothManager.getImpl().sendCmdSimpleObj(new LiveStatusRequestBean());
//        }
//    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordStoppedOnLowStorage(RecordStoppedOnLowStorageEvent event) {
        stopRtmp();
        showRecordStoppedOnLowStorageDialog();
    }

    @Override
    public void onRecordVideoStop() {
        AppTrace.d(TAG, "onRecordVideoStop");
        _deviceIsRecord = false;
        _url_pull_commentary = null;
        _url_push_commentary = null;
        _url_pull = null;
        _url_push = null;

//        updateVideoStop(_recordTime);

        ScoreCenterUtil.removeCustomDotRecord();

        runOnUiThread(() -> {
            AppTrace.t(TAG, "onRecordVideoStop 录制停止 ");
            updateUI();
            ToastUtil.s(getString(R.string.info_live_stop));
        });

        //reset videoId
        _recordFecVideoId = "";
    }


    @Subscribe
    public void onFecRtmpRemoveUrl(FecRtmpRemoveUrlEvent event) {
        AppTrace.d(TAG, "onFecRtmpRemoveUrl");
        runOnUiThread(() -> {
            if(event != null) {
                AppTrace.t(TAG, "onFecRtmpRemoveUrl 直播已关闭 ");
                if(event.status == 1) {
                    EventBus.getDefault().post(new LiveStatusResultEvent(0, null));
                } else {
                    // 删除fec直播url不成功,那么我们需要刷一下状态,看看相机那边到底咋了
                    DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
                }
                if (_recordInfoBean != null) {
                    if (stopRequest) {
                        AppTrace.t(TAG, "停止录制");
                        //停止录制
                        DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                        stopRequest = false;
                        ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_STOP);
                    } else {
                        dismissConnectingDialog();
                        ApiUtil.comprehensiveReportCompetitionLiveStatus(_recordInfoBean.live_cid, _url_push, _3rdPlt, Constants.COMPETITION_PAUSE);
                    }
                }
            }
        });

    }

    @Override
    public void onRecordException(String msg) {
        AppTrace.t(TAG, "onRecordException   " + msg);
        onRecordVideoStop();
    }


    //    --- 电量存储 start  ----
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
    public void onStorageInfoEvent(DeviceStorageInfoEvent event) {
        AppTrace.d(TAG, "onStorageInfoEvent");
        _handler.post(() -> {
            if(event != null && event.storageBean != null) {
                updateStorageInfo(event.storageBean);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceUsageInfoEvent(DeviceUsageInfoEvent data) {
        AppTrace.d(TAG, "onDeviceUsageInfoEvent  设备电量和存储返回");
        _handler.post(() -> {
            if(data != null && data.deviceUsageInfo != null) {
                updateUsageInfo(data.deviceUsageInfo);
            }
        });
    }

    private void getNetStatus() {
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
    }

    private void getLiveStatus() {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new LiveStatusRequestBean());
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStatusResultEvent(LiveStatusResultEvent data) {
        _handler.removeCallbacks(_timeOutRunnable);
        try {
            DialogUtil.dismissDialog();
            if(data == null) return;

            // if no status, think not living
            if(data.status == 0 || data.bean == null) {
                AppTrace.d(TAG, "直播状态返回 :未开启");
                isPlay = false;
                _handler.post(() -> updateUI());
                return;
            }

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
                    errorDialog = DevicesDialogUtils.liveCameraNoNetworkError(LiveActivity.this,
                        (code, objects) -> DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean()));
                } else if(isLiving) {
                    switch(errCode) {
                        case LiveUrl.ERROR_SOCKET_CONNECT:
                        case LiveUrl.ERROR_SOCKET_TIMEOUT:
                        case LiveUrl.ERROR_SOCKET_WAIT:
                        case LiveUrl.ERROR_SOCKET_WRITE:
                            _handler.post(() -> {
                                //直播中
                                errorDialog = DevicesDialogUtils.liveSocketError(LiveActivity.this,
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
                                errorDialog = DevicesDialogUtils.livePhoneError(LiveActivity.this,
                                    (code, objects) -> {
                                        startPreview();
                                    });
                            });
                            break;
                    }
                } else {
                    _handler.post(() -> {
                        //直播失败
                        errorDialog = DevicesDialogUtils.liveError(LiveActivity.this,
                            (code, objects) -> {
                                boolean isOK = DeviceApiManager.sendCmdSimpleObj(new
                                    FecRtmpAddUrlRequestBean(_url_push));
                                if(isOK) {
                                    showConnectingDialog(getString(R.string.loading_live_ready));
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
                    updateUI();
                } else {
                    rtmpUrlCheckCount = 0;
                    if(!isPlay ||
                        viewBinding.playerCover.getVisibility() == View.VISIBLE) {
                        startPreview();
                    }
                }
            });
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    public void updateUsageInfo(DeviceUsageInfo data) {
        _deviceUsageInfo = data;
        if(_deviceUsageInfo != null) {

            viewBinding.tvTemp.setText(String.format("%d°C", _deviceUsageInfo.temperature));

            if(_deviceUsageInfo.energy >= 0) {
                viewBinding.tvEnergy.setText(_deviceUsageInfo.energy + "%");
            } else {
                viewBinding.tvEnergy.setText("-");
            }
            if(_deviceUsageInfo.charging == 1) {
                Drawable dra = LiveActivity.this.getDrawable(R.mipmap.energy_charging);
                dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
                viewBinding.tvEnergyLabel.setCompoundDrawables(dra, null, null, null);
            } else {
                Drawable dra = LiveActivity.this.getDrawable(R.mipmap.energy);
                dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
                viewBinding.tvEnergyLabel.setCompoundDrawables(dra, null, null, null);
            }

            updateStorageInfo(_deviceUsageInfo.storage);
        }

    }
    //    --- 电量存储 end  ----

    BluetoothLoadingDialog _connectingDialog;

    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(LiveActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    @Subscribe
    public void onRecordInfo(RecordStartEvent event) {
        if(event != null) {
            _recordInfoBean = event.getRecordInfoBean();
            AppTrace.d(TAG, "RecordStartEvent got, _recordInfoBean updated: " + _recordInfoBean);
            if (_recordInfoBean != null) {
                AppTrace.d(TAG, "RecordStartEvent got, _recordInfoBean updated, competition id: " + _recordInfoBean.live_cid);
            }
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

    ModalDialog _restartRkipcDialog;

    private void showRestartDialog() {
        if(_restartRkipcDialog == null) {
            _restartRkipcDialog = new ModalDialog(LiveActivity.this);
            _restartRkipcDialog.setMessage(getString(R.string.restart_camera_on_live_stream_exception));
            _restartRkipcDialog.setNegativeButton(LiveActivity.this.getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(_restartRkipcDialog != null && _restartRkipcDialog.isShowing()) {
                        _restartRkipcDialog.dismiss();
                    }
                    return true;
                }
            });
            _restartRkipcDialog.setPositiveButton(LiveActivity.this.getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
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
    public void onRkipcStop(RkIpcStopEvent event) {
        AppTrace.d(TAG, "onRkipcStop");

        if(_handler != null) {
            //检查直播状态回调
            _handler.post(() -> {
                DialogUtil.dismissDialog();
                if(event != null) {
                    if(event.status == 1) {
                        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                        if(!isOk) {
                            AppTrace.d(TAG, "启动rkipc命令发送失败");
                        }
                    } else {
                        AppTrace.d(TAG, "rkipc 停止异常状态返回");
                    }
                }
            });
        }
    }

    @Override
    public void onStatusChanged(int newStatus, int oldStatus) {
        new Handler().postDelayed(() -> {
            if(!isDestroyed() && !isFinishing()) {
                if(!NetUtil.isConnectedAvailableNetwork()) {
                    Log.i(TAG, "网络变化 - 无网络");
                    DevicesDialogUtils.openWifiSetting(LiveActivity.this);
                    if(viewBinding.player.isPlaying()) {
                        viewBinding.player.release(false);
                        viewBinding.player.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
                    }
                    if(viewBinding.commentaryPlayer.isPlaying()) {
                        viewBinding.commentaryPlayer.release(false);
                        viewBinding.commentaryPlayer.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
                    }
                } else {
                    Log.i(TAG, "网络变化");
                    DevicesDialogUtils.dismiss();
                    if(!TextUtils.isEmpty(_url_pull) && (!_commentary || !TextUtils.isEmpty(_url_pull_commentary))) {
                        startPlay();
                    }
                }
            }
        }, 500);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkStatusEvent(NetworkStatusEvent event) {
        AppTrace.d(TAG, "onNetworkStatus");
        NetworkBean networkBean = event.n;
        boolean hasSim = CameraManager.isSimValid();
        boolean hasWifi = CameraManager.hasWifi();
        boolean hasEth = CameraManager.hasEthernet();
        if(hasEth) {
            viewBinding.ivSignal.setImageResource(R.mipmap.ic_live_signal_eth);
            viewBinding.tvSignalType.setText(getString(R.string.connected));
        } else if(hasWifi) {
            Resources rs = LiveActivity.this.getResources();
            int id = rs.getIdentifier(String.format("ic_live_signal_wifi_%d", networkBean.getSim().rssi_lv), "mipmap", LiveActivity.this.getPackageName());
            viewBinding.ivSignal.setImageResource(id);
            viewBinding.tvSignalType.setText("WIFI");
        } else if(hasSim) {
            Resources rs = LiveActivity.this.getResources();
            int id = rs.getIdentifier(String.format("ic_live_signal_sim_%d", networkBean.getSim().rssi_lv), "mipmap", LiveActivity.this.getPackageName());
            viewBinding.ivSignal.setImageResource(id);
            viewBinding.tvSignalType.setText("4G");
        } else {
            viewBinding.ivSignal.setImageResource(R.mipmap.ic_live_signal_wifi_3);
            viewBinding.tvSignalType.setText(getString(R.string.without_network));
        }
    }
//    //这里获取不到视频的大小，时长，感觉有问题，不过先按老黄的流程走吧
//    private void addFecVideo(RecordInfoBean recordInfoBean) {
//        HmVideo video = new HmVideo();
//        video.setDeviceId(CameraManager.getDeviceId());
//        video.setCompetitionId(recordInfoBean.live_cid);
//        video.setCompetitionType(Constants.COMPETITION_TYPE_LIVE);
//        video.setVideoClassify(Constants.VIDEO_CLASSIFY_PLAYBACK);
//        video.setVideoStatus(Constants.STATUS_STARTING);
//        video.setUploadType(Constants.UPLOAD_MANUAL);   //手动上传是2； 1是自动上传
//
//        // 使用文件名作为视频名称, 避免重复上传, 但是要看看这个有没有副作用
//        video.setVideoName(recordInfoBean.fec_fn);
//        ApiUtil.addVideo(video, new IGenericApiCallback<HmVideo>() {
//            @Override
//            public void onSuccess(HmVideo data) {
//                if (data != null) {
//                    _recordFecVideoId = data.getVideoId();
//                    _recordInfoBean.videoId = data.getVideoId();
//
//                    AppTrace.d(TAG, "获取视频id：" + _recordInfoBean.videoId);
//                    ScoreCenterActivity.start(LiveActivity.this, _recordInfoBean);
//                } else {
//                    ToastUtil.s("获取视频id失败");
//                }
//            }
//
//            @Override
//            public void onFail(String code, String message) {
//                AppTrace.d(TAG, "上传视频失败：" + message);
//                ToastUtil.s("获取视频id失败");
//            }
//
//            @Override
//            public void onFinish() {
//
//            }
//        });
//    }
//
//    private void updateVideoStop(int duration){
//        if(TextUtils.isEmpty(_recordFecVideoId)){
//           return;
//        }
//
//        VideoDurationUpdateBean uploadBean = new VideoDurationUpdateBean();
//        uploadBean.setVideoId(_recordFecVideoId);
//        uploadBean.setVideoDuration(duration);
//        ApiUtil.updateCompetitionVideoTime(uploadBean, new IApiCallback() {
//            @Override
//            public void onSuccess() {
//                AppTrace.d(TAG, "update success");
//
//            }
//
//            @Override
//            public void onFail(String code, String message) {
//
//            }
//
//            @Override
//            public void onFinish() {
//            }
//        });
//
//    }
}
