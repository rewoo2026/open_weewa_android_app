package com.xh.hotme.live;

import com.google.gson.Gson;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.live.bean.HmCompetitionLive;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class CoordinatedLiveStarterTest {
    static class Fixture implements CoordinatedLiveStarter.Backend, CoordinatedLiveStarter.Timer,
            CoordinatedLiveStarter.Listener {
        IGenericApiCallback<CommonResponse> startCallback;
        IGenericApiCallback<HmCompetitionLive> queryCallback;
        Map<Runnable, Long> pending = new LinkedHashMap<>();
        int starts, queries, successes, failures;
        String error;
        CoordinatedLiveStarter starter = new CoordinatedLiveStarter(this, this, this);
        public void start(String id, IGenericApiCallback<CommonResponse> callback) { starts++; startCallback = callback; }
        public void query(String id, IGenericApiCallback<HmCompetitionLive> callback) { queries++; queryCallback = callback; }
        public void post(Runnable runnable, long delay) { pending.put(runnable, delay); }
        public void remove(Runnable runnable) { pending.remove(runnable); }
        public void onStarted() { successes++; }
        public void onFailure(String message) { failures++; error = message; }
        void tick(long delay) {
            Runnable next = pending.entrySet().stream().filter(e -> e.getValue() == delay)
                .findFirst().get().getKey();
            pending.remove(next); next.run();
        }
        void respond(int code) {
            CommonResponse response = new CommonResponse(); response.code = code;
            startCallback.onSuccess(response);
        }
        HmCompetitionLive task(String state, int push) {
            HmCompetitionLive task = new HmCompetitionLive();
            task.competitionId = "match"; task.competitionStatus = state; task.pushStreamStatus = push;
            return task;
        }
    }

    @Test public void confirmedStartFinishesOnceAndClearsTimeout() {
        Fixture f = new Fixture(); f.starter.start("match"); f.respond(200); f.respond(200);
        assertEquals(1, f.successes); assertTrue(f.pending.isEmpty());
    }
    @Test public void pendingResponseWithNullDataDoesNotClaimSuccess() {
        Fixture f = new Fixture(); f.starter.start("match");
        CommonResponse response = new Gson().fromJson("{\"code\":202,\"msg\":\"等待设备确认\",\"data\":null}", CommonResponse.class);
        f.startCallback.onSuccess(response);
        assertEquals(0, f.successes);
        f.tick(2000); f.queryCallback.onSuccess(f.task("1", 0));
        assertEquals(0, f.successes);
        f.tick(2000);
        HmCompetitionLive starting = f.task("1", 1); starting.recordingExecutionStatus = "starting";
        f.queryCallback.onSuccess(starting); assertEquals(0, f.successes);
        f.tick(2000); f.queryCallback.onSuccess(f.task("1", 1));
        assertEquals(1, f.successes); assertTrue(f.pending.isEmpty());
    }
    @Test public void pausedOrEndedTaskFailsAndStopsPolling() {
        for(String state : new String[]{"2", "3"}) {
            Fixture f = new Fixture(); f.starter.start("match"); f.respond(202); f.tick(2000);
            f.queryCallback.onSuccess(f.task(state, 1));
            assertEquals(1, f.failures); assertEquals(0, f.successes); assertTrue(f.pending.isEmpty());
        }
    }
    @Test public void timeoutIgnoresLateReply() {
        Fixture f = new Fixture(); f.starter.start("match"); f.respond(202); f.tick(2000);
        f.tick(180000); f.queryCallback.onSuccess(f.task("1", 1));
        assertEquals(1, f.failures); assertEquals(0, f.successes); assertTrue(f.pending.isEmpty());
    }
    @Test public void cancelIgnoresInFlightStartAndDuplicateStartIsSuppressed() {
        Fixture f = new Fixture(); f.starter.start("match"); f.starter.start("match");
        assertEquals(1, f.starts); f.starter.cancel(); f.respond(200);
        assertEquals(0, f.successes); assertEquals(0, f.failures); assertTrue(f.pending.isEmpty());
    }
    @Test public void failureShowsServerReason() {
        Fixture f = new Fixture(); f.starter.start("match"); f.startCallback.onFail("501", "设备不在线");
        assertEquals("设备不在线", f.error); assertEquals(1, f.failures); assertTrue(f.pending.isEmpty());
    }
    @Test public void failedExecutionOrDifferentTaskCannotReportSuccess() {
        Fixture f = new Fixture(); f.starter.start("match"); f.respond(202); f.tick(2000);
        HmCompetitionLive task = f.task("1", 1); task.recordingExecutionStatus = "failed";
        f.queryCallback.onSuccess(task); assertEquals(1, f.failures);
        Fixture other = new Fixture(); other.starter.start("match"); other.respond(202); other.tick(2000);
        task = other.task("1", 1); task.competitionId = "other";
        other.queryCallback.onSuccess(task); assertEquals(1, other.failures);
    }
    @Test public void transientQueryFailureRetriesWithoutAnotherStartRequest() {
        Fixture f = new Fixture(); f.starter.start("match"); f.respond(202); f.tick(2000);
        f.queryCallback.onFail("-1", "timeout"); f.tick(2000); f.queryCallback.onSuccess(f.task("1", 1));
        assertEquals(1, f.starts); assertEquals(2, f.queries); assertEquals(1, f.successes);
    }
    @Test public void protocolMetadataKeepsOlderCamerasOnLegacyPath() {
        Gson gson = new Gson();
        assertEquals(3, gson.fromJson("{\"recordingProtocolVersion\":3,\"st\":0}", RecordStatusResultBean.class).recordingProtocolVersion);
        assertEquals(0, gson.fromJson("{\"st\":0}", RecordStatusResultBean.class).recordingProtocolVersion);
    }
}
