package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.camera.wifi.WifiInfo;

public interface IWifiListAdapterCallback {

    void onCancel();

    void onWifiClick(WifiInfo device, int position);
}