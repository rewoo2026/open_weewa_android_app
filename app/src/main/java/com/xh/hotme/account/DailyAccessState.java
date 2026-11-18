package com.xh.hotme.account;

/** A successful server exchange is required before offline use of this account. */
public class DailyAccessState {
    public String account;
    public long serverTime, refreshAt, savedWall, savedElapsed;

    public boolean ready(String currentAccount, long wall, long elapsed) {
        if (account == null || !account.equals(currentAccount) || refreshAt <= serverTime
                || serverTime <= 0 || wall < savedWall) return false;
        long delta;
        if (elapsed >= savedElapsed) {
            delta = elapsed - savedElapsed;
            if (Math.abs((wall - savedWall) - delta) > 120000) return false;
        } else {
            // A reboot may reset elapsedRealtime; never extend the signed day.
            delta = wall - savedWall;
        }
        return delta >= 0 && delta < refreshAt - serverTime;
    }
}
