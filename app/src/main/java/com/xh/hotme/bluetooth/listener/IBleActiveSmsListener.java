package com.xh.hotme.bluetooth.listener;


public interface IBleActiveSmsListener {
    void onSendActiveSms(String code);

    void onSendActiveSmsFail(String message);
}
