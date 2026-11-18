package com.xh.hotme.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogOpenBluetoothLoadingBinding;
import com.xh.hotme.utils.ClickGuard;


@Keep
@SheetStyle(heightGap = 20)
public class BluetoothLoadingDialog extends BaseDialog<DialogOpenBluetoothLoadingBinding> {
    // listener
    private OnClickListener _listener;

    private int _type;  //0 连接蓝牙； 1:重连蓝牙；2:获取设备信息

    public static final int TYPE_SCAN = 10;
    public static final int TYPE_CONNECT = 0;
    public static final int TYPE_DISCONNECT = 1;
    public static final int TYPE_RECONNECT = 2;
    public static final int TYPE_GET_DEVICE_INFO = 3;
    public static final int TYPE_GET_NET_INFO = 4;
    public static final int TYPE_GET_WEEWA_INFO = 5;


    public BluetoothLoadingDialog(@NonNull final Context context) {
        super(context);
    }

    @Override
    protected void onInit() {
        // views
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onClick(BluetoothLoadingDialog.this, DialogInterface.BUTTON_NEGATIVE);
                }
                dismiss();
                return true;
            }
        });


        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setOnClickListener(OnClickListener listener) {
        _listener = listener;
    }

    public void setMessage(String message) {
        if(!TextUtils.isEmpty(message)) {
            viewBinding.message.setText(message);
            viewBinding.message.setVisibility(View.VISIBLE);
        }
    }

    public void setType(int type) {
        _type = type;
    }

    public int getType() {
        return _type;
    }
}
