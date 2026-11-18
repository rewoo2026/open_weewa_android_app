package com.xh.hotme.bluetooth.listener;


public interface IBleLoginTokenListener {
    void onSyncToken();

    void onSyncTokenFail(String msg);
}
