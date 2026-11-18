package com.xh.hotme.record;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalRecordingWindowTest {
    @Test public void blankTimesResolveAtClickWithTwoHourDuration() {
        LocalRecordingWindow window = LocalRecordingWindow.resolve(null, null, 100_000L);
        assertEquals(100_000L, window.startAt);
        assertEquals(7_300_000L, window.endAt);
    }
    @Test public void blankEndUsesSelectedStartIncludingNextDay() {
        long start = 86_400_000L - 60_000L;
        LocalRecordingWindow window = LocalRecordingWindow.resolve(start, null, 100L);
        assertEquals(start + 7_200_000L, window.endAt);
    }
    @Test public void blankStartUsesNowAndPreservesExplicitEnd() {
        LocalRecordingWindow window = LocalRecordingWindow.resolve(null, 200_000L, 100_000L);
        assertEquals(100_000L, window.startAt); assertEquals(200_000L, window.endAt);
    }
    @Test public void explicitWindowIsNotShiftedToNow() {
        LocalRecordingWindow window = LocalRecordingWindow.resolve(100_000L, 200_000L, 150_000L);
        assertFalse(window.contains(99_999)); assertTrue(window.contains(100_000));
        assertTrue(window.contains(199_999)); assertFalse(window.contains(200_000));
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsEndBeforeStart() {
        LocalRecordingWindow.resolve(200_000L, 100_000L, 0L);
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsEmptyWindow() {
        LocalRecordingWindow.resolve(100_000L, 100_000L, 0L);
    }
}
