package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.RecordStatusResultBean;


/*
{"c":"record_status","au":"","audio":1,"city":"广东省深圳市","duration":252,"guest_team":"巴西","host_team":"阿根廷","mbr":8192,"motion_type":"足球","place_type":"11人场","r":"ok","st":1,"vh":1080,"vn":"世界杯预选赛","vs":"桃园","vt":1701932743487,"vw":1920}
 */
@Keep
public class RecordStatusResultEvent {
    public final int status;
    public final RecordStatusResultBean bean;

    public RecordStatusResultEvent(int st, RecordStatusResultBean bean) {
        this.status = st;
        this.bean = bean;
    }

}
