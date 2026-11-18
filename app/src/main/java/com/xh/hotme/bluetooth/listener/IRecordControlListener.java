package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.RecordStatusResultBean;

public interface IRecordControlListener {
    void onRecordVideoStart();

    void onRecordVideoStatusUpdate(RecordStatusResultBean recordInfo);

    void onRecordVideoStop();

    void onRecordException(String msg);
}
