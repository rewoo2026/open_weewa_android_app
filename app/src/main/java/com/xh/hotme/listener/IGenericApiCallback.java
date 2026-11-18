package com.xh.hotme.listener;


public interface IGenericApiCallback<T> {
    void onSuccess(T data);

    void onFail(String code, String message);

    void onFinish();
}
