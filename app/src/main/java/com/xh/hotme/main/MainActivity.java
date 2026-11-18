package com.xh.hotme.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.umeng.socialize.UMShareAPI;
import com.weewa.lib.WeewaLib;
import com.weewa.lib.WeewaLibService;
import com.weewa.lib.WeewaMode;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.VersionResultBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.bean.WifiListRequestBean;
import com.xh.hotme.bluetooth.bean.WifiSetupRequestBean;
import com.xh.hotme.bluetooth.event.BleConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.DeviceOnlineEvent;
import com.xh.hotme.bluetooth.event.DeviceOnlineSwitchEvent;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.bluetooth.event.RemoteSwitchEvent;
import com.xh.hotme.bluetooth.event.SearchDeviceEvent;
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.camera.CameraOnlineThread;
import com.xh.hotme.camera.softap.NetworkConnectManager;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.databinding.ActivityMainBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.rabbitmq.RabbitMQ;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothDialog;
import com.xh.hotme.widget.MyRadioGroup;
import com.xh.hotme.widget.VersionDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public class MainActivity extends BaseActivity<ActivityMainBinding> implements MyRadioGroup.OnCheckedChangeListener, IBleScanRequestListener, EasyPermissions.PermissionCallbacks {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_SELECTED_TAB = "main_selected_tab";
    private static final String TAB_FRAGMENT_TAG_PREFIX = "main_tab_";

    private Map<Integer, BaseFragment<?>> _fragments;
    private Map<Integer, Class<?>> _fragmentClasses;
    private int _selectedTabId = -1;

    Device readyDevice = null;
    BindDeviceBean readyBindDevice;

    Handler mHandler = new Handler();

    public static final int REQUEST_CODE_WRITE_PERMISSION = 2003;
    public static final int REQUEST_CODE_INSTALL_PERMISSION = 2004;
    VersionDialog mVersionDialog;

    ServiceConnection _fileConnection;

    BluetoothDialog _bluetoothDialog;

    int _tabIndex;

    CameraOnlineThread _onlineThread;

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // BaseActivity invokes initView() from super.onCreate(). Read the tab
        // first so initialization can reuse FragmentManager's restored pages.
        if (savedInstanceState != null) {
            _selectedTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, -1);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, _selectedTabId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restored RadioButton states may refer to a protected tab that fell
        // back to Home because the login expired during recreation.
        syncTabSelection();
    }

    @Override
    protected void initView() {
        WeewaLib.Companion.setMode(WeewaMode.SLAVE1);
//        WeewaLib.Companion.shared().launch(this);
        WeewaLib.Companion.shared().launchAsService(this);
        ConnectLogic.getImpl();
        CameraMonitor.getImpl();

        Intent intent = new Intent(this, WeewaLibService.class);

        _onlineThread = new CameraOnlineThread(MainActivity.this);

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            if (_onlineThread != null) {
                _onlineThread.startThread();
            }
        }
        _fileConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AppTrace.d("WeewaLibService", "onServiceConnected");
                ConnectLogic.getImpl().setConnected(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                AppTrace.d("WeewaLibService", "onServiceDisconnected");
                ConnectLogic.getImpl().setConnected(false);
            }
        };
        bindService(intent, _fileConnection, BIND_AUTO_CREATE);

        _fragments = new HashMap<>();
        _fragmentClasses = new HashMap<>();
        _fragmentClasses.put(R.id.tab_home, HomeDeviceListRecordFragment.class);
        _fragmentClasses.put(R.id.tab_icloud, CloudFragment.class);
        _fragmentClasses.put(R.id.tab_live, HomeDeviceListLiveFragment.class);
        _fragmentClasses.put(R.id.tab_me, MeFragment.class);


        restoreTabFragments();
        int initialTab = _fragmentClasses.containsKey(_selectedTabId)
                ? _selectedTabId : R.id.tab_home;
        if ((initialTab == R.id.tab_me || initialTab == R.id.tab_live)
                && !LoginManager.isSignedIn()) {
            initialTab = R.id.tab_home;
        }
        showTab(initialTab);
        syncTabSelection();

        mHandler = new Handler();

