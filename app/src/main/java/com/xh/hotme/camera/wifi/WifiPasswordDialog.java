package com.xh.hotme.camera.wifi;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.WifiSetupRequestBean;
import com.xh.hotme.bluetooth.event.WifiSetupEvent;
import com.xh.hotme.databinding.WifiDialogSetupBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;


@Keep
@SheetStyle(heightGap = 20)
public class WifiPasswordDialog extends BaseDialog<WifiDialogSetupBinding> implements IWifiNotifyListener {
    private static final String TAG = "WifiPasswordDialog";

    // views
    // listener
    private OnClickListener _listener;


    private Handler _handler;

    final String _ssid;
    String _password;

    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    public WifiPasswordDialog(@NonNull final Context context, String ssid) {
        super(context);

        _ssid = ssid;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        BluetoothHandle.addWifiNotifyListener(this);

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        BluetoothHandle.removeWifiNotifyListener(this);
        if(_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        _listener = listener;
    }


    @Override
    public void onWifiList(List<WifiInfo> device) {

    }

    @Override
    public void onSetupStatus(int status, String message) {
        AppTrace.d(TAG, "onSetupStatus： status：" + status);
        _handler.removeCallbacks(_timeOutRunnable);
        MainHandler.runOnUiThread(() -> {
            DialogUtil.dismissDialog();
            if(status == 1) {
                if(_listener != null) {
                    _listener.onClick(WifiPasswordDialog.this, DialogInterface.BUTTON_POSITIVE);
                }
                EventBus.getDefault().post(new WifiSetupEvent(_ssid, _password));
//                    dismiss();
            } else {
                String content = "输入密码错误";
                if(message.contains("password incorrect")) {
                    content = "输入密码错误";
                } else {
                    content = "连接失败，请重试";
                }

                if(getContext() instanceof BaseActivity) {
                    BaseActivity<?> act = (BaseActivity<?>) getContext();
                    act.showHintDialog("", content, "知道了", null);
                }
            }
        });

    }

    @Override
    public void onTurnOff(int status, String message) {

    }

    @Override
    protected void onInit() {
        _handler = new Handler();
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_listener != null) {
                    _listener.onClick(WifiPasswordDialog.this, DialogInterface.BUTTON_NEGATIVE);
                }
                dismiss();

                return true;
            }
        });

        viewBinding.tvTitle.setText(_ssid);

        viewBinding.etPassword.setFocusable(true);
        viewBinding.etPassword.requestFocus();

        //打开软键盘
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(viewBinding.etPassword, InputMethodManager.HIDE_NOT_ALWAYS);

//		// ok button
        viewBinding.tvRefresh.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String password = viewBinding.etPassword.getText().toString();
                if(TextUtils.isEmpty(_ssid) || TextUtils.isEmpty(password)) {
                    ToastUtil.s(getContext().getString(R.string.info_wifi_ssid_or_password_is_null));
                    return true;
                }
                _password = password;

                boolean isSuc = DeviceApiManager.sendCmdSimpleObj(new WifiSetupRequestBean(_ssid, password));
                if(isSuc) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getContext().getString(R.string.loading_connect));
                } else {
                    showDisconnectToast();
                }

                return true;
            }
        });


        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
