package com.xh.hotme.bluetooth.bean;
import androidx.annotation.Keep;

/** The camera allocates the stop sequence under its control lock. */
@Keep
public class LocalRecordStopRequestBean extends BaseBluetoothBean {
    public final boolean local_control = true;
    public final String competition_id, session_id;
    public LocalRecordStopRequestBean(String id, String session) {
        c = "record_stop"; competition_id = id; session_id = session;
    }
}
