package com.xh.hotme.account;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xh.hotme.bluetooth.bean.SetTokenRequestBean;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CameraTokenSynchronizerTest {
    private final Clock clock = new Clock();
    private final CameraTokenSynchronizer sync = new CameraTokenSynchronizer(clock);
    private final Sender sender = new Sender();
    private final Object camera = new Object();

    @Test public void loginBeforeConnectionSendsWhenCameraReady() {
        sync.setToken("phone-token");
        assertTrue(sender.tokens.isEmpty());
        sync.connected(camera, sender);
        assertEquals("phone-token", sender.tokens.get(0));
        sender.reply(0, true);
        sync.connected(camera, sender);
        sync.setToken("phone-token");
        assertEquals(1, sender.tokens.size());
    }

    @Test public void connectionBeforeLoginWaitsForPhoneToken() {
        sync.connected(camera, sender);
        sync.setToken("");
        sync.setToken("  ");
        assertTrue(sender.tokens.isEmpty());
        sync.setToken("fresh-token");
        assertEquals("fresh-token", sender.tokens.get(0));
    }

    @Test public void reloginCoalescesChangesAndFinishesWithLatestToken() {
        sync.connected(camera, sender);
        sync.setToken("old");
        sync.setToken("intermediate");
        sync.setToken("latest");
        assertEquals(1, sender.tokens.size());
        sender.reply(0, true);
        assertEquals("latest", sender.tokens.get(1));
        List<Boolean> manual = new ArrayList<>();
        sync.sync(manual::add);
        sender.reply(1, true);
        assertEquals(1, manual.size());
        assertTrue(manual.get(0));
        sender.reply(0, false);
        assertEquals(2, sender.tokens.size());
    }

    @Test public void reconnectResendsAndOldConnectionResponseIsIgnored() {
        sync.setToken("token");
        sync.connected(camera, sender);
        Object replacement = new Object();
        sync.connected(replacement, sender);
        sync.disconnected(camera);
        sender.reply(0, true);
        List<Boolean> result = new ArrayList<>();
        sync.sync(result::add);
        assertTrue(result.isEmpty());
        sender.reply(1, true);
        assertEquals(Boolean.TRUE, result.get(0));
        assertEquals(2, sender.tokens.size());
    }

    @Test public void logoutCancelsPendingRetriesAndNeverSendsEmptyToken() {
        sync.setToken("old");
        sync.connected(camera, sender);
        sync.setToken("");
        sender.reply(0, false);
        clock.advance(100000);
        assertEquals(1, sender.tokens.size());
        sync.setToken("new");
        assertEquals("new", sender.tokens.get(1));
    }

    @Test public void failedCameraResponseRetriesAtMostThreeTimes() {
        sync.setToken("token");
        sync.connected(camera, sender);
        List<Boolean> result = new ArrayList<>();
        sync.sync(result::add);
        for (int i = 0; i < 3; i++) {
            sender.reply(i, false);
            clock.advance(1000);
        }
        assertEquals(3, sender.tokens.size());
        assertEquals(Boolean.FALSE, result.get(0));
        sync.connected(camera, sender);
        clock.advance(100000);
        assertEquals(3, sender.tokens.size());
    }

    @Test public void missingCameraReplyTimesOutAndReleasesManualRequest() {
        sync.setToken("token");
        sync.connected(camera, sender);
        List<Boolean> result = new ArrayList<>();
        sync.sync(result::add);
        clock.advance(70000);
        assertEquals(3, sender.tokens.size());
        assertEquals(Boolean.FALSE, result.get(0));
    }

    @Test public void disconnectedCameraCancelsRetryAndNewSessionRestoresSync() {
        sync.setToken("token");
        sync.connected(camera, sender);
        sender.reply(0, false);
        sync.disconnected(camera);
        clock.advance(100000);
        assertEquals(1, sender.tokens.size());
        sync.connected(new Object(), sender);
        assertEquals(2, sender.tokens.size());
    }

    @Test public void queueRejectionDoesNotReportTokenAsSaved() {
        sender.accepted = false;
        sync.setToken("token");
        sync.connected(camera, sender);
        clock.advance(5000);
        assertEquals(3, sender.tokens.size());
        sender.accepted = true;
        List<Boolean> result = new ArrayList<>();
        sync.sync(result::add);
        assertTrue(result.isEmpty());
        sender.reply(3, true);
        assertEquals(Boolean.TRUE, result.get(0));
    }

    @Test public void synchronousTransportExceptionCanRetry() {
        sender.throwOnSend = true;
        sync.setToken("token");
        sync.connected(camera, sender);
        sender.throwOnSend = false;
        clock.advance(1000);
        assertEquals(2, sender.tokens.size());
        sender.reply(1, true);
        clock.advance(100000);
        assertEquals(2, sender.tokens.size());
    }

    @Test public void unbindWaitsForConfirmationAndCachedConfirmationDoesNotResend() {
        sync.setToken("token");
        sync.connected(camera, sender);
        List<Boolean> result = new ArrayList<>();
        assertTrue(result.isEmpty());
        sync.sync(result::add);
        assertTrue(result.isEmpty());
        sender.reply(0, true);
        assertEquals(Boolean.TRUE, result.get(0));
        sync.sync(result::add);
        assertEquals(2, result.size());
        assertEquals(1, sender.tokens.size());
    }

    @Test public void tokenCommandMatchesCameraProtocol() {
        JsonObject json = new Gson().toJsonTree(new SetTokenRequestBean("synthetic.jwt.token"))
                .getAsJsonObject();
        assertEquals("set_token", json.get("c").getAsString());
        assertEquals("synthetic.jwt.token", json.get("t").getAsString());
        assertEquals(2, json.size());
    }

    private static class Sender implements CameraTokenSynchronizer.Transport {
        final List<String> tokens = new ArrayList<>();
        final List<CameraTokenSynchronizer.Callback> callbacks = new ArrayList<>();
        boolean accepted = true;
        boolean throwOnSend;
        public boolean send(String token, CameraTokenSynchronizer.Callback callback) {
            tokens.add(token);
            callbacks.add(callback);
            if (throwOnSend) throw new IllegalStateException("synthetic transport failure");
            return accepted;
        }
        void reply(int index, boolean success) { callbacks.get(index).onResult(success); }
    }

    private static class Clock implements CameraTokenSynchronizer.Scheduler {
        private static class Task {
            Runnable action;
            long time;
        }
        private long now;
        private final List<Task> tasks = new ArrayList<>();
        public void postDelayed(Runnable action, long delayMillis) {
            Task task = new Task();
            task.action = action;
            task.time = now + delayMillis;
            tasks.add(task);
        }
        public void remove(Runnable action) { tasks.removeIf(task -> task.action == action); }
        void advance(long millis) {
            long end = now + millis;
            while (true) {
                Task next = null;
                for (Task task : tasks) {
                    if (task.time <= end && (next == null || task.time < next.time)) next = task;
                }
                if (next == null) break;
                tasks.remove(next);
                now = next.time;
                next.action.run();
            }
            now = end;
        }
    }
}
