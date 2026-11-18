package com.xh.hotme.record.bean;

import androidx.annotation.Keep;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;

@Keep
public class LocalRecordingGrant {
    public String c, live_cid, competition_id, session_id, operation_id, request_id;
    public long device_version, expected_device_version, schedule_version, stop_at, start_before;
    public int ctype;
    public boolean local_control;
    public boolean valid(String id, long observed) {
        return "record_start".equals(c) && local_control && ctype == 1 && id.equals(live_cid)
                && id.equals(competition_id) && session_id != null && !session_id.isEmpty()
                && operation_id != null && !operation_id.isEmpty() && device_version > observed
                && expected_device_version == observed && schedule_version > 0
                && start_before > System.currentTimeMillis() && stop_at > System.currentTimeMillis();
    }
    public void apply(RecordStartRequestBean request) {
        request.local_control = local_control;
        request.device_version = device_version; request.expected_device_version = expected_device_version;
        request.session_id = session_id; request.operation_id = operation_id;
        request.schedule_version = schedule_version; request.stop_at = stop_at; request.start_before = start_before;
    }
}
