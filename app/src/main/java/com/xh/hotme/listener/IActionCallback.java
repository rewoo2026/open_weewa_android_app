package com.xh.hotme.listener;


public interface IActionCallback {
    int CALLBACK_ACTION_OK = 1; // 没实际含义, 就代表按了确定按钮
    int CALLBACK_ACTION_DELETE = 2;
    int CALLBACK_ACTION_EDIT = 3;
    int CALLBACK_ACTION_START = 4;

    void onReturn(int code, Object... objects);
}
