package com.xh.hotme.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Serializes token updates for one local camera connection. Call on the main thread. */
public final class CameraTokenSynchronizer {
    public interface Callback {
        void onResult(boolean success);
    }

    public interface Transport {
        boolean send(String token, Callback callback);
    }

    public interface Scheduler {
        void postDelayed(Runnable task, long delayMillis);
        void remove(Runnable task);
    }

    private final Scheduler scheduler;
    private final List<Callback> waiters = new ArrayList<>();
    private Object connection;
    private Transport transport;
    private String token;
    private String confirmedToken;
    private String attemptedToken;
    private int attempts;
    private int requestId;
    private boolean sending;
    private Runnable timeout;
    private final Runnable retry = this::sendIfNeeded;

    public CameraTokenSynchronizer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setToken(String token) {
        if (token != null && token.trim().isEmpty()) token = null;
        if (Objects.equals(this.token, token)) return;
        this.token = token;
        notifyWaiters(false);
        scheduler.remove(retry);
        attempts = 0;
        if (token == null) {
            cancelRequest();
            confirmedToken = null;
        } else {
            sendIfNeeded();
        }
    }

    public void connected(Object connection, Transport transport) {
        if (!Objects.equals(this.connection, connection)) {
            cancelRequest();
            notifyWaiters(false);
            this.connection = connection;
            confirmedToken = null;
            attempts = 0;
        }
        this.transport = transport;
        sendIfNeeded();
    }

    public void disconnected(Object connection) {
        if (!Objects.equals(this.connection, connection)) return;
        cancelRequest();
        notifyWaiters(false);
        this.connection = null;
        transport = null;
        confirmedToken = null;
    }

    public void sync(Callback callback) {
        if (token == null || transport == null) {
            callback.onResult(false);
        } else if (Objects.equals(token, confirmedToken)) {
            callback.onResult(true);
        } else {
            waiters.add(callback);
            if (!sending) attempts = 0;
            sendIfNeeded();
        }
    }

    private void sendIfNeeded() {
        if (sending || token == null || transport == null
                || Objects.equals(token, confirmedToken)) return;
        if (!Objects.equals(token, attemptedToken)) {
            attemptedToken = token;
            attempts = 0;
        }
        if (attempts >= 3) return;
        scheduler.remove(retry);
        final String sentToken = token;
        final int id = ++requestId;
        sending = true;
        attempts++;
        timeout = () -> complete(id, sentToken, false);
        scheduler.postDelayed(timeout, 20000);
        try {
            if (!transport.send(sentToken, success -> complete(id, sentToken, success))) {
                complete(id, sentToken, false);
            }
        } catch (RuntimeException error) {
            complete(id, sentToken, false);
        }
    }

    private void complete(int id, String sentToken, boolean success) {
        if (!sending || id != requestId) return;
        sending = false;
        scheduler.remove(timeout);
        timeout = null;
        if (success) confirmedToken = sentToken;
        if (!Objects.equals(token, sentToken)) {
            sendIfNeeded();
        } else if (success) {
            notifyWaiters(true);
        } else if (attempts < 3) {
            scheduler.postDelayed(retry, 1000);
        } else {
            notifyWaiters(false);
        }
    }

    private void cancelRequest() {
        requestId++;
        sending = false;
        if (timeout != null) scheduler.remove(timeout);
        timeout = null;
        scheduler.remove(retry);
    }

    private void notifyWaiters(boolean success) {
        List<Callback> callbacks = new ArrayList<>(waiters);
        waiters.clear();
        for (Callback callback : callbacks) callback.onResult(success);
    }
}
