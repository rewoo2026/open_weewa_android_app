package com.xh.hotme.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogConnectFailBinding;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DeviceUtil;


@SheetStyle(heightGap = 20)
public class ConnectFailDialog extends BaseDialog<DialogConnectFailBinding> {
    public interface ICallback {
        void onCancel();

        void onConfirm();
    }

    private ICallback iCallback;

    public ConnectFailDialog(@NonNull Context context) {
        super(context);
    }

    public ConnectFailDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }


    @Override
    protected void onInit() {
        viewBinding.ivClose.setOnClickListener(v -> {
            cancel();
            if(iCallback != null) {
                iCallback.onConfirm();
            }
        });
    }
}