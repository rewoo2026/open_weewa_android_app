package com.xh.hotme.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.listener.IBluetoothDeviceAdapterCallback;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.GsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class BluetoothGattManager {
    private static final String TAG = BluetoothGattManager.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private final BluetoothGattCallback mBluetoothGattCallback;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    //    private final BluetoothDeviceAdapter mBluetoothDeviceAdapter;
    private IBluetoothDeviceAdapterCallback mBluetoothDeviceAdapterCallback;
    private final GattUtils.RequestQueue mRequestQueue = GattUtils.createRequestQueue();
    private boolean mIsLeScanning = false;

    ScanCallback mScanCallback;

    public static final int packetLength = 100;
    public static int batchNumber = 0;

    public BluetoothGattManager() {
        BluetoothManager bluetoothManager = (BluetoothManager) HotmeApplication.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if(bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
//        mBluetoothDeviceAdapter = new BluetoothDeviceAdapter(mContext);

        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                boolean error = (status != 0);
                boolean isConnected = (newState == BluetoothAdapter.STATE_CONNECTED);

                AppTrace.d(TAG, "onConnectionStateChange error:" + error + "; isConnected:" + isConnected);
                if(!error && isConnected) {
                    error = gatt.discoverServices();
                }

                if(error) {
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                AppTrace.d(TAG, "onServicesDiscovered status:" + status);
                if(status != 0) {
                    gatt.close();
                } else {

                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
            }
        };
    }

    public void setBluetoothDeviceAdapterCallback(IBluetoothDeviceAdapterCallback bluetoothDeviceAdapterCallback) {
        mBluetoothDeviceAdapterCallback = bluetoothDeviceAdapterCallback;
//        mBluetoothDeviceAdapter.setCallback(bluetoothDeviceAdapterCallback);
    }

//    public BluetoothDeviceAdapter getBluetoothDeviceAdapter() {
//        return mBluetoothDeviceAdapter;
//    }

    public boolean startLeScan() {
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            return false;

        // 如果用户的设备没有开启蓝牙，则弹出开启蓝牙设备的对话框，让用户开启蓝牙
        if(!mBluetoothAdapter.isEnabled()) {
            AppTrace.d(TAG, "请求用户打开蓝牙");
        }

        if(Build.VERSION.SDK_INT < 21) {

            if(mLeScanCallback == null) {
                mLeScanCallback = (device, rssi, scanRecord) -> {
                    if(device != null && device.getName() != null) {
                        System.out.println("find bluetooth: " + device.getName());
                    }
//                        mBluetoothDeviceAdapter.onLeScan(device, rssi, scanRecord);
                };
            }

//            mBluetoothDeviceAdapter.getDevices().clear();
            boolean res = mBluetoothAdapter.startLeScan(/*new UUID[]{Constants.WIFI_SERVICE_UUID}, */mLeScanCallback);
            if(res) {
                mIsLeScanning = true;
//                if (mBluetoothDeviceAdapterCallback != null)
//                    mBluetoothDeviceAdapterCallback.onLeScanStart();
            }
            return res;
        } else {
            if(mScanCallback == null) {
                mScanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        System.out.println(TAG + "\t" + "onScanResult, result = " + result);
//                        Log.d(TAG, "onScanResult: " + result.getDevice().getAddress() + ", " + result.getDevice().getName());
//                        mBluetoothDeviceAdapter.onLeScan(result.getDevice(), result.getRssi(), null);
                    }

                    @Override
                    public void onBatchScanResults(List results) {
                        super.onBatchScanResults(results);
                        AppTrace.d(TAG, "onBatchScanResults");
                    }

                    //当扫描不能开启时回调
                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                        //扫描太频繁会返回ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED，表示app无法注册，无法开始扫描。
                        AppTrace.d(TAG, "onScanFailed. errorCode: " + errorCode);
                    }
                };
            }
//            mBluetoothDeviceAdapter.getDevices().clear();
            //开始扫描
            final BluetoothLeScanner mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mIsLeScanning = true;
            ScanSettings.Builder builder = new ScanSettings.Builder()
                //设置高功耗模式
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
            //android 6.0添加设置回调类型、匹配模式等
            if(Build.VERSION.SDK_INT >= 23) {
                //定义回调类型
                builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
                builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
//                builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
            }

            // 若设备支持批处理扫描，可以选择使用批处理，但此时扫描结果仅触发onBatchScanResults()
            if(mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
                //设置蓝牙LE扫描的报告延迟的时间(以毫秒为单位)
                //设置为0以立即通知结果
                builder.setReportDelay(0L);
            }

            ScanSettings scanSettings = builder.build();
            if(mBLEScanner != null) {
                // 判断是否在搜索,如果在搜索，就取消搜索
                if(mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                AppTrace.d(TAG, "startDiscovery");
                mBluetoothAdapter.startDiscovery();
//                mBLEScanner.startScan(null, scanSettings, mScanCallback);
                if(mBluetoothDeviceAdapterCallback != null)
                    mBluetoothDeviceAdapterCallback.onLeScanStart();

                return true;
            } else {
                mIsLeScanning = false;
                return false;
            }
        }
    }

    public boolean stopLeScan() {
        if(mBluetoothAdapter == null)
            return false;
        if(Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else {
            if(mScanCallback != null) {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
            }

            mBluetoothAdapter.cancelDiscovery();
        }
        mIsLeScanning = false;
        if(mBluetoothDeviceAdapterCallback != null)
            mBluetoothDeviceAdapterCallback.onLeScanStop(0);
        return true;
    }

    public boolean isLeScanning() {
        return mIsLeScanning;
    }

    public BluetoothGatt connectGatt(String address, BluetoothGattCallback callback) {
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if(bluetoothDevice == null)
            return null;
        AppTrace.d(TAG, "connectGatt address: " + address);
        return bluetoothDevice.connectGatt(HotmeApplication.getContext(), false, callback);
    }

    private static boolean mNotified = false;

    public void setNotify(boolean notify) {
        mNotified = notify;
    }

    public void notify(BluetoothGatt gatt) {
        if(mNotified)
            return;

        mNotified = true;
        BluetoothGattCharacteristic characteristic = null;
        characteristic = GattUtils.getCharacteristic(gatt, BleConstants.BLE_READ_SERVICE_UUID, BleConstants.BLE_READ_CHARACTERISTIC_UUID);
        if(characteristic == null) {
            Log.d(TAG, "BluetoothGattManager notify get characteristic failed.");
            return;
        }

        BluetoothGattDescriptor descriptor = null;
        descriptor = GattUtils.getDescriptor(gatt,
            BleConstants.BLE_READ_SERVICE_UUID,
            characteristic.getUuid(),
            BleConstants.CLIENT_CONFIG_DESCRIPTOR_UUID);
        if(descriptor != null) {
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor1 = new BluetoothGattDescriptor(descriptor.getUuid(), descriptor.getPermissions());
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
//        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mRequestQueue.addWriteDescriptor(gatt, descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            //具有NOTIFY|INDICATE 属性，根据属性设置相应的值，这里先默认设置为ENABLE_NOTIFICATION_VALUE, tiantian
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mRequestQueue.addWriteDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            mRequestQueue.execute();
        }
    }

    public boolean sendCmdNetStatus(BluetoothGatt gatt) {
        AppTrace.d(TAG, "sendCmdNetStatus ...");

        return sendCmdSimple(gatt, BleConstants.BLE_CMD_NETWORK_STATUS);
    }

    public boolean sendCmdWifiLists(BluetoothGatt gatt) {
        AppTrace.d(TAG, "sendCmdWifiLists ...");

        return sendCmdSimple(gatt, BleConstants.BLE_CMD_WIFILIST);
    }

    public boolean sendCmdWifiSetup(BluetoothGatt gatt, String ssid, String pwd) {
        return sendCmdWifiSetup(gatt, ssid, pwd, null);
    }

    private boolean sendCmdWifiSetup(BluetoothGatt gatt, String ssid, String pwd, String userdata) {
        AppTrace.d(TAG, "sendCmdWifiSetup ...");
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_WIFI_SETUP);
            obj.put(BleConstants.BLE_KEY_SSID, ssid);
            obj.put(BleConstants.BLE_KEY_PASSWORD, pwd);
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return sendCmdSimple(gatt, obj);
    }

    private boolean sendCmdSimple(BluetoothGatt gatt, String cmd) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, cmd);
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return sendCmdSimple(gatt, obj);
    }

    private boolean sendCmdSimpleJsonStr(BluetoothGatt gatt, String jsonStr, boolean sensitive) {
        AppTrace.t(TAG, sensitive ? "sendCmdSimple: set_token packet (redacted)"
                : "sendCmdSimple:\"" + jsonStr + "\"");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BleConstants.BLE_CMD_CHAR_START);
        try {
            baos.write(jsonStr.getBytes());
        } catch(IOException e) {
        }
        baos.write(BleConstants.BLE_CMD_CHAR_END);

        return sendCmd(gatt, baos.toByteArray());
    }


    private boolean sendCmdSimple(BluetoothGatt gatt, JSONObject obj) {
        AppTrace.t(TAG, "sendCmdSimple:\"" + obj.toString() + "\"");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BleConstants.BLE_CMD_CHAR_START);
        try {
            baos.write(obj.toString().getBytes());
        } catch(IOException e) {
        }
        baos.write(BleConstants.BLE_CMD_CHAR_END);

        return sendCmd(gatt, baos.toByteArray());
    }

    private boolean sendCmdMultiPacket(BluetoothGatt gatt, String cmd, JSONObject sourceJson) {
        AppTrace.t(TAG, "sendCmdMultiPacket:\"" + sourceJson.toString() + "\"");

        String dataString = sourceJson.toString();

        return sendCmdMultiPacket(gatt, cmd, dataString);
    }

    private boolean sendCmdMultiPacket(BluetoothGatt gatt, String cmd, String dataString) {
        boolean sensitive = BleConstants.BLE_CMD_SET_TOKEN.equals(cmd);
        AppTrace.t(TAG, sensitive ? "sendCmdMultiPacket: set_token (redacted)"
                : "sendCmdMultiPacket:\"" + dataString + "\"");

        int packetNum = (dataString.length() + packetLength - 1) / packetLength;
        if(packetNum > 1) {
            long batch = batchNumber++;
            //构建多包的消息列表
            AppTrace.t(TAG, String.format("sendCmdMultiPacket: split total  %d packet to send", packetNum));
            String[] packetSplit = new String[packetNum];
            ArrayList<String> cmdList = new ArrayList<>();
            for(int i = 0; i < packetNum; i++) {
                if(i < (packetNum - 1)) {
                    packetSplit[i] = dataString.substring(i * packetLength, (i + 1) * packetLength);
                } else {
                    packetSplit[i] = dataString.substring(i * packetLength);
                }
                JSONObject obj = new JSONObject();
                try {
                    obj.put(BleConstants.BLE_CMD, cmd);
                    JSONObject ext = new JSONObject();
                    ext.put(BleConstants.BLE_CMD_EXT_TOTAL, packetNum);
                    ext.put(BleConstants.BLE_CMD_EXT_ORDER, i);
                    ext.put(BleConstants.BLE_CMD_EXT_BATCH, batch);
                    ext.put(BleConstants.BLE_CMD_EXT_DATA, packetSplit[i]);
                    obj.put(BleConstants.BLE_CMD_EXT, ext);
                } catch(JSONException e) {
                    e.printStackTrace();
                }
                cmdList.add(obj.toString());
            }
            //逐包发送
            for(int i = 0; i < packetNum; i++) {
                AppTrace.t(TAG, String.format("sendCmdMultiPacket: 共 %d 个, 当前第 %d 个 ", packetNum, i+1 ));
                sendCmdSimpleJsonStr(gatt, cmdList.get(i), sensitive);
                try {
                    Thread.sleep(20);  // 加点时间间隔，防止接收端太忙，处理不了
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(BleConstants.BLE_CMD_CHAR_START);
            try {
                baos.write(dataString.getBytes());
            } catch(IOException e) {
                e.printStackTrace();
            }
            baos.write(BleConstants.BLE_CMD_CHAR_END);

            return sendCmd(gatt, baos.toByteArray());
        }

        return true;
    }


    private boolean sendCmd(BluetoothGatt gatt, byte[] data) {
        BluetoothGattCharacteristic characteristic = null;

        characteristic = GattUtils.getCharacteristic(gatt, BleConstants.BLE_WRITE_SERVICE_UUID, BleConstants.BLE_WRITE_CHARACTERISTIC_UUID);
        if(characteristic == null) {
            AppTrace.t(TAG, "sendCmd error：characteristic is null");
            return false;
        }

        characteristic.setValue(data);
        mRequestQueue.addWriteCharacteristic(gatt, characteristic);
        mRequestQueue.execute();
        AppTrace.t(TAG, "sendCmd finish");
        return true;
    }

    private final Object lock = new Object();

    public void next() {
        synchronized(lock) {
            mRequestQueue.next();
        }
    }

    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecor) {
//        if (mBluetoothDeviceAdapter != null) {
//            mBluetoothDeviceAdapter.onLeScan(device, rssi, null);
//        }
    }

    public boolean isEnable() {
        if(mBluetoothAdapter != null) {
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }


    /**
     * 打开蓝牙
     *
     * @param isFast true 直接打开蓝牙  false 提示用户打开
     */
    @SuppressLint("MissingPermission")
    public void openBluetooth(Context context, boolean isFast) {
        if(!isEnable()) {
            if(isFast) {
                Log.d(TAG, "直接打开手机蓝牙");
                mBluetoothAdapter.enable();  //BLUETOOTH_ADMIN权限
            } else {
                Log.d(TAG, "提示用户去打开手机蓝牙");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        } else {
            Log.d(TAG, "手机蓝牙状态已开");
        }
    }

    /**
     * 打开蓝牙
     *
     * @param isFast true 直接打开蓝牙  false 提示用户打开
     */
    @SuppressLint("MissingPermission")
    public void openBluetooth(Activity context, boolean isFast, int requestCode) {
        if(!isEnable()) {
            if(isFast) {
                Log.d(TAG, "直接打开手机蓝牙");
                mBluetoothAdapter.enable();  //BLUETOOTH_ADMIN权限
            } else {
                Log.d(TAG, "提示用户去打开手机蓝牙");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivityForResult(enableBtIntent, requestCode);
            }
        } else {
            Log.d(TAG, "手机蓝牙状态已开");
        }
    }

    /**
     * 直接关闭蓝牙
     */
    @SuppressLint("MissingPermission")
    public void closeBluetooth() {
        if(mBluetoothAdapter == null)
            return;

        mBluetoothAdapter.disable();
    }

    /**
     * 发送关机命令
     *
     * @param gatt
     * @return
     */
    public boolean sendCmdPowerOn(BluetoothGatt gatt) {
        AppTrace.d(TAG, "sendCmdPowerOn ...");
        //return  sendCmdSimple(gatt, BleConstants.BLE_CMD_POWER_OFF);

        //开机命令
        byte[] powneron = new byte[] { (byte) 0xFF, (byte) 0xFE, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01, 0x05 };

        return sendCmd(gatt, powneron);
    }

    public boolean sendCmdRecordStart(BluetoothGatt gatt, RecordInfoBean recordInfoBean) {
        AppTrace.d(TAG, "sendCmdRecordStart ...");
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_RECORDER_START);
            obj.put(BleConstants.BLE_KEY_CAMPAIGN_NAME, recordInfoBean.campaignName);
            obj.put(BleConstants.BLE_KEY_VIDEO_NAME, recordInfoBean.name);
            obj.put(BleConstants.BLE_KEY_MAX_RATE, recordInfoBean.maxRate);
            obj.put(BleConstants.BLE_KEY_AUTHOR, recordInfoBean.author);
            obj.put(BleConstants.BLE_KEY_VIDEO_WIDTH, recordInfoBean.width);
            obj.put(BleConstants.BLE_KEY_VIDEO_HEIGHT, recordInfoBean.height);
            obj.put(BleConstants.BLE_KEY_VIDEO_ADDRESS, recordInfoBean.place);
            obj.put(BleConstants.BLE_KEY_VIDEO_TIME, recordInfoBean.time);
            obj.put(BleConstants.BLE_KEY_VIDEO_CITY, recordInfoBean.city);
            obj.put(BleConstants.BLE_KEY_VIDEO_PLACE_TYPE, "" + recordInfoBean.placeType);
            obj.put(BleConstants.BLE_KEY_MOTION_TYPE, recordInfoBean.motionType);
            obj.put(BleConstants.BLE_KEY_HOST_TEAM, recordInfoBean.hostTeam);
            obj.put(BleConstants.BLE_KEY_GUSET_TEAM, recordInfoBean.guestTeam);
            obj.put(BleConstants.BLE_KEY_APPEND, recordInfoBean.append ? 1 : 0);
            obj.put(BleConstants.BLE_KEY_PLAYER_NUM, recordInfoBean.player_num);
            obj.put(BleConstants.BLE_KEY_AVS_MBR, recordInfoBean.avs_mbr);
            obj.put(BleConstants.BLE_KEY_PROVINCE_CODE, recordInfoBean.provinceId);
            obj.put(BleConstants.BLE_KEY_CITY_CODE, recordInfoBean.cityId);
            obj.put(BleConstants.BLE_KEY_DISABLE_AVS, recordInfoBean.disable_avs);
            obj.put(BleConstants.BLE_KEY_LIVE_COMPETITION_ID, recordInfoBean.live_cid);
            obj.put(BleConstants.BLE_KEY_COMPETITION_TYPE, recordInfoBean.ctype);
            obj.put(BleConstants.BLE_KEY_LIVE_RATE, recordInfoBean.live_mbr);
            obj.put(BleConstants.BLE_KEY_HIDE_OSD, recordInfoBean.hide_osd);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdMultiPacket(gatt, BleConstants.BLE_CMD_RECORDER_START, obj);
    }

    public boolean sendCmdVideoList(BluetoothGatt gatt) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_LIST);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdSimple(gatt, obj);
    }

    public boolean sendCmdVideoDetailList(BluetoothGatt gatt, String path) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DETAIL_LIST);
            obj.put(BleConstants.BLE_KEY_PATH, path);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdSimple(gatt, obj);
    }

    public boolean sendCmdVideoDirUploadList(BluetoothGatt gatt, List<String> path) {
        AppTrace.d(TAG, "sendCmdVideoDirUploadList ...");
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DIR_UPLOAD);
            obj.put(BleConstants.BLE_KEY_VIDEO, path);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdSimple(gatt, obj);
    }

    public boolean sendCmdVideoUploadList(BluetoothGatt gatt, List<String> path) {
        AppTrace.d(TAG, "sendCmdVideoUploadList ...");
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DIR_UPLOAD);
            obj.put(BleConstants.BLE_KEY_VIDEO, path);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdSimple(gatt, obj);
    }

    public boolean sendCmdVideoFilterList(BluetoothGatt gatt, int type) {
        AppTrace.d(TAG, "sendCmdVideoFilterList ...");
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_FILTER);
            obj.put(BleConstants.BLE_KEY_FILTER, type);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return sendCmdSimple(gatt, obj);
    }

    public boolean sendCmdSimpleObj(BluetoothGatt gatt, BaseBluetoothBean requestBean) {
        String cmd = requestBean.c;
        String jsonStr = GsonUtils.GsonString(requestBean);

        return sendCmdMultiPacket(gatt, cmd, jsonStr);
    }
}
