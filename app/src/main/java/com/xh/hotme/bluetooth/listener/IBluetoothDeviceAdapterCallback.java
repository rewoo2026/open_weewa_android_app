package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bluetooth.Device;

public interface IBluetoothDeviceAdapterCallback {
    void onLeScanStart();

    void onLeScan(Device device, int position);

    void onLeScanStop(int size);

    void onBluetoothDeviceClick(Device device, int position);

    void onRemoveDevice(Device device, int position);


    void openDevice(Device device);

    void disconnectDevice(Device device);
}