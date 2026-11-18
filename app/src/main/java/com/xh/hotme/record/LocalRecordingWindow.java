package com.xh.hotme.record;

/** Phone wall-clock window; the end instant is reserved for stopping. */
final class LocalRecordingWindow {
    static final long DEFAULT_DURATION_MS = 2 * 60 * 60 * 1000L;
    final long startAt, endAt;

    private LocalRecordingWindow(long startAt, long endAt) {
        if (endAt <= startAt) throw new IllegalArgumentException("End must follow start");
        this.startAt = startAt;
        this.endAt = endAt;
    }

    static LocalRecordingWindow resolve(Long start, Long end, long now) {
        long startAt = start == null ? now : start;
        return new LocalRecordingWindow(startAt, end == null
                ? Math.addExact(startAt, DEFAULT_DURATION_MS) : end);
    }

    boolean contains(long now) { return now >= startAt && now < endAt; }
}
