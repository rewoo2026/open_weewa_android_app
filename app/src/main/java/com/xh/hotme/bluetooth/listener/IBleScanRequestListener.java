package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.Device;

public interface IBleScanRequestListener {
    void onScanStart(boolean isBackground);

    void onScan(Device device, int position);

    void onScanStop(final int size);

    void onBluetoothDeviceClick(Device device, int position, int action);
    void onBluetoothDeviceClick(BindDeviceBean device, int position, int action);

    void onRequestWifiList();

    void onSetupWifi(String ssid, String password);

    @Deprecated
    void onRemove(Device device, int position);

}
