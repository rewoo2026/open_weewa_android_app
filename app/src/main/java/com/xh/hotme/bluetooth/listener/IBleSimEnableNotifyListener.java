package com.xh.hotme.bluetooth.listener;

public interface IBleSimEnableNotifyListener {
    void onSimOpen();

    void onSimOpenFail(String msg);

    void onSimClose();

    void onSimCloseFail(String msg);
}
