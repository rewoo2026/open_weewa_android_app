package com.xh.hotme.bluetooth.listener;

public interface IBleSoftApNotifyListener {
    void onSoftApStart(String ssid, String password);

    void onSoftApStartFail(String message);

    void onSoftApStop();

    void onSoftApStopFail(String message);

    void onSoftApStatus(int status);

    void onSoftApConnectStart();

    void onSoftApConnectSuccess();

    void onSoftApConnectFail(String message);
}
