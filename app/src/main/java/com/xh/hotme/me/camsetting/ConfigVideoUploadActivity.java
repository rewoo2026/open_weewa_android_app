package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.LiveUrl;
import com.xh.hotme.bean.PostProc;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.LiveStatusRequestBean;
import com.xh.hotme.bluetooth.bean.PostProcStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.StartAutoUploadRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsRequestBean;
import com.xh.hotme.bluetooth.event.AutoUploadStartEvent;
import com.xh.hotme.bluetooth.event.LiveStatusResultEvent;
import com.xh.hotme.bluetooth.event.PostProcStateEvent;
import com.xh.hotme.bluetooth.event.RecordStatusResultEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsUpdateEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.CameraNetworkSettingActivity;
import com.xh.hotme.databinding.ActivityConfigVideoUploadBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.storage.VideoClipActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

@IEventBus
public class ConfigVideoUploadActivity extends BaseActivity<ActivityConfigVideoUploadBinding> {
    private ModalDialog _clipVideoDialog;
    private ModalDialog _uploadingDialog;
    private ModalDialog _networkDialog;
    private ModalDialog _recordingDialog;

    private PostProc _postProc;
    boolean unhandleProVideo;

    boolean isCheckProVideo = false;

    private boolean isAutoUpload = false;
    private boolean isCheckSettings = false;

    private int isRecording = -1;
    private int isLiving = -1;

