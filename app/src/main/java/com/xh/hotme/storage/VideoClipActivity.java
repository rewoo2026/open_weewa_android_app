package com.xh.hotme.storage;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.PostProcProgressRequestBean;
import com.xh.hotme.bluetooth.bean.PostProcStartRequestBean;
import com.xh.hotme.bluetooth.bean.PostProcStatusRequestBean;
import com.xh.hotme.bluetooth.event.PostProcProgressEvent;
import com.xh.hotme.bluetooth.event.PostProcStartEvent;
import com.xh.hotme.bluetooth.event.PostProcStateEvent;
import com.xh.hotme.databinding.ActivityVideoClipBinding;
import com.xh.hotme.databinding.LayoutVideoClipEmptyBinding;
import com.xh.hotme.databinding.LayoutVideoClipFailBinding;
import com.xh.hotme.databinding.LayoutVideoClipFinishBinding;
import com.xh.hotme.databinding.LayoutVideoClipProcessBinding;
import com.xh.hotme.databinding.LayoutVideoClipSettingBinding;
import com.xh.hotme.databinding.LayoutVideoClipWaitBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


@IEventBus
public class VideoClipActivity extends BaseActivity<ActivityVideoClipBinding> {
    LayoutVideoClipProcessBinding procBind;
    LayoutVideoClipSettingBinding settingBind;
    LayoutVideoClipEmptyBinding emptyView;
    LayoutVideoClipWaitBinding waitView;
    LayoutVideoClipFailBinding failView;
    LayoutVideoClipFinishBinding finishBind;

    BluetoothLoadingDialog _connectingDialog;

    private final static int state_empty = 0;
    private final static int state_idle = 1;
    private final static int state_wait = 2;
    private final static int state_process = 3;
    private final static int state_ok = 4;
    private final static int state_error = 5;
    private final static int state_finish = 6;

    int cur_state;

    float _threshold;
    long _videoDuration;

    private final static int _interval = 1000;

    private boolean isFirstClip = true;


    private int _type = 0;
    private int _clipType = 2;
    public static final String TYPE = "type";
    public static final String CLIP_TYPE = "clip_type";
    public static final int type_storage = 0;
    public static final int type_setting = 1;

    public static final int clip_type_all = 0;
    public static final int clip_type_ai = 1;
    public static final int clip_type_manual_dotting = 2;

    private final Runnable _timeOutRunnable = () -> {
        try {
            dismissConnectingDialog();
        } catch (Throwable e) {

        }
    };

    private final Runnable _postProcProgressRunnable = () -> {
        try {
            getPostProcProgress();
        } catch (Throwable e) {
        }
    };

