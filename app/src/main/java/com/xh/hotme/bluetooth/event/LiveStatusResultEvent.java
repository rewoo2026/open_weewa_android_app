package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.LiveStatusResultBean;

/**
 * 检查直播状态
 * {"c":"live_status","avs_live":1,"fec_live":1,"avs_timeout":0,"fec_timeout":0,"r":"ok","avs_live_urls":["rtmp://127.0.0.1:1935/live/preview_avs"],"fec_live_urls":["rtmp://127.0.0.1:1935/live/preview","rtmp://push.rewoo360.com/rewoo/20f0c449c23ae3f03d4d825daaf346ba91749ae44d64d8e21786697d2ec14f77?hwSecret=a9db0c2263aef5b48fbbfd93779459a14860d27b02a282e0de43303a3d2f3f1f&hwTime=65a2a71f"]}
 */

@Keep
public class LiveStatusResultEvent {
    public final int status;
    public final LiveStatusResultBean bean;

    public LiveStatusResultEvent(int st, LiveStatusResultBean bean) {
        this.status = st;
        this.bean = bean;
    }

}
