package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.record.bean.RecordInfoBean;

@Keep
public class RecordStartEvent {
    RecordInfoBean recordInfoBean;

    public RecordStartEvent(RecordInfoBean info) {
        this.recordInfoBean = info;
    }

    public RecordInfoBean getRecordInfoBean() {
        return recordInfoBean;
    }

    public void setRecordInfoBean(RecordInfoBean recordInfoBean) {
        this.recordInfoBean = recordInfoBean;
    }
}
