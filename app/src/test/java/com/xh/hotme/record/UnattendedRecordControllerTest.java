package com.xh.hotme.record;

import com.xh.hotme.bean.RecordStatusResultBean;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnattendedRecordControllerTest {
    private static final class Fixture implements UnattendedRecordController.Port {
        final UnattendedRecordController controller;
        boolean connected = true, querySent = true, startSent = true;
        int queries, starts, stops, finishes;
        Fixture() { this(0, Long.MAX_VALUE); }
        Fixture(long startAt, long endAt) { controller = new UnattendedRecordController(this, startAt, endAt); }
        public boolean connected() { return connected; }
        public boolean query() { queries++; return querySent; }
        public boolean start() { starts++; return startSent; }
        public boolean stop() { stops++; return connected; }
        public void finished() { finishes++; }
        RecordStatusResultBean status(int recording) {
            RecordStatusResultBean status = new RecordStatusResultBean();
            status.setR("ok"); status.setStatus(recording);
            return status;
        }
        void poll(long now) { controller.tick(now); }
        void reply(RecordStatusResultBean status, long now) { controller.status(status, now); }
    }
    @Test public void checksAtThirtySecondIntervals() {
        Fixture f = new Fixture(); f.poll(100_000); assertEquals(1, f.queries);
        f.reply(f.status(1), 100_001);
        f.poll(129_999); assertEquals(1, f.queries);
        f.poll(130_000); assertEquals(2, f.queries);
    }
    @Test public void ordinaryIdleReplyStartsWithoutAnyNewFirmwareFields() {
        Fixture f = new Fixture(); f.poll(100_000); f.reply(f.status(0), 100_001);
        assertEquals(1, f.starts);
    }
    @Test public void recordingReplyNeverStartsAgain() {
        Fixture f = new Fixture(); f.poll(100_000); f.reply(f.status(1), 100_001);
        f.poll(130_000); f.reply(f.status(1), 130_001);
        assertEquals(0, f.starts);
    }
    @Test public void failedNullAndExpiredRepliesDoNotStartRecording() {
        Fixture f = new Fixture(); f.poll(100_000);
        RecordStatusResultBean bad = f.status(0); bad.setR("failed");
        f.reply(bad, 100_001); f.reply(null, 100_002);
        assertEquals(0, f.starts);
        f.reply(f.status(0), 116_000); assertEquals(0, f.starts);
    }
    @Test public void onlyOneStartPerPoll() {
        Fixture f = new Fixture(); f.poll(100_000);
        f.reply(f.status(0), 100_001); f.reply(f.status(0), 100_002);
        assertEquals(1, f.starts);
        f.poll(130_000); f.reply(f.status(0), 130_001);
        assertEquals(2, f.starts);
    }
    @Test public void disconnectionDefersUntilNextCheck() {
        Fixture f = new Fixture(); f.connected = false; f.poll(100_000);
        assertEquals(0, f.queries); assertEquals(0, f.starts);
        f.connected = true; f.poll(129_999); assertEquals(0, f.queries);
        f.poll(130_000); f.reply(f.status(0), 130_001);
        assertEquals(1, f.starts);
    }
    @Test public void failedQueryDoesNotAcceptAnUnsolicitedIdleReply() {
        Fixture f = new Fixture(); f.querySent = false; f.poll(100_000);
        f.reply(f.status(0), 100_001); assertEquals(0, f.starts);
    }
    @Test public void cancellingPreventsPollingAndLateReplyRestart() {
        Fixture f = new Fixture(); f.poll(100_000); f.controller.cancel();
        f.reply(f.status(0), 100_001); f.poll(130_000);
        assertEquals(1, f.queries); assertEquals(0, f.starts);
    }
    @Test public void inFlightStartOrStopDoesNotCreateDuplicateRecording() {
        Fixture f = new Fixture(); f.poll(100_000);
        RecordStatusResultBean status = f.status(0); status.deviceState = "starting";
        f.reply(status, 100_001);
        f.poll(130_000); status.deviceState = "stopping"; f.reply(status, 130_001);
        assertEquals(0, f.starts);
    }
    @Test public void waitsUntilStartAndStartsAtTheBoundary() {
        Fixture f = new Fixture(100_000, 200_000);
        f.poll(99_999); f.reply(f.status(0), 99_999);
        assertEquals(0, f.queries); assertEquals(0, f.starts);
        assertEquals(1, f.controller.nextDelay(99_999));
        f.poll(100_000); f.reply(f.status(0), 100_000);
        assertEquals(1, f.starts);
    }
    @Test public void endBoundaryStopsWithoutWaitingForNextThirtySecondPoll() {
        Fixture f = new Fixture(100_000, 110_000);
        f.poll(100_000); f.reply(f.status(1), 100_001);
        f.poll(109_999); assertEquals(0, f.stops);
        f.poll(110_000); assertEquals(1, f.stops); assertEquals(2, f.queries);
        f.reply(f.status(0), 110_001); assertEquals(1, f.finishes);
        f.poll(140_000); f.reply(f.status(0), 140_001); assertEquals(0, f.starts);
    }
    @Test public void lateIdleReplyAtEndCannotStart() {
        Fixture f = new Fixture(100_000, 110_000);
        f.poll(100_000); f.reply(f.status(0), 110_000);
        assertEquals(0, f.starts); assertEquals(1, f.stops);
    }
    @Test public void retriesFailedStartsAndInterruptionsOnlyInsideWindow() {
        Fixture f = new Fixture(100_000, 170_000); f.startSent = false;
        f.poll(100_000); f.reply(f.status(0), 100_001);
        f.startSent = true; f.poll(130_000); f.reply(f.status(0), 130_001);
        f.poll(160_000); f.reply(f.status(0), 160_001);
        assertEquals(3, f.starts);
        f.poll(170_000); f.reply(f.status(0), 170_001); assertEquals(3, f.starts);
    }
    @Test public void expiredTaskRestoredAfterRestartOnlyStops() {
        Fixture f = new Fixture(100_000, 110_000);
        f.poll(200_000); f.reply(f.status(1), 200_001);
        assertEquals(0, f.starts); assertEquals(2, f.stops);
        f.poll(202_000); f.reply(f.status(0), 202_001); assertEquals(1, f.finishes);
    }
    @Test public void disconnectedAtDeadlineCancelsStartAndRetriesStopAfterReconnect() {
        Fixture f = new Fixture(100_000, 110_000);
        f.connected = false; f.poll(110_000);
        assertEquals(1, f.stops); assertEquals(0, f.finishes);
        f.connected = true; f.poll(112_000); f.reply(f.status(0), 112_001);
        assertEquals(2, f.stops); assertEquals(1, f.finishes); assertEquals(0, f.starts);
    }
    @Test public void endingIsNotFinishedWhileAStartOrStopIsStillExecuting() {
        Fixture f = new Fixture(100_000, 110_000);
        f.poll(110_000); RecordStatusResultBean status = f.status(0); status.deviceState = "starting";
        f.reply(status, 110_001); assertEquals(0, f.finishes);
        f.poll(112_000); status.deviceState = "stopping"; f.reply(status, 112_001);
        assertEquals(0, f.finishes); assertEquals(0, f.starts);
    }
    @Test public void clockRollbackCannotReopenAnEndedWindow() {
        Fixture f = new Fixture(100_000, 110_000);
        f.poll(110_000); f.reply(f.status(0), 105_000);
        assertEquals(0, f.starts); assertEquals(1, f.finishes);
    }
}
