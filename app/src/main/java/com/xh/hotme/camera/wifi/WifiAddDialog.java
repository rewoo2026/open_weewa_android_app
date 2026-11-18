package com.xh.hotme.camera.wifi;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.WifiSetupRequestBean;
import com.xh.hotme.bluetooth.event.WifiSetupEvent;
import com.xh.hotme.databinding.WifiDialogAddBinding;
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
public class WifiAddDialog extends BaseDialog<WifiDialogAddBinding> implements IWifiNotifyListener {
    private static final String TAG = "WifiAddDialog";

    // listener
    private ICallback _listener;

    Handler _handler;

    String _ssid;
    String _password;

    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    public interface ICallback {
        void onCancel();

        void onConfirm(String name, String password);
    }


    public WifiAddDialog(@NonNull Context context) {
        super(context);
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

    public void setCallback(ICallback listener) {
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
                    _listener.onConfirm(_ssid, _password);
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
                } else {
                    ToastUtil.s(content);
                }
            }
        });

    }

    @Override
    public void onTurnOff(int status, String message) {

    }

    @Override
    protected void onInit() {
        AppTrace.d(TAG, "onInit");
        _handler = new Handler();

        // views
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (_listener != null) {
                    _listener.onCancel();
                }
                dismiss();

                return true;
            }
        });

        viewBinding.tvTitle.setText(_ssid);

        viewBinding.etName.setFocusable(true);
        viewBinding.etName.requestFocus();

        //打开软键盘
        InputMethodManager imm = (InputMethodManager) HotmeApplication.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(viewBinding.etName, InputMethodManager.HIDE_NOT_ALWAYS);

        // ok button
        viewBinding.tvRefresh.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                String ssid = viewBinding.etName.getText().toString();
                String password = viewBinding.etPassword.getText().toString();
                if(TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
                    ToastUtil.s(getContext().getString(R.string.info_wifi_ssid_or_password_is_null));
                    return true;
                }
                _password = password;
                _ssid = ssid;

                boolean isSuc = DeviceApiManager.sendCmdSimpleObj(new WifiSetupRequestBean(ssid, password));
                if(isSuc) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getContext().getString(R.string.loading_connect));
                } else {
                    showDisconnectToast();
                }

                return true;
            }
        });
    }

    public void setDefaultSsidAndPassword(String ssid, String password) {
        this._ssid = ssid;
        this._password = password;
        viewBinding.etName.setText(ssid);
        viewBinding.etPassword.setText(password);
    }
}
