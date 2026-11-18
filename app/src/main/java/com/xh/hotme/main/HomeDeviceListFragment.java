package com.xh.hotme.main;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
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
import com.xh.hotme.bluetooth.event.ScanStopEvent;
import com.xh.hotme.bluetooth.event.ScanTimeoutEvent;
import com.xh.hotme.bluetooth.event.ScanningEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.HomeDeviceAdapter;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.live.dialog.DevicesDialogUtils;
import com.xh.hotme.record.RewooIntroActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.CommonDialogUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothDialog;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ConnectFailDialog;
import com.xh.hotme.widget.GridSpacingItemDecoration;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public abstract class HomeDeviceListFragment<T extends ViewBinding> extends BaseFragment<T> implements View.OnClickListener, IBleDeviceInfoNotifyListener {
    private static final String TAG = HomeDeviceListFragment.class.getSimpleName();
    private static final Logger log = LoggerFactory.getLogger(HomeDeviceListFragment.class);
    public ImageView mAddButton, refreshButton;
    private TextView mTipView;
    RecyclerView recyclerView;

    HomeDeviceAdapter mAdapter;

    public List<BindDeviceBean> _dataList = new ArrayList<>();
    IBleWeewaInfoNotifyListener _weewaInfoNotifyListener;

    Device _connectDevice;
    DeviceInfo _connectDeviceInfo;

    BluetoothLoadingDialog _connectingDialog;

    ModalDialog _notFoundDialog;


    ObjectAnimator _refreshAnimator;

    boolean isFirstScan = true;

    protected boolean connectBluetoothRequest = false;

    WeewaInfo _weewaInfo;

    boolean isDisconnectDevice = false;

    public SwipeRefreshLayout _refreshLayout;

    ConnectFailDialog _connectFailDialog;

    private String str_request_timeout;

    private final Runnable _timeOutRunnable = () -> {

        dismissLoadingDialog();
        ToastUtil.s(str_request_timeout);
    };

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        if(BluetoothManager.getImpl().isScanning()) {
            startAnim();
        } else {
            stopAnim();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeWeewaInfoNotifyListener(_weewaInfoNotifyListener);
        BluetoothHandle.removeDeviceInfoNotifyListener(this);
    }

    @Override
    protected void onVisibleChanged(boolean isVisible) {
        if(isVisible) {
            reloadDeviceList();
        }
    }

    public void reloadDeviceList() {
        AppTrace.d(TAG, "reloadDeviceList");
        MainHandler.runOnUiThread(() -> {
            try {
                _dataList = DeviceManager.getBindDeviceList();

                mAdapter.notifyDataSetChanged();
            } catch(Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshDevice(final DeviceEvent event) {
        AppTrace.d(TAG, "onRefreshDeviceEvent");
        reloadDeviceList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRenameDevice(final RenameEvent event) {
        AppTrace.d(TAG, "onRefreshDeviceEvent");
        reloadDeviceList();
    }

    private void initListener() {
        _weewaInfoNotifyListener = new IBleWeewaInfoNotifyListener() {
            @Override
            public void onWeewaInfo(WeewaInfo data) {
                if(!isFragmentVisible()) {
                    return;
                }
                AppTrace.d(TAG, "onWeewaInfo");
                dismissLoadingDialog();
                if(_handler != null) {
                    _handler.removeCallbacks(_timeOutRunnable);
                }
                if(data != null) {
                    _weewaInfo = data;
                    jumpAction();

//                    BindDeviceBean bindDevice = DeviceManager.getImpl().getConnectedBindDevice();
//                    if(bindDevice != null) {
//                        if(!bindDevice.getDeviceName().equalsIgnoreCase(_weewaInfo.d.device_name)) {
//                            try {
//                                if(NetUtil.isConnectedAvailableNetwork()) {
//                                    ApiUtil.updateDeviceName(_weewaInfo.d.device_id, data.d.device_name, null);
//                                }
//                            } catch(Throwable e) {
//                                e.printStackTrace();
//                            }
//
//                        }
//                    }
                } else {
                    ToastUtil.s(getString(R.string.info_please_try_it_later_get_weewa_info_fail));
                }
            }

            @Override
            public void onWeewaInfoFail() {
                if(!isFragmentVisible()) {
                    return;
                }
                dismissLoadingDialog();
                if(_handler != null) {
                    _handler.removeCallbacks(_timeOutRunnable);
                }
                ToastUtil.s(getString(R.string.info_please_try_it_later_get_weewa_info_fail));
            }
        };
    }


    protected int getLayoutId() {
        return R.layout.home_fragment_default;
    }

    protected void initViewBinding(LayoutInflater inflater, ViewGroup container) {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        assert type != null;
        Class<?> cls = (Class<?>) type.getActualTypeArguments()[0];
        try {
            Method bind = cls.getDeclaredMethod("bind", View.class);
            _viewBinding = (T) bind.invoke(null, inflater.inflate(getLayoutId(), container, false));
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initView() {

        str_request_timeout = getString(R.string.request_timeout_get_weewa_info);

        initListener();

        BluetoothHandle.addWeewaInfoNotifyListener(_weewaInfoNotifyListener);
        BluetoothHandle.addDeviceInfoNotifyListener(this);

        mAddButton = findViewById(R.id.iv_add_device);
        refreshButton = findViewById(R.id.iv_refresh);
        mTipView = findViewById(R.id.tv_camera_tip);
        recyclerView = findViewById(R.id.recyclerView);
        _refreshLayout = findViewById(R.id.refreshLayout);

        _refreshLayout.setOnRefreshListener(() -> {
            if(!NetUtil.isConnectedAvailableNetwork()) {
                DevicesDialogUtils.openWifiSetting(getActivity());
                _refreshLayout.setRefreshing(false);
                return;
            }

            if(!LoginManager.isSignedIn()) {
                _refreshLayout.setRefreshing(false);
                MobileLoginActivity.startActivityByRequestCode(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                return;
            }
            if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth() && PermissionsUtil.hasBlePermission(getActivity())) {
                if (BluetoothManager.getImpl().isScanning()) {
                    BluetoothManager.getImpl().stopScan();
                }
            }

            DeviceManager.getImpl().onRefreshList(false);

            MainHandler.getImpl().post(() -> _refreshLayout.setRefreshing(false));

        });

        _refreshAnimator = ObjectAnimator.ofFloat(refreshButton, "rotation", 1080f);
        _refreshAnimator.setDuration(2000L);
        _refreshAnimator.setRepeatCount(-1);

        mAddButton.setOnClickListener(this);
        mTipView.setOnClickListener(this);
        refreshButton.setOnClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, DensityUtil.dip2px(10), false));

        _dataList = DeviceManager.getBindDeviceList();

        mAdapter = new HomeDeviceAdapter(getActivity(), _dataList, () -> {
            connectBluetoothRequest = true;
            isDisconnectDevice = false;
        });

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
        //添加空视图
        mAdapter.setEmptyView(emptyView);


        // BRVAH 4 composes footer rows with ConcatAdapter. Hide the add row
        // while the empty state is visible, as the previous adapter did.
        RecyclerView.Adapter<RecyclerView.ViewHolder> footerAdapter =
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        return new RecyclerView.ViewHolder(createDeviceFooter(parent)) {};
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}

                    @Override
                    public int getItemCount() {
                        return mAdapter.getItems().isEmpty() ? 0 : 1;
                    }
                };
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                footerAdapter.notifyDataSetChanged();
            }
        });


        mAdapter.setScanRequestListener(bleScanRequestListener);

        recyclerView.setAdapter(new ConcatAdapter(mAdapter, footerAdapter));

        loadData();

        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void initData() {
    }

    private View createDeviceFooter(ViewGroup parent) {
        View footView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_fragment_device_add, parent, false);
        footView.findViewById(R.id.foot_device_add).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
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
        return footView;
    }

    private boolean addDevice() {
        if(!BluetoothManager.getImpl().enableBlueTooth()) {
            BluetoothDialog dialog = new BluetoothDialog(requireActivity());
            dialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == DialogInterface.BUTTON_POSITIVE) {
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
            if(!hasPermission) {
                EasyPermissions.requestPermissions(getActivity(), getString(R.string.bluetooth_permission_title), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                return false;
            }
            if(!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(getActivity());
                return false;
            }

            ActiveListActivity.start(getActivity());

            return true;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        dismissLoadingDialog();

        dismissNotFoundDialog();
    }

    private void startScanDevice() {
        if(BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth() && PermissionsUtil.hasBlePermission(getActivity())) {
            DeviceManager.getImpl().searchBluetoothDevice(true);
        }
    }

    protected void loadData() {
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();

        if(!isCurrentHomeFragment()) {
            return;
        }

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            //判断网络是否可用，无网络则扫描周边设备
            if (!NetUtil.isConnectedAvailableNetwork()) {
                startScanDevice();
                return;
            }
        }
        DeviceManager.getImpl().onRefreshList(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.iv_add_device) {
            if(!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(getActivity());
                return;
            }
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                    return;
                }

                if (!PermissionsUtil.isLocServiceEnable(getActivity())) {
                    PermissionsUtil.openLocationService(getActivity());
                    return;
                }

                boolean hasPermission = PermissionsUtil.hasBlePermission(getActivity());
                if (!hasPermission) {
                    //去请求权限
                    requestBluetoothPermission();
                    return;
                }

                ActiveListActivity.start(getActivity());
            } else {
                CommonDialogUtils.showAccessModelDialog(getActivity(), Constants.ACCESS_MODEL, null);
            }
        } else if(id == R.id.tv_camera_tip) {
            RewooIntroActivity.start(getActivity());
        } else if(id == R.id.iv_refresh) {

            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {

                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(getActivity(), false);
                    return;
                }

                if (!PermissionsUtil.isLocServiceEnable(getActivity())) {
                    PermissionsUtil.openLocationService(getActivity());
                    return;
                }

                boolean hasPermission = PermissionsUtil.hasBlePermission(getActivity());
                if (!hasPermission) {
                    //去请求权限
                    requestBluetoothPermission();
                    return;
                }

                //开始搜索设备
                startScanDevice();
            } else {
                if (!NetUtil.isConnectedAvailableNetwork()) {
                    if(NetworkManager.getImpl().isConnectedSoftAp()){
                        showNetworkDisconnectDialog(getString(R.string.phone_no_network_title), getString(R.string.phone_no_network_message_connnect_weewa));
                    } else {
                        showNetworkDisconnectDialog();
                    }
                    return;
                }

                onScanning(null);
                DeviceManager.getImpl().onRefreshList(false);
            }
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

    private void showLoadingDialog(String message) {
        AppTrace.d(TAG, "showLoadingDialog");
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(getActivity());
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissLoadingDialog() {
        AppTrace.d(TAG, "dismissLoadingDialog");
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    private void showNotFoundDialog() {
        _notFoundDialog = new ModalDialog(getActivity());
        _notFoundDialog.setTitle(getString(R.string.loading_dialog_device_not_found));
        _notFoundDialog.setMessage(getString(R.string.loading_dialog_device_message));
        _notFoundDialog.setNegativeButton(getString(R.string.loading_dialog_cancel), v -> _notFoundDialog.dismiss());
        _notFoundDialog.setPositiveButton(getString(R.string.loading_dialog_refresh), v -> {
            _notFoundDialog.dismiss();

            if(bleScanRequestListener != null) {
                bleScanRequestListener.onScanStart(false);
            }
            showLoadingDialog(getActivity().getString(R.string.loading_dialog_search_device));
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
    public void onDeviceInfo(DeviceInfo data) {
        dismissLoadingDialog();
        _connectDeviceInfo = data;
        if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            Device device = DeviceManager.getImpl().getBindConnectedDevice();
            if(device != null) {
                data.device_name = device.name;
                PlayUtil.startPlay(getActivity(), data, device);
            }
        }
    }

    @Override
    public void onDeviceInfoFail() {
        dismissLoadingDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshBindDevice(RefreshBindDeviceEvent event) {
        reloadDeviceList();
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
            stopAnim();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void renameDevice(RenameEvent event) {
        try {
            if(_dataList != null) {
                for(BindDeviceBean device : _dataList) {
                    if(event != null && device != null && device.getBluetoothUuid() != null && device.getBluetoothUuid().equalsIgnoreCase(event.address)) {
                        device.setDeviceName(event.name);
                        break;
                    }
                }
                if(mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        } catch(Throwable e) {

        }
    }

    private void postConnected(String address, int status) {
        _connectDevice = DeviceManager.getImpl().getBindConnectedDevice();
        if(_connectDevice == null) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        if(_dataList != null && !_dataList.isEmpty()) {
            boolean isValid = false;
            for(int i = 0; i < _dataList.size(); i++) {
                BindDeviceBean blue = _dataList.get(i);
//                fix 蓝牙地址
//                && blue.getBluetoothUuid().equals(_connectDevice.getAddress())
                if(_connectDevice != null && blue.getBluetoothUuid() != null && blue.getBluetoothUuid().equals(_connectDevice.getAddress())) {
                    blue.setStatus(Constants.DEVICE_STATUS_CONNECT);
                    onHomeConnectSuccess(blue, status);
                    isValid = true;
                } else {
                    if(blue.getStatus() == Constants.DEVICE_STATUS_CONNECT) {
                        blue.setStatus(Constants.DEVICE_STATUS_ONLINE);
                    }
                }
            }
            if(isValid) {

                _handler.postDelayed(() -> {

                    mAdapter.notifyDataSetChanged();

                    if(!connectBluetoothRequest) {
                        connectBluetoothRequest = false;
                        return;
                    }

                    if(isDisconnectDevice) {
                        AppTrace.d(TAG, "getWeewaInfo cancel: isDisconnectDevice");
                        isDisconnectDevice = false;
                        if(_connectDevice != null) {
                            boolean isOk = BluetoothManager.getImpl().disconnectBle(_connectDevice);
                        }
                        dismissLoadingDialog();
                        return;
                    }

                    if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), MainActivity.class.getName())) {

                        updateConnectMessage(getActivity().getString(R.string.loading_dialog_load_device_info), BluetoothLoadingDialog.TYPE_GET_WEEWA_INFO);

                        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
                        if(isOk) {
                            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                        } else {
                            dismissLoadingDialog();
                            showDisconnectToast();
                        }
                    }

                }, 500);
            }
        }
    }

    private void startAnim() {
        if(_refreshAnimator != null && !_refreshAnimator.isRunning()) {
            _refreshAnimator.start();
        }
    }


    private void stopAnim() {
        if(_refreshAnimator != null && _refreshAnimator.isRunning()) {
            _refreshAnimator.end();
        }
    }

    protected void jumpAction() {
        AppTrace.d(TAG, "jumpAction");
        if(!connectBluetoothRequest) {
            connectBluetoothRequest = false;
            return;
        }

        if(mAdapter.getScene() == HomeDeviceAdapter.SCENE_TYE_RECORD) {
            if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
            }
        }
    }

    protected void onHomeConnectSuccess(BindDeviceBean device, int status) {

    }

    protected abstract boolean isCurrentHomeFragment();

    public void showConnectFailDialog() {
        if(_connectFailDialog != null && _connectFailDialog.isShowing()) {
            return;
        }
        _connectFailDialog = new ConnectFailDialog(requireActivity());
        _connectFailDialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectFailDevice(BleConnectFailEvent event) {
        AppTrace.d(TAG, "onDeviceConnectFailDevice");
        if(event == null || event.address == null) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();

        if(!isFragmentVisible()) {
            AppTrace.d(TAG, "onDeviceConnectSuccess skip for not visible " + event.address);
            return;
        }
        //关闭loading
        dismissLoadingDialog();

        //提示连接失败
        showConnectFailDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnecting(BleConnectingEvent event) {
        AppTrace.d(TAG, "onDeviceConnecting");
        if(event == null || event.address == null) {
            return;
        }
        if(isFragmentVisible()) {
            updateConnectMessage(String.format("%s%s", getString(R.string.loading_dialog_connect_device), BluetoothManager.getDisplayDeviceName(event.name)), BluetoothLoadingDialog.TYPE_CONNECT);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectSuccess(BleConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceConnectSuccess" + event + isFragmentVisible());
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();
        if(!isFragmentVisible()) {
            AppTrace.d(TAG, "onDeviceConnectSuccess skip for not visible " + event.address);
            return;
        }
        if(event != null) {
            String address = event.address;
            postConnected(address, Constants.DEVICE_STATUS_CONNECT);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectTimeout(BleConnectTimeoutEvent event) {
        AppTrace.d(TAG, "onDeviceConnectTimeout");
        if(event == null || event.address == null) {
            return;
        }
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();
        if(!isFragmentVisible()) {
            AppTrace.d(TAG, "onConnectTimeOut skip for not visible " + event.address);
            return;
        }
        if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            dismissLoadingDialog();
            if(getContext() != null) {
                ToastUtil.s(getContext().getString(R.string.info_ble_connect_timeout));
            }
        } else {
            AppTrace.d(TAG, "onConnectTimeOut skip for not isForegroundActivity " + event.address);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnecting(BleDisConnectingEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnecting");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnectSuccess(BleDisConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnectSuccess");
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanDevice(ScanDeviceEvent event) {
        AppTrace.d(TAG, "onScanDevice");
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanning(ScanningEvent event) {
        AppTrace.d(TAG, "onScanning");
        startAnim();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanStop(ScanStopEvent event) {
        AppTrace.d(TAG, "onScanStop");
        stopAnim();

        if(isFragmentVisible()) {
            if(_dataList == null || _dataList.isEmpty()) {
                showNotFoundDialog();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanTimeout(ScanTimeoutEvent event) {
        AppTrace.d(TAG, "onScanTimeout");
        stopAnim();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveDevice(RemoveDeviceEvent event) {
        _dataList = DeviceManager.getBindDeviceList();
        mAdapter.notifyDataSetChanged();
    }
}