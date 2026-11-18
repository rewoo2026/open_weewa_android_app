package com.xh.hotme.camera.softap;


import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.SimCloseRequestBean;
import com.xh.hotme.bluetooth.bean.SimOpenRequestBean;
import com.xh.hotme.bluetooth.bean.SoftApCloseRequestBean;
import com.xh.hotme.bluetooth.event.NetChangeEvent;
import com.xh.hotme.bluetooth.event.NetworkSetupCompleteEvent;
import com.xh.hotme.bluetooth.event.WifiSetupEvent;
import com.xh.hotme.bluetooth.event.WifiTurnOffEvent;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSimEnableNotifyListener;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.ConnectActivity;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.camera.wifi.WifiListDialog;
import com.xh.hotme.camera.wifi.WifiPasswordDialog;
import com.xh.hotme.camera.wifi.WifiSettingActivity;
import com.xh.hotme.databinding.ActivityCameraNetworkSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.live.ConnectWifiOkActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;


@IEventBus
public class CameraNetworkSettingActivity extends BaseActivity<ActivityCameraNetworkSettingBinding> implements IWifiNotifyListener, IBleSimEnableNotifyListener, IBleNetworkInfoNotifyListener {
    private static final String TAG = CameraNetworkSettingActivity.class.getSimpleName();

    boolean _isLive;

    BluetoothLoadingDialog _bluetoothLoadingDialog;


    WifiListDialog _wifiListDialog;
    WifiPasswordDialog _setupDialog;

    public final static int REQUEST_ACTIVE = 0;
    public final static int REQUEST_NETWORK_CONFIG = 1;
    public String str_connect_timeout = "";

    public int _requestType = 0;

    Device _device;

    private final Runnable _timeOutRunnable = () -> {
        MainHandler.runOnUiThread(() -> ToastUtil.s(str_connect_timeout));
        dismissLoading();
    };

