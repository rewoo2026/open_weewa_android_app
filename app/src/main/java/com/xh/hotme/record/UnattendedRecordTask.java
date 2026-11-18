package com.xh.hotme.record;

import androidx.annotation.Keep;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.environment.RuntimeEnvironment;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.Constants;
import java.util.Locale;

/** Normal local recording parameters, retained on the phone for automatic retries. */
@Keep
public final class UnattendedRecordTask {
    public String id, deviceId, userId, environment;
    public RecordStartRequestBean request;
    public RecordInfoBean info;
    public boolean localAuthorized;
    public boolean hasRecorded;
    public long startAt, endAt;

    boolean isSelectedDevice() {
        return Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL && deviceId.equals(CameraManager.getDeviceId())
                && userId.equals(LoginManager.getUserId()) && environment.equals(RuntimeEnvironment.cacheScope());
    }

    static String remainingTime(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    static UnattendedRecordTask fromRecording(RecordInfoBean info, boolean localAuthorized) {
        return fromRecording(info, localAuthorized, LocalRecordingWindow.resolve(null, null, System.currentTimeMillis()));
    }

    static UnattendedRecordTask fromRecording(RecordInfoBean info, boolean localAuthorized, LocalRecordingWindow window) {
        UnattendedRecordTask task = new UnattendedRecordTask();
        task.id = java.util.UUID.randomUUID().toString();
        task.deviceId = CameraManager.getDeviceId();
        task.userId = LoginManager.getUserId();
        task.environment = RuntimeEnvironment.cacheScope();
        task.request = info.convertRecordStartRequestBean();
        task.info = info;
        task.localAuthorized = localAuthorized;
        task.startAt = window.startAt;
        task.endAt = window.endAt;
        return task;
    }
}
