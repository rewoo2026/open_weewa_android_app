package com.xh.hotme.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaConnectedRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.BleConnectFailEvent;
import com.xh.hotme.bluetooth.event.BleConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.BleConnectTimeoutEvent;
import com.xh.hotme.bluetooth.event.BleConnectingEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectingEvent;
import com.xh.hotme.bluetooth.event.DeviceEvent;
import com.xh.hotme.bluetooth.event.RefreshBindDeviceEvent;
import com.xh.hotme.bluetooth.event.RemoveDeviceEvent;
import com.xh.hotme.bluetooth.event.RenameEvent;
import com.xh.hotme.bluetooth.event.ScanDeviceEvent;
import com.xh.hotme.bluetooth.event.WeewaInfoOutEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.CameraSoftApInfoActivity;
import com.xh.hotme.databinding.FragmentActiveDeviceListBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.storage.VideoClipActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.CommonDialogUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothDialog;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.GridSpacingItemDecoration;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


/**
 * Created by GJK on 2018/11/9.
 */
@IEventBus
public class BindDeviceListFragment extends BaseFragment<FragmentActiveDeviceListBinding> implements IBleScanRequestListener, IBleNetworkInfoNotifyListener, IBleDeviceInfoNotifyListener, IBleWeewaInfoNotifyListener {
    private static final String TAG = BindDeviceListFragment.class.getSimpleName();

    HomeDeviceAdapter _adapter;
//    BluetoothDeviceAdapter _adapter;

    List<BindDeviceBean> _dataList = new ArrayList<>();

    Device _connectDevice;

    BluetoothLoadingDialog _connectingDialog;

    ModalDialog _notFoundDialog;

    Device _readyDevice;
    Device _pendingDevice;

    boolean isBackgroundScan = false;

    private String str_connect_load_device_info;
    private String str_connect_load_device_network;
    private String str_connect_timeout;


    private int type = 0;
    private boolean isSkipCameraInfo = false;
    public static final int type_camera_manager = 0;
    public static final int type_fast_transfer = 1;
    public static final int type_setting_video_clip_manual_dotting = 2;
    public static final int type_setting_video_clip_ai = 3;
    public static final int type_camera_video_storage = 4;

    NetworkBean _networkBean;
    DeviceInfo _deviceInfo;

    final int retryMaxNumber = 3;
    int retryNumber = 0;


    int loadingStep = 0;
    final static int LOADING_STEP_CONNECT = 0;
    final static int LOADING_STEP_DEVICE_INFO = 1;
    final static int LOADING_STEP_NETWORK_INFO = 2;

    boolean isDisconnectDevice = false;

    private final Runnable _timeOutRunnable = () -> {
        MainHandler.runOnUiThread(() -> {
            ToastUtil.s(str_connect_timeout);
            if (isSkipCameraInfo) {
                AppTrace.d("超时退出");
                EventBus.getDefault().post(new WeewaInfoOutEvent());
                BindDeviceListFragment.this.getActivity().finish();
            }
        });
        dismissConnectingDialog();
    };

    public static BindDeviceListFragment newInstance() {
        BindDeviceListFragment fragment = new BindDeviceListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.VIDEO_CATEGORY, type_camera_manager);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static BindDeviceListFragment newInstance(int type, boolean skipGetCameraInfo) {
        BindDeviceListFragment fragment = new BindDeviceListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.VIDEO_CATEGORY, type);
        bundle.putBoolean(Constants.SKIP_CAMERA_INFO, skipGetCameraInfo);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshDevice(final DeviceEvent event) {
//        load();
    }

