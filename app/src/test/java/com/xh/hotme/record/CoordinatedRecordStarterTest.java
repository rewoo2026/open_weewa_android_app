package com.xh.hotme.record;

import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class CoordinatedRecordStarterTest {
    private static class Fixture implements CoordinatedRecordStarter.Backend,
            CoordinatedRecordStarter.Timer, CoordinatedRecordStarter.Listener {
        IGenericApiCallback<CommonResponse> start;
        IGenericApiCallback<HmCompetitionRecordBean> query;
        Map<Runnable, Long> timers = new LinkedHashMap<>();
        int requests, successes, failures, cancellations;
        String error;
        boolean delayCancel;
        Runnable cancelled;
        CoordinatedRecordStarter starter = new CoordinatedRecordStarter(this, this, this);
        public void start(String id, IGenericApiCallback<CommonResponse> callback) { requests++; start = callback; }
        public void query(String id, IGenericApiCallback<HmCompetitionRecordBean> callback) { query = callback; }
        public void cancel(String id, Runnable finished) {
            assertEquals("record", id); cancellations++; cancelled = finished;
            if (!delayCancel) finished.run();
        }
        public void post(Runnable action, long delay) { timers.put(action, delay); }
        public void remove(Runnable action) { timers.remove(action); }
        public void onStarted() { successes++; }
        public void onFailure(String message) { failures++; error = message; }
        void respond(int code) { CommonResponse r = new CommonResponse(); r.code = code; start.onSuccess(r); }
        void tick(long delay) {
            Runnable action = timers.entrySet().stream().filter(e -> e.getValue() == delay)
                    .findFirst().get().getKey();
            timers.remove(action); action.run();
        }
        HmCompetitionRecordBean task(int status, String execution) {
            HmCompetitionRecordBean task = new HmCompetitionRecordBean();
            task.competitionId = "record"; task.competitionStatus = status;
            task.recordingExecutionStatus = execution; return task;
        }
    }

    @Test public void pendingStartWaitsForConfirmedExecution() {
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(202);
        assertEquals(0, f.successes);
        f.tick(2000); f.query.onSuccess(f.task(1, "starting"));
        assertEquals(0, f.successes);
        f.tick(2000); f.query.onSuccess(f.task(1, null));
        assertEquals(0, f.successes);
        f.tick(2000); f.query.onSuccess(f.task(1, "recording"));
        assertEquals(1, f.successes); assertTrue(f.timers.isEmpty());
        assertEquals(0, f.cancellations);
    }
    @Test public void confirmedStartCompletesOnce() {
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(200); f.respond(200);
        assertEquals(1, f.successes); assertTrue(f.timers.isEmpty());
    }
    @Test public void duplicateClickAndCancelledReplyCannotStartAgain() {
        Fixture f = new Fixture(); f.starter.start("record"); f.starter.start("record");
        assertEquals(1, f.requests); f.starter.cancel(); f.respond(200);
        assertEquals(0, f.successes); assertTrue(f.timers.isEmpty());
        assertEquals(1, f.cancellations);
    }
    @Test public void endedPausedOrFailedTaskDoesNotSucceed() {
        for (int status : new int[]{2, 3}) {
            Fixture f = new Fixture(); f.starter.start("record"); f.respond(202); f.tick(2000);
            f.query.onSuccess(f.task(status, "recording"));
            assertEquals(1, f.failures); assertTrue(f.timers.isEmpty());
        }
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(202); f.tick(2000);
        f.query.onSuccess(f.task(1, "failed")); assertEquals(1, f.failures);
    }
    @Test public void differentTaskCannotConfirmThisStart() {
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(202); f.tick(2000);
        HmCompetitionRecordBean other = f.task(1, "recording"); other.competitionId = "other";
        f.query.onSuccess(other); assertEquals(1, f.failures); assertEquals(0, f.successes);
    }
    @Test public void timeoutDiscardsLateConfirmation() {
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(202); f.tick(2000);
        f.tick(180000); f.query.onSuccess(f.task(1, "recording"));
        assertEquals(1, f.failures); assertEquals(0, f.successes); assertTrue(f.timers.isEmpty());
        assertEquals(1, f.cancellations);
    }
    @Test public void networkRetryDoesNotRepeatStart() {
        Fixture f = new Fixture(); f.starter.start("record"); f.respond(202); f.tick(2000);
        f.query.onFail("-1", "timeout"); f.tick(2000); f.query.onSuccess(f.task(1, "recording"));
        assertEquals(1, f.requests); assertEquals(1, f.successes);
    }
    @Test public void rejectionKeepsServerMessage() {
        Fixture f = new Fixture(); f.starter.start("record"); f.start.onFail("403", "录制次数不足");
        assertEquals("录制次数不足", f.error); assertEquals(1, f.failures); assertTrue(f.timers.isEmpty());
        assertEquals(0, f.cancellations);
    }
    @Test public void transportFailureCancelsPotentiallyAcceptedStart() {
        Fixture f = new Fixture(); f.starter.start("record"); f.start.onFail("-1", "timeout");
        assertEquals(1, f.failures); assertEquals(1, f.cancellations);
        f.respond(200); assertEquals(0, f.successes);
    }
    @Test public void cancellationRecoveryWaitsForServerResponse() {
        Fixture f = new Fixture(); f.delayCancel = true; f.starter.start("record");
        int[] recovered = {0};
        f.starter.cancel(() -> recovered[0]++); f.starter.cancel(() -> recovered[0]++);
        assertEquals(1, f.cancellations); assertEquals(0, recovered[0]);
        f.respond(200); assertEquals(0, f.successes);
        f.cancelled.run(); assertEquals(2, recovered[0]);
        f.starter.start("record"); assertEquals(1, f.requests);
    }
}
