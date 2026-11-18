package com.xh.hotme.record;

import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.record.bean.LocalRecordingGrant;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class LocalRecordStarterTest {
    static class Fixture implements LocalRecordStarter.Backend, LocalRecordStarter.Device,
            LocalRecordStarter.Timer, LocalRecordStarter.Listener {
        IGenericApiCallback<LocalRecordingGrant> prepare;
        Map<Runnable,Long> timers = new LinkedHashMap<>();
        String requestId, error; int starts, stops, queries, reports, successes, failures, cancels, detached;
        boolean connected = true, delayCancel;
        long receivedVersion;
        Runnable cancelled;
        LocalRecordStarter starter = new LocalRecordStarter(this, this, this, this, () -> detached++);
        public void prepare(String id, long version, String requestId, IGenericApiCallback<LocalRecordingGrant> callback) {
            assertEquals("record", id); assertEquals(40, version); this.requestId = requestId; prepare = callback;
        }
        public void cancel(LocalRecordingGrant grant, Runnable finished) {
            assertEquals("record", grant.live_cid); cancels++; cancelled = finished; if (!delayCancel) finished.run();
        }
        public void report(RecordStatusResultBean status) { reports++; }
        public boolean start(LocalRecordingGrant grant) { starts++; receivedVersion = grant.device_version; return connected; }
        public boolean query() { queries++; return connected; }
        public boolean stop(LocalRecordingGrant grant) { assertEquals("record", grant.live_cid); assertEquals("session", grant.session_id); stops++; return connected; }
        public void post(Runnable action, long delay) { timers.put(action, delay); }
        public void remove(Runnable action) { timers.remove(action); }
        public void onStarted() { successes++; }
        public void onFailure(String message) { failures++; error = message; }
        public void onWarning(String message) { error = message; }
        void begin() { starter.start("record",40); }
        LocalRecordingGrant grant() {
            LocalRecordingGrant g = new LocalRecordingGrant();
            g.c="record_start"; g.ctype=1; g.local_control=true; g.live_cid=g.competition_id="record";
            g.session_id="session"; g.operation_id="server-operation"; g.request_id=requestId;
            g.device_version=48; g.expected_device_version=40; g.schedule_version=1;
            g.start_before=System.currentTimeMillis()+60000; g.stop_at=System.currentTimeMillis()+120000;
            return g;
        }
        RecordStatusResultBean status(int value, String state, String session, int frame) {
            RecordStatusResultBean s = new RecordStatusResultBean(); s.setR("ok"); s.setStatus(value);
            s.competitionId="record"; s.sessionId=session; s.firstFrame=frame; s.localControl=1;
            s.deviceState=state; s.deviceVersion=48; return s;
        }
        void tick(long delay) {
            Runnable action = timers.entrySet().stream().filter(e -> e.getValue()==delay).findFirst().get().getKey();
            timers.remove(action); action.run();
        }
    }
    @Test public void serverGrantIsRelayedWithoutInventingASequence() {
        Fixture f=new Fixture(); f.begin(); f.prepare.onSuccess(f.grant());
        assertEquals(48,f.receivedVersion); assertEquals(1,f.starts); assertEquals(0,f.successes);
        f.starter.onStatus(f.status(1,"starting","session",0)); assertEquals(0,f.successes);
        f.starter.onStatus(f.status(1,"recording","session",1));
        assertEquals(1,f.successes); assertEquals(1,f.reports); assertTrue(f.timers.isEmpty());
        f.starter.cancel(); assertEquals(0,f.stops); assertEquals(0,f.cancels);
    }
    @Test public void quotaRejectionNeverTouchesCameraRecording() {
        Fixture f=new Fixture(); f.begin(); f.prepare.onFail("602","录制剩余次数不足");
        assertEquals("录制剩余次数不足",f.error); assertEquals(1,f.failures);
        assertEquals(0,f.starts); assertEquals(0,f.stops); assertEquals(0,f.cancels); assertEquals(1,f.detached);
    }
    @Test public void canceledHttpResponseCannotStartLater() {
        Fixture f=new Fixture(); f.begin(); int[] finished={0};
        f.starter.cancel(() -> finished[0]++); assertEquals(1,finished[0]);
        f.prepare.onSuccess(f.grant()); assertEquals(0,f.starts); assertEquals(1,f.cancels);
    }
    @Test public void cancelWaitsForDeviceIdleAndServerAck() {
        Fixture f=new Fixture(); f.begin(); f.prepare.onSuccess(f.grant()); f.delayCancel=true;
        int[] finished={0}; f.starter.cancel(() -> finished[0]++);
        assertEquals(1,f.stops); assertEquals(0,finished[0]);
        f.starter.onStatus(f.status(0,"idle","",0)); f.starter.onStatus(f.status(0,"idle","",0));
        assertEquals(1,f.cancels); assertEquals(0,finished[0]);
        f.cancelled.run(); assertEquals(1,finished[0]); assertTrue(f.timers.isEmpty());
    }
    @Test public void otherSessionAndUnconfirmedFrameCannotComplete() {
        Fixture f=new Fixture(); f.begin(); f.prepare.onSuccess(f.grant());
        f.starter.onStatus(f.status(1,"recording","other",1));
        f.starter.onStatus(f.status(1,"recording","session",0));
        RecordStatusResultBean old=f.status(1,"recording","session",1); old.deviceVersion=39;
        f.starter.onStatus(old); assertEquals(0,f.successes);
    }
    @Test public void timeoutStopsPendingLocalSessionAndIgnoresLateFrame() {
        Fixture f=new Fixture(); f.begin(); f.prepare.onSuccess(f.grant()); f.tick(90000);
        assertEquals(1,f.stops); assertEquals(0,f.failures);
        f.starter.onStatus(f.status(0,"idle","",0)); assertEquals(1,f.failures);
        f.starter.onStatus(f.status(1,"recording","session",1)); assertEquals(0,f.successes);
    }
    @Test public void invalidResponseCannotCancelAnotherTask() {
        Fixture f=new Fixture(); f.begin(); LocalRecordingGrant other=f.grant(); other.request_id="other-request";
        f.prepare.onSuccess(other); assertEquals(1,f.failures); assertEquals(0,f.starts); assertEquals(0,f.cancels);
    }
    @Test public void disconnectBeforeSendCancelsOnlyTheServerGrant() {
        Fixture f=new Fixture(); f.begin(); f.connected=false; f.prepare.onSuccess(f.grant());
        assertEquals(1,f.failures); assertEquals(1,f.cancels); assertEquals(0,f.stops);
    }
}
