package com.xh.hotme.test;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogAutoRkipcInfoBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;


@Keep
@SheetStyle(heightGap = 20)
public class AutoRkipcDialog extends BaseDialog<DialogAutoRkipcInfoBinding> {
    private static final String TAG = AutoRkipcDialog.class.getSimpleName();

    // listener
    private IAutoRkipcInfoListener _listener;

    public AutoRkipcDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    protected void onInit() {
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onCancel();
                }
                dismiss();
                return true;
            }
        });

        viewBinding.btnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                long loopTime = 1000, duration = 1800, interval = 60;
                String loopTimeStr = viewBinding.etLoopTime.getText().toString().trim();
                if(TextUtils.isEmpty(loopTimeStr)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_loop_times));
                    return true;
                } else {
                    loopTime = Long.parseLong(loopTimeStr);
                }

                String durationStr = viewBinding.etRunDuration.getText().toString().trim();
                if(TextUtils.isEmpty(durationStr)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_run_duration));
                    return true;
                } else {
                    duration = Long.parseLong(durationStr);
                }

                String intervalStr = viewBinding.etSleepTime.getText().toString().trim();
                if(TextUtils.isEmpty(intervalStr)) {
                    ToastUtil.s(getContext().getString(R.string.info_input_run_duration));
                    return true;
                } else {
                    interval = Long.parseLong(intervalStr);
                }

                if(_listener != null) {
                    _listener.onUpdate(loopTime, duration, interval);
                }
                dismiss();
                return true;
            }
        });

        viewBinding.etLoopTime.setText("1000");
        viewBinding.etRunDuration.setText("1800");
        viewBinding.etSleepTime.setText("60");


        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setOnClickListener(IAutoRkipcInfoListener listener) {
        _listener = listener;
    }

    public interface IAutoRkipcInfoListener {
        void onUpdate(long loop, long duration, long interval);

        void onCancel();
    }
}
