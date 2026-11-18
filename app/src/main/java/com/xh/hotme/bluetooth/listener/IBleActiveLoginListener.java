package com.xh.hotme.bluetooth.listener;


import com.xh.hotme.bean.LoginResultBean;

public interface IBleActiveLoginListener {
    void onActive(LoginResultBean loginBean);

    void onActiveFail(String message);
}
