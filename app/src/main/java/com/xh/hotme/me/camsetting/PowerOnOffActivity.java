package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.PoweroffRequestBean;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.DeviceWeewaStartEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RebootEvent;
import com.xh.hotme.bluetooth.event.WeewaInfoOutEvent;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityPowerOnOffBinding;

import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class PowerOnOffActivity extends BaseActivity<ActivityPowerOnOffBinding> implements IBleWeewaInfoNotifyListener {

    boolean isDeviceOn = false;

    String str_request_timeout;

    BluetoothLoadingDialog _connectingDialog;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
        ToastUtil.s(str_request_timeout);
    };

    private final Runnable _timeOutWeewaInfoRunnable = () -> {
        DialogUtil.dismissDialog();
        dismissConnectingDialog();
        isDeviceOn = false;
        updateStatus();
        EventBus.getDefault().post(new WeewaInfoOutEvent());
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        str_request_timeout = getString(R.string.request_timeout);

        viewBinding.titleBar.tvTitle.setText(R.string.remote_device_on_off_title);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.btnPowerOn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                    ToastUtil.s(getString(R.string.remote_model_offline_message));
                    return true;
                }

                doPowerOn();
                return true;
            }
        });

        viewBinding.btnPowerOff.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                doPowerOff();
                return true;
            }
        });

        viewBinding.btnReboot.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                doReboot();
                return true;
            }
        });

        BluetoothHandle.addWeewaInfoNotifyListener(this);
    }

    @Override
    protected void initData() {
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo != null){
            isDeviceOn = true;
        } else {
            isDeviceOn = false;
        }

        updateStatus();

        _handler.post(new Runnable() {
            @Override
            public void run() {
                getWeewaInfo();
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeWeewaInfoNotifyListener(this);
    }

    private void getWeewaInfo(){
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
        if(!isOk){
            ToastUtil.s(getString(R.string.info_bluetooth_disconnected));
        } else {
            _handler.postDelayed(_timeOutWeewaInfoRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
            showConnectingDialog("正在获取相机状态...");
        }
    }


    private void doPowerOn(){
        if(isDeviceOn) {
            ToastUtil.s(getString(R.string.info_device_had_been_power_on));
            return;
        }
        boolean isOk = BluetoothManager.getImpl().sendCmdPowerOn();
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(R.string.loading_request_power_on));
        }
    }

    private void doPowerOff(){
        if(!isDeviceOn) {
            ToastUtil.s(getString(R.string.info_device_had_been_power_off));
            return;
        }

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PoweroffRequestBean());
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(R.string.loading_request_power_off));
        }
    }


    private void doReboot(){
        if(!isDeviceOn) {
            ToastUtil.s(getString(R.string.info_device_had_been_power_off));
            return;
        }

        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
        if(isOk) {
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                DialogUtil.showDialog(getString(R.string.loading_request_reboot));
            } else{
                ToastUtil.s("请等待重启成功再试");
                finish();
            }
        }
    }

    private void updateStatus(){
        if(isDeviceOn){
            viewBinding.tvPowerOn.setTextColor(getResources().getColor(R.color.color_C5C5C5));
            viewBinding.ivPowerOn.setImageResource(R.mipmap.photo_open_gray);

            viewBinding.tvPowerOff.setTextColor(getResources().getColor(R.color.black));
            viewBinding.ivPowerOff.setImageResource(R.mipmap.photo_close);
        } else {

            viewBinding.tvPowerOn.setTextColor(getResources().getColor(R.color.black));
            viewBinding.ivPowerOn.setImageResource(R.mipmap.photo_open);

            viewBinding.tvPowerOff.setTextColor(getResources().getColor(R.color.color_C5C5C5));
            viewBinding.ivPowerOff.setImageResource(R.mipmap.photo_close_gray);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaInfoOutEvent(WeewaInfoOutEvent event) {
        if(BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            return;
        }
        isDeviceOn = false;
        updateStatus();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRebootEvent(RebootEvent event) {
        if(BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            return;
        }
        isDeviceOn = false;
        updateStatus();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPowerOff(PowerOffEvent event) {
        AppTrace.d(TAG, "onPowerOff");
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        isDeviceOn = false;

        if(!BaseAppUtil.isDestroy(PowerOnOffActivity.this) ) {
            if(_handler != null) {
                _handler.post(() -> {
                    DialogUtil.dismissDialog();
                    dismissConnectingDialog();
                    if(BaseAppUtil.isForegroundActivity(PowerOnOffActivity.this, PowerOnOffActivity.this.getClass().getName())) {
                        ToastUtil.s(PowerOnOffActivity.this.getString(R.string.device_power_off_success));
                    }
                    updateStatus();
                });
            }
        }
    }

    @Override
    public void onWeewaInfo(WeewaInfo data) {
        isDeviceOn = true;
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutWeewaInfoRunnable);
        }
        if(!BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            if(_handler != null) {
                _handler.post(() -> {
                    dismissConnectingDialog();
                    DialogUtil.dismissDialog();
                    updateStatus();
                });
            }
        }
    }

    @Override
    public void onWeewaInfoFail() {
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutWeewaInfoRunnable);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaStart(DeviceWeewaStartEvent event) {
        AppTrace.d(TAG, "onWeewaStart");
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }

        if(!BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            if(_handler != null) {
                _handler.post(() -> {
                    if(!isDeviceOn){
                        ToastUtil.s(PowerOnOffActivity.this.getString(R.string.device_launch_success));
                        isDeviceOn = true;
                        DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
                        DialogUtil.dismissDialog();
                        updateStatus();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(_connectingDialog!=null && _connectingDialog.isShowing()){
            _connectingDialog.dismiss();
        }
    }

    private void showConnectingDialog(String message) {
        AppTrace.d(TAG, "showConnectingDialog: " + message);
        if (!BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                    _connectingDialog = null;
                }
                _connectingDialog = new BluetoothLoadingDialog(PowerOnOffActivity.this);
                _connectingDialog.setMessage(message);
                _connectingDialog.setOnClickListener((dialog, which) -> {

                });
                _connectingDialog.show();
            });
        }
    }

    private void updateConnectMessage(String message, int type) {
        AppTrace.d(TAG, "updateConnectMessage: " + message);
        if (!BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            runOnUiThread(() -> {
                if (_connectingDialog == null || !_connectingDialog.isShowing()) {
                    _connectingDialog = new BluetoothLoadingDialog(PowerOnOffActivity.this);
                    _connectingDialog.setMessage(message);
                    _connectingDialog.setType(type);
                    _connectingDialog.setOnClickListener((dialog, which) -> {
                    });
                    _connectingDialog.show();
                } else if (_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.setType(type);
                    _connectingDialog.setMessage(message);
                }
            });
        }
    }

    private void dismissConnectingDialog() {
        AppTrace.d(TAG, "dismissConnectingDialog");
        if (!BaseAppUtil.isDestroy(PowerOnOffActivity.this)) {
            runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                }
                _connectingDialog = null;
            });
        }
    }

}
