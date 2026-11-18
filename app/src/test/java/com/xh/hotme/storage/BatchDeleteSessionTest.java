package com.xh.hotme.storage;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
import static com.xh.hotme.storage.BatchDeleteSession.*;

public class BatchDeleteSessionTest {
    private BatchDeleteSession session() { BatchDeleteSession s=new BatchDeleteSession();s.start("camera-a",Arrays.asList(new Item("20261007/0900","A"),new Item("20261007/1000","B")));return s; }
    @Test public void serialQueueAndPartialFailure() { BatchDeleteSession s=session();Item a=s.next();assertNull(s.next());assertTrue(s.reply(s.cameraId,a.requestId,a.path,true,""));Item b=s.next();assertTrue(s.reply(s.cameraId,b.requestId,b.path,false,"uploading"));assertNull(s.next());assertEquals(State.FINISHED,s.state);assertEquals(1,s.count(Outcome.SUCCESS));assertEquals(Collections.singleton(b.path),s.remainingPaths()); }
    @Test public void staleDuplicateAndOtherCameraCannotAdvance() { BatchDeleteSession s=session();Item a=s.next();assertFalse(s.reply("camera-b",a.requestId,a.path,true,""));assertFalse(s.reply(s.cameraId,"old",a.path,true,""));assertFalse(s.reply(s.cameraId,a.requestId,"different",true,""));assertTrue(s.reply(s.cameraId,a.requestId,a.path,true,""));Item b=s.next();assertFalse(s.reply(s.cameraId,a.requestId,a.path,true,""));assertSame(b,s.current()); }
    @Test public void stopFinishesCurrentAndLeavesRest() { BatchDeleteSession s=session();Item a=s.next();s.requestStop();assertEquals(State.RUNNING,s.state);assertTrue(s.reply(s.cameraId,a.requestId,a.path,true,""));assertNull(s.next());assertEquals(1,s.count(Outcome.SUCCESS));assertEquals(1,s.count(Outcome.PENDING)); }
    @Test public void timeoutDoesNotCountAsFailureOrSendNext() { BatchDeleteSession s=session();Item a=s.next();s.pause("timeout");assertEquals(Outcome.UNKNOWN,a.outcome);assertNull(s.next());assertFalse(s.reply(s.cameraId,a.requestId,a.path,true,""));assertTrue(s.hasUnknown());assertEquals(0,s.count(Outcome.SUCCESS)); }
    @Test public void reconcileMissingIsSuccessWithoutAutoResume() { BatchDeleteSession s=session();Item a=s.next();s.pause("disconnect");s.reconcile("other",Collections.emptySet());assertTrue(s.hasUnknown());s.reconcile(s.cameraId,Collections.singleton("20261007/1000"));assertEquals(Outcome.SUCCESS,a.outcome);assertEquals(State.FINISHED,s.state);assertNull(s.next());assertEquals(1,s.count(Outcome.PENDING)); }
    @Test public void reconcilePresentAllowsExplicitRetryWithNewId() { BatchDeleteSession s=session();Item a=s.next();String old=a.requestId;s.pause("disconnect");s.reconcile(s.cameraId,Collections.singleton(a.path));assertEquals(Outcome.FAILED,a.outcome);s.start(s.cameraId,Collections.singleton(a));assertNotEquals(old,s.next().requestId); }
    @Test(expected=IllegalStateException.class) public void cannotRetryUnknown() { BatchDeleteSession s=session();s.next();s.pause("timeout");s.start(s.cameraId,Collections.singleton(new Item("20261007/0900","A"))); }
    @Test public void processDeathPreservesUncertainty() throws Exception { BatchDeleteSession s=session();s.next();ByteArrayOutputStream data=new ByteArrayOutputStream();new ObjectOutputStream(data).writeObject(s);BatchDeleteSession restored=(BatchDeleteSession)new ObjectInputStream(new ByteArrayInputStream(data.toByteArray())).readObject();restored.pause("process restarted");assertTrue(restored.hasUnknown());assertNull(restored.next()); }
    @Test public void duplicatePathsOnlyDeleteOnce() { BatchDeleteSession s=new BatchDeleteSession();s.start("camera-a",Arrays.asList(new Item("same","A"),new Item("same","B")));assertEquals(1,s.items.size()); }
}
