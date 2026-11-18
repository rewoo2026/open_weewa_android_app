package com.xh.hotme.listener;

import androidx.annotation.Keep;

@Keep
public interface IRecordInfoListener {
    void onUpdate(String name, String place);

    void onCancel();
}