    public static void start(Context context, Device device, boolean isLive) {
        if(null != context) {
            Intent intent = new Intent(context, CameraNetworkSettingActivity.class);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_TYPE, REQUEST_ACTIVE);
            intent.putExtra(Constants.REQUEST_ACTIVE_LIVE, isLive);
            context.startActivity(intent);
        }
    }

    public static void start(Context context, Device device, int requestType) {
        if(null != context) {
            Intent intent = new Intent(context, CameraNetworkSettingActivity.class);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_TYPE, requestType);

            context.startActivity(intent);
        }
    }

    public static void startActivityByRequestCode(Activity context, int requestCode) {
        if(null != context) {
            Intent intent = new Intent(context, CameraNetworkSettingActivity.class);
            intent.putExtra(REQUEST_CODE, requestCode);

            context.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addSimEnableNotifyListener(this);
        BluetoothHandle.addNetworkNotifyListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy");

        BluetoothHandle.removeSimEnableNotifyListener(this);
        BluetoothHandle.removeNetworkNotifyListener(this);
    }

    @Override
    protected void initView() {
        str_connect_timeout = getString(R.string.request_timeout);

        viewBinding.titleBar.tvTitle.setText(getString(R.string.setup_wifi));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.wifiLayout.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String wifiSsid = "";
                CameraInfo cameraInfo = CameraManager.getCameraInfo(null);
                if(cameraInfo != null && cameraInfo.deviceNetworkInfo != null) {
                    NetworkBean networkBean = cameraInfo.deviceNetworkInfo;
                    if(networkBean.getWifi() != null) {
                        wifiSsid = networkBean.getWifi().s;
                    }
                }
                WifiSettingActivity.start(CameraNetworkSettingActivity.this, _device, wifiSsid, _requestType, _isLive);

                return true;
            }
        });

        viewBinding.btnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                postSubmit();
                return true;
            }
        });

    }

    private void postSubmit() {
        if(_isLive) {
            if(viewBinding.wifiSsid.getText().toString().isEmpty() && !viewBinding.sbFourG.isChecked()) {
                ToastUtil.s(getString(R.string.select_network));
                return;
            }
            startActivity(new Intent(CameraNetworkSettingActivity.this, ConnectWifiOkActivity.class));
        } else if(_requestType == REQUEST_ACTIVE) {
            if(viewBinding.wifiSsid.getText().toString().isEmpty() && !viewBinding.sbFourG.isChecked()) {
                ToastUtil.s(getString(R.string.select_network));
                return;
            }
            ConnectActivity.start(CameraNetworkSettingActivity.this, _device, Constants.ACTIVE_STEP_ACTIVE);
        }
        finish();
    }

    @Override
    protected void initData() {
        if(getIntent() != null) {
            String deviceInfo = getIntent().getStringExtra(Constants.REQUEST_DEVICE);
            if(!TextUtils.isEmpty(deviceInfo)) {
                _device = GsonUtils.GsonToBean(deviceInfo, Device.class);
            }
            _requestType = getIntent().getIntExtra(Constants.REQUEST_TYPE, REQUEST_ACTIVE);

            _isLive = getIntent().getBooleanExtra(Constants.REQUEST_ACTIVE_LIVE, false);
        } else {
            _requestType = REQUEST_NETWORK_CONFIG;
        }

        if(_requestType == REQUEST_ACTIVE) {
            viewBinding.btnSubmit.setText(getString(R.string.next_step));
        } else {
            viewBinding.btnSubmit.setText(getString(R.string.confirm));
        }

        // update net status
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
        if(isOk){
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            showLoading();
        } else {
            showDisconnectDialog();
        }
    }

    private void updateNetStatusUI() {
        String wifiSsid = "";
        boolean hasSim = false;
        boolean simOn = false;
        boolean ethOn = false;
        NetworkBean networkBean = CameraManager.getNetworkInfo(null);
        if(networkBean != null) {
            if(networkBean.getWifi() != null) {
                wifiSsid = networkBean.getWifi().s;
            }
            if(networkBean.getSim() != null) {
                hasSim = networkBean.getSim().has_card == 1;
                simOn = hasSim && networkBean.getSim().connected == 1;
            }

            if(networkBean.getEth() != null && networkBean.getEth().st == 1) {
                ethOn = true;
            }
        }

        if(!TextUtils.isEmpty(wifiSsid)) {
            viewBinding.wifiSsid.setText(wifiSsid);
            viewBinding.btnSubmit.setFocusable(true);
        } else {
            viewBinding.wifiSsid.setText(getString(R.string.close));
        }
        viewBinding.sbFourG.setOnCheckedChangeListener(null);
        viewBinding.sbFourG.setChecked(simOn);
        if(!hasSim) {
            viewBinding.sbFourG.setOnClickListener(v -> {
                ToastUtil.s(getString(R.string.info_no_sim_card));
                viewBinding.sbFourG.setChecked(false);
            });
            viewBinding.sbFourG.setOnCheckedChangeListener(null);

            viewBinding.simLayout.setVisibility(View.GONE);
        } else {
            viewBinding.sbFourG.setOnClickListener(null);
            viewBinding.sbFourG.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggle4G(isChecked);
            });

            viewBinding.simLayout.setVisibility(View.VISIBLE);
            String iccid = networkBean.getSim().iccid;
            if(!TextUtils.isEmpty(iccid)){
                viewBinding.simIccid.setText(iccid);
            }

            viewBinding.simIccid.setOnClickListener(v -> {
                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("iccid", iccid));
                ToastUtil.s(getString(R.string.copied));
            });

        }

        viewBinding.ethStatus.setText(ethOn ? getString(R.string.connected) : getString(R.string.unconnect));

    }

    private void toggle4G(boolean isOpen) {
        if(isOpen) {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SimOpenRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                showLoading(getString(R.string.loading_open_4g_network));
            } else {
                showDisconnectToast();
            }

        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SimCloseRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                showLoading(getString(R.string.loading_close_4g_network));
            } else {
                showDisconnectToast();
            }
        }
    }


    private void showLoadingDialog(String message) {
        if(_bluetoothLoadingDialog != null && _bluetoothLoadingDialog.isShowing()) {
            _bluetoothLoadingDialog.dismiss();
            _bluetoothLoadingDialog = null;
        }
        _bluetoothLoadingDialog = new BluetoothLoadingDialog(CameraNetworkSettingActivity.this);
        _bluetoothLoadingDialog.setMessage(message);
        _bluetoothLoadingDialog.show();
    }


    private void dismissLoadingDialog() {
        if(_bluetoothLoadingDialog != null && _bluetoothLoadingDialog.isShowing()) {
            _bluetoothLoadingDialog.dismiss();
        }
        _bluetoothLoadingDialog = null;
    }


    @Override
    public void onWifiList(List<WifiInfo> device) {
        AppTrace.d(TAG, "onWifiList");
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            dismissLoadingDialog();
            if(_wifiListDialog != null && _wifiListDialog.isShowing()) {
                _wifiListDialog.dismiss();
            }
            _wifiListDialog = new WifiListDialog(CameraNetworkSettingActivity.this, "网络配置", device, new IWifiListAdapterCallback() {
                @Override
                public void onCancel() {

                }

                @Override
                public void onWifiClick(WifiInfo device1, int position) {
                    if(_setupDialog != null && _setupDialog.isShowing()) {
                        _setupDialog.dismiss();
                    }
                    _setupDialog = null;

                    _setupDialog = new WifiPasswordDialog(CameraNetworkSettingActivity.this, device1.getSsid());
                    _setupDialog.setOnClickListener((dialog, which) -> {
                        if(which == DialogInterface.BUTTON_POSITIVE) {
                            viewBinding.wifiSsid.setText(device1.getSsid());

                            _setupDialog.dismiss();

                            _wifiListDialog.dismiss();

                            //同步
                            DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());

                            //配网成功，直接跳到下一个页面
                            postSubmit();
                        }
                    });
                    _setupDialog.show();
                }
            });
            //关闭热点
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SoftApCloseRequestBean());
            _wifiListDialog.show();

        });
    }

    @Override
    public void onSetupStatus(int status, String message) {

    }

    @Override
    public void onTurnOff(int status, String message) {
        //同步
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
    }

    @Override
    public void onSimOpen() {
        if(BaseAppUtil.isDestroy(CameraNetworkSettingActivity.this)) {
            return;
        }
        MainHandler.runOnUiThread(() -> {
            try {
                dismissLoading();

                //同步
                DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());

                //配网成功，直接跳到下一个页面
                postSubmit();
            } catch(Throwable e) {

            }
        });
    }

    @Override
    public void onSimOpenFail(String msg) {
        try {
            if(BaseAppUtil.isDestroy(CameraNetworkSettingActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(() -> {
                try {
                    DialogUtil.dismissDialog();
                    ToastUtil.s(getString(R.string.info_network_error_open_4g));
                } catch(Throwable e) {

                }
            });

        } catch(Throwable e) {

        }
    }

    @Override
    public void onSimClose() {
        try {
            if(BaseAppUtil.isDestroy(CameraNetworkSettingActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(this::dismissLoading);

            //关闭后，延时1秒钟再查询，立马查询网络状态不会立马更新的
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
                }
            }, 1000);

        } catch(Throwable e) {

        }
    }

    @Override
    public void onSimCloseFail(String msg) {
        try {
            if(BaseAppUtil.isDestroy(CameraNetworkSettingActivity.this)) {
                return;
            }
            MainHandler.runOnUiThread(() -> {
                try {
                    DialogUtil.dismissDialog();
                    ToastUtil.s(getString(R.string.info_network_error_close_4g));
                } catch(Throwable e) {

                }
            });

        } catch(Throwable e) {

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiTurnOff(WifiTurnOffEvent event) {
        AppTrace.d(TAG, "onWifiTurnOff");
        viewBinding.wifiSsid.setText(getString(R.string.close));

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWifiSetup(WifiSetupEvent event) {
        AppTrace.d(TAG, "onWifiSetup");
        viewBinding.wifiSsid.setText(event.ssid);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkSetupCompleteEvent(NetworkSetupCompleteEvent event) {
        finish();
    }

    @Override
    public void onNetworkStatus(NetworkBean networkBean) {
        AppTrace.d(TAG, "onNetworkStatus");
        _handler.removeCallbacks(_timeOutRunnable);
        try {
            if(BaseAppUtil.isDestroy(CameraNetworkSettingActivity.this)) {
                return;
            }
            dismissLoading();
            MainHandler.runOnUiThread(this::updateNetStatusUI);
        } catch(Throwable e) {

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetChanged(NetChangeEvent e) {
        showLoading("侦测到网络变化,正在刷新网络状态");
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
    }
}
