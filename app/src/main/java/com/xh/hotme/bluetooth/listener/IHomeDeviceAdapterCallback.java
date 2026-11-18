package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.Device;

public interface IHomeDeviceAdapterCallback {
    void onLeScanStart();

    void onLeScan(Device device, int position);

    void onLeScanStop(int size);

    void onBluetoothDeviceClick(BindDeviceBean device, int position);

    void onRemoveDevice(BindDeviceBean device, int position);


    void openDevice(BindDeviceBean device);

    void disconnectDevice(BindDeviceBean device);
}