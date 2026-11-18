package com.xh.hotme.camera;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.recyclerview.widget.GridLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothDeviceAdapter;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.DeviceInfoRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaConnectedRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.DeviceEvent;
import com.xh.hotme.bluetooth.event.RenameEvent;
import com.xh.hotme.bluetooth.listener.IBleConnectListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBluetoothDeviceAdapterCallback;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.databinding.FragmentActiveDeviceListBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.storage.VideoClipActivity;
import com.xh.hotme.test.TestActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ConnectFailDialog;
import com.xh.hotme.widget.GridSpacingItemDecoration;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Created by GJK on 2018/11/9.
 */
@IEventBus
public class DeviceListFragment extends BaseFragment<FragmentActiveDeviceListBinding> implements IBleScanRequestListener, IBleNetworkInfoNotifyListener, IBleDeviceInfoNotifyListener, IBleWeewaInfoNotifyListener {
    private static final String TAG = DeviceListFragment.class.getSimpleName();
    BluetoothDeviceAdapter _adapter;

    final List<Device> _dataList = new ArrayList<>();

    private List<Device> originalDevices = new ArrayList<>();
    private List<Device> filteredDevices = new ArrayList<>();

    private  String _keyword = "";

    IBleScanNotifyListener _scanNotifyListener;
    IBleConnectListener _connectNotifyListener;

    Device _connectDevice;

    BluetoothLoadingDialog _connectingDialog;

    ConnectFailDialog _connectFailDialog;

    ModalDialog _notFoundDialog;

    Device _readyDevice;
    Device _pendingDevice;

    boolean isBackgroundScan = false;

    private String str_connect_load_device_info;
    private String str_connect_load_device_network;
    private String str_connect_timeout;


    private int type = 0;
    public static final int type_active = 0;
    public static final int type_soft_ap = 11;
    public static final int type_camera_manager = 21;
    public static final int type_admin = 98;
    public static final int type_test = 99;

    public static final int type_setting_video_clip_manual_dotting = 31;
    public static final int type_setting_video_clip_ai = 32;

    NetworkBean _networkBean;
    DeviceInfo _deviceInfo;

    final int retryMaxNumber = 1;
    int retryNumber = 0;


    int loadingStep = 0;
    final static int LOADING_STEP_CONNECT = 0;
    final static int LOADING_STEP_DEVICE_INFO = 1;
    final static int LOADING_STEP_NETWORK_INFO = 2;

    boolean isDisconnectDevice = false;

    private final Runnable _timeOutRunnable = () -> {
        MainHandler.runOnUiThread(() -> ToastUtil.s(str_connect_timeout));
        dismissConnectingDialog();
    };

    public static DeviceListFragment newInstance(int type) {
        DeviceListFragment fragment = new DeviceListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.VIDEO_CATEGORY, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeConnectNotifyListener(_connectNotifyListener);

        BluetoothHandle.removeScanNotifyListener(_scanNotifyListener);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshDevice(final DeviceEvent event) {
        loadData();
    }

    @Override
    protected void initView() {
        Bundle bundle = getArguments();
        if(bundle != null) {
            type = bundle.getInt(Constants.VIDEO_CATEGORY, type_active);
        }

        str_connect_load_device_info = getActivity().getString(R.string.loading_dialog_load_device_info);
        str_connect_load_device_network = getActivity().getString(R.string.loading_dialog_load_device_network);
        str_connect_timeout = getActivity().getString(R.string.please_check_device_power_on_for_timeout);


        _viewBinding.recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        _viewBinding.recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, DensityUtil.dip2px(10), false));

        loadData();

        _adapter = new BluetoothDeviceAdapter(getActivity(), _dataList);

        _adapter.setCallback(new IBluetoothDeviceAdapterCallback() {
            @Override
            public void onLeScanStart() {

            }

            @Override
            public void onLeScan(Device device, int position) {

            }

            @Override
            public void onLeScanStop(int size) {
            }

            @Override
            public void onBluetoothDeviceClick(Device device, int position) {
                isDisconnectDevice = false;
                connectDevice(device, position);
            }

            @Override
            public void onRemoveDevice(Device device, int position) {

            }

            @Override
            public void openDevice(Device device) {
                AppTrace.d(TAG, "进入设备");
                isDisconnectDevice = false;
                if(type == type_active) {
                    if(_networkBean != null) {
                        if(_networkBean != null && _networkBean.getWifi() != null && (_networkBean.getWifi().ip.isEmpty() || _networkBean.getWifi().ip.startsWith("169.254"))) {
                            ConnectActivity.start(getActivity(), _connectDevice, Constants.ACTIVE_STEP_SETUP_WIFI);
                            getActivity().finish();
                        } else {
                            ConnectActivity.start(getActivity(), _connectDevice, Constants.ACTIVE_STEP_ACTIVE);
                            getActivity().finish();
                        }
                    } else {
                        loadingStep = LOADING_STEP_NETWORK_INFO;
                        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
                        if(isOk) {
                            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                            showConnectingDialog(getString(R.string.loading_dialog_connect_device));
                        } else {
                            showDisconnectToast();
                            resetDeviceStatus();
                        }
                    }
                } else {
                    getWeewaInfo();
                }
            }

            @Override
            public void disconnectDevice(Device device) {
                isDisconnectDevice = true;
                boolean isOk = BluetoothManager.getImpl().disconnectBle(device);
                isDisconnectDevice = false;
            }
        });

        _viewBinding.recyclerView.setAdapter(_adapter);