    @Override
    protected void initView() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            type = bundle.getInt(Constants.VIDEO_CATEGORY, type_camera_manager);
            isSkipCameraInfo = bundle.getBoolean(Constants.SKIP_CAMERA_INFO);
            AppTrace.d("camera list  type=" + type);
        }

        str_connect_load_device_info = getActivity().getString(R.string.loading_dialog_load_device_info);
        str_connect_load_device_network = getActivity().getString(R.string.loading_dialog_load_device_network);
        str_connect_timeout = getActivity().getString(R.string.please_check_device_power_on_for_timeout);

        _viewBinding.recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        _viewBinding.recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, DensityUtil.dip2px(10), false));

        _dataList = DeviceManager.getBindDeviceList();

        _adapter = new HomeDeviceAdapter(getActivity(), _dataList, () -> {
            //  connectBluetoothRequest = true;
        });

        _adapter.setScanRequestListener(new IBleScanRequestListener() {
            @Override
            public void onScanStart(boolean isBackground) {

            }

            @Override
            public void onScan(Device device, int position) {

            }

            @Override
            public void onScanStop(int size) {

            }

            @Override
            public void onBluetoothDeviceClick(Device device, int position, int action) {

                isDisconnectDevice = false;

                Log.d(TAG, "click to =");
                if (action == Constants.DEVICE_ACTION_OPEN) {
                    openDevice(device);
                } else if (action == Constants.DEVICE_ACTION_DISCONNECT) {
                    isDisconnectDevice = true;
                    disconnectDevice(device);
                } else if (action == Constants.DEVICE_ACTION_REMOVE) {
                    if (DeviceManager.getImpl() != null) {
                        DeviceManager.getImpl().removeBindDeviceData(device);
                    }
                } else {
                    if (!LoginManager.isSignedIn()) {
                        readyDevice = device;
                        MobileLoginActivity.startActivityByRequestCode(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                        return;
                    }
                    readyDevice = null;
                    if (BluetoothManager.getImpl() != null) {
                        if (!BluetoothManager.getImpl().enableBlueTooth()) {
                            BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                            return;
                        }
                        BluetoothManager.getImpl().connectToBle(device);
                    }
                }
            }

            @Override
            public void onBluetoothDeviceClick(BindDeviceBean device, int position, int action) {

                CameraInfo cameraInfo = CameraManager.getCameraInfo("");
                if(cameraInfo != null) {
                    CameraManager.release();
                    EventBus.getDefault().post(new BleDisConnectSuccessEvent(device.getBluetoothUuid(), true));
                }

                CameraManager.setDeviceId(device.getDeviceId());

                CameraManager.newCamera(device.getBluetoothUuid());

                DeviceManager.getImpl().setBindConnectedDevice(new Device(device.getBluetoothUuid(),0, device.getDeviceName()));
                DeviceManager.getImpl().setConnectedBindDevice(device);

                if(device.getOnlineStatus() == 1) {
                    CameraMonitor.getImpl().setOnline(true);
                }

                EventBus.getDefault().post(new BleConnectSuccessEvent(device.getBluetoothUuid()));
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
        });

        _viewBinding.recyclerView.setAdapter(_adapter);

        View emptyView = LayoutInflater.from(getActivity()).inflate(R.layout.home_fragment_device_no, null);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.findViewById(R.id.btn_device_add).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    addDevice();
                } else {
                    CommonDialogUtils.showAccessModelDialog(getActivity(), Constants.ACCESS_MODEL, null);
                }

                return true;
            }
        });
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            emptyView.findViewById(R.id.btn_device_add).setVisibility(View.GONE);
        }
        //添加空视图
        _adapter.setEmptyView(emptyView);


        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();

        BluetoothHandle.addNetworkNotifyListener(this);
        BluetoothHandle.addDeviceInfoNotifyListener(this);
        BluetoothHandle.addWeewaInfoNotifyListener(this);


        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
            } else {
                if (!PermissionsUtil.hasBlePermission(getActivity())) {
                    requestBluetoothPermission();
                }
            }
            //判断网络是否可用，无网络则扫描周边设备
            if (!NetUtil.isConnectedAvailableNetwork()) {
                if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth() && PermissionsUtil.hasBlePermission(getActivity())) {
                    if (BluetoothManager.getImpl().isScanning()) {
                        BluetoothManager.getImpl().stopScan();
                    }
                    if (DeviceManager.getImpl() != null) {
                        DeviceManager.getImpl().searchBluetoothDevice(true);
                    }
                }
            } else {
                DeviceManager.getImpl().onRefreshList(false);
            }
        } else {
            if (NetUtil.isConnectedAvailableNetwork()) {
                DeviceManager.getImpl().onRefreshList(false);
            }
        }
    }

    @Override
    protected void initData() {

    }

    private boolean addDevice() {
        if (!BluetoothManager.getImpl().enableBlueTooth()) {
            BluetoothDialog dialog = new BluetoothDialog(requireActivity());
            dialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        ((Activity) getActivity()).startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_SCAN);
                    }

                    dialog.dismiss();
                }
            });
            dialog.show();

            return false;
        } else {
            boolean hasPermission = PermissionsUtil.hasBlePermission(getActivity());
            if (!hasPermission) {
                EasyPermissions.requestPermissions(getActivity(), getString(R.string.bluetooth_permission_title), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                return false;
            }
            if (!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(getActivity());
                return false;
            }

            ActiveListActivity.start(getActivity());

            return true;
        }

    }

    private void postConnected() {
        if (isDisconnectDevice) {
            AppTrace.d(TAG, "postConnected cancel: isDisconnectDevice");
            return;
        }
        if (_connectDevice != null) {
            for (BindDeviceBean blue : _dataList) {
                if (blue.getBluetoothUuid() != null && TextUtils.isEmpty(blue.getBluetoothUuid()) && blue.getBluetoothUuid().equals(_connectDevice.getAddress())) {
                    blue.setStatus(Constants.DEVICE_STATUS_CONNECT);
                    //更新设备的蓝牙连接状态
                    _connectDevice.status = Constants.DEVICE_STATUS_CONNECT;
                    //添加本地记录
                    BluetoothManager.getImpl().updateConnectDeviceStatus(_connectDevice);
                    break;
                } else {
                    blue.setStatus(Constants.DEVICE_STATUS_ONLINE);
                }
            }
        }
        retryNumber = 0;
        _networkBean = null;
        _deviceInfo = null;
        isBackgroundScan = true;
        _handler.postDelayed(() -> {
            //测试发送一个weewa连接成功的消息
            DeviceApiManager.sendCmdSimpleObj(new WeewaConnectedRequestBean());
        }, 100);

        _handler.postDelayed(() -> {
            _adapter.notifyDataSetChanged();
            if (isDisconnectDevice) {
                AppTrace.d(TAG, "getWeewaInfo cancel: isDisconnectDevice");
                return;
            }

            if (loadingStep == LOADING_STEP_CONNECT) {
                getWeewaInfo();
            } else if (loadingStep == LOADING_STEP_DEVICE_INFO) {
                getNetworkInfo();
            }

        }, 500);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        BluetoothHandle.removeNetworkNotifyListener(this);
        BluetoothHandle.removeDeviceInfoNotifyListener(this);
        BluetoothHandle.removeWeewaInfoNotifyListener(this);

        dismissConnectingDialog();

        dismissNotFoundDialog();

        _handler = null;

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    public boolean onRefresh() {

        if (BluetoothManager.getImpl().isScanning()) {
            ToastUtil.s(getString(R.string.loading_dialog_search_device));
            return true;
        }

        if (!BluetoothManager.getImpl().enableBlueTooth()) {
            BluetoothManager.getImpl().openBluetooth(getActivity(), false);
            return false;
        } else {
            if (!PermissionsUtil.isLocServiceEnable(getActivity())) {
                ToastUtil.s(getString(R.string.open_location_service));
                return false;
            }
            if (PermissionsUtil.hasBlePermission(getActivity())) {
                if (BluetoothManager.getImpl() != null) {
                    BluetoothManager.getImpl().startScan(false);
                }
                return true;
            }
        }
        return false;
    }

    private void cancelLoading(int loadingType) {
        AppTrace.d(TAG, String.format("cancelLoading: %d", loadingType));
        switch (loadingType) {
            case BluetoothLoadingDialog.TYPE_CONNECT:
            case BluetoothLoadingDialog.TYPE_DISCONNECT:
            case BluetoothLoadingDialog.TYPE_RECONNECT:
                BluetoothManager.getImpl().disconnectBle();
                CameraManager.release();
                break;
            case BluetoothLoadingDialog.TYPE_GET_DEVICE_INFO:
            case BluetoothLoadingDialog.TYPE_GET_NET_INFO:
            case BluetoothLoadingDialog.TYPE_GET_WEEWA_INFO:
                isDisconnectDevice = true;
                boolean isok = BluetoothManager.getImpl().disconnectBle();
                CameraManager.release();
                break;
        }
    }

    private void showConnectingDialog(String message) {
        AppTrace.d(TAG, "showConnectingDialog: " + message);
        if (!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
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
        if (!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if (_connectingDialog == null || !_connectingDialog.isShowing()) {
                    _connectingDialog = new BluetoothLoadingDialog(getActivity());
                    _connectingDialog.setMessage(message);
                    _connectingDialog.setType(type);
                    _connectingDialog.setOnClickListener((dialog, which) -> {
                        int type1 = _connectingDialog.getType();
                        cancelLoading(type1);
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
        if (!BaseAppUtil.isDestroy(getActivity())) {
            getActivity().runOnUiThread(() -> {
                if (_connectingDialog != null && _connectingDialog.isShowing()) {
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
        if (_notFoundDialog != null && _notFoundDialog.isShowing()) {
            _notFoundDialog.dismiss();
            _notFoundDialog = null;
        }
    }

    @Override
    public void onScanStart(boolean isBackground) {
        if (BluetoothManager.getImpl() != null) {
            BluetoothManager.getImpl().startScan(isBackground);
        }
    }

    @Override
    public void onScan(Device device, int position) {

    }

    @Override
    public void onScanStop(int size) {
        if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth() && PermissionsUtil.hasBlePermission(getActivity())) {
            if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().isScanning()) {
                BluetoothManager.getImpl().stopScan();
            }
        }
    }

    public void connectDevice(Device device, int position) {
        onBluetoothDeviceClick(device, position, Constants.DEVICE_ACTION_CLICK);
    }

    @Override
    public void onBluetoothDeviceClick(Device device, int position, int action) {
        isBackgroundScan = true;
        if (type == type_camera_manager) {
            if (!LoginManager.isSignedIn()) {
                MobileLoginActivity.startActivityByRequestCode(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                return;
            }
        }

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (BluetoothManager.getImpl() != null) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                    return;
                }

                Device connectDevice = DeviceManager.getImpl().getBindConnectedDevice();
                if (connectDevice != null && !connectDevice.getAddress().equalsIgnoreCase(device.address)) {
                    updateConnectMessage(String.format("正在断开 %s...", connectDevice.name), BluetoothLoadingDialog.TYPE_DISCONNECT);
                    BluetoothManager.getImpl().disconnectBle(connectDevice);
                    _pendingDevice = device;
                } else {
                    loadingStep = LOADING_STEP_CONNECT;
                    _readyDevice = null;
                    _pendingDevice = null;
                    BluetoothManager.getImpl().connectToBle(device);
                }

            }
        } else {
            _connectDevice = device;
            loadingStep = LOADING_STEP_CONNECT;
            DeviceManager.getImpl().setBindConnectedDevice(device);
            postConnected();
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


    public void getNetworkInfo() {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (BluetoothManager.getImpl() != null) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                    return;
                }
            }
        }
        updateConnectMessage(str_connect_load_device_network, BluetoothLoadingDialog.TYPE_GET_NET_INFO);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
        if (isOk) {
            loadingStep = LOADING_STEP_NETWORK_INFO;
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
            dismissConnectingDialog();
        }
    }

    public void getWeewaInfo() {
        if (isDisconnectDevice) {
            AppTrace.d(TAG, "getWeewaInfo cancel: isDisconnectDevice");
            return;
        }
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (BluetoothManager.getImpl() != null) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                    return;
                }
            }
        }
        updateConnectMessage(str_connect_load_device_info, BluetoothLoadingDialog.TYPE_GET_WEEWA_INFO);

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
        if (isOk) {
            loadingStep = LOADING_STEP_DEVICE_INFO;
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
            dismissConnectingDialog();
        }
    }

    private void jumpAction() {
        AppTrace.d(TAG, "jumpAction");
        if (isDisconnectDevice) {
            AppTrace.d(TAG, "jumpAction cancel: isDisconnectDevice");
            return;
        }
        if (!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            if (type == type_camera_manager) {
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if (type == type_camera_video_storage) {
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if (type == type_fast_transfer) {
                String ssid = "";
                String password = "";
                int status = 0;
                if (_networkBean != null && _networkBean.getAp() != null) {
                    ssid = _networkBean.getAp().ssid;
                    password = _networkBean.getAp().password;
                    status = _networkBean.getAp().st;
                }
                if (status == 0) {
                    CameraSoftApActivity.start(getActivity(), ssid, password, status, CameraSoftApActivity.REQUEST_SOFT_AP_TRANSFER);
                } else {
                    CameraSoftApInfoActivity.startActivity(getActivity(), ssid, password);
                }

                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if (type == type_setting_video_clip_manual_dotting) {
                VideoClipActivity.start(getActivity(), VideoClipActivity.type_setting,  VideoClipActivity.clip_type_manual_dotting);
                _pendingDevice = null;
                loadingStep = LOADING_STEP_CONNECT;
                isDisconnectDevice = false;
                getActivity().finish();
            } else if (type == type_setting_video_clip_ai) {
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
        AppTrace.d("onNetworkStatus");
        dismissConnectingDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        _networkBean = networkBean;
        if(loadingStep == LOADING_STEP_DEVICE_INFO){
            jumpAction();
        }
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
//        dismissConnectingDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        _deviceInfo = data;
//        if (type == type_soft_ap) {
//        if (!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
//            PlayUtil.startPlay(getActivity(), data, _connectDevice);
//            }
//        }

        getNetworkInfo();
    }

    @Override
    public void onDeviceInfoFail() {
        dismissConnectingDialog();
        _handler.removeCallbacks(_timeOutRunnable);

        ToastUtil.s(getString(R.string.please_retry_on_device_info_fail));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void renameDevice(RenameEvent event) {
        if (_dataList != null) {
            for (BindDeviceBean device : _dataList) {
                if (event != null && device != null && device.getBluetoothUuid() != null && device.getBluetoothUuid().equalsIgnoreCase(event.address)) {
                    device.setDeviceName(event.name);
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
        _handler.removeCallbacks(_timeOutRunnable);
        if (data != null) {
            _deviceInfo = data.d;
            _networkBean = data.n;
            jumpAction();
        } else {
            AppTrace.d(TAG, "onWeewaInfo fail，data is null");
            ToastUtil.s(getString(R.string.please_retry_on_weewa_info_fail));
        }
    }

    @Override
    public void onWeewaInfoFail() {
        AppTrace.d(TAG, "onWeewaInfoFail");
        dismissConnectingDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        ToastUtil.s(getString(R.string.please_retry_on_weewa_info_fail));
    }

//    private void getDeviceList() {
//        UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
//        if (userInfoBean == null) {
//            AppTrace.e(TAG, "not login");
//            if (_dataList != null) {
//                _dataList.clear();
//            }
//            _adapter.notifyDataSetChanged();
//            return;
//        }
//
//        DeviceManager.getImpl().getDeviceList();
//    }

    Device readyDevice;

    private void openDevice(Device device) {
        AppTrace.d(TAG, "openDevice =");
        if (!LoginManager.isSignedIn()) {
            readyDevice = device;
            MobileLoginActivity.startActivityByRequestCode(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
            return;
        }
        readyDevice = null;
        if (BluetoothManager.getImpl() != null) {
            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                return;
            }
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
            if (!isOk) {
                BluetoothManager.getImpl().connectToBle(device);
            }
        }
    }

    public void disconnectDevice(Device device) {
        if (BluetoothManager.getImpl() != null) {
            boolean isOk = BluetoothManager.getImpl().disconnectBle(device);
            if (!isOk) {
                ToastUtil.s(getString(R.string.device_offline));
            }
        }
    }

    protected void loadData() {

        _dataList = DeviceManager.getBindDeviceList();

        _adapter.notifyDataSetChanged();

        //判断网络是否可用，无网络则扫描周边设备
        if (!NetUtil.isConnectedAvailableNetwork()) {
            if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth() && PermissionsUtil.hasBlePermission(getActivity())) {
                if (BluetoothManager.getImpl().isScanning()) {
                    BluetoothManager.getImpl().stopScan();
                }
                DeviceManager.getImpl().searchBluetoothDevice(true);
            }
            return;
        }

        DeviceManager.getImpl().onRefreshList(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshBindDevice(RefreshBindDeviceEvent event) {
        AppTrace.d(TAG, "onRefreshBindDeviceEvent");
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        reloadDeviceList();
    }

    public void reloadDeviceList() {
        AppTrace.d(TAG, "reloadDeviceList");
        MainHandler.runOnUiThread(() -> {
            try {
                _dataList = DeviceManager.getBindDeviceList();

                _adapter.notifyDataSetChanged();

            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectFailDevice(BleConnectFailEvent event) {
        AppTrace.d(TAG, "onDeviceConnectFailDeviceEvent");
        if (event == null || event.address == null) {
            return;
        }
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();

        //提示连接失败
        if (_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(event.address)) {
            if (retryNumber > retryMaxNumber) {
                dismissConnectingDialog();
                if (getContext() != null) {
                    ToastUtil.s(getContext().getString(R.string.info_ble_connect_fail));
                }
                _connectDevice = null;
                retryNumber = 0;
            } else {
                retryNumber++;
                updateConnectMessage(String.format("正在重连%s， 第%d次...", BluetoothManager.getDisplayDeviceName(_connectDevice.name), retryNumber), BluetoothLoadingDialog.TYPE_RECONNECT);
                AppTrace.d(TAG, "reconnect " + event.address + ", count = " + retryNumber);
                try {
                    Thread.sleep(1000);
                    connectDevice(_connectDevice, 0);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnecting(BleConnectingEvent event) {
        AppTrace.d(TAG, "onDeviceConnectingEvent");
        if (event == null || event.address == null) {
            return;
        }
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _connectDevice = DeviceManager.getImpl().getBindConnectedDevice();
        updateConnectMessage(String.format("%s%s", getString(R.string.loading_dialog_connect_device), BluetoothManager.getDisplayDeviceName(event.name)), BluetoothLoadingDialog.TYPE_CONNECT);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectSuccess(BleConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceConnectSuccessEvent");
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();
        _connectDevice = DeviceManager.getImpl().getBindConnectedDevice();

        if (event != null) {
            String address = event.address;
//            postConnected(address, Constants.DEVICE_STATUS_CONNECT);
            postConnected();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectTimeout(BleConnectTimeoutEvent event) {
        AppTrace.d(TAG, "onDeviceConnectTimeoutEvent");
        if (event == null || event.address == null) {
            return;
        }
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();

        if (!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            dismissConnectingDialog();
            if (getContext() != null) {
                ToastUtil.s(getContext().getString(R.string.info_ble_connect_timeout));
            }
        } else {
            AppTrace.d(TAG, "onConnectTimeOut skip for not isForegroundActivity " + event.address);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnecting(BleDisConnectingEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnectingEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnectSuccess(BleDisConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnectSuccessEvent");
        if (event == null) {
            return;
        }
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();
        _connectDevice = DeviceManager.getImpl().getBindConnectedDevice();

        if (_connectDevice != null && _connectDevice.getAddress().equalsIgnoreCase(event.address)) {
            if (!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                if(!event.isRemote) {
                    if (_pendingDevice != null) {
                        _connectDevice = null;
                        _networkBean = null;
                        connectDevice(_pendingDevice, 0);
                    } else {
                        dismissConnectingDialog();
                        if (getContext() != null) {
                            ToastUtil.s(getContext().getString(R.string.info_ble_disconnect));
                        }
                        if (!isDisconnectDevice) {
                            ModalDialog modalDialog = new ModalDialog(getActivity());
                            modalDialog.setMessage(String.format("%s已断开，重试进入相机", BluetoothManager.getDisplayDeviceName(_connectDevice.getName())));
                            modalDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                                @Override
                                public boolean onClicked() {
                                    _connectDevice = null;
                                    _networkBean = null;
                                    return true;
                                }
                            });
                            modalDialog.setPositiveButton(getString(R.string.retry), new ClickGuard.GuardedOnClickListener() {
                                @Override
                                public boolean onClicked() {
                                    connectDevice(_connectDevice, 0);
                                    return true;
                                }
                            });
                            modalDialog.show();
                        } else {
                            _connectDevice = null;
                            _networkBean = null;
                        }
                        isDisconnectDevice = false;
                    }
                } else {
                    _connectDevice = null;
                    _networkBean = null;
                }
            } else {
                _connectDevice = null;
                _networkBean = null;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanDevice(ScanDeviceEvent event) {
        AppTrace.d(TAG, "onScanDeviceEvent");
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveDevice(RemoveDeviceEvent event) {
        AppTrace.d(TAG, "onRemoveDeviceEvent");
        if (BaseAppUtil.isDestroy(getActivity())) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        _adapter.notifyDataSetChanged();
    }
}
