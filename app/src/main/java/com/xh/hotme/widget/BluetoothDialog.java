package com.xh.hotme.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogOpenBluetoothBinding;
import com.xh.hotme.utils.ClickGuard;


@Keep
@SheetStyle(heightGap = 20)
public class BluetoothDialog extends BaseDialog<DialogOpenBluetoothBinding> {
    // listener
    private OnClickListener _listener;


    public BluetoothDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    protected void onInit() {
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onClick(BluetoothDialog.this, DialogInterface.BUTTON_NEGATIVE);
                }
                return true;
            }
        });

        // ok button
        viewBinding.openBluetooth.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onClick(BluetoothDialog.this, DialogInterface.BUTTON_POSITIVE);
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

    public void setNegativeButtonVisible(boolean visible) {
        viewBinding.ivClose.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setPositiveButtonTitle(String title) {
        viewBinding.openBluetooth.setText(title);
    }

}
