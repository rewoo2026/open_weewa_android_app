package com.xh.hotme.camera.wifi;


import java.util.List;

public interface IWifiNotifyListener {
    void onWifiList(List<WifiInfo> device);

    void onSetupStatus(int status, String message);

    void onTurnOff(int status, String message);
}
