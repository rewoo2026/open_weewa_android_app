package com.xh.hotme.camera.wifi;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.WifiListRequestBean;
import com.xh.hotme.bluetooth.bean.WifiTurnOffRequestBean;
import com.xh.hotme.bluetooth.event.NetworkSetupCompleteEvent;
import com.xh.hotme.bluetooth.event.WifiTurnOffEvent;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.ConnectActivity;
import com.xh.hotme.databinding.ActivityWifiListBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.live.ConnectWifiOkActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.RecycleViewDivider;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;


public class WifiSettingActivity extends BaseActivity<ActivityWifiListBinding> implements IWifiNotifyListener {
    private static final String TAG = WifiSettingActivity.class.getSimpleName();

    boolean _isLive;

    WifiPasswordDialog _setupDialog;

    int _network_wifi = -1;

    String _wifiSsid;

    WifiInfo _wifiInfo;

    public final static int REQUEST_ACTIVE = 0;
    public final static int REQUEST_NETWORK_CONFIG = 1;

    public int _requestType = 0;

    Device _device;


    WifiListAdapter _adapter;

    final List<WifiInfo> _dataList = new ArrayList<>();

    WifiAddDialog _wifiAddDialog;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
    };

    public static void start(Context context, Device device, String ssid, int requestType, boolean isLive) {
        if(null != context) {
            Intent intent = new Intent(context, WifiSettingActivity.class);
            intent.putExtra(BleConstants.BLE_KEY_SOFTAP_SSID, ssid);
            intent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(device));
            intent.putExtra(Constants.REQUEST_TYPE, requestType);
            intent.putExtra(Constants.REQUEST_ACTIVE_LIVE, isLive);
            context.startActivity(intent);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeWifiNotifyListener(this);
    }

    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(getString(R.string.network_wifi));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        viewBinding.titleBar.tvRight.setVisibility(View.VISIBLE);
        viewBinding.titleBar.tvRight.setText(getString(R.string.network_add));
        viewBinding.titleBar.tvRight.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_wifiAddDialog != null && _wifiAddDialog.isShowing()) {
                    _wifiAddDialog.dismiss();
                }
                _wifiAddDialog = null;

                _wifiAddDialog = new WifiAddDialog(WifiSettingActivity.this);
                _wifiAddDialog.setCallback(new WifiAddDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onConfirm(String name, String password) {
                        _wifiSsid = name;
                        viewBinding.connectNetwork.setVisibility(View.VISIBLE);
                        viewBinding.tvConnectedSsid.setText(_wifiSsid);

                        _network_wifi = 1;
                        _wifiAddDialog.dismiss();

                        //同步
                        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());

                        //配网成功，直接跳到下一个页面
                        postSubmit();
                    }
                });
                _wifiAddDialog.show();
                return true;
            }
        });


        viewBinding.sbWifi.setOnCheckedChangeListener((buttonView, isChecked) -> doWifiOpen(isChecked));

        viewBinding.tvRefresh.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiListRequestBean());
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getString(R.string.loading_open_wifi));
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });


        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(WifiSettingActivity.this));
        viewBinding.recyclerView.addItemDecoration(new RecycleViewDivider(WifiSettingActivity.this, LinearLayoutManager.VERTICAL, WifiSettingActivity.this.getResources().getColor(R.color.bg_gray)));

        _adapter = new WifiListAdapter(WifiSettingActivity.this, _dataList);
        _adapter.setCallback(new IWifiListAdapterCallback() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onWifiClick(WifiInfo device, int position) {
                if(device == null) {
                    return;
                }
                if(!TextUtils.isEmpty(_wifiSsid) && !TextUtils.isEmpty(device.getSsid()) && _wifiSsid.equalsIgnoreCase(device.getSsid())) {
                    ToastUtil.s(getString(R.string.connected_network));
                    return;
                }

                if(_setupDialog != null && _setupDialog.isShowing()) {
                    _setupDialog.dismiss();
                }
                _setupDialog = null;

                _setupDialog = new WifiPasswordDialog(WifiSettingActivity.this, device.getSsid());
                _setupDialog.setOnClickListener((dialog, which) -> {
                    if(which == DialogInterface.BUTTON_POSITIVE) {
                        _wifiSsid = device.getSsid();
                        viewBinding.connectNetwork.setVisibility(View.VISIBLE);
                        viewBinding.tvConnectedSsid.setText(_wifiSsid);

                        _network_wifi = 1;
                        _setupDialog.dismiss();

                        //同步
                        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());

                        //配网成功，直接跳到下一个页面
                        postSubmit();
                    }
                });
                _setupDialog.show();
            }
        });

        viewBinding.recyclerView.setAdapter(_adapter);

    }

    @Override
    protected void initData() {

        if(getIntent() != null) {
            String deviceInfo = getIntent().getStringExtra(Constants.REQUEST_DEVICE);
            if(!TextUtils.isEmpty(deviceInfo)) {
                _device = GsonUtils.GsonToBean(deviceInfo, Device.class);
            }
            _wifiSsid = getIntent().getStringExtra(BleConstants.BLE_KEY_SOFTAP_SSID);
            _requestType = getIntent().getIntExtra(Constants.REQUEST_TYPE, REQUEST_ACTIVE);

            _isLive = getIntent().getBooleanExtra(Constants.REQUEST_ACTIVE_LIVE, false);
        } else {
            _requestType = REQUEST_NETWORK_CONFIG;
            CameraInfo cameraInfo = CameraManager.getCameraInfo(null);
            if(cameraInfo != null && cameraInfo.deviceNetworkInfo != null) {
                NetworkBean networkBean = cameraInfo.deviceNetworkInfo;
                if(networkBean != null && networkBean.getWifi() != null) {
                    _wifiSsid = networkBean.getWifi().s;
                }
            }
        }
        if(!TextUtils.isEmpty(_wifiSsid)) {
            viewBinding.sbWifi.setChecked(true);
            viewBinding.connectNetwork.setVisibility(View.VISIBLE);
            viewBinding.tvConnectedSsid.setText(_wifiSsid);
            viewBinding.layoutList.setVisibility(View.VISIBLE);
            _network_wifi = 1;
        } else {
            viewBinding.sbWifi.setChecked(false);
            viewBinding.connectNetwork.setVisibility(View.GONE);
            viewBinding.tvConnectedSsid.setText("");
            viewBinding.layoutList.setVisibility(View.GONE);

        }

        BluetoothHandle.addWifiNotifyListener(this);
    }

    private void doWifiOpen(boolean isOpen) {
        if(isOpen) {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiListRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                DialogUtil.showDialog(getString(R.string.loading_open_wifi));
            } else {
                showDisconnectToast();
            }

        } else {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiTurnOffRequestBean());
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                DialogUtil.showDialog(getString(R.string.loading_close_wifi));
            } else {
                showDisconnectToast();
            }
        }
    }

    @Override
    public void onWifiList(List<WifiInfo> device) {
        AppTrace.d(TAG, "onWifiList");
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            viewBinding.layoutList.setVisibility(View.VISIBLE);
            DialogUtil.dismissDialog();
            _dataList.clear();
            _dataList.addAll(device);

            if(!TextUtils.isEmpty(_wifiSsid)) {
                for(WifiInfo wifiInfo : device) {
                    if(wifiInfo.getSsid().equalsIgnoreCase(_wifiSsid)) {
                        _wifiInfo = wifiInfo;
                        int level = WifiManager.calculateSignalLevel(Integer.parseInt(wifiInfo.getSignalLevel()), 5);
                        if(wifiInfo.getFlags() == null || wifiInfo.getFlags().contains("WEP") || wifiInfo.getFlags().contains("PSK") || wifiInfo.getFlags().contains("EAP")) {
                            viewBinding.ivConnectedWifiLevel.setImageResource(R.drawable.wifi_signal_lock);
                        } else {
                            viewBinding.ivConnectedWifiLevel.setImageResource(R.drawable.wifi_signal_open);
                        }
                        viewBinding.ivConnectedWifiLevel.setImageLevel(level);
                        _dataList.remove(wifiInfo);
                        break;
                    }
                }
            }

            _adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onSetupStatus(int status, String message) {
        AppTrace.d(TAG, "onSetupStatus： status：" + status);
    }

    @Override
    public void onTurnOff(int status, String message) {
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.post(() -> {
            DialogUtil.dismissDialog();

            _wifiSsid = "";
            viewBinding.connectNetwork.setVisibility(View.GONE);
            viewBinding.tvConnectedSsid.setText("");

            ToastUtil.s(getString(R.string.info_wifi_turn_off));

            DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());

            EventBus.getDefault().post(new WifiTurnOffEvent());

        });
    }


    private void postSubmit() {
        if(_isLive) {
            startActivity(new Intent(WifiSettingActivity.this, ConnectWifiOkActivity.class));
        } else if(_requestType == REQUEST_ACTIVE) {
            ConnectActivity.start(WifiSettingActivity.this, _device, Constants.ACTIVE_STEP_ACTIVE);
        }
        EventBus.getDefault().post(new NetworkSetupCompleteEvent());  //
        finish();
    }

    @Override
    protected int getNavigationBarColor() {
        return 0xffededed;
    }

    @Override
    protected int getStatusBarColor() {
        return 0xffededed;
    }
}
