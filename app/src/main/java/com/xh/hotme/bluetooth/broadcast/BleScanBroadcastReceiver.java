package com.xh.hotme.bluetooth.broadcast;


import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;

import com.xh.hotme.HotmeApplication;

//监听扫描广播
public class BleScanBroadcastReceiver {
    private static final class HolderClass {
        private static final BleScanBroadcastReceiver INSTANCE = new BleScanBroadcastReceiver();
    }

    ScanBroadcastReceiver _receiver;

    public static BleScanBroadcastReceiver getImpl() {
        return HolderClass.INSTANCE;
    }

    private BleScanBroadcastReceiver() {

    }

    public void registerScanBroadcast() {
        Application application = HotmeApplication.getInstance();
        //注册蓝牙扫描状态广播接收者
        if(_receiver == null && application != null) {
            _receiver = new ScanBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            //监听蓝牙关闭和打开状态
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            //开始扫描
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            //扫描结束
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            //扫描中，返回结果
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            //扫描模式改变
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            //注册广播接收监听，用完不要忘了解注册哦
            application.registerReceiver(_receiver, filter);
        }
    }

    public void unregisterScanBroadcast() {
        if(_receiver != null) {
            Application application = HotmeApplication.getInstance();
            application.unregisterReceiver(_receiver);
        }
    }
}