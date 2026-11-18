package com.xh.hotme.listener;


public interface IApiCallback {
    void onSuccess();

    void onFail(String code, String message);

    void onFinish();
}