//        if (!PermissionsUtil.isLocServiceEnable(MainActivity.this)) {
//            PermissionsUtil.openLocationService(MainActivity.this);
//        }
//
//        boolean hasPermission = PermissionsUtil.hasBlePermission(MainActivity.this);
//        if (!hasPermission) {
//            PermissionsUtil.requestBlePermission(MainActivity.this);
//        }

        NetworkManager.getImpl().registerNetworkStatusChangeListener((newStatus, oldStatus) -> {
            if (!WifiManager.getImpl().isWifiConnected()) {
                EventBus.getDefault().post(new NetWorkForQEvent(NetWorkForQEvent.UNCONNECT));
            }
        }, false);

        NetworkConnectManager.getInstance().init(MainActivity.this);

        getVersion();
        checkSetMeal();

        LoginManager.checkUserLoginStatus(this);

        CameraManager.loadGlobalDeviceDefaultConfig();

        ApiUtil.getObsInfo(null);
    }

    @Override
    protected void initData() {
        RabbitMQ.Companion.launch();
        TaskManager.getImpl().onCreate();
    }

    @Override
    public void onCheckedChanged(MyRadioGroup group, int checkedId) {
        if (!_fragmentClasses.containsKey(checkedId)) {
            return;
        }
        if (checkedId == R.id.tab_me) {
            if (!LoginManager.isSignedIn()) {
                MobileLoginActivity.startActivityByRequestCode(MainActivity.this, Constants.REQUEST_CODE_LOGIN_TAB_ME);
                return;
            }

            if (_fragments.get(R.id.tab_me) != null) {
                MeFragment meFragment = (MeFragment) _fragments.get(R.id.tab_me);
                if (meFragment != null) {
                    meFragment.notifyAdapter();
                }
            }

        } else if (checkedId == R.id.tab_live) {
            if (!LoginManager.isSignedIn()) {
                MobileLoginActivity.startActivityByRequestCode(MainActivity.this, Constants.REQUEST_CODE_LOGIN_TAB_LIVE);
                return;
            }
            if (_fragments.get(R.id.tab_home) instanceof HomeDeviceListRecordFragment) {
                HomeDeviceListRecordFragment homeFragment = (HomeDeviceListRecordFragment) _fragments.get(R.id.tab_home);
                boolean isShowDialog = homeFragment._dataList.isEmpty();
                if (isShowDialog) {
                    showHintDialog(
                            getString(R.string.dialog_camera_add_title),
                            getString(R.string.dialog_camera_add_hint),
                            getString(R.string.dialog_camera_add_bt),
                            new ClickGuard.GuardedOnClickListener() {
                                @Override
                                public boolean onClicked() {
                                    viewBinding.tabGroup.check(R.id.tab_home);
                                    return true;
                                }
                            }
                    );
                    return;
                }

            }
            //检查热点
            if (_fragments.get(R.id.tab_live) != null) {
                HomeDeviceListLiveFragment liveListFragment = (HomeDeviceListLiveFragment) _fragments.get(R.id.tab_live);
                if (liveListFragment != null) {
                    liveListFragment.changeWifi();
                }
            }

        }

        showTab(checkedId);
    }

    private void restoreTabFragments() {
        FragmentTransaction cleanup = getSupportFragmentManager().beginTransaction();
        boolean hasDuplicates = false;
        // Match by class as well as container so untagged pages saved by older
        // versions are reused too. Remove duplicates left by the previous bug.
        for (Fragment restored : getSupportFragmentManager().getFragments()) {
            if (restored.getId() != R.id.container) {
                continue;
            }
            for (Map.Entry<Integer, Class<?>> entry : _fragmentClasses.entrySet()) {
                if (restored.getClass() == entry.getValue()) {
                    int tabId = entry.getKey();
                    if (_selectedTabId == -1 && !restored.isHidden()) {
                        _selectedTabId = tabId;
                    }
                    if (_fragments.containsKey(tabId)) {
                        cleanup.remove(restored);
                        hasDuplicates = true;
                    } else {
                        BaseFragment<?> fragment = (BaseFragment<?>) restored;
                        fragment.bleScanRequestListener = this;
                        _fragments.put(tabId, fragment);
                    }
                    break;
                }
            }
        }
        if (hasDuplicates) {
            cleanup.commitNowAllowingStateLoss();
        }
    }

    private void syncTabSelection() {
        viewBinding.tabGroup.setOnCheckedChangeListener(null);
        viewBinding.tabGroup.check(_selectedTabId);
        viewBinding.tabGroup.setOnCheckedChangeListener(this);
    }

    private void showTab(int checkedId) {
        // lazy create fragment
        BaseFragment<?> fragment = _fragments.get(checkedId);
        if (fragment == null) {
            try {
                Class<?> klass = _fragmentClasses.get(checkedId);
                Method m = klass.getDeclaredMethod("newInstance");
                fragment = (BaseFragment<?>) m.invoke(klass);
                fragment.bleScanRequestListener = this;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (fragment != null) {
                _fragments.put(checkedId, fragment);
            }
        }

        if (fragment == null) {
            return;
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!fragment.isAdded()) {
            fragmentTransaction.add(R.id.container, fragment, TAB_FRAGMENT_TAG_PREFIX + checkedId);
        }
        // Reconcile every page, including ones restored by the system. Hiding
        // only an in-memory previous page leaves restored views overlapping.
        for (Fragment other : getSupportFragmentManager().getFragments()) {
            if (other.getId() == R.id.container && other != fragment) {
                fragmentTransaction.hide(other);
            }
        }
        fragmentTransaction.show(fragment).commitNowAllowingStateLoss();
        _selectedTabId = checkedId;
        _tabIndex = checkedId == R.id.tab_me ? 3 : checkedId == R.id.tab_live ? 2
                : checkedId == R.id.tab_icloud ? 1 : 0;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_LOGIN_TAB_ME) {
            if (resultCode == 1) {
                viewBinding.tabGroup.check(R.id.tab_me);
            }
        } else if (requestCode == Constants.REQUEST_CODE_LOGIN_TAB_LIVE) {
            if (resultCode == 1) {
                viewBinding.tabGroup.check(R.id.tab_live);
            }
        } else if (requestCode == Constants.REQUEST_CODE_LOGIN_HOME_DEVICE) {
            if (resultCode == 1) {
                if (readyDevice != null) {
                    onBluetoothDeviceClick(readyDevice, 0, Constants.DEVICE_ACTION_CLICK);
                }
            }
        } else if (requestCode == REQUEST_CODE_WRITE_PERMISSION) {

            String versionContent = AppFileUtil.loadStringFromFile(AppFileUtil.VERSION_FILE);
            if (!TextUtils.isEmpty(versionContent)) {
                VersionResultBean versionResultBean = GsonUtils.GsonToBean(versionContent, VersionResultBean.class);
                if (versionResultBean != null) {
                    int curCode = BaseAppUtil.getAppVersionCode();
                    int latestCode = Integer.parseInt(versionResultBean.getVersionCode());
                    if (latestCode > curCode) {
                        if (EasyPermissions.hasPermissions(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            showVersionDialog(versionResultBean);
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_INSTALL_PERMISSION) {
            if (mVersionDialog != null) {
                boolean isGranted = getApplication().getPackageManager().canRequestPackageInstalls();
                if(isGranted){
                    mVersionDialog.installApp();
                }
            }
        }
    }

    @Override
    protected void onBluetoothMayBeOpened() {
        if (BluetoothManager.getImpl().enableBlueTooth()) {
            if (BluetoothManager.getImpl() != null) {
                BluetoothManager.getImpl().startScan(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                if (_bluetoothDialog != null && _bluetoothDialog.isShowing()) {
                    return;
                }
                _bluetoothDialog = new BluetoothDialog(MainActivity.this);
                _bluetoothDialog.setOnClickListener(new DialogInterface.OnClickListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            requestOpenBluetooth();
                        }

                        dialog.dismiss();
                    }
                });
                _bluetoothDialog.show();
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        TaskManager.getImpl().onDestroy();

        if(BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth()  && PermissionsUtil.hasBlePermission(MainActivity.this)) {
            BluetoothManager.getImpl().stopScan();
        }

        NetworkConnectManager.getInstance().unregisterReceivers(this);

        NetworkManager.getImpl().unregisterReceivers(this);
        if (_fileConnection != null) {
            unbindService(_fileConnection);
        }

        if(_onlineThread != null){
            _onlineThread.stopThread();
        }
        _onlineThread = null;
    }

    @Override
    public void onScanStart(boolean isBackground) {
        AppTrace.d(TAG, "onScanStart");
        if (BluetoothManager.getImpl() != null) {
            BluetoothManager.getImpl().startScan(isBackground);
        }
    }

    @Override
    public void onScan(Device device, int position) {

    }

    @Override
    public void onScanStop(final int size) {
        if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth()  && PermissionsUtil.hasBlePermission(MainActivity.this)) {
            if (BluetoothManager.getImpl() != null) {
                BluetoothManager.getImpl().stopScan();
            }
        }
    }


    private void showError() {
        showHintDialog(
                getString(R.string.dialog_camera_not_found),
                getString(R.string.dialog_camera_reboot),
                getString(R.string.dialog_camera_found_ok),
                null
        );
    }

    @Override
    public void onBluetoothDeviceClick(Device device, int position, int action) {
        if (device == null) {
            if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth()  && PermissionsUtil.hasBlePermission(MainActivity.this)) {
                if (BluetoothManager.getImpl() != null) {
                    BluetoothManager.getImpl().stopScan();
                }
                BluetoothManager.getImpl().startScan(true);
            }
            showError();
            return;
        }
        Log.d(TAG, "connect to =");
        if (action == Constants.DEVICE_ACTION_OPEN) {
            openDevice(device);
        } else if (action == Constants.DEVICE_ACTION_DISCONNECT) {
            disconnectDevice(device);
        } else if (action == Constants.DEVICE_ACTION_REMOVE) {
            if (DeviceManager.getImpl() != null) {
                DeviceManager.getImpl().removeBindDeviceData(device);
            }
        } else {

            if (!LoginManager.isSignedIn()) {
                readyDevice = device;
                MobileLoginActivity.startActivityByRequestCode(MainActivity.this, Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                return;
            }
            readyDevice = null;
            if (BluetoothManager.getImpl() != null) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(MainActivity.this, false);
                    return;
                }
                BluetoothManager.getImpl().connectToBle(device);
            }
        }
    }


    @Override
    public void onBluetoothDeviceClick(BindDeviceBean device, int position, int action) {
        if (device == null) {
            if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().enableBlueTooth()  && PermissionsUtil.hasBlePermission(MainActivity.this)) {
                if (BluetoothManager.getImpl() != null && BluetoothManager.getImpl().isScanning()) {
                    BluetoothManager.getImpl().stopScan();
                }
                BluetoothManager.getImpl().startScan(true);
            }
            showError();
            return;
        }
        Log.d(TAG, "connect to =");

        if (!LoginManager.isSignedIn()) {
            readyBindDevice = device;
            MobileLoginActivity.startActivityByRequestCode(MainActivity.this, Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
            return;
        }
        readyBindDevice = null;

        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo != null){
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
        if (BluetoothManager.getImpl() != null) {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiListRequestBean());
            if (!isOk) {
                ToastUtil.s(getString(R.string.device_offline));
            }
        }
    }

    @Override
    public void onSetupWifi(String ssid, String password) {
        if (BluetoothManager.getImpl() != null) {
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WifiSetupRequestBean(ssid, password));
            if (!isOk) {
                ToastUtil.s(getString(R.string.device_offline));
            }
        }
    }

    @Override
    public void onRemove(Device device, int position) {
        if (DeviceManager.getImpl() != null) {
            DeviceManager.getImpl().removeBindDeviceData(device);
        }
    }

    private void openDevice(Device device) {
        AppTrace.d(TAG, "openDevice =");
        if (!LoginManager.isSignedIn()) {
            readyDevice = device;
            MobileLoginActivity.startActivityByRequestCode(MainActivity.this, Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
            return;
        }
        readyDevice = null;
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (BluetoothManager.getImpl() != null) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothManager.getImpl().openBluetooth(MainActivity.this, false);
                    return;
                }
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
                if (!isOk) {
                    BluetoothManager.getImpl().connectToBle(device);
                }
            }
        } else {
            if (DeviceManager.getImpl() != null) {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
                if (!isOk) {
                    BluetoothManager.getImpl().connectToBle(device);
                }
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

    private void getVersion() {

        String versionUrl = SdkApi.getAppVersion(Constants.getAppType(), BaseAppUtil.getAppVersionCode());
        OkHttpUtil.get(versionUrl, new OkHttpCallbackDecode<VersionResultBean>() {
            @Override
            public void onDataSuccess(VersionResultBean data) {
                try {
                    if (data != null) {
                        AppFileUtil.saveJson(GsonUtils.GsonString(data), AppFileUtil.VERSION_FILE);
                        int curCode = BaseAppUtil.getAppVersionCode();
                        int latestCode = Integer.parseInt(data.getVersionCode());
                        if (latestCode > curCode) {
                            if (EasyPermissions.hasPermissions(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                showVersionDialog(data);
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PERMISSION);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String code, String msg) {
                AppTrace.d(TAG, "error: code=" + code + ", msg=" + msg);
            }
        });
    }


    private void showVersionDialog(final VersionResultBean version) {

        //非强制升级的情况下，每天限制只弹窗显示一次
        if (version.getForceUpdateFlag() == 0) {
            if (SharedPrefs.getImpl().shownVersionToday()) {
                return;
            }
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (mVersionDialog != null) {
                    mVersionDialog.dismiss();
                    mVersionDialog = null;
                }
                mVersionDialog = new VersionDialog();

                //强制更新
                mVersionDialog.showDialog(MainActivity.this, version, new VersionDialog.ConfirmDialogListener() {
                    @Override
                    public void ok() {
                        SharedPrefs.getImpl().setShowVersionToday(System.currentTimeMillis());
                    }

                    @Override
                    public void cancel() {
                        SharedPrefs.getImpl().setShowVersionToday(System.currentTimeMillis());
                    }

                    @Override
                    public void dismiss() {

                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //将请求结果传递EasyPermission库处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
//        com.xh.hotme.lay.utils.ToastUtil.showToastShort(MainActivity.this, "用户授权成功");
        AppTrace.d(TAG, "onPermissionsGranted");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            showConfirmDialog(
                    getString(R.string.permission_deny_title),
                    getString(R.string.permission_deny_content),
                    getString(R.string.setting),
                    getString(R.string.cancel),
                    new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            //跳转应用消息，间接打开应用权限设置-效率高
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                            intent.setData(uri);
                            MainActivity.this.startActivity(intent);
                            return true;
                        }
                    },
                    null
            );
        } else {
            AppTrace.d(TAG, "onPermissionsDenied");
            //跳转应用消息，间接打开应用权限设置-效率高
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
            intent.setData(uri);
            MainActivity.this.startActivity(intent);
        }
    }

    public boolean isLiveFragmentCheck() {
        return _tabIndex == 2;
    }

    public boolean isRecordFragmentCheck() {
        return _tabIndex == 0;
    }

    public boolean isCloudFragmentCheck() {
        return _tabIndex == 1;
    }


    public void checkSetMeal() {
        SetMealCheckManager.checkSetMeal(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReload(LoginEvent event) {
        AppTrace.d(TAG, "onLoginEvent");
        checkSetMeal();

        DeviceManager.getImpl().onRefreshList(false);

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSearchDevice(SearchDeviceEvent event) {
        AppTrace.d(TAG, "SearchDeviceEvent");
        try {

            if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().size() == 0){
                AppTrace.d(TAG, "skip search because without no bind device");
                return;
            }

            if (!PermissionsUtil.isLocServiceEnable(MainActivity.this)) {
                PermissionsUtil.openLocationService(MainActivity.this);
                return;
            }

            boolean hasPermission = PermissionsUtil.hasBlePermission(MainActivity.this);
            if (!hasPermission) {
                checkBluetoothPermission();
            } else {
                if(BluetoothManager.getImpl().isScanning()) {
                    BluetoothManager.getImpl().stopScan();
                }
                DeviceManager.getImpl().searchBluetoothDevice(event.isBackGround);
            }

        } catch(Throwable e) {
            e.printStackTrace();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelSwitchEvent(RemoteSwitchEvent event) {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
            if(_onlineThread != null){
                _onlineThread.stopThread();
            }
        } else {
            if(_onlineThread != null){
                _onlineThread.startThread();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        AppTrace.d(TAG, "onDeviceOnlineEvent isOnline = " +event.isOnline);
        if(event.isOnline != CameraMonitor.getImpl().isOnline()){
            EventBus.getDefault().post(new DeviceOnlineSwitchEvent(event.deviceId, event.isOnline));
        }
        CameraMonitor.getImpl().setOnline(event.isOnline);
    }
}
