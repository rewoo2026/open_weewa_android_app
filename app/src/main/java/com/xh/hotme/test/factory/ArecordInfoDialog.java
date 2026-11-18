package com.xh.hotme.test.factory;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogArecordEditBinding;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.ToastUtil;


public class ArecordInfoDialog extends BaseDialog<DialogArecordEditBinding> {
    public interface ICallback {
        void onCancel();

        void onRecord(String cardName, int channels, int duration, String path);
    }

    private ICallback iCallback;

    private String _cardNmae = "hw:1,0";
    private int _channelNum = 2;
    private int _duration = 10;
    private String _path = "/data/weewa/debug/arecord.wav";

    public ArecordInfoDialog(@NonNull Context context) {
        super(context);
    }

    public ArecordInfoDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    @Override
    protected void onInit() {
        initValue();
        viewBinding.tvCancel.setOnClickListener(v -> {
            cancel();
            if (iCallback != null) {
                iCallback.onCancel();
            }
        });

        viewBinding.tvConfirm.setOnClickListener(v -> {
            String cardName = viewBinding.etCardName.getText().toString();
            String path = viewBinding.etPath.getText().toString();
            String channelNumStr = viewBinding.etChannelNum.getText().toString();
            String durationStr = viewBinding.etDuration.getText().toString();

            if (TextUtils.isEmpty(cardName)) {
                ToastUtil.s("请输入声卡名");
                return;
            }
            if (TextUtils.isEmpty(path)) {
                ToastUtil.s("请输入路径");
                return;
            }
            if (TextUtils.isEmpty(channelNumStr)) {
                ToastUtil.s("请输入音频通道数");
                return;
            }
            if (TextUtils.isEmpty(durationStr)) {
                ToastUtil.s("请输入录制时长");
                return;
            }

            int channelNum = Integer.parseInt(channelNumStr);
            int duration = Integer.parseInt(durationStr);
            dismiss();
            if (iCallback != null) {
                iCallback.onRecord(cardName, channelNum, duration, path);
            }
        });


        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = BaseAppUtil.getDeviceWidth() - (2 * DensityUtil.dip2px(20));
        windowparams.gravity = Gravity.CENTER;
    }

    public void  initValue(){
        viewBinding.etCardName.setText(_cardNmae);
        viewBinding.etChannelNum.setText(String.valueOf(_channelNum));
        viewBinding.etDuration.setText(String.valueOf(_duration));
        viewBinding.etPath.setText(_path);
    }
}