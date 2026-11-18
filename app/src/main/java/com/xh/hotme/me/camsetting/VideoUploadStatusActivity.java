package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.AutoUploadStatusRequestBean;
import com.xh.hotme.bluetooth.bean.StartAutoUploadRequestBean;
import com.xh.hotme.bluetooth.event.AutoUploadStartEvent;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import com.xh.hotme.databinding.ActivityCameraVideoUploadBinding;

import com.xh.hotme.databinding.LayoutCameraVideoUploadEmptyBinding;
import com.xh.hotme.databinding.LayoutCameraVideoUploadFinishBinding;
import com.xh.hotme.databinding.LayoutCameraVideoUploadProcessBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


@IEventBus
public class VideoUploadStatusActivity extends BaseActivity<ActivityCameraVideoUploadBinding> {


    LayoutCameraVideoUploadProcessBinding procBind;
    LayoutCameraVideoUploadEmptyBinding emptyView;
    LayoutCameraVideoUploadFinishBinding finishBind;

    private final static int state_empty = 0;
    private final static int state_process = 1;
    private final static int state_ok = 2;

    int cur_state = state_process;

    private final static int _interval = 1000;

    private boolean initStatus = false;

    ModalDialog _errorDialog;


    private final Runnable _timeOutRunnable = () -> {
        try {
            DialogUtil.dismissDialog();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    private final Runnable _postProcProgressRunnable = () -> {
        try {
            getUploadProgress();
        } catch (Throwable e) {
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

        emptyView = LayoutCameraVideoUploadEmptyBinding.bind(viewBinding.getRoot());
        procBind = LayoutCameraVideoUploadProcessBinding.bind(viewBinding.getRoot());
        finishBind = LayoutCameraVideoUploadFinishBinding.bind(viewBinding.getRoot());

        finishBind.btnOk.setOnClickListener(v -> {
            cur_state = state_ok;
            finish();
        });

        //初始化状态
        cur_state = state_process;
        updateStatus(0);
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getUploadProgress();
            }
        }, 100);
    }

    public static void start(Context context) {
        if (context == null) {
            return;
        }
        context.startActivity(new Intent(context, VideoUploadStatusActivity.class));
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
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutoUploadStatusEvent(AutoUploadStatusEvent event) {
        DialogUtil.dismissDialog();
        if(!event.hasUpload){
            cur_state = state_empty;
            updateStatus(0);
        } else {
            if (!event.isForceEnd) {
                cur_state = state_process;
                updateStatus(event.progress);
                _handler.postDelayed(_postProcProgressRunnable, _interval);
            } else {
                if(event.failed != null && !event.failed.isEmpty()){
                    showErrorDialog();
                } else {
                    cur_state = state_ok;
                    updateStatus(100);
                }
            }
        }

    }


    private void getUploadProgress() {
        if (cur_state == state_process) {
            boolean ok = DeviceApiManager.sendCmdSimpleObj(new AutoUploadStatusRequestBean());
            if (!ok) {
                AppTrace.d(TAG, "send fail");
                return;
            }
            if(!initStatus){
                DialogUtil.showDialog(getString(R.string.check_upload_file_status));
                initStatus = true;
            }
        }
    }


    private void updateStatus(int progress) {
        if (cur_state == state_ok) {
            finishBind.layoutFinish.setVisibility(View.VISIBLE);
            emptyView.layoutEmpty.setVisibility(View.GONE);

            procBind.layoutProcessing.setVisibility(View.GONE);
        } else if (cur_state == state_process) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.GONE);
            procBind.layoutProcessing.setVisibility(View.VISIBLE);

            procBind.progressBar.setProgress(progress);
            procBind.tvProgress.setText(progress + "%");

        } else if (cur_state == state_empty) {
            finishBind.layoutFinish.setVisibility(View.GONE);
            emptyView.layoutEmpty.setVisibility(View.VISIBLE);
            procBind.layoutProcessing.setVisibility(View.GONE);
        }
    }

    private void showErrorDialog(){
        _errorDialog = new ModalDialog(VideoUploadStatusActivity.this);
        String title = getString(R.string.video_clip_title_help);
        _errorDialog.setTitle(title);
        _errorDialog.setMessage(getString(R.string.video_upload_error_message));
        _errorDialog.hideNegativeButton();

        _errorDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                restartUpload();
                _errorDialog.dismiss();
                return true;
            }
        });

        _errorDialog.show();
    }

    private void restartUpload(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        StartAutoUploadRequestBean bean = new StartAutoUploadRequestBean();
        boolean isOK = DeviceApiManager.sendCmdSimpleObj(bean);
        if (isOK) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartAutoUploadEvent(AutoUploadStartEvent event) {
        AppTrace.d(TAG, "onStartAutoUploadEvent");
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cur_state = state_process;
                getUploadProgress();
            }
        }, 100);

    }
}
