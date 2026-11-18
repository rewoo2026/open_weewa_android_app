package com.xh.hotme.live;

import android.os.Handler;

import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.live.bean.HmCompetitionLive;

/** Starts a protocol 3 camera through the server that owns its recording authorization. */
final class CoordinatedLiveStarter {
    interface Backend {
        void start(String id, IGenericApiCallback<CommonResponse> callback);
        void query(String id, IGenericApiCallback<HmCompetitionLive> callback);
    }
    interface Timer {
        void post(Runnable runnable, long delay);
        void remove(Runnable runnable);
    }
    interface Listener {
        void onStarted();
        void onFailure(String message);
    }

    private final Backend backend;
    private final Timer timer;
    private final Listener listener;
    private boolean active;
    private String competitionId;
    private final Runnable poll = this::query;
    private final Runnable timeout = () -> fail("直播启动尚未确认，请检查设备状态后重试");

    static CoordinatedLiveStarter create(Handler handler, Listener listener) {
        return new CoordinatedLiveStarter(new Backend() {
            public void start(String id, IGenericApiCallback<CommonResponse> callback) {
                ApiUtil.startCompetitionLive(id, callback);
            }
            public void query(String id, IGenericApiCallback<HmCompetitionLive> callback) {
                ApiUtil.getCompetitionLive(id, callback);
            }
        }, new Timer() {
            public void post(Runnable runnable, long delay) { handler.postDelayed(runnable, delay); }
            public void remove(Runnable runnable) { handler.removeCallbacks(runnable); }
        }, listener);
    }

    CoordinatedLiveStarter(Backend backend, Timer timer, Listener listener) {
        this.backend = backend;
        this.timer = timer;
        this.listener = listener;
    }

    void start(String id) {
        if(active) return;
        competitionId = id;
        active = true;
        timer.post(timeout, 180000);
        backend.start(id, new IGenericApiCallback<CommonResponse>() {
            public void onSuccess(CommonResponse response) {
                if(!active) return;
                if(response != null && response.code == 200) complete();
                else if(response != null && response.code == 202) timer.post(poll, 2000);
                else fail("启动直播响应异常");
            }
            public void onFail(String code, String message) { fail(message); }
            public void onFinish() {}
        });
    }

    private void query() {
        if(!active) return;
        backend.query(competitionId, new IGenericApiCallback<HmCompetitionLive>() {
            public void onSuccess(HmCompetitionLive task) {
                if(!active) return;
                if(task == null || !competitionId.equals(task.competitionId)) {
                    fail("直播任务状态异常");
                } else if("2".equals(task.competitionStatus) || "3".equals(task.competitionStatus)
                    || "failed".equals(task.recordingExecutionStatus)) {
                    fail("直播启动失败，任务已暂停或结束");
                } else if("1".equals(task.competitionStatus) && Integer.valueOf(1).equals(task.pushStreamStatus)
                    && (task.recordingExecutionStatus == null || "recording".equals(task.recordingExecutionStatus))) {
                    complete();
                } else {
                    timer.post(poll, 2000);
                }
            }
            public void onFail(String code, String message) {
                if(!active) return;
                if("401".equals(code) || "403".equals(code)) fail(message);
                else timer.post(poll, 2000);
            }
            public void onFinish() {}
        });
    }

    private void complete() {
        if(!active) return;
        cancel();
        listener.onStarted();
    }

    private void fail(String message) {
        if(!active) return;
        cancel();
        listener.onFailure(message == null || message.isEmpty() ? "启动直播失败，请检查设备状态" : message);
    }

    void cancel() {
        active = false;
        timer.remove(poll);
        timer.remove(timeout);
    }
}