    private final Runnable _timeOutRunnable = () -> {
        try {
            DialogUtil.dismissDialog();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(R.string.video_upload);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.cbAvsVideo.setChecked(true);
        viewBinding.cbRecordVideo.setChecked(true);
        viewBinding.cbAiVideo.setChecked(true);

        viewBinding.btnAvsUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewBinding.cbAvsVideo.setChecked(!viewBinding.cbAvsVideo.isChecked());
            }
        });
        viewBinding.btnRecordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewBinding.cbRecordVideo.setChecked(!viewBinding.cbRecordVideo.isChecked());
            }
        });
        viewBinding.btnAiVideoUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewBinding.cbAiVideo.setChecked(!viewBinding.cbAiVideo.isChecked());
            }
        });

        viewBinding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewBinding.cbAiVideo.isChecked()) {
                    if (!isCheckProVideo) {

                        if(!DeviceApiManager.isConnected()){
                            showDisconnectDialog();
                            return;
                        }

                        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PostProcStatusRequestBean(VideoClipActivity.clip_type_manual_dotting));
                        if (isOk) {
                            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                            DialogUtil.showDialog(getString(R.string.loading_video_clip_status));
                        }
                    } else {
                        startUpload();
                    }
                } else {
                    startUpload();
                }
            }
        });


    }

    private void startUpload() {
        AppTrace.d(TAG, "startUpload");

        if (!CameraManager.hasWifi() && !CameraManager.hasEthernet()) {
            if (_networkDialog != null && _networkDialog.isShowing()) {
                _networkDialog.dismiss();
            }
            _networkDialog = null;

            _networkDialog = new ModalDialog(ConfigVideoUploadActivity.this);
            _networkDialog.setTitle(getString(R.string.connect_network));
            _networkDialog.setMessage(getString(R.string.video_upload_connect_workd_tip));
            _networkDialog.hideNegativeButton();
            _networkDialog.setPositiveButton(getString(R.string.connect_network), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    Device connectDevice = BluetoothManager.getConnectDevice();
                    CameraNetworkSettingActivity.start(ConfigVideoUploadActivity.this, connectDevice, CameraNetworkSettingActivity.REQUEST_NETWORK_CONFIG);
                    _networkDialog.dismiss();
                    return true;
                }
            });

            _networkDialog.show();
            return;
        }

        if (isLiving == -1 || isRecording == -1) {
            ToastUtil.s(getString(R.string.video_upload_sync_status));
            return;
        }

        if (isLiving == 1 || isRecording == 1) {
            if (_recordingDialog != null && _recordingDialog.isShowing()) {
                _recordingDialog.dismiss();
            }
            _recordingDialog = null;

            _recordingDialog = new ModalDialog(ConfigVideoUploadActivity.this);
            _recordingDialog.setMessage(isLiving == 1 ? getString(R.string.video_upload_stop_living) : getString(R.string.video_upload_stop_recording));
            _recordingDialog.hideNegativeButton();
            _recordingDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    _recordingDialog.dismiss();
                    return true;
                }
            });
            _recordingDialog.show();
            return;
        }

        UpdateSettingsRequestBean bean = new UpdateSettingsRequestBean();
        bean.setAutoUploadFec(viewBinding.cbRecordVideo.isChecked() ? 1 : 0);
        bean.setAutoUploadAvs(viewBinding.cbAvsVideo.isChecked() ? 1 : 0);
        bean.setAutoUploadHl(viewBinding.cbAiVideo.isChecked() ? 1 : 0);
        boolean isOK = DeviceApiManager.sendCmdSimpleObj(bean);
        if (isOK) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
        }

    }

    @Override
    protected void initData() {
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        // update settings
        DeviceApiManager.sendCmdSimpleObj(new GetSettingsRequestBean());

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
            }
        }, 50);

        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DeviceApiManager.sendCmdSimpleObj(new LiveStatusRequestBean());
            }
        }, 1000);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }

        if (_uploadingDialog != null && _uploadingDialog.isShowing()) {
            _uploadingDialog.dismiss();
        }
        _uploadingDialog = null;

        if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
            _clipVideoDialog.dismiss();
        }
        _clipVideoDialog = null;

        if (_networkDialog != null && _networkDialog.isShowing()) {
            _networkDialog.dismiss();
        }
        _networkDialog = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStateEvent(PostProcStateEvent event) {
        AppTrace.d(TAG, "PostProcStateEvent");
        DialogUtil.dismissDialog();
        _postProc = event.postProcInfo;
        List<String> waitList = _postProc.wait;
        unhandleProVideo = waitList != null && !waitList.isEmpty();
        isCheckProVideo = true;
        if (unhandleProVideo) {
            if (!BaseAppUtil.isDestroy(ConfigVideoUploadActivity.this) && BaseAppUtil.isForegroundActivity(ConfigVideoUploadActivity.this, ConfigVideoUploadActivity.this.getClass().getName())) {

                if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
                    _clipVideoDialog.dismiss();
                }
                _clipVideoDialog = null;

                _clipVideoDialog = new ModalDialog(ConfigVideoUploadActivity.this);
                String title = getString(R.string.video_clip_title_help);
                _clipVideoDialog.setTitle(title);
                _clipVideoDialog.setMessage(getString(R.string.video_clip_content));
                _clipVideoDialog.setPositiveButton(getString(R.string.video_clip_btn_clip), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        _clipVideoDialog.dismiss();
                        startActivity(new Intent(ConfigVideoUploadActivity.this, VideoClipActivity.class));
                        return true;
                    }
                });

                _clipVideoDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        _clipVideoDialog.dismiss();
                        return true;
                    }
                });

                _clipVideoDialog.show();
            }
        } else {
            if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
                _clipVideoDialog.dismiss();
            }
            _clipVideoDialog = null;

            if (!viewBinding.cbRecordVideo.isChecked() && !viewBinding.cbRecordVideo.isChecked()) {
                _clipVideoDialog = new ModalDialog(ConfigVideoUploadActivity.this);
                String title = getString(R.string.video_clip_title_help);
                _clipVideoDialog.setTitle(title);
                _clipVideoDialog.setMessage(getString(R.string.video_upload_without_clip_content));
                _clipVideoDialog.hideNegativeButton();

                _clipVideoDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        _clipVideoDialog.dismiss();
                        return true;
                    }
                });

                _clipVideoDialog.show();
                return;
            }

            if (isCheckSettings) {
                startUpload();
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettings(WeewaSettingsEvent event) {
        AppTrace.d(TAG, "onUpdateSettings");
        if (!BaseAppUtil.isDestroy(this)) {
            runOnUiThread(() -> {
                if (event != null && event.settings != null) {
                    isAutoUpload = event.settings.getAuto_upload() != 0;
                }
                isCheckSettings = true;
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateSettings(WeewaSettingsUpdateEvent event) {
        AppTrace.d(TAG, "onUpdateSettings");
        if (!BaseAppUtil.isDestroy(this)) {
            runOnUiThread(() -> {
                StartAutoUploadRequestBean bean = new StartAutoUploadRequestBean();
                boolean isOK = DeviceApiManager.sendCmdSimpleObj(bean);
                if (isOK) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                } else {
                    showDisconnectToast();
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartAutoUpload(AutoUploadStartEvent event) {
        AppTrace.d(TAG, "onStartAutoUpload");

        if(!BaseAppUtil.isDestroy(ConfigVideoUploadActivity.this) && BaseAppUtil.isForegroundActivity(ConfigVideoUploadActivity.this, ConfigVideoUploadActivity.this.getClass().getName())) {
            //更新成功后。则跳转到下载进度页面上
            VideoUploadStatusActivity.start(ConfigVideoUploadActivity.this);
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStatusResultEvent(LiveStatusResultEvent data) {
        if (data == null || data.bean == null) {
            isLiving = 0;
        } else {
            isLiving = 0;
            // 判断是否有fec直播进行中, 这里我们不管他是否连上了, 只要有外部推流地址就行
            try {
                for (LiveUrl info : data.bean.fec_live_urls) {
                    boolean isLocal = info.url.contains("127.0.0.1") || info.url.contains("localhost");
                    if (!isLocal) {
                        isLiving = 1;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordVideoStatusEvent(RecordStatusResultEvent data) {
        if (data == null || data.bean == null) {
            isRecording = 0;
        } else {
            isRecording = data.bean.getStatus() == 1 ? 1 : 0;
        }
    }
}