    public static void start(Context context, int type, int clipType) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, VideoClipActivity.class);
        intent.putExtra(TYPE, type);
        intent.putExtra(CLIP_TYPE, clipType);
        context.startActivity(intent);
    }

    public void onDestroy(){
        super.onDestroy();

        //断开蓝牙连接
        if(_type == type_setting){
            DeviceApiManager.disConnect();
        }
    }


    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(getString(R.string.video_clip_title));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        emptyView = LayoutVideoClipEmptyBinding.bind(viewBinding.getRoot());
        waitView = LayoutVideoClipWaitBinding.bind(viewBinding.getRoot());
        settingBind = LayoutVideoClipSettingBinding.bind(viewBinding.getRoot());
        procBind = LayoutVideoClipProcessBinding.bind(viewBinding.getRoot());
        failView = LayoutVideoClipFailBinding.bind(viewBinding.getRoot());
        finishBind = LayoutVideoClipFinishBinding.bind(viewBinding.getRoot());

        settingBind.btnStartPostProc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_clipType != VideoClipActivity.clip_type_manual_dotting ) {
                    String thresholdStr = settingBind.etNumber.getText().toString();
                    if (TextUtils.isEmpty(thresholdStr)) {
                        ToastUtil.s(getString(R.string.info_threshold_empty));
                        return true;
                    }

                    float threshold = Float.parseFloat(thresholdStr) / 100;
                    if (threshold > 1) {
                        ToastUtil.s(getString(R.string.info_threshold_format_error));
                        return true;
                    }
                }
                String durationStr = settingBind.etVideoDuration.getText().toString();
                if (TextUtils.isEmpty(durationStr)) {
                    ToastUtil.s(getString(R.string.info_video_duration_empty));
                    return true;
                }
                long duration = Long.parseLong(durationStr);
                if (duration < 15 || duration > 1800) {
                    ToastUtil.s(getString(R.string.info_video_duration_error));
                    return true;
                }
                _threshold = 0.95f; //缺省值设置成95
                _videoDuration = duration * 1000;
                isFirstClip = false;

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PostProcStartRequestBean(_clipType, _threshold, _videoDuration));
                return true;
            }
        });
        procBind.btnOk.setOnClickListener(v -> {
            cur_state = state_finish;
            updateStatus(0);
        });

        failView.btnStartPostProcAgain.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (_threshold == 0 || _videoDuration == 0) {
                    cur_state = state_wait;
                    updateStatus(0);
                    return true;
                }

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PostProcStartRequestBean(_clipType,_threshold, _videoDuration));
                return true;
            }
        });

        waitView.btnSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                cur_state = state_wait;
                updateStatus(0);
                return true;
            }
        });

        cur_state = state_idle;
        updateStatus(0);

        _threshold = 0.95F;
        _videoDuration = 30 * 1000;

        //设置置信度缺省值95
        settingBind.etNumber.setText("95");
        //设置剪辑视频时长30秒
        settingBind.etVideoDuration.setText("30");
    }

    @Override
    protected void initData() {
        if (getIntent() != null) {
            _type = getIntent().getIntExtra(TYPE, type_storage);
            _clipType = getIntent().getIntExtra(CLIP_TYPE, 2);
        }

        if(_clipType == 2){  //手动打点，隐藏置信度
            settingBind.etNumber.setVisibility(View.GONE);
            settingBind.tvThreshold.setVisibility(View.GONE);
            settingBind.btnStartPostProc.setText(getString(R.string.btn_start_video_clip_manual_dotting));
        }else if(_clipType == 1){
            settingBind.etNumber.setVisibility(View.VISIBLE);
            settingBind.tvThreshold.setVisibility(View.VISIBLE);
            settingBind.btnStartPostProc.setText(getString(R.string.btn_start_video_clip_ai));
        }else if(_clipType == 0){
            settingBind.etNumber.setVisibility(View.VISIBLE);
            settingBind.tvThreshold.setVisibility(View.VISIBLE);
            settingBind.btnStartPostProc.setText(getString(R.string.btn_start_video_clip));
        }

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PostProcStatusRequestBean(_clipType));
        if (isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            showConnectingDialog(getString(R.string.loading_video_clip_status));
        }
    }

    private void updateStatus(int progress) {
        if (cur_state == state_ok) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.VISIBLE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.ivProcessState.setImageResource(R.mipmap.postproc_ok);
            procBind.progressBar.setProgress(100);
            procBind.tvProgress.setText("100%");
            procBind.btnOk.setVisibility(View.VISIBLE);
            procBind.tvStatus.setText(getString(R.string.info_video_clip_finish));

        } else if (cur_state == state_process) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.VISIBLE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.ivProcessState.setImageResource(R.mipmap.postproc_wait);
            procBind.progressBar.setProgress(progress);
            procBind.tvProgress.setText(progress + "%");
            procBind.tvStatus.setText(getString(R.string.video_cliping));

        } else if (cur_state == state_idle) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.VISIBLE);
            procBind.layoutProcessing.setVisibility(View.GONE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.progressBar.setProgress(0);
            procBind.tvProgress.setText("0%");
        } else if (cur_state == state_wait) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.GONE);
            settingBind.layoutSetting.setVisibility(View.VISIBLE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.progressBar.setProgress(0);
            procBind.tvProgress.setText("0%");
        } else if (cur_state == state_empty) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.VISIBLE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.GONE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.progressBar.setProgress(0);
            procBind.tvProgress.setText("0%");
        } else if (cur_state == state_error) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.GONE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.VISIBLE);

            procBind.progressBar.setProgress(0);
            procBind.tvProgress.setText("0%");
        } else if (cur_state == state_finish) {
            finishBind.layoutFinish.setVisibility(View.VISIBLE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            waitView.layoutWait.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.GONE);
            settingBind.layoutSetting.setVisibility(View.GONE);
            failView.layoutFail.setVisibility(View.GONE);

            procBind.progressBar.setProgress(0);
            procBind.tvProgress.setText("0%");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStateEvent(PostProcStateEvent event) {
        dismissConnectingDialog();
        if (!event.postProcInfo.error.isEmpty()) {
            cur_state = state_error;
            if (isFirstClip) {
                cur_state = state_wait;
            }
        } else if (!event.postProcInfo.ongoing.isEmpty()) {
            cur_state = state_process;
        } else if (!event.postProcInfo.wait.isEmpty()) {
            cur_state = state_wait;
        } else {
            cur_state = state_empty;
        }

        updateStatus(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartEvent(PostProcStartEvent event) {
        if (event.state == 1) {
            cur_state = state_process;
            updateStatus(0);
            _handler.postDelayed(_postProcProgressRunnable, _interval);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostProcProgress(PostProcProgressEvent event) {
        if (event.progress == -1) {
            cur_state = state_ok;
            updateStatus(100);
            _handler.removeCallbacks(_postProcProgressRunnable);

        } else if (event.progress >= 0 && event.progress <= 100) {
            cur_state = state_process;
            updateStatus(event.progress);

            _handler.postDelayed(_postProcProgressRunnable, _interval);
        }
    }

    private void showConnectingDialog(String message) {
        AppTrace.d(TAG, "showConnectingDialog: " + message);
        if (!BaseAppUtil.isDestroy(VideoClipActivity.this)) {
            VideoClipActivity.this.runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                    _connectingDialog = null;
                }
                _connectingDialog = new BluetoothLoadingDialog(VideoClipActivity.this);
                _connectingDialog.setMessage(message);
                _connectingDialog.setOnClickListener((dialog, which) -> {
                });
                _connectingDialog.show();
            });
        }
    }


    private void dismissConnectingDialog() {
        AppTrace.d(TAG, "dismissConnectingDialog");
        if (!BaseAppUtil.isDestroy(VideoClipActivity.this)) {
            VideoClipActivity.this.runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                }
                _connectingDialog = null;
            });
        }
    }

    private void getPostProcProgress() {
        if (cur_state == state_process) {
            boolean ok = DeviceApiManager.sendCmdSimpleObj(new PostProcProgressRequestBean());
            if (!ok) {
                AppTrace.d(TAG, "send fail");
            }

        }

    }
}
