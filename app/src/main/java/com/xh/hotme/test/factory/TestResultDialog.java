package com.xh.hotme.test.factory;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogConnectFailBinding;
import com.xh.hotme.databinding.TestDialogConfirmBinding;
import com.xh.hotme.utils.ClickGuard;


public class TestResultDialog extends BaseDialog<TestDialogConfirmBinding> {
    public interface ICallback {
        void onCancel(TestModelBean testModelBean);

        void onTryAgain(TestModelBean testModelBean);

        void onReboot(TestModelBean testModelBean);

        void onPass(TestModelBean testModelBean);

        void onFail(TestModelBean testModelBean);
    }

    public interface IMediaCallback {
        void onPlayAnrIn(TestModelBean testModelBean);

        void onPlayAnrOut(TestModelBean testModelBean);

        void onPlayAgcOut(TestModelBean testModelBean);

        void onPlayHpfOut(TestModelBean testModelBean);

        void onPlayArecord(TestModelBean testModelBean);
    }

    public interface IRecordCallback {
        void onRecordStart(TestModelBean testModelBean);

        void onRecordStop(TestModelBean testModelBean);


        void onDownload(TestModelBean testModelBean);
    }

    private ICallback iCallback;
    private IMediaCallback iMediaCallback;

    private IRecordCallback iRecordCallback;


    TestModelBean _testModelBean;

    boolean isShowReboot = false;

    boolean isAudioSelected = false;
    boolean isCameraSelected = false;

    boolean isRecording = false;


    public TestResultDialog(@NonNull Context context) {
        super(context);
    }


    public TestResultDialog setTestModelBean(TestModelBean testModelBean) {
        this._testModelBean = testModelBean;
        updateModel();
        return this;
    }

    public TestResultDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public TestResultDialog setMediaCallback(IMediaCallback iCallback) {
        this.iMediaCallback = iCallback;
        return this;
    }

    public TestResultDialog setRecordCallback(IRecordCallback iCallback) {
        this.iRecordCallback = iCallback;
        return this;
    }


    @Override
    protected void onInit() {
        viewBinding.dlgTitleView.setVisibility(View.VISIBLE);
        viewBinding.dlgBtnView.setVisibility(View.VISIBLE);
        viewBinding.dlgLeftBtn.setOnClickListener(v -> {
            cancel();
            if (iCallback != null) {
                iCallback.onFail(_testModelBean);
            }
        });

        viewBinding.dlgRightBtn.setOnClickListener(v -> {
            dismiss();
            if (iCallback != null) {
                iCallback.onPass(_testModelBean);
            }
        });

        viewBinding.dlgRetryBtn.setOnClickListener(v -> {
            if (iCallback != null) {
                iCallback.onTryAgain(_testModelBean);
            }
        });

        viewBinding.dlgRebootBtn.setOnClickListener(v -> {
            if (iCallback != null) {
                iCallback.onReboot(_testModelBean);
            }
        });

        viewBinding.dlgAnrInPlay.setOnClickListener(v -> {
            if (iMediaCallback != null) {
                iMediaCallback.onPlayAnrIn(_testModelBean);
            }
        });
        viewBinding.dlgAnrOutPlay.setOnClickListener(v -> {
            if (iMediaCallback != null) {
                iMediaCallback.onPlayAnrOut(_testModelBean);
            }
        });

        viewBinding.dlgAgcOutPlay.setOnClickListener(v -> {
            if (iMediaCallback != null) {
                iMediaCallback.onPlayAgcOut(_testModelBean);
            }
        });
        viewBinding.dlgHpfOutPlay.setOnClickListener(v -> {
            if (iMediaCallback != null) {
                iMediaCallback.onPlayHpfOut(_testModelBean);
            }
        });

        viewBinding.dlgArecordPlay.setOnClickListener(v -> {
            if (iMediaCallback != null) {
                iMediaCallback.onPlayArecord(_testModelBean);
            }
        });

        viewBinding.dlgAudioRecord.setOnClickListener(v -> {
            if (!isRecording) {
                if (iRecordCallback != null) {
                    iRecordCallback.onRecordStart(_testModelBean);
                }
                isRecording = true;
            } else {
                if (iRecordCallback != null) {
                    iRecordCallback.onRecordStop(_testModelBean);
                }
                isRecording = false;
            }
        });

        viewBinding.dlgAudioDownload.setOnClickListener(v -> {

            if (iRecordCallback != null) {
                iRecordCallback.onDownload(_testModelBean);
            }
        });

        updateModel();

    }

    public void updateModel(){
        if (_testModelBean != null) {

            viewBinding.dlgTitle.setText(this._testModelBean.getName());
            viewBinding.dlgMsg.setText(this._testModelBean.getDescription());

            if (_testModelBean.getType() == TestModelBean.TYPE_RTC_TIME
                    || _testModelBean.getType() == TestModelBean.TYPE_NETWORK_4G
                    || _testModelBean.getType() == TestModelBean.TYPE_NETWORK_ETH
            ) {
                viewBinding.dlgRebootBtn.setVisibility(View.VISIBLE);
            }
            if (_testModelBean.getType() == TestModelBean.TYPE_AUDIO) {
                viewBinding.dlgAgainView.setVisibility(View.GONE);
                viewBinding.dlgAudioView.setVisibility(View.VISIBLE);

                viewBinding.dlgDumpView.setVisibility(View.VISIBLE);
                viewBinding.dlgArecordView.setVisibility(View.GONE);
            } else if (_testModelBean.getType() == TestModelBean.TYPE_ARECORD) {
                viewBinding.dlgAgainView.setVisibility(View.GONE);
                viewBinding.dlgAudioView.setVisibility(View.VISIBLE);

                viewBinding.dlgDumpView.setVisibility(View.GONE);
                viewBinding.dlgArecordView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void showRebootBtn(boolean isShowReboot) {
        viewBinding.dlgRebootBtn.setVisibility(isShowReboot ? View.VISIBLE : View.INVISIBLE);
    }

    public void setExtendInfo(String extendInfo) {
        viewBinding.dlgExtendMsg.setText(extendInfo);
    }

    public void addExtendInfo(String extendInfo) {
        String info = viewBinding.dlgExtendMsg.getText().toString();
        if (TextUtils.isEmpty(info)) {
            viewBinding.dlgExtendMsg.setText(extendInfo);
        } else {
            viewBinding.dlgExtendMsg.setText(info + "\n" + extendInfo);
        }

    }

    public String getExtendInfo() {
        return viewBinding.dlgExtendMsg.getText().toString();
    }

    public void showDownloadView(boolean isShow) {
        viewBinding.dlgDownloadView.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public void setRecordStatus(boolean isRecord) {
        viewBinding.dlgAudioRecord.setText(isRecord ? "停止录制" : "开始录制");
    }
}