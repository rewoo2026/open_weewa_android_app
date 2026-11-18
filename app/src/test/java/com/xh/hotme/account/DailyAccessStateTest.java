package com.xh.hotme.account;
import org.junit.Test;
import static org.junit.Assert.*;
public class DailyAccessStateTest {
    private DailyAccessState state(){DailyAccessState s=new DailyAccessState();s.account="user-token";s.serverTime=1800000000000L;s.refreshAt=s.serverTime+10000;s.savedWall=s.serverTime;s.savedElapsed=5000;return s;}
    @Test public void validatedDayWorksWithoutServer(){DailyAccessState s=state();assertTrue(s.ready(s.account,s.savedWall+9999,14999));}
    @Test public void midnightRequiresNewExchange(){DailyAccessState s=state();assertFalse(s.ready(s.account,s.savedWall+10000,15000));}
    @Test public void changingAccountOrTokenInvalidatesSnapshot(){DailyAccessState s=state();assertFalse(s.ready("other-token",s.savedWall,5000));}
    @Test public void failedOrMissingExchangeNeverAdmits(){assertFalse(new DailyAccessState().ready("",1800000000000L,1));}
    @Test public void clockRollbackCannotExtendPermission(){DailyAccessState s=state();assertFalse(s.ready(s.account,s.savedWall-1,5001));}
    @Test public void largeClockJumpRequiresExchange(){DailyAccessState s=state();s.refreshAt+=86400000;assertFalse(s.ready(s.account,s.savedWall+130000,5001));}
    @Test public void rebootDoesNotRestartSignedDay(){DailyAccessState s=state();assertTrue(s.ready(s.account,s.savedWall+5000,1));assertFalse(s.ready(s.account,s.savedWall+10001,1));}
}
