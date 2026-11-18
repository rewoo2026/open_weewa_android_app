package com.xh.hotme.camera;

import android.os.Handler;
import android.os.Looper;

import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.EnergyInfoRequestBean;
import com.xh.hotme.bluetooth.bean.StorageInfoRequestBean;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.http.DeviceApiManager;

/**
 * 定时获取相机的运行数据
 */
public class CameraMonitor {
    private final Handler _handler;

    final IBleDeviceUsageInfoNotifyListener _usageInfoListener;

    private final int DEVICE_USAGE_INFO_DELAY = 60000;  //定时1分钟

    DeviceUsageInfo _usageInfo;

    boolean isRunning = false;
    boolean isPause = false;

    boolean isPolling = false;

    StorageBean _storage;

    int _energy;

    private long _recordStartTime = 0;

    boolean isOnline = false;

    private final Runnable _deviceUsageInfoRunnable = new Runnable() {
        @Override
        public void run() {
            //
            if(isPolling) {
                if(!isPause) {
                    DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
                }
                _handler.postDelayed(_deviceUsageInfoRunnable, DEVICE_USAGE_INFO_DELAY);
            }
        }
    };

    private static final class HolderClass {
        public static final CameraMonitor INSTANCE = new CameraMonitor();
    }


    public static CameraMonitor getImpl() {
        return HolderClass.INSTANCE;
    }

    private CameraMonitor() {
        _handler = new Handler(Looper.getMainLooper());

        _usageInfoListener = new IBleDeviceUsageInfoNotifyListener() {
            @Override
            public void onStorageInfo(StorageBean storageBean) {
                _storage = storageBean;
            }

            @Override
            public void onEnergy(int energy) {
                _energy = energy;
            }

            @Override
            public void onDeviceInfo(DeviceUsageInfo data) {
                _usageInfo = data;
            }
        };
    }

    public void release() {
        isPolling = false;
        if(_usageInfoListener != null) {
            BluetoothHandle.removeDeviceUsageInfoNotifyListener(_usageInfoListener);
        }
        isRunning = false;
        if(_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }
    }

    public void start() {
        isPolling = true;
        BluetoothHandle.addDeviceUsageInfoNotifyListener(_usageInfoListener);

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
        isRunning = true;
        if(_handler != null) {
            _handler.postDelayed(_deviceUsageInfoRunnable, DEVICE_USAGE_INFO_DELAY);
        }
    }

    public void stop() {
        isPolling = false;
        _usageInfo = null;
        _energy = 0;
        _storage = null;
        BluetoothHandle.removeDeviceUsageInfoNotifyListener(_usageInfoListener);
        isRunning = false;
        if(_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }
    }

    public DeviceUsageInfo getDeviceUsageInfo() {
        return _usageInfo;
    }

    public int getDeviceEnergy() {
        return _energy;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void requestEnergyInfo() {
        DeviceApiManager.sendCmdSimpleObj(new EnergyInfoRequestBean());
    }

    public void requestStorageInfo() {
        DeviceApiManager.sendCmdSimpleObj(new StorageInfoRequestBean());
    }

    public void pause() {
        isPause = true;
    }

    public void resume() {
        isPause = false;
    }

    public void setRecordStartTime(long timeStamp){
        _recordStartTime = timeStamp;
    }

    public long getRecordStartTime(){
        return _recordStartTime;
    }

    public int getRecordDuration(){
        int duration = (int) (System.currentTimeMillis() - _recordStartTime)/1000;
        return duration;
    }

    public void stopRecord(){
        _recordStartTime = -1;
    }

    public boolean isOnline(){
        return isOnline;
    }

    public void setOnline(boolean online){
         isOnline = online;
    }
}
