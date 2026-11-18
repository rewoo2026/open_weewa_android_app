package com.xh.hotme.record;

import android.os.Handler;

import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

final class CoordinatedRecordStarter {
    interface Backend {
        void start(String id, IGenericApiCallback<CommonResponse> callback);
        void query(String id, IGenericApiCallback<HmCompetitionRecordBean> callback);
        void cancel(String id, Runnable finished);
    }
    interface Timer {
        void post(Runnable action, long delay);
        void remove(Runnable action);
    }
    interface Listener {
        void onStarted();
        void onFailure(String message);
    }

    private final Backend backend;
    private final Timer timer;
    private final Listener listener;
    private boolean active;
    private boolean requested;
    private boolean cancelling;
    private final List<Runnable> cancelCallbacks = new ArrayList<>();
    private String id;
    private final Runnable poll = this::query;
    private final Runnable timeout = () -> fail("录制启动尚未确认，请检查设备状态后重试", true);

    static CoordinatedRecordStarter create(Handler handler, Listener listener) {
        return new CoordinatedRecordStarter(new Backend() {
            public void start(String id, IGenericApiCallback<CommonResponse> callback) { RecordingApi.start(id, callback); }
            public void query(String id, IGenericApiCallback<HmCompetitionRecordBean> callback) { RecordingApi.query(id, callback); }
            public void cancel(String id, Runnable finished) {
                // Stop an outstanding server start too, so leaving the form or
                // timing out cannot silently start recording later.
                RecordingApi.pause(id, new IGenericApiCallback<CommonResponse>() {
                    public void onSuccess(CommonResponse response) { }
                    public void onFail(String code, String message) {
                        ToastUtil.s("取消录制尚未确认，请检查相机状态");
                    }
                    public void onFinish() { finished.run(); }
                });
            }
        }, new Timer() {
            public void post(Runnable action, long delay) { handler.postDelayed(action, delay); }
            public void remove(Runnable action) { handler.removeCallbacks(action); }
        }, listener);
    }

    CoordinatedRecordStarter(Backend backend, Timer timer, Listener listener) {
        this.backend = backend; this.timer = timer; this.listener = listener;
    }

    void start(String competitionId) {
        if (requested) return;
        requested = true;
        id = competitionId;
        active = true;
        timer.post(timeout, 180000);
        backend.start(id, new IGenericApiCallback<CommonResponse>() {
            public void onSuccess(CommonResponse response) {
                if (!active) return;
                if (response != null && response.code == 200) complete();
                else if (response != null && response.code == 202) timer.post(poll, 2000);
                else fail("录制服务响应异常", true);
            }
            public void onFail(String code, String message) {
                fail(message, "-1".equals(code) || "-2".equals(code));
            }
            public void onFinish() { }
        });
    }

    private void query() {
        if (!active) return;
        backend.query(id, new IGenericApiCallback<HmCompetitionRecordBean>() {
            public void onSuccess(HmCompetitionRecordBean task) {
                if (!active) return;
                if (task == null || !id.equals(task.competitionId)) fail("录制任务状态异常", true);
                else if (task.competitionStatus == 2 || task.competitionStatus == 3
                        || "failed".equals(task.recordingExecutionStatus)) fail("录制启动失败，任务已暂停或结束", false);
                else if (task.competitionStatus == 1 && "recording".equals(task.recordingExecutionStatus)) complete();
                else timer.post(poll, 2000);
            }
            public void onFail(String code, String message) {
                if (!active) return;
                if ("401".equals(code) || "403".equals(code)) fail(message, true);
                else timer.post(poll, 2000);
            }
            public void onFinish() { }
        });
    }

    private void complete() {
        if (!active) return;
        clear(); listener.onStarted();
    }
    private void fail(String message, boolean cancelPending) {
        if (!active) return;
        Runnable failed = () -> listener.onFailure(message == null || message.isEmpty()
                ? "启动录制失败，请检查设备状态" : message);
        if (cancelPending) cancel(failed);
        else { clear(); failed.run(); }
    }
    void cancel() {
        cancel(() -> { });
    }
    void cancel(Runnable finished) {
        if (cancelling) { cancelCallbacks.add(finished); return; }
        if (!active) { finished.run(); return; }
        clear(); cancelling = true; cancelCallbacks.add(finished);
        backend.cancel(id, () -> {
            cancelling = false;
            List<Runnable> callbacks = new ArrayList<>(cancelCallbacks);
            cancelCallbacks.clear();
            for (Runnable callback : callbacks) callback.run();
        });
    }
    private void clear() {
        active = false; timer.remove(poll); timer.remove(timeout);
    }
}
