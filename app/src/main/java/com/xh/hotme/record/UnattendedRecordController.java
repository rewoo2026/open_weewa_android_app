package com.xh.hotme.record;

import com.xh.hotme.bean.RecordStatusResultBean;

/** Checks within the window and stops at its deadline, including late start replies. */
final class UnattendedRecordController {
    static final long POLL_INTERVAL_MS = 30_000L;
    static final long STOP_INTERVAL_MS = 2_000L;
    interface Port {
        boolean connected();
        boolean query();
        boolean start();
        boolean stop();
        void finished();
    }
    private final Port port;
    private final long startAt, endAt;
    private boolean active = true, waitingForStatus, ending;
    private long nextCheckAt, replyBefore;

    UnattendedRecordController(Port port, long startAt, long endAt) {
        this.port = port; this.startAt = startAt; this.endAt = endAt;
    }
    void cancel() { active = false; waitingForStatus = false; }
    void tick(long now) {
        if (!active) return;
        if (!ending && now >= endAt) {
            ending = true;
            nextCheckAt = 0;
        }
        if ((!ending && now < startAt) || now < nextCheckAt) return;
        nextCheckAt = now + (ending ? STOP_INTERVAL_MS : POLL_INTERVAL_MS);
        waitingForStatus = false;
        // Also cancel a pending local start when the transport has disconnected.
        if (ending) port.stop();
        if (!port.connected()) return;
        replyBefore = now + 15_000L;
        waitingForStatus = port.query();
    }

    long nextDelay(long now) {
        if (!active) return POLL_INTERVAL_MS;
        long next = ending ? nextCheckAt : now < startAt ? startAt : Math.min(nextCheckAt, endAt);
        // Recheck the wall clock after sleep or a phone clock change.
        return Math.max(1L, Math.min(1_000L, next - now));
    }

    void status(RecordStatusResultBean status, long now) {
        if (active && !ending && now >= endAt) { tick(now); return; }
        if (!active || !waitingForStatus || now > replyBefore || !port.connected()
                || status == null || !"ok".equalsIgnoreCase(status.getR())) return;
        waitingForStatus = false;
        boolean transitioning = "starting".equals(status.deviceState) || "stopping".equals(status.deviceState);
        if (ending) {
            if (status.getStatus() == 0 && !transitioning) {
                active = false;
                port.finished();
            } else port.stop();
            return;
        }
        // An existing start/stop request may still be executing on newer cameras.
        if (now >= startAt && status.getStatus() == 0 && !transitioning) port.start();
    }
}
