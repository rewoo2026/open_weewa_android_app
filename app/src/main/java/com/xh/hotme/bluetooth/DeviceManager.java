package com.xh.hotme.bluetooth;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.BindDeviceResultBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.event.BleConnectFailEvent;
import com.xh.hotme.bluetooth.event.BleConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.BleConnectTimeoutEvent;
import com.xh.hotme.bluetooth.event.BleConnectingEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectingEvent;
import com.xh.hotme.bluetooth.event.RefreshBindDeviceEvent;
import com.xh.hotme.bluetooth.event.ScanDeviceEvent;
import com.xh.hotme.bluetooth.event.ScanStopEvent;
import com.xh.hotme.bluetooth.event.ScanTimeoutEvent;
import com.xh.hotme.bluetooth.event.ScanningEvent;
import com.xh.hotme.bluetooth.event.SearchDeviceEvent;
import com.xh.hotme.bluetooth.listener.IBleConnectListener;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeviceManager {

    private final static String TAG = "DeviceManager";
    private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

    final IBleScanNotifyListener _scanNotifyListener;
    final IBleConnectListener _connectNotifyListener;

    private static CopyOnWriteArrayList<BindDeviceBean> _bindDeviceList = new CopyOnWriteArrayList<>();

    public static BindDeviceResultBean _bindDeviceResult;
    public static BindDeviceBean _bindDevice;

    Device _connectDevice;


    long scanStartTime = 0;
    long scanEndTime = 0;

    private int pageIndex = 0;
    boolean _isRequest = false;

    private static final class HolderClass {
        private static final DeviceManager INSTANCE = new DeviceManager();
    }

    public static DeviceManager getImpl() {
        return HolderClass.INSTANCE;
    }

    public DeviceManager() {
        _scanNotifyListener = new IBleScanNotifyListener() {
            @Override
            public void onScanning(boolean isBackground) {
                AppTrace.d(TAG, "onScanning");
                scanEndTime = 0;
                scanStartTime = System.currentTimeMillis();
                EventBus.getDefault().post(new ScanningEvent());
            }

            @Override
            public void onLeScan(Device device) {
                boolean searched = false;
                for(BindDeviceBean blue : _bindDeviceList) {
                    // 改进设备名称匹配逻辑，处理列表中的名称是扫描到名称的后缀或相反的情况
                    if (!TextUtils.isEmpty(blue.getDeviceName()) && !TextUtils.isEmpty(device.getName())) {
                        // 检查是否完全匹配，或者一个是另一个的后缀（考虑可能的前缀差异如"weewa_"）
                        if (blue.getDeviceName().equalsIgnoreCase(device.getName()) || 
                            device.getName().toLowerCase().endsWith(blue.getDeviceName().toLowerCase()) ||
                            blue.getDeviceName().toLowerCase().endsWith(device.getName().toLowerCase())) {
                            // fix 蓝牙地址 - 使用设备名称进行匹配，并将扫描到的蓝牙地址赋值给绑定设备
                            if(blue.blueDevice == null) {
                                blue.blueDevice = device.clone();
                            }
                            // 将扫描到的蓝牙地址赋值到绑定设备列表中
                            if(!TextUtils.isEmpty(device.getAddress())) {
                                blue.setBluetoothUuid(device.getAddress());
                            }
                            blue.setStatus(Constants.DEVICE_STATUS_ONLINE);
                            searched = true;
                            break;
                        }
                    }
                }
                if(searched) {
                    List<BindDeviceBean> list = new ArrayList<>();
                    list.addAll(_bindDeviceList);
                    Collections.sort(list, new Comparator<BindDeviceBean>() {
                        @Override
                        public int compare(BindDeviceBean chain1, BindDeviceBean chain2) {
                            int orderO = chain1.getStatus();
                            int orderT = chain2.getStatus();
                            if (orderO > orderT) {
                                return -1;
                            } else if (orderO < orderT) {
                                return 1;
                            }
                            return 0;
                        }

                    });
                    _bindDeviceList.clear();
                    _bindDeviceList.addAll(list);

                    EventBus.getDefault().post(new ScanDeviceEvent(device));
                }
            }

            @Override
            public void onTimeout() {
                AppTrace.d("扫描超时");
                scanEndTime = System.currentTimeMillis();
                EventBus.getDefault().post(new ScanTimeoutEvent());
            }

            @Override
            public void onScanEnd() {
                AppTrace.d("扫描结束");
                scanEndTime = System.currentTimeMillis();
                EventBus.getDefault().post(new ScanStopEvent());
            }
        };

        _connectNotifyListener = new IBleConnectListener() {
            @Override
            public void onConnecting(Device device) {
                AppTrace.d(TAG, "onConnectStart....");
                _connectDevice = device.clone();

                EventBus.getDefault().post(new BleConnectingEvent(device.address, device.name));
            }

            @Override
            public void onConnectSuccess(Device device, int status) {  //
                AppTrace.d(TAG, "onConnectSuccess status=" + status);
            }

            @Override
            public void onConnectFailure(String address, String error) {
                if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.address) && _connectDevice.address.equals(address)) {
                    _connectDevice = null;
                    EventBus.getDefault().post(new BleConnectFailEvent(address));
                }
            }

            @Override
            public void onDisConnecting(String address) {
                AppTrace.d(TAG, "onDisConnecting=" + address);
                if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.address) && _connectDevice.address.equals(address)) {
                    EventBus.getDefault().post(new BleDisConnectingEvent(address));
                }
            }

            @Override
            public void onDisConnectSuccess(String address, int status) {
                AppTrace.d(TAG, "onDisConnectSuccess=" + address);
                if(_connectDevice == null) {
                    AppTrace.d(TAG, "onDisConnectSuccess skip for null connect device");
                    return;
                }
                if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
                    for(BindDeviceBean blue : _bindDeviceList) {
                        if(!TextUtils.isEmpty(blue.getBluetoothUuid()) && blue.getBluetoothUuid().equalsIgnoreCase(address)) {
                            blue.setStatus(Constants.DEVICE_STATUS_ONLINE);
                        }
                    }
                }
                if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.address) && _connectDevice.address.equals(address)) {
                    _connectDevice = null;
                    EventBus.getDefault().post(new BleDisConnectSuccessEvent(address));
                }
            }

            @Override
            public void onConnectTimeOut(String address) {
                AppTrace.d(TAG, "onConnectTimeOut " + address);
                if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.address) && _connectDevice.address.equals(address)) {
                    _connectDevice = null;
                    EventBus.getDefault().post(new BleConnectTimeoutEvent(address));
                }

            }

            @Override
            public void onServiceDiscoverySucceed(String address, int status) {
                AppTrace.d(TAG, "onServiceDiscoverySucceed " + address);

                if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
                    for(BindDeviceBean blue : _bindDeviceList) {
                        if(!TextUtils.isEmpty(blue.getBluetoothUuid()) && blue.getBluetoothUuid().equalsIgnoreCase(address)) {
                            blue.setStatus(Constants.DEVICE_STATUS_CONNECT);
                        }
                    }
                }

                if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.address) && _connectDevice.address.equals(address)) {
                    _connectDevice.status = Constants.DEVICE_STATUS_CONNECT;
                    EventBus.getDefault().post(new BleConnectSuccessEvent(address));
                }
            }

            @Override
            public void onServiceDiscoveryFailed(String address, String message) {

            }
        };

        loadBindDeviceData();

        BluetoothHandle.addScanNotifyListener(_scanNotifyListener);
        BluetoothHandle.addConnectNotifyListener(_connectNotifyListener);
    }

    public Device getBindConnectedDevice() {

        return _connectDevice;
    }

    public void setBindConnectedDevice(Device device) {
        _connectDevice = device;
    }

    public void setConnectedBindDevice(BindDeviceBean bindDevice){
        _bindDevice = bindDevice;
    }

    public BindDeviceBean getBindDevice(){
        if(_bindDevice != null){
            return _bindDevice;
        }

        if(_connectDevice != null && !TextUtils.isEmpty(_connectDevice.getAddress())){
            if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
                for (BindDeviceBean bindDevice : _bindDeviceList ) {
                    if(bindDevice.getBluetoothUuid().equalsIgnoreCase(_connectDevice.getAddress())){
                        return bindDevice;
                    }
                }
            }
        }

        return null;
    }

    public static List<BindDeviceBean> getBindDeviceList() {
        return _bindDeviceList;
    }

    public void clearCacheBindDeviceList() {
        if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
            _bindDeviceList.clear();
        }
    }

    public void loadBindDeviceData() {
        try {
            UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
            if(userInfoBean == null) {
                _bindDeviceList.clear();
                return;
            }
            String deviceString = AppFileUtil.getBindDeviceInfo(userInfoBean.phone);

            _bindDeviceResult = new Gson().fromJson(deviceString, new TypeToken<BindDeviceResultBean>() {
            }.getType());

            if(_bindDeviceResult != null) {
                List<BindDeviceBean> bleDeviceList = _bindDeviceResult.list;

                if(bleDeviceList != null && !bleDeviceList.isEmpty()) {
                    for(BindDeviceBean device : bleDeviceList) {
                        device.setStatus(Constants.DEVICE_STATUS_OFFLINE);
                    }
                }

                if(_bindDeviceList == null) {
                    _bindDeviceList = new CopyOnWriteArrayList<>();
                }
                _bindDeviceList.clear();
                _bindDeviceList.addAll(bleDeviceList);
            }
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    private void syncLocalBindDeviceData() {
        if(_bindDeviceResult != null) {
            String deviceString = new Gson().toJson(_bindDeviceResult);
            UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
            AppFileUtil.saveBindDeviceInfo(userInfoBean.phone, deviceString);
        }
    }

    public void updateBindDeviceStatus(Device connectDevice) {
        if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
            BindDeviceBean cloneDevice = null;
            for(BindDeviceBean device : _bindDeviceList) {
                if(device != null && !TextUtils.isEmpty(device.getBluetoothUuid()) && device.getBluetoothUuid().equalsIgnoreCase(connectDevice.getAddress())) {
                    device.setStatus(connectDevice.status);
                    cloneDevice = device.clone();
                    break;
                }
            }
            if(cloneDevice != null) {
                syncLocalBindDeviceData();
            }
        }
    }

    public void removeBindDeviceData(String bluetoothUuid) {
        if(TextUtils.isEmpty(bluetoothUuid)) {
            return;
        }
        if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
            for(int i = 0; i < _bindDeviceList.size(); i++) {
                BindDeviceBean bindDevice = _bindDeviceList.get(i);
                if(bindDevice != null && !TextUtils.isEmpty(bindDevice.getBluetoothUuid()) && bindDevice.getBluetoothUuid().equalsIgnoreCase(bluetoothUuid)) {
                    _bindDeviceList.remove(bindDevice);
                    break;
                }
            }
        }

        //保存本地记录
        if(_bindDeviceResult != null) {
            List<BindDeviceBean> bleDeviceList = _bindDeviceResult.list;
            for(int i = 0; i < bleDeviceList.size(); i++) {
                BindDeviceBean bindDevice = bleDeviceList.get(i);
                if(bindDevice != null && !TextUtils.isEmpty(bindDevice.getBluetoothUuid()) && bindDevice.getBluetoothUuid().equalsIgnoreCase(bluetoothUuid)) {
                    bleDeviceList.remove(bindDevice);
                    break;
                }
            }

            String deviceString = new Gson().toJson(_bindDeviceResult);
            UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
            AppFileUtil.saveBindDeviceInfo(userInfoBean.phone, deviceString);
        }
    }

    public void removeBindDeviceData(Device device) {
        if(device == null) {
            return;
        }
        String address = device.getAddress();

        removeBindDeviceData(address);
    }

    public void searchBluetoothDevice(boolean isBackground) {
        if(BluetoothManager.getImpl().isScanning()) {
            AppTrace.d(TAG, "isScanning, waiting...");
            return;
        }
        BluetoothManager.getImpl().startScan(isBackground);
    }

    public void onRefreshList(boolean isBackground) {
        AppTrace.d(TAG, "onRefreshList: " + isBackground);
        pageIndex = 0;
        getDeviceList(isBackground);
    }

    public void clearDeviceList() {
        if(_bindDeviceList != null) {
            _bindDeviceList.clear();
        }
    }

    public void getDeviceList(boolean isBackGround) {
        UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
        if(userInfoBean == null) {
            AppTrace.e(TAG, "not login");
            if(_bindDeviceList != null) {
                _bindDeviceList.clear();
            }
            return;
        }

        if(_isRequest) {
            return;
        }
        pageIndex++;
        _isRequest = true;
        ApiUtil.getDeviceList(pageIndex, Constants.BIND_DEVICE_PAGE_NUMBER, new IGenericApiCallback<BindDeviceResultBean>() {
            @Override
            public void onSuccess(BindDeviceResultBean data) {
                AppTrace.d(TAG, "deviceList: " + GsonUtils.GsonString(data));
                _isRequest = false;
                if(pageIndex == 1) {
                    if(_bindDeviceList != null) {
                        _bindDeviceList.clear();
                    }
                }
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (data != null && data.list != null && !data.list.isEmpty()) {
                        _bindDeviceList.addAll(data.list);
                    }

                    loadConnectDevice();
                } else {
                    if (data != null && data.list != null && !data.list.isEmpty()) {
                        List<? extends BindDeviceBean> list = data.list;

                        // 需要验证
                        // 在远程模式下，检查每个设备是否有蓝牙地址
                        for (BindDeviceBean device : list) {
                            if (device != null && (device.getBluetoothUuid() == null || TextUtils.isEmpty(device.getBluetoothUuid()))) {
                                AppTrace.d(TAG, "Remote device missing bluetooth address: " + device.getDeviceName() + ", ID: " + device.getDeviceId());
                                // 在bluetoothUuid为空或null时，将设备ID临时赋值给bluetoothUuid
                                if (!TextUtils.isEmpty(device.getDeviceId())) {
                                    AppTrace.d(TAG, "Temporarily assigning device ID to bluetoothUuid: " + device.getDeviceId());
                                    device.setBluetoothUuid(device.getDeviceId());
                                }
                            }
                        }

                        Collections.sort(list, new Comparator<BindDeviceBean>() {

                            @Override
                            public int compare(BindDeviceBean chain1, BindDeviceBean chain2) {
                                int orderO = chain1.getOnlineStatus();
                                int orderT = chain2.getOnlineStatus();
                                if (orderO > orderT) {
                                    return -1;
                                } else if (orderO < orderT) {
                                    return 1;
                                }
                                return 0;
                            }

                        });

                        _bindDeviceList.addAll(list);
                    }
                }

                EventBus.getDefault().post(new RefreshBindDeviceEvent());

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    EventBus.getDefault().post(new SearchDeviceEvent(isBackGround));
                }
            }

            @Override
            public void onFail(String code, String msg) {
                AppTrace.d(TAG, "fail: code =" + code + ", msg=" + msg);
                _isRequest = false;
                MainHandler.runOnUiThread(() -> {
                    try {
                        EventBus.getDefault().post(new RefreshBindDeviceEvent());
                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFinish() {
                _isRequest = false;
                new Handler(Looper.getMainLooper()).post(() -> {
//                        _refreshLayout.setRefreshing(false);
                });
            }
        });
    }

    public void loadConnectDevice() {
        AppTrace.d(TAG, "loadConnectDevice...");
        if(_bindDeviceList != null && !_bindDeviceList.isEmpty()) {
            AppTrace.d(TAG, "update connected device status");
            List<Device> scanDeviceList = null;
            if (BluetoothManager.getImpl() != null) {
                scanDeviceList = BluetoothManager.getImpl().getBleDeviceList();
            }

            for(int i = 0; i < _bindDeviceList.size(); i++) {
                BindDeviceBean bindDeviceBean = _bindDeviceList.get(i);
                if(bindDeviceBean != null && !TextUtils.isEmpty(bindDeviceBean.getBluetoothUuid()) && _connectDevice != null && bindDeviceBean.getBluetoothUuid().equals(_connectDevice.getAddress())) {
                    bindDeviceBean.setStatus(Constants.DEVICE_STATUS_CONNECT);
                    AppTrace.d(TAG, "update connected device status: " + Constants.DEVICE_STATUS_CONNECT);
                    break;
                }
                if(scanDeviceList != null && !scanDeviceList.isEmpty()) {
                    for(int j = 0; j < scanDeviceList.size(); j++) {
                        Device blue = scanDeviceList.get(j);
                        if(blue != null && !TextUtils.isEmpty(blue.getAddress()) && bindDeviceBean != null && blue.getAddress().equals(bindDeviceBean.getBluetoothUuid())) {
                            bindDeviceBean.setStatus(Constants.DEVICE_STATUS_ONLINE);
                            AppTrace.d(TAG, "update connected device status: " + Constants.DEVICE_STATUS_ONLINE);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void saveBindDeviceData() {
        if(_bindDeviceResult != null) {
            String deviceString = new Gson().toJson(_bindDeviceResult);
            UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
            AppFileUtil.saveBindDeviceInfo(userInfoBean.phone, deviceString);
        }
    }

    public static void updateDeviceName(String address, String deviceName) {
        if(_bindDeviceList == null) {
            return;
        }

        for(BindDeviceBean device : _bindDeviceList) {
            if(device != null && !TextUtils.isEmpty(device.getBluetoothUuid()) && device.getBluetoothUuid().equalsIgnoreCase(address)) {
                device.setDeviceName(deviceName);

                if(device.blueDevice != null) {
                    device.blueDevice.setName(deviceName);
                }
                break;
            }
        }
    }

    public BindDeviceBean getConnectedBindDevice() {
        if(_connectDevice != null && _bindDeviceList != null) {
            for(BindDeviceBean device : _bindDeviceList) {
                if(device != null && !TextUtils.isEmpty(device.getBluetoothUuid()) && device.getBluetoothUuid().equalsIgnoreCase(_connectDevice.getAddress())) {
                    return device;
                }
            }

        }
        return null;
    }
}
