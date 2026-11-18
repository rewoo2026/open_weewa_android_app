package com.xh.hotme.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.view.accessibility.AccessibilityViewCommand;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.account.CameraTokenManager;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.listener.IBleScanListener;
import com.xh.hotme.bluetooth.listener.IBluetoothDeviceAdapterCallback;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClsUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by GJK on 2018/11/9.
 */

public class BluetoothManager {
    private static final String TAG = BluetoothManager.class.getSimpleName();

    private int mMtu = 20;

    private BluetoothGatt mBluetoothGatt;
    private final BluetoothGattManager mBluetoothGattManager;
    private final BluetoothGattCallback mBluetoothGattCallback;

    private BluetoothGattService mBluetoothGattReadService;
    private BluetoothGattService mBluetoothGattWriteService;

    private BluetoothGattCharacteristic mReadCharacteristic;  //读特征
    private BluetoothGattCharacteristic mWriteCharacteristic; //写特征
    private boolean tokenNotificationsReady;

    IBleScanListener _scanListener;

    private static CopyOnWriteArrayList<Device> _scanDeviceList = new CopyOnWriteArrayList<>();

    public static final String weewa_prefix = "weewa";
    public static final String weewa_prefix_v2 = "w_";

    private static final Handler mHandler = new Handler();
    private boolean isScanning = false;  //是否正在扫描
    private boolean isBackgroundScanning = false;  //是否正在后台扫描

    private static Device _connectDevice;

    public final static long scanTime = 20000;
    public final static long RESP_TIMEOUT = 20000;

    public final static long RESP_TIMEOUT_SHORT = 10000;

    public final static long RESP_TIMEOUT_LIGHT = 5000;
    private static final long MAX_CONNECT_TIME = 10000;  //连接超时时间10s

    private static final class HolderClass {
        private static final BluetoothManager INSTANCE = new BluetoothManager();
    }

    //连接超时
    private final Runnable connectOutTimeRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(mBluetoothGatt == null) {
                Log.e(TAG, "connectOuttimeRunnable-->mBluetoothGatt == null");
                return;
            }
            //连接超时当作连接失败回调
            String deviceAddress = "";
            BluetoothDevice device = mBluetoothGatt.getDevice();
            if(device != null) {
                deviceAddress = device.getAddress();
            }

            mBluetoothGatt.disconnect();

            BluetoothHandle.onConnectTimeoutNotify(deviceAddress);
        }
    };

    //发现服务超时
    private final Runnable serviceDiscoverOutTimeRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(mBluetoothGatt == null) {
                Log.e(TAG, "connectOuttimeRunnable-->mBluetoothGatt == null");
                return;
            }
            //连接超时当作连接失败回调
            String deviceAddress = "";
            BluetoothDevice device = mBluetoothGatt.getDevice();
            if(device != null) {
                deviceAddress = device.getAddress();
            }

            mBluetoothGatt.disconnect();

            //发现服务超时当作连接失败回调
            BluetoothHandle.onConnectTimeoutNotify(deviceAddress);
        }
    };

    private final Runnable stopScanRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            isScanning = false;
            BluetoothHandle.onScanTimeout();

            //scanTime之后还没有扫描到设备，就停止扫描。
            stopDiscoveryDevice();
        }
    };

    //////////////////////////////////////  停止扫描  /////////////////////////////////////////////

    /**
     * 停止扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopDiscoveryDevice() {
        mHandler.removeCallbacks(stopScanRunnable);

        if(mBluetoothGattManager == null) {
            AppTrace.t(TAG, "stopDiscoveryDevice-->bluetooth4Adapter == null");
            return;
        }

        AppTrace.t(TAG, "停止扫描设备");
        mBluetoothGattManager.stopLeScan();
    }

    public static BluetoothManager getImpl() {
        return HolderClass.INSTANCE;
    }

    public BluetoothManager() {
        mBluetoothGattManager = new BluetoothGattManager();
        mBluetoothGattManager.setBluetoothDeviceAdapterCallback(new IBluetoothDeviceAdapterCallback() {
            @Override
            public void onLeScanStart() {
                AppTrace.t(TAG, "onLeScanStart");
                if(_scanListener != null) {
                    _scanListener.onScanStart(false);
                }
            }

            @Override
            public void onLeScan(Device device, int position) {
                AppTrace.t(TAG, "onLeScan position:" + position);

                if(_scanListener != null) {
                    _scanListener.onScan(device, position);
                }
            }

            @Override
            public void onLeScanStop(final int size) {
                AppTrace.t(TAG, "Scaned devices " + size);

                if(_scanListener != null) {
                    _scanListener.onScanStop(size);
                }
            }

            @Override
            public void onBluetoothDeviceClick(Device device, int position) {
                AppTrace.t(TAG, "onBluetoothDeviceClick name:" + device.getName() + " - " + position);

                if(_scanListener != null) {
                    _scanListener.onBluetoothDeviceClick(device, position);
                }
            }

            @Override
            public void onRemoveDevice(Device device, int position) {

            }

            @Override
            public void openDevice(Device device) {

            }

            @Override
            public void disconnectDevice(Device device) {

            }
        });
        mBluetoothGattCallback = new BluetoothGattCallback() {
            private ByteArrayOutputStream builder = new ByteArrayOutputStream();

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                BluetoothDevice device = gatt.getDevice();
                String devAddress = "";
                if(device != null) {
                    devAddress = device.getAddress();
                }
                switch(status) {
                    case BluetoothGatt.GATT_SUCCESS:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_SUCCESS");
                        break;
                    case BluetoothGatt.GATT_FAILURE:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_FAILURE");
                        break;
                    case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_CONNECTION_CONGESTED");
                        break;
                    case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION");
                        break;
                    case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION");
                        break;
                    case BluetoothGatt.GATT_INVALID_OFFSET:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_INVALID_OFFSET");
                        break;
                    case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_READ_NOT_PERMITTED");
                        break;
                    case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                        AppTrace.t(TAG, "BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED");
                        break;
                }

                //移除连接超时
                mHandler.removeCallbacks(connectOutTimeRunnable);

                if(newState == BluetoothGatt.STATE_CONNECTED) {
                    AppTrace.t(TAG, "连接成功, address=" + devAddress);
                    //连接成功去发现服务
                    gatt.discoverServices();

                    if(mBluetoothGatt != null) {
                        mBluetoothGatt.discoverServices();
                    }

                    //设置发现服务超时时间
                    mHandler.postDelayed(serviceDiscoverOutTimeRunnable, MAX_CONNECT_TIME);

                    CameraManager.newCamera(devAddress);

                    BluetoothHandle.onConnectSuccess(status);   //连接成功回调

                } else if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                    CameraTokenManager.onBluetoothDisconnected(gatt);
                    if (gatt == mBluetoothGatt) tokenNotificationsReady = false;
                    //清空系统缓存
                    ClsUtils.refreshDeviceCache(gatt);
                    AppTrace.t(TAG, "断开连接status:" + status + ", address=" + devAddress);
                    gatt.disconnect();
                    gatt.close();  //断开连接释放连接

                    if(mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                    }

                    //更新scan列表状态
                    if(_scanDeviceList != null && !_scanDeviceList.isEmpty()) {
                        for(Device tmpDevice : _scanDeviceList) {
                            if(tmpDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                                tmpDevice.status = Constants.DEVICE_STATUS_ONLINE;
                                break;
                            }
                        }
                    }

                    _connectDevice = null;
                    if(status == 133) {
                        //无法连接
//                        gatt.close();
                        BluetoothHandle.onConnectFailure(devAddress, "连接异常！");

                    } else if(status == 62) {
                        //成功连接没有发现服务断开
//                        gatt.close();
                        BluetoothHandle.onConnectFailure(devAddress, "连接成功服务未发现断开！");
                        AppTrace.t(TAG, "连接成功服务未发现断开status:" + status);

                    } else if(status == 0) {
                        AppTrace.t(TAG, "断开成功： status=" + status);
                        BluetoothHandle.onDisConnectSuccess(devAddress, status); //0正常断开 回调
                    } else if(status == 8) {
                        AppTrace.t(TAG, "距离远或者电池无法供电断开连接：status=" + status);
                        //因为距离远或者电池无法供电断开连接
                        // 已经成功发现服务
                        BluetoothHandle.onDisConnectSuccess(devAddress, status);
                    } else if(status == 34) {
                        AppTrace.t(TAG, "断开成功： status=" + status);
                        BluetoothHandle.onDisConnectSuccess(devAddress, status);
                    } else {
                        AppTrace.d(TAG, "断开连接：status=" + status);
                        //其它断开连接
                        BluetoothHandle.onDisConnectSuccess(devAddress, status);
                    }
                    //释放
                    CameraManager.release();
                } else if(newState == BluetoothGatt.STATE_CONNECTING) {
                    AppTrace.t(TAG, "正在连接..." + ", address=" + devAddress);
                    BluetoothHandle.onConnecting(new Device(devAddress, 0, ""));

                } else if(newState == BluetoothGatt.STATE_DISCONNECTING) {
                    AppTrace.t(TAG, "正在断开..." + ", address=" + devAddress);
                    BluetoothHandle.onDisConnecting(devAddress); //正在断开回调
                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                AppTrace.t(TAG, "onServicesDiscovered status:" + status);
                BluetoothDevice device = gatt.getDevice();
                String devAddress = "";
                if(device != null) {
                    devAddress = device.getAddress();
                }
                //移除发现服务超时
                mHandler.removeCallbacks(serviceDiscoverOutTimeRunnable);
//                AppTrace.t(TAG, "移除发现服务, address=" + devAddress);

                if(status != 0) {
                    gatt.close();
                } else {
                    if(setupService(gatt, BleConstants.BLE_WRITE_SERVICE_UUID, BleConstants.BLE_READ_SERVICE_UUID, BleConstants.BLE_READ_CHARACTERISTIC_UUID, BleConstants.BLE_WRITE_CHARACTERISTIC_UUID)) {
                        mBluetoothGattManager.setNotify(true);
                        //这一步必须要有，否则接收不到通知
                        //gatt.setCharacteristicNotification(characteristic,enable);
//                        gatt.requestMtu(134 + 3);
                        gatt.requestMtu(253 + 3);

                        //更新scan列表状态
                        if(_scanDeviceList != null && !_scanDeviceList.isEmpty()) {
                            for(Device tmpDevice : _scanDeviceList) {
                                if(tmpDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                                    tmpDevice.status = Constants.DEVICE_STATUS_CONNECT;
                                    break;
                                }
                            }
                        }

                        BluetoothHandle.onServiceDiscoverySucceed(devAddress, status);
                        AppTrace.t(TAG, "发现服务成功, address=" + devAddress);
                    } else {
                        BluetoothHandle.onServiceDiscoveryFailed(devAddress, "获取服务特征异常");  //发现服务失败回调
                        AppTrace.t(TAG, "发现服务失败, address=" + devAddress);
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                AppTrace.t(TAG, "onCharacteristicRead status = " + status);
                if(status == 0) {
                    try {
                        handleCharacteristicRead(gatt, characteristic);
                    } catch(Exception e) {
                        Log.d(TAG, Log.getStackTraceString(e));
                    }
                }
                mBluetoothGattManager.next();
            }

            private void handleCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) throws JSONException, UnsupportedEncodingException {

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                AppTrace.t(TAG, "onCharacteristicWrite status = " + status);
                if(status == 0) {
                    try {
                        handleCharacteristicWrite(gatt, characteristic);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                mBluetoothGattManager.next();
            }

            private void handleCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                AppTrace.t(TAG, "handleCharacteristicWrite ");

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                AppTrace.t(TAG, "onCharacteristicChanged ");

                try {
                    handleCharacteristicChanged(gatt, characteristic);
                } catch(JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            private void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) throws JSONException, UnsupportedEncodingException {
                try {
                    byte[] bytes = characteristic.getValue();
                    AppTrace.t(TAG, "handleCharacteristicChanged read bytes: " + bytes.length);
                    // 忽略APK 发送出去的数据
                    if(builder.size() == 0 && bytes[0] != BleConstants.BLE_CMD_CHAR_START) {
                        // 往前找0x01, 0x01之前的全部抛弃
                        int len = bytes.length;
                        for(int i = 0; i < len; i++) {
                            if(bytes[i] == BleConstants.BLE_CMD_CHAR_START) {
                                builder.write(bytes, i, len - i);
                                break;
                            }
                        }
                    } else {
                        builder.write(bytes);
                    }

//                    AppTrace.t(TAG, "handleCharacteristicChanged mtu:" + mMtu);
                    AppTrace.t(TAG, "handleCharacteristicChanged builder data size: " + builder.size());
                    if(isDataVerify(builder.toByteArray())) {
                        AppTrace.t(TAG, "data verify success");
                        if(builder.size() > 2) {
                            //这里有个问题是当收到的是多条命令的组合数据时，需要先做切分；
                            byte[] dataByte = builder.toByteArray();
                            ArrayList<String> dataList = new ArrayList();
                            int start = 1;
                            for(int i = 1; i < dataByte.length; i++) {
                                if(dataByte[i] == BleConstants.BLE_CMD_CHAR_END) {
                                    if(start < i) {
                                        byte[] tempByte = new byte[i - start];
                                        System.arraycopy(dataByte, start, tempByte, 0, i - start);
                                        String data = new String(tempByte, StandardCharsets.UTF_8);
                                        dataList.add(data);
                                    } else {
                                        AppTrace.t(TAG, "data length exception");
                                    }
                                } else if(dataByte[i] == BleConstants.BLE_CMD_CHAR_START) {
                                    start = i + 1;
                                }
                            }
                            if(dataList != null && !dataList.isEmpty()) {
                                for(int i = 0; i < dataList.size(); i++) {
                                    String data = dataList.get(i);
                                    if(isJSONValid(data)) {
                                        AppTrace.t(TAG, "receive cmd:" + data);
                                        JSONObject jsonRoot = (JSONObject) JSON.parse(data);
                                        if (BleConstants.BLE_CMD_SET_TOKEN.equals(
                                                jsonRoot.getString(BleConstants.BLE_CMD))) {
                                            CameraTokenManager.onBluetoothTokenResponse(gatt, jsonRoot);
                                        } else {
                                            BluetoothHandle.handle(jsonRoot, null);
                                        }
                                    } else {
                                        AppTrace.t(TAG, "error cmd:" + data);
                                    }
                                }
                            }
                            dataList.clear();
                            dataList = null;
                        }
                        builder.reset();
                    } else {
                        AppTrace.t(TAG, "data verify fail");
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    builder = new ByteArrayOutputStream();
                }

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                AppTrace.t(TAG, "onDescriptorRead ");
                if(status == 0) {
                    try {
                        handleDescriptorRead(gatt, descriptor);
                    } catch(Exception e) {

                    }
                }
                mBluetoothGattManager.next();
            }

            private void handleDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                AppTrace.t(TAG, "onDescriptorWrite status =" + status);
                if(status == 0) {
                    if (gatt == mBluetoothGatt && BleConstants.CLIENT_CONFIG_DESCRIPTOR_UUID
                            .equals(descriptor.getUuid()) && descriptor.getCharacteristic() != null
                            && BleConstants.BLE_READ_CHARACTERISTIC_UUID
                            .equals(descriptor.getCharacteristic().getUuid())) {
                        tokenNotificationsReady = true;
                        syncTokenWhenReady(gatt);
                    }
                    try {
                        handleDescriptorWrite(gatt, descriptor);
                    } catch(Exception e) {

                    }
                }
                mBluetoothGattManager.next();
            }

            private void handleDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {

            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                AppTrace.t(TAG, "onMtuChanged mtu:" + mtu + "; status:" + status);
//                mBluetoothGattManager.notify(gatt);
                mMtu = mtu - 3;
                syncTokenWhenReady(gatt);
            }
        };

//        loadLocalData();
//        loadBindDeviceData();
    }

    private void syncTokenWhenReady(BluetoothGatt gatt) {
        // Allow the notification setup/MTU operation to finish before queueing credentials.
        mHandler.postDelayed(() -> {
            if (gatt == mBluetoothGatt && tokenNotificationsReady) {
                CameraTokenManager.onBluetoothReady(gatt);
            }
        }, 500);
    }

    private boolean isJSONValid(String str) {
        try {
            JSONObject.parseObject(str);
        } catch(JSONException ex) {
            try {
                JSONObject.parseArray(str);
            } catch(JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private boolean isDataVerify(byte[] data) {
        int len = data.length;
        if(len <= 1) {
            return false;
        }
        return data[0] == BleConstants.BLE_CMD_CHAR_START && data[len - 1] == BleConstants.BLE_CMD_CHAR_END;
    }

    /**
     * 获取特定服务及特征
     * 1个serviceUUID -- 1个readUUID -- 1个writeUUID
     *
     * @param bluetoothGatt
     * @param writeServiceUUID
     * @param readServiceUUID
     * @param readUUID
     * @param writeUUID
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean setupService(BluetoothGatt bluetoothGatt, UUID writeServiceUUID, UUID readServiceUUID, UUID readUUID, UUID writeUUID) {
        if(bluetoothGatt == null) {
            AppTrace.t(TAG, "setupService()-->bluetoothGatt == null");
            return false;
        }

        if(writeServiceUUID == null || readServiceUUID == null) {
            AppTrace.t(TAG, "setupService()-->serviceUUID == null");
            return false;
        }

        for(BluetoothGattService service : bluetoothGatt.getServices()) {
//            Log.d(TAG, "service = " + service.getUuid().toString());
            if(service.getUuid().equals(readServiceUUID)) {
                mBluetoothGattReadService = service;
            } else if(service.getUuid().equals(writeServiceUUID)) {
                mBluetoothGattWriteService = service;
            }
        }
        //通过上面方法获取bluetoothGattService
//        bluetoothGattService = bleManager.getBluetoothGattService(bluetoothGatt,ConsData.MY_BLUETOOTH4_UUID);
        if(mBluetoothGattReadService == null) {
            //找不到该服务就立即断开连接
            AppTrace.e(TAG, "setupService()-->bluetoothGattReadService == null");
            return false;
        }
        if(mBluetoothGattWriteService == null) {
            //找不到该服务就立即断开连接
            AppTrace.e(TAG, "setupService()-->bluetoothGattWriteService == null");
            return false;
        }
        AppTrace.d(TAG, "setupService()-->bluetoothGattReadService = " + mBluetoothGattReadService);
        AppTrace.d(TAG, "setupService()-->bluetoothGattWriteService = " + mBluetoothGattWriteService.toString());

        if(readUUID == null || writeUUID == null) {
            AppTrace.e(TAG, "setupService()-->readUUID == null || writeUUID == null");
            return false;
        }

        for(BluetoothGattCharacteristic characteristic : mBluetoothGattReadService.getCharacteristics()) {
            if(characteristic.getUuid().equals(readUUID)) {  //读特征
                mReadCharacteristic = characteristic;
            }
        }
        for(BluetoothGattCharacteristic characteristic : mBluetoothGattWriteService.getCharacteristics()) {
            if(characteristic.getUuid().equals(writeUUID)) {  //写特征
                mWriteCharacteristic = characteristic;
            }
        }
        if(mReadCharacteristic == null) {
            AppTrace.e(TAG, "setupService()-->readCharacteristic == null");
            return false;
        }
        if(mWriteCharacteristic == null) {
            AppTrace.e(TAG, "setupService()-->writeCharacteristic == null");
            return false;
        }
        //打开读通知
        enableNotification(true, bluetoothGatt, mReadCharacteristic);

        //重点中重点，需要重新设置
        List<BluetoothGattDescriptor> descriptors = mReadCharacteristic.getDescriptors();
        for(BluetoothGattDescriptor descriptor : descriptors) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }

        //延迟2s，保证所有通知都能及时打开
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 2000);

        return true;

    }

    /////////////////////////////////////////  打开通知  //////////////////////////////////////////

    /**
     * 设置读特征接收通知
     *
     * @param enable         为true打开通知
     * @param gatt           连接
     * @param characteristic 特征
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(gatt == null) {
            AppTrace.e(TAG, "enableNotification-->gatt == null");
            return;
        }
        if(characteristic == null) {
            AppTrace.e(TAG, "enableNotification-->characteristic == null");
            return;
        }
        //这一步必须要有，否则接收不到通知
        gatt.setCharacteristicNotification(characteristic, enable);
    }


    public void setScanListener(IBleScanListener listener) {
        this._scanListener = listener;
    }

    public IBleScanListener getScanListener() {
        return _scanListener;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void stopScan() {
        isScanning = false;
        mHandler.removeCallbacks(stopScanRunnable);
        if(mBluetoothGattManager != null) {
            mBluetoothGattManager.stopLeScan();
        }
    }

    public void startScan(boolean isBackground) {
        AppTrace.t(TAG, "startScan....");

        isBackgroundScanning = isScanning;
        if(isScanning) {
            AppTrace.t(TAG, "skip, scanning....");
            BluetoothHandle.onScanning(isBackground);
            return;
        }

        boolean start = false;
        if(mBluetoothGattManager != null) {
            start = mBluetoothGattManager.startLeScan();
        }

        BluetoothHandle.onScanning(isBackground);

        if(start) {
            if(_scanDeviceList != null) {
                _scanDeviceList.clear();
            }

            //设定最长扫描时间
            mHandler.postDelayed(stopScanRunnable, scanTime);

            isScanning = true;
        } else {
            isScanning = false;
        }
    }

    public boolean disconnectBle() {
        if(mBluetoothGatt != null && mBluetoothGatt.getDevice() != null) {
            String address = mBluetoothGatt.getDevice().getAddress();

            return disconnectBle(address);
        }
        return false;
    }

    public boolean disconnectBle(String address) {
        AppTrace.t(TAG, "disconnectBle ...");
        try {
            if(_scanDeviceList != null) {
                for(Device device : _scanDeviceList) {
                    if(device.address.equalsIgnoreCase(address)) {
                        device.status = Constants.DEVICE_STATUS_ONLINE;
                        break;
                    }
                }
            }

            if(mBluetoothGatt == null) {
                return false;
            }
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;

            _connectDevice = null;

            CameraManager.release();
            BluetoothHandle.onDisConnectSuccess(address, 0);

            return true;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean disconnectBle(Device device) {
        AppTrace.t(TAG, "disconnectBle devce ...");
        return disconnectBle(device.address);
    }

    public void connectToBle(Device device) {
        AppTrace.t(TAG, "connectToBle : address = " + device.getAddress());
        if(mBluetoothGattManager.isLeScanning())
            mBluetoothGattManager.stopLeScan();

        if(mBluetoothGatt != null) {
            CameraTokenManager.onBluetoothDisconnected(mBluetoothGatt);
            try {
                AppTrace.t(TAG, "disconnect pre device");
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }


        tokenNotificationsReady = false;
        mBluetoothGatt = mBluetoothGattManager.connectGatt(device.getAddress(), mBluetoothGattCallback);

        _connectDevice = device;

        //连接开始回调
        BluetoothHandle.onConnecting(device);

    }

    public boolean enableBlueTooth() {
        if(mBluetoothGattManager != null) {
            return mBluetoothGattManager.isEnable();
        }
        return false;
    }

    /**
     * 打开蓝牙
     *
     * @param isFast true 直接打开蓝牙  false 提示用户打开
     */
    public void openBluetooth(Context context, boolean isFast) {
        if(mBluetoothGattManager != null) {
            mBluetoothGattManager.openBluetooth(context, isFast);
        }

    }

    /**
     * 打开蓝牙
     *
     * @param isFast true 直接打开蓝牙  false 提示用户打开
     */
    public void openBluetooth(Activity context, boolean isFast, int requestCode) {
        if(mBluetoothGattManager != null) {
            mBluetoothGattManager.openBluetooth(context, isFast, requestCode);
        }

    }

    /**
     * 直接关闭蓝牙连接
     */
    public void closeBluetooth() {
        if(mBluetoothGattManager != null) {
            mBluetoothGattManager.closeBluetooth();
        }
    }

    public void onScanning() {
        //
        isScanning = true;
        mHandler.removeCallbacks(stopScanRunnable);

        BluetoothHandle.onScanning(isBackgroundScanning);

    }

    public void onScanEnd() {
        //
        isScanning = false;
        mHandler.removeCallbacks(stopScanRunnable);

        BluetoothHandle.onScanEnd();

    }

    public static String getBluetoothName(String macAddress) {

        //更新本地记录的在线状态
        if(_scanDeviceList != null && !_scanDeviceList.isEmpty()) {
            for(Device tmpDevice : _scanDeviceList) {
                if(tmpDevice.getAddress().equalsIgnoreCase(macAddress)) {
                    return tmpDevice.name;
                }
            }
        }

        return "";
    }

    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecor) {
        if(device != null && device.getName() != null) {
            String deviceName = device.getName().toLowerCase(Locale.ROOT);
            if(deviceName.startsWith(weewa_prefix) || deviceName.startsWith(weewa_prefix_v2)) {
                AppTrace.t(TAG, "found weewa: " + deviceName +", bluetoothUUID: " + device.getAddress());
                Device blueDevice = new Device(device, rssi, deviceName.trim());
                blueDevice.setStatus(Constants.DEVICE_STATUS_ONLINE);

                //更新本地记录的在线状态
                if(_scanDeviceList != null && !_scanDeviceList.isEmpty()) {
                    boolean exist = false;
                    for(Device tmpDevice : _scanDeviceList) {
                        if(tmpDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                            if(tmpDevice.status == Constants.DEVICE_STATUS_OFFLINE) {
                                tmpDevice.status = Constants.DEVICE_STATUS_ONLINE;
                            }
                            exist = true;
                            break;
                        }
                    }
                    if(!exist) {
                        _scanDeviceList.add(blueDevice);
                    }
                } else {
                    _scanDeviceList.add(blueDevice);
                }

                BluetoothHandle.onLeScan(blueDevice);
            }
        }
    }

    public void updateConnectDeviceStatus(Device connectDevice) {

        if(_scanDeviceList == null) {
            _scanDeviceList = new CopyOnWriteArrayList<>();
        }

        if(!_scanDeviceList.isEmpty()) {
            Device cloneDevice = null;
            int curIndex = 0;
            for(int i = 0; i < _scanDeviceList.size(); i++) {
                Device device = _scanDeviceList.get(i);
                if(!TextUtils.isEmpty(device.getAddress()) && device.getAddress().equalsIgnoreCase(connectDevice.getAddress())) {
                    device.status = connectDevice.status;

                    cloneDevice = device.clone();
                    curIndex = i;
                    break;
                }
            }
            if(cloneDevice != null) {
                _scanDeviceList.remove(curIndex);

                _scanDeviceList.add(0, cloneDevice);

                saveLocalData();
            }
        } else {
            Device cloneDevice = connectDevice.clone();
            _scanDeviceList.add(cloneDevice);
        }
    }

    private void saveLocalData() {
        if(_scanDeviceList != null && !_scanDeviceList.isEmpty()) {
            String deviceString = new Gson().toJson(_scanDeviceList);
            AppFileUtil.saveString(deviceString, AppFileUtil.CACHE_DEVICE_FILE);
        }
    }

    private void loadLocalData() {
        String deviceString = AppFileUtil.loadStringFromFile(AppFileUtil.CACHE_DEVICE_FILE);
        List<Device> bleDeviceList = new Gson().fromJson(deviceString, new TypeToken<List<Device>>() {
        }.getType());

        if(bleDeviceList != null && !bleDeviceList.isEmpty()) {
            for(Device device : bleDeviceList) {
                device.setStatus(Constants.DEVICE_STATUS_OFFLINE);
            }

            if(_scanDeviceList == null) {
                _scanDeviceList = new CopyOnWriteArrayList<>();
            }
            _scanDeviceList.addAll(bleDeviceList);
        }
    }


    public void removeLocalData(Device device) {
        String deviceString = AppFileUtil.loadStringFromFile(AppFileUtil.CACHE_DEVICE_FILE);
        List<Device> bleDeviceList = new Gson().fromJson(deviceString, new TypeToken<List<Device>>() {
        }.getType());

        if(bleDeviceList != null && !bleDeviceList.isEmpty()) {
            for(Device tmpDevice : bleDeviceList) {
                if(!TextUtils.isEmpty(tmpDevice.getAddress()) && tmpDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                    bleDeviceList.remove(device);
                    return;
                }

            }
        }

    }

    public boolean isConnected() {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            return true;
        }

        if(!enableBlueTooth()) {
            return false;
        }
        if(mBluetoothGatt == null || _connectDevice == null) {
            return false;
        }

        return mBluetoothGattManager != null && mBluetoothGattManager.isEnable();
    }

    public List<Device> getBleDeviceList() {
        return _scanDeviceList;
    }

    public boolean sendCmdPowerOn() {
        AppTrace.t(TAG, "sendCmdPowerOn ...");
        if(mBluetoothGatt == null || _connectDevice == null) {
            AppTrace.t(TAG, "sendCmdPowerOn fail，ble disconnected...");
            return false;
        }
        return mBluetoothGattManager.sendCmdPowerOn(mBluetoothGatt);
    }

    public boolean sendCmdStartRecord(RecordInfoBean info) {
        AppTrace.d(TAG, "sendCmdStartRecord ...");
        if(!isConnected()) {
            AppTrace.t(TAG, "sendCmdStartRecord fail， ble disconnected");
            return false;
        }
        boolean result = mBluetoothGattManager.sendCmdRecordStart(mBluetoothGatt, info);

        return result;
    }

    public static String getDisplayAddress(String address) {
        if(!TextUtils.isEmpty(address) && address.length() > 4) {
            return address.substring(address.length() - 5);
        }
        return address;
    }


    public static String getDisplayDeviceName(String deviceName) {
        if(!TextUtils.isEmpty(deviceName)) {
            if(deviceName.startsWith("weewa_")) {
                return deviceName.substring(6);
            } else if(deviceName.startsWith("w_") || deviceName.startsWith("W_")) {
                return deviceName.substring(2);
            }
        }
        return deviceName.trim();
    }

    public static String getDisplayDeviceName(Device device) {
        if(device == null || TextUtils.isEmpty(device.getName())) {
            return "";
        }
        return getDisplayDeviceName(device.getName());
    }


    //获取视频列表
    public boolean sendCmdVideoList() {
        AppTrace.t(TAG, "sendCmdVideoList ...");
        if(!isConnected()) {
            AppTrace.t(TAG, "sendCmdVideoList fail， ble disconnected");
            return false;
        }
        return mBluetoothGattManager.sendCmdVideoList(mBluetoothGatt);
    }


    //获取单个录制视频的详情列表
    public boolean sendCmdVideoDetailList(String path) {
        AppTrace.t(TAG, "sendCmdVideoDetailList ...");
        if(!isConnected()) {
            AppTrace.t(TAG, "sendCmdVideoDetailList fail， ble disconnected");
            return false;
        }
        return mBluetoothGattManager.sendCmdVideoDetailList(mBluetoothGatt, path);
    }

    /**
     * 上传比赛视频目录下的所有（包括其精彩视频，及）列表
     *
     * @param path 比赛视频根目录 20230708/1030
     * @return
     */
    public boolean sendCmdVideoDirUploadList(List<String> path) {
        Log.d(TAG, "sendCmdVideoUploadList ...");
        if(!isConnected()) {
            AppTrace.t(TAG, "sendCmdVideoDirUploadList fail， ble disconnected");
            return false;
        }
        return mBluetoothGattManager.sendCmdVideoDirUploadList(mBluetoothGatt, path);
    }

    /**
     * 上传单个视频列表
     *
     * @param path
     * @return
     */
    public boolean sendCmdVideoUploadList(List<String> path) {
        Log.d(TAG, "sendCmdVideoUploadList ...");
        if(!isConnected()) {
            return false;
        }
        return mBluetoothGattManager.sendCmdVideoDirUploadList(mBluetoothGatt, path);
    }


    public boolean sendCmdVideoFilterList(int type) {
        Log.d(TAG, "sendCmdVideoFilterList ...");
        if(!isConnected()) {
            return false;
        }
        return mBluetoothGattManager.sendCmdVideoFilterList(mBluetoothGatt, type);
    }

    public static String getCameraFileUrl(String path) {
        return ConnectLogic.getImpl().getConnectBaseurl() + "/file?p=" + path;
    }

    public static String getConnectDeviceName() {
        if(_connectDevice != null) {
            String address = _connectDevice.getAddress();
            return address.replace(":", "");
        }
        return "";
    }

    public static String getConnectDeviceAddress() {
        if(_connectDevice != null) {
            return _connectDevice.getAddress();
        }
        return "";
    }

    public static Device getConnectDevice() {
        if(BluetoothManager.getImpl().mBluetoothGatt != null && BluetoothManager.getImpl().mBluetoothGatt.connect()) {
            return _connectDevice;
        } else {
            _connectDevice = null;
            return null;
        }
    }

    /**
     * @param obj
     * @return
     */
    public boolean sendCmdSimpleObj(BaseBluetoothBean obj) {
        AppTrace.t(TAG, "sendCmdSimpleObj ...");
        if(!isConnected()) {
            AppTrace.t(TAG, "sendCmdSimpleObj fail， ble disconnected");
            return false;
        }
        return mBluetoothGattManager.sendCmdSimpleObj(mBluetoothGatt, obj);
    }
}