        _scanNotifyListener = new IBleScanNotifyListener() {
            @Override
            public void onScanning(boolean isBackground) {
                if(!isBackground) {
                    showConnectingDialog(getString(R.string.loading_dialog_search_device));
                }
                isBackgroundScan = isBackground;
            }

            @Override
            public void onLeScan(Device device) {
                AppTrace.d(TAG, "onLeScan device.");
                try {
                    if(!isBackgroundScan) {
                        dismissConnectingDialog();
                    }
                    for(Device blue : originalDevices) {
                        if(blue.getAddress().equals(device.getAddress())) {
                            blue.setRssi(device.rssi);
                            return;
                        }
                    }

                    if(device != null && device.getName() != null) {
                        String deviceName = device.getName().toLowerCase(Locale.ROOT);
                        if(deviceName.startsWith(BluetoothManager.weewa_prefix) || deviceName.startsWith(BluetoothManager.weewa_prefix_v2)) {
                            Device blueDevice = new Device(device.address, device.rssi, deviceName);
                            blueDevice.setStatus(Constants.DEVICE_STATUS_ONLINE);

//                            _dataList.add(blueDevice);

                            originalDevices.add(blueDevice);
                            AppTrace.d(TAG, "originalDevices data size 添加: " + originalDevices.size());
                            filterDevices(_keyword);

//                            _adapter.notifyDataSetChanged();
                        }
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTimeout() {
                AppTrace.d(TAG, "onScan timeout....");
                if(!isBackgroundScan) {
                    dismissConnectingDialog();
                }

                if(_connectDevice == null) {
                    return;
                }
                if(!isBackgroundScan) {
                    if(_dataList.isEmpty()) {
                        showNotFoundDialog();
                    }
                }
            }

            @Override
            public void onScanEnd() {
                AppTrace.d(TAG, "onScanEnd.");
                if(!isBackgroundScan) {
                    dismissConnectingDialog();
                }
            }
        };

        _connectNotifyListener = new IBleConnectListener() {
            @Override
            public void onConnecting(Device device) {
                AppTrace.d(TAG, "onConnectStart....");
                _connectDevice = device;
                updateConnectMessage(String.format("%s%s", getString(R.string.loading_dialog_connect_device), BluetoothManager.getDisplayDeviceName(device.getName())), BluetoothLoadingDialog.TYPE_CONNECT);
            }

            @Override
            public void onConnectFailure(String address, String error) {
                AppTrace.d(TAG, "onConnectFailure...." + address + ", error = " + error);
                if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                    if(_handler != null) {
                        _handler.post(() -> {
                            try {
                                if(_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(address)) {
                                    if(retryNumber > retryMaxNumber) {
                                        dismissConnectingDialog();

                                        showConnectFailDialog();

                                        _connectDevice = null;
                                        retryNumber = 0;
                                        BluetoothManager.getImpl().disconnectBle();
                                    } else {
                                        retryNumber++;
                                        updateConnectMessage(String.format("正在重连%s， 第%d次...", BluetoothManager.getDisplayDeviceName(_connectDevice.name), retryNumber), BluetoothLoadingDialog.TYPE_RECONNECT);
                                        AppTrace.d(TAG, "reconnect " + address + ", count = " + retryNumber);

                                        Thread.sleep(1000);
                                        connectDevice(_connectDevice, 0);
                                    }
                                }
                            } catch(InterruptedException e) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onDisConnecting(String address) {
                AppTrace.d(TAG, "onDisConnecting " + address);
            }

            @Override
            public void onDisConnectSuccess(String address, int status) {
                AppTrace.d(TAG, "onDisConnectSuccess " + address);
                if(!BaseAppUtil.isDestroy(getActivity())) {

                    if(_handler != null) {
                        _handler.post(() -> {
                            if(_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(address)) {
                                for(Device blue : _dataList) {
                                    if(blue.getAddress().equals(_connectDevice.getAddress())) {
                                        blue.status = Constants.DEVICE_STATUS_ONLINE;
                                        //更新本地记录
                                        BluetoothManager.getImpl().updateConnectDeviceStatus(blue);
                                        break;
                                    }
                                }
                            }
                            _adapter.notifyDataSetChanged();

                            if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                                if(_pendingDevice != null) {
                                    _connectDevice = null;
                                    _networkBean = null;
                                    connectDevice(_pendingDevice, 0);
                                } else {
                                    dismissConnectingDialog();
                                    if(getContext() != null) {
                                        ToastUtil.s(getContext().getString(R.string.info_ble_disconnect));
                                    }
                                    _connectDevice = null;
                                    _networkBean = null;
                                }
                            } else {
                                _connectDevice = null;
                                _networkBean = null;
                            }
                            isDisconnectDevice = false;
                        });
                    }
                }
            }

            @Override
            public void onConnectTimeOut(String address) {
                AppTrace.d(TAG, "onConnectTimeOut " + address);
                if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                    if(_handler != null) {
                        _handler.post(() -> {
                            if(_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(address)) {
                                dismissConnectingDialog();
                                if(getContext() != null) {
                                    ToastUtil.s(getContext().getString(R.string.info_ble_connect_timeout));
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onServiceDiscoverySucceed(String address, int status) {
                AppTrace.d(TAG, "onServiceDiscoverySucceed " + address);
                postConnected();
            }

            @Override
            public void onServiceDiscoveryFailed(String address, String message) {
                AppTrace.d(TAG, "onServiceDiscoveryFailed " + address + ", message=" + message);
                if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                    if(_handler != null) {
                        _handler.post(() -> {
                            if(_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(address)) {
                                dismissConnectingDialog();
                                if(getContext() != null) {
                                    ToastUtil.s(message);
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onConnectSuccess(Device device, int status) {  //
                AppTrace.d(TAG, "onConnect status=" + status);
                _handler.postDelayed(() -> {
                    //测试发送一个weewa连接成功的消息
                    DeviceApiManager.sendCmdSimpleObj(new WeewaConnectedRequestBean());
                }, 1500);
            }
        };

        BluetoothHandle.addConnectNotifyListener(_connectNotifyListener);
        BluetoothHandle.addNetworkNotifyListener(this);
        BluetoothHandle.addDeviceInfoNotifyListener(this);
        BluetoothHandle.addWeewaInfoNotifyListener(this);
        BluetoothHandle.addScanNotifyListener(_scanNotifyListener);

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
            } else {
                if (PermissionsUtil.hasBlePermission(getActivity())) {
                    if (!BluetoothManager.getImpl().isScanning()) {
                        if (BluetoothManager.getImpl() != null) {
                            BluetoothManager.getImpl().startScan(false);
                        }
                    } else {
                        if (getActivity() != null && getActivity() instanceof ActiveListActivity) {
                            ((ActiveListActivity) getActivity()).onScanning(true);
                        } else if (getActivity() != null && getActivity() instanceof CameraListActivity) {
                            ((CameraListActivity) getActivity()).onScanning(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void initData() {

    }

    public void filterDevices(String keyword) {
        _keyword = keyword;
        // 避免不必要的处理
        if (originalDevices.isEmpty()) {
            return;
        }

        if (TextUtils.isEmpty(keyword)) {
            if (_dataList.size() != originalDevices.size()) {
                _dataList.clear();
                _dataList.addAll(originalDevices);
                _adapter.notifyDataSetChanged();
            }
        } else {
            keyword = keyword.toLowerCase();
            List<Device> filteredList = new ArrayList<>();

            for (Device device : originalDevices) {
                if (device.getName() != null && device.getName().toLowerCase().contains(keyword)) {
                    filteredList.add(device);
                }
            }

            // 只有当过滤结果与当前显示不同时才更新
            if (_dataList.size() != filteredList.size() || !_dataList.containsAll(filteredList)) {
                _dataList.clear();
                _dataList.addAll(filteredList);
                _adapter.notifyDataSetChanged();
            }
        }
    }

    private void postConnected() {
        if(isDisconnectDevice) {
            AppTrace.d(TAG, "postConnected cancel: isDisconnectDevice");
            isDisconnectDevice = false;
            if(_connectDevice != null) {
                boolean isOk = BluetoothManager.getImpl().disconnectBle(_connectDevice);
            }
            dismissConnectingDialog();
            return;
        }
        if(_connectDevice != null) {
            for(Device blue : _dataList) {
                if(blue.getAddress().equals(_connectDevice.getAddress())) {
                    blue.status = Constants.DEVICE_STATUS_CONNECT;
                    //更新设备的蓝牙连接状态
                    _connectDevice.status = Constants.DEVICE_STATUS_CONNECT;
                    //添加本地记录
                    BluetoothManager.getImpl().updateConnectDeviceStatus(_connectDevice);
                    break;
                } else {
                    blue.status = Constants.DEVICE_STATUS_ONLINE;
                }
            }
        }
        retryNumber = 0;
        _networkBean = null;
        _deviceInfo = null;
        isBackgroundScan = true;
//        _handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                //测试发送一个weewa连接成功的消息
//                BluetoothManager.getImpl().sendCmdWeewaConnected();
//            }
//        }, 100);

        _handler.postDelayed(() -> {
            _adapter.notifyDataSetChanged();
            if(isDisconnectDevice) {
                AppTrace.d(TAG, "getWeewaInfo cancel: isDisconnectDevice");
                isDisconnectDevice = false;
                if(_connectDevice != null) {
                    boolean isOk = BluetoothManager.getImpl().disconnectBle(_connectDevice);
                }
                dismissConnectingDialog();
                return;
            }
            if(loadingStep == LOADING_STEP_CONNECT) {
                getWeewaInfo();
            } else if(loadingStep == LOADING_STEP_DEVICE_INFO) {
                getNetworkInfo();
            }

        }, 500);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        BluetoothHandle.removeNetworkNotifyListener(this);
        BluetoothHandle.removeConnectNotifyListener(_connectNotifyListener);
        BluetoothHandle.removeScanNotifyListener(_scanNotifyListener);
        BluetoothHandle.removeDeviceInfoNotifyListener(this);
        BluetoothHandle.removeWeewaInfoNotifyListener(this);

        dismissConnectingDialog();

        dismissNotFoundDialog();

        if(_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }
        _handler = null;

        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    public boolean onRefresh() {
        AppTrace.d(TAG, "onRefresh");
        if(BluetoothManager.getImpl().isScanning()) {
            ToastUtil.s(getString(R.string.loading_dialog_search_device));
            return true;
        }

        if(!BluetoothManager.getImpl().enableBlueTooth()) {
            BluetoothManager.getImpl().openBluetooth(getActivity(), false);
            return false;
        } else {
            if(!PermissionsUtil.isLocServiceEnable(getActivity())) {
                ToastUtil.s(getString(R.string.open_location_service));
                return false;
            }
            if(PermissionsUtil.hasBlePermission(getActivity())) {
                if(BluetoothManager.getImpl() != null) {
                    BluetoothManager.getImpl().startScan(false);
                }
                return true;
            }
        }
        return false;
    }

    private void loadData() {
        AppTrace.d(TAG, "load data");
        List<Device> deviceList = BluetoothManager.getImpl().getBleDeviceList();
        Device connectDevice = BluetoothManager.getConnectDevice();
        if(connectDevice != null) {
            _dataList.add(connectDevice);

            if(deviceList != null && !deviceList.isEmpty()) {
                for(Device device : deviceList) {
                    if(device.address.equalsIgnoreCase(connectDevice.address)) {
                        continue;
                    }
                    _dataList.add(device);
                }
            }
        } else {
            if(deviceList != null && !deviceList.isEmpty()) {
                _dataList.addAll(deviceList);
            }
        }
        AppTrace.d(TAG, "load data size : " + _dataList.size());
        originalDevices.clear();
        originalDevices.addAll(_dataList);
        AppTrace.d(TAG, "originalDevices data size : " + originalDevices.size());
    }

    private void cancelLoading(int loadingType) {
        AppTrace.d(TAG, String.format("cancelLoading: %d", loadingType));
        switch(loadingType) {
            case BluetoothLoadingDialog.TYPE_CONNECT:
            case BluetoothLoadingDialog.TYPE_DISCONNECT:
            case BluetoothLoadingDialog.TYPE_RECONNECT:
                BluetoothManager.getImpl().disconnectBle();
                break;
            case BluetoothLoadingDialog.TYPE_GET_DEVICE_INFO:
            case BluetoothLoadingDialog.TYPE_GET_NET_INFO:
            case BluetoothLoadingDialog.TYPE_GET_WEEWA_INFO:
                isDisconnectDevice = true;
                boolean isok = BluetoothManager.getImpl().disconnectBle();
                break;
        }
    }

    private void showConnectingDialog(String message) {
        AppTrace.d(TAG, "showConnectingDialog: " + message);
        if(!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if(_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                    _connectingDialog = null;
                }
                _connectingDialog = new BluetoothLoadingDialog(getActivity());
                _connectingDialog.setMessage(message);
                _connectingDialog.setOnClickListener((dialog, which) -> {
                    int type = _connectingDialog.getType();
                    cancelLoading(type);

                });
                _connectingDialog.show();
            });
        }
    }

    private void updateConnectMessage(String message, int type) {
        AppTrace.d(TAG, "updateConnectMessage: " + message);
        if(!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if(_connectingDialog == null || !_connectingDialog.isShowing()) {
                    _connectingDialog = new BluetoothLoadingDialog(getActivity());
                    _connectingDialog.setMessage(message);
                    _connectingDialog.setType(type);
                    _connectingDialog.setOnClickListener((dialog, which) -> {
                        int type1 = _connectingDialog.getType();
                        cancelLoading(type1);
                    });
                    _connectingDialog.show();
                } else if(_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.setType(type);
                    _connectingDialog.setMessage(message);
                }
            });
        }
    }

    private void dismissConnectingDialog() {
        AppTrace.d(TAG, "dismissConnectingDialog");
        if(!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if(_connectingDialog != null && _connectingDialog.isShowing()) {
                    _connectingDialog.dismiss();
                }
                _connectingDialog = null;
            });
        }
    }


    private void showNotFoundDialog() {
        _notFoundDialog = new ModalDialog(getActivity());
        _notFoundDialog.setTitle(getString(R.string.loading_dialog_device_not_found));
        _notFoundDialog.setMessage(getString(R.string.loading_dialog_device_message));
        _notFoundDialog.setNegativeButton(getString(R.string.loading_dialog_cancel), v -> _notFoundDialog.dismiss());
        _notFoundDialog.setPositiveButton(getString(R.string.loading_dialog_refresh), v -> {
            _notFoundDialog.dismiss();

            onScanStart(false);

            showConnectingDialog(getActivity().getString(R.string.loading_dialog_search_device));
        });

        _notFoundDialog.show();
    }

    private void dismissNotFoundDialog() {
        if(_notFoundDialog != null && _notFoundDialog.isShowing()) {
            _notFoundDialog.dismiss();
            _notFoundDialog = null;
        }
    }

    @Override
    public void onScanStart(boolean isBackground) {
        if(BluetoothManager.getImpl() != null) {
            BluetoothManager.getImpl().startScan(isBackground);
        }
    }

    @Override
    public void onScan(Device device, int position) {

    }

    @Override
    public void onScanStop(int size) {
        if(BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth()  && PermissionsUtil.hasBlePermission(getActivity())) {
            BluetoothManager.getImpl().stopScan();
        }
    }

    public void connectDevice(Device device, int position) {
        onBluetoothDeviceClick(device, position, Constants.DEVICE_ACTION_CLICK);
    }

    @Override
    public void onBluetoothDeviceClick(Device device, int position, int action) {
        isBackgroundScan = true;
        if(action == Constants.DEVICE_ACTION_CLICK) {
            gotoClickDevice(device);
        }
    }

    @Override
    public void onBluetoothDeviceClick(BindDeviceBean device, int position, int action) {

    }

    @Override
    public void onRequestWifiList() {

    }

    @Override
    public void onSetupWifi(String ssid, String password) {

    }

    @Override
    public void onRemove(Device device, int position) {

    }

    private void gotoClickDevice(Device device) {
        if(type != type_active && type != type_test) {
            if(!LoginManager.isSignedIn()) {
                MobileLoginActivity.startActivityByRequestCode(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                return;
            }
        }

        if(BluetoothManager.getImpl() != null) {
            if(!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                return;
            }

            Device connectDevice = BluetoothManager.getConnectDevice();
            if(connectDevice != null && !connectDevice.getAddress().equalsIgnoreCase(device.address)) {
                resetDeviceStatus();
                boolean isOk = BluetoothManager.getImpl().disconnectBle(connectDevice);
                if(isOk) {
                    updateConnectMessage(String.format("正在断开 %s...", connectDevice.name), BluetoothLoadingDialog.TYPE_DISCONNECT);
                    _pendingDevice = device;
                    _connectDevice = null;
                } else {
                    loadingStep = LOADING_STEP_CONNECT;
                    _readyDevice = null;
                    _pendingDevice = null;
                    BluetoothManager.getImpl().connectToBle(device);
                }
            } else {
                if(_connectDevice != null) {
                    for(Device blue : _dataList) {
                        if(blue.getAddress().equals(_connectDevice.getAddress())) {
                            blue.status = Constants.DEVICE_STATUS_ONLINE;
                            //更新设备的蓝牙连接状态
                            _connectDevice.status = Constants.DEVICE_STATUS_ONLINE;
                            //添加本地记录
                            BluetoothManager.getImpl().updateConnectDeviceStatus(_connectDevice);
                            break;
                        } else {
                            blue.status = Constants.DEVICE_STATUS_ONLINE;
                        }
                    }
                    _adapter.notifyDataSetChanged();

                    _connectDevice = null;
                }
                loadingStep = LOADING_STEP_CONNECT;
                _readyDevice = null;
                _pendingDevice = null;
                BluetoothManager.getImpl().connectToBle(device);
            }

        }
    }

    private void resetDeviceStatus() {
        for(Device blue : _dataList) {
            if(_connectDevice != null && blue.getAddress().equals(_connectDevice.getAddress())) {
                blue.status = Constants.DEVICE_STATUS_ONLINE;
                //更新设备的蓝牙连接状态
                _connectDevice.status = Constants.DEVICE_STATUS_ONLINE;
                //添加本地记录
                BluetoothManager.getImpl().updateConnectDeviceStatus(_connectDevice);
                break;
            } else {
                blue.status = Constants.DEVICE_STATUS_ONLINE;
            }
        }
        _adapter.notifyDataSetChanged();

        _connectDevice = null;
    }

    private void doBluetoothDisconnect() {
        resetDeviceStatus();

    }

    public void getNetworkInfo() {
        if(BluetoothManager.getImpl() != null) {
            if(!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                return;
            }
        }
        updateConnectMessage(str_connect_load_device_network, BluetoothLoadingDialog.TYPE_GET_NET_INFO);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
        if(isOk) {
            loadingStep = LOADING_STEP_NETWORK_INFO;
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            resetDeviceStatus();
            showDisconnectToast();
            dismissConnectingDialog();
        }
    }

    public void getWeewaInfo() {
        if(BluetoothManager.getImpl() != null) {
            if(!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                return;
            }
        }
        if(isDisconnectDevice) {
            AppTrace.d(TAG, "getWeewaInfo cancel: isDisconnectDevice");
            isDisconnectDevice = false;
            return;
        }
        updateConnectMessage(str_connect_load_device_info, BluetoothLoadingDialog.TYPE_GET_WEEWA_INFO);

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
        if(isOk) {
            loadingStep = LOADING_STEP_DEVICE_INFO;
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
            dismissConnectingDialog();
            resetDeviceStatus();
        }
    }

    public void getDeviceInfo() {
        if(BluetoothManager.getImpl() != null) {
            if(!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                return;
            }
        }
        updateConnectMessage(str_connect_load_device_info, BluetoothLoadingDialog.TYPE_GET_DEVICE_INFO);

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceInfoRequestBean());
        if(isOk) {
            loadingStep = LOADING_STEP_DEVICE_INFO;
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
            dismissConnectingDialog();
            resetDeviceStatus();
        }
    }

    private void jumpAction() {
        AppTrace.d(TAG, "jumpAction");
        if(isDisconnectDevice) {
            AppTrace.d(TAG, "jumpAction cancel: isDisconnectDevice");
            isDisconnectDevice = false;
            return;
        }


        if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            if(type == type_active) {
                if(_connectDevice != null && CameraManager.isBindDevice(_connectDevice.address)) {
                    // PlayUtil.startPlay(getActivity(), _deviceInfo, _connectDevice, _networkBean);
                    // _pendingDevice = null;
                    // loadingStep = LOADING_STEP_CONNECT;
                    // isDisconnectDevice = false;
                    getActivity().finish();
                    return;
                }
                if(!CameraManager.isNetworkValid(_networkBean)) {
                    ConnectActivity.start(getActivity(), _connectDevice, Constants.ACTIVE_STEP_SETUP_WIFI);
//                    getActivity().finish();
                } else {
                    ConnectActivity.start(getActivity(), _connectDevice, Constants.ACTIVE_STEP_ACTIVE);
//                    getActivity().finish();
                }
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
            } else if(type == type_soft_ap) {
                String ssid = "";
                String password = "";
                int status = 0;
                if(_networkBean != null && _networkBean.getAp() != null) {
                    ssid = _networkBean.getAp().ssid;
                    password = _networkBean.getAp().password;
                    status = _networkBean.getAp().st;
                }
                CameraSoftApActivity.start(getActivity(), ssid, password, status, CameraSoftApActivity.REQUEST_SOFT_AP_TRANSFER);
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
            } else if(type == type_admin) {
                PlayUtil.startPlay(getActivity(), _deviceInfo, _connectDevice, _networkBean);
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
            } else if(type == type_test) {
                TestActivity.start(getActivity());
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if(type == type_camera_manager) {
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if(type == type_setting_video_clip_manual_dotting) {
                VideoClipActivity.start(getActivity(), VideoClipActivity.type_setting,  VideoClipActivity.clip_type_manual_dotting);
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            }else if(type == type_setting_video_clip_ai) {
                VideoClipActivity.start(getActivity(), VideoClipActivity.type_setting,  VideoClipActivity.clip_type_ai);
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            }

        }
    }

    @Override
    public void onNetworkStatus(NetworkBean networkBean) {
        dismissConnectingDialog();
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        _networkBean = networkBean;
        jumpAction();
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        _deviceInfo = data;

        getNetworkInfo();
    }

    @Override
    public void onDeviceInfoFail() {
        dismissConnectingDialog();
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }

        ToastUtil.s(getString(R.string.please_retry_on_device_info_fail));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void renameDevice(RenameEvent event) {
        if(_dataList != null) {
            for(Device device : _dataList) {
                if(event != null && device != null && device.address != null && device.address.equalsIgnoreCase(event.address)) {
                    device.name = event.name;
                    break;
                }
            }
            _adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onWeewaInfo(WeewaInfo data) {
        AppTrace.d(TAG, "onWeewaInfo");
        dismissConnectingDialog();
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        if(data != null) {
            _deviceInfo = data.d;
            _networkBean = data.n;
            jumpAction();
        } else {
            ToastUtil.s(getString(R.string.please_retry_on_weewa_info_null_fail));
        }
    }

    @Override
    public void onWeewaInfoFail() {
        dismissConnectingDialog();
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        ToastUtil.s(getString(R.string.please_retry_on_weewa_info_fail));
    }


    public void showConnectFailDialog() {
        if(_connectFailDialog != null && _connectFailDialog.isShowing()) {
            return;
        }
        _connectFailDialog = new ConnectFailDialog(requireActivity());
        _connectFailDialog.show();
    }
}
