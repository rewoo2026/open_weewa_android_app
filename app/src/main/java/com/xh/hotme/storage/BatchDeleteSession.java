package com.xh.hotme.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One camera, one in-flight request. An uncertain reply must be reconciled before retry. */
public final class BatchDeleteSession implements Serializable {
    private static final long serialVersionUID=1L;
    public enum State { IDLE, RUNNING, PAUSED, FINISHED }
    public enum Outcome { PENDING, IN_FLIGHT, SUCCESS, FAILED, UNKNOWN }
    public static final class Item implements Serializable {
        private static final long serialVersionUID=1L;
        public final String path, title, identity;
        public String requestId, reason = "";
        public Outcome outcome = Outcome.PENDING;
        public Item(String path, String title) { this(path,title,""); }
        public Item(String path,String title,String identity){this.path=path;this.title=title;this.identity=identity;}
    }
    public final List<Item> items = new ArrayList<>();
    public String cameraId = "";
    public State state = State.IDLE;
    public boolean stopRequested;

    public void start(String camera, Collection<Item> chosen) {
        if (state == State.RUNNING || hasUnknown()) throw new IllegalStateException("Unfinished deletion");
        if (camera == null || camera.isEmpty() || chosen.isEmpty()) throw new IllegalArgumentException("Empty selection");
        LinkedHashMap<String,Item> unique=new LinkedHashMap<>();
        for (Item item:chosen) {
            if (item.path == null || item.path.isEmpty()) throw new IllegalArgumentException("Empty path");
            unique.put(item.path,new Item(item.path,item.title,item.identity));
        }
        items.clear(); items.addAll(unique.values()); cameraId=camera; stopRequested=false; state=State.RUNNING;
    }
    public Item current() {
        for (Item item:items) if (item.outcome == Outcome.IN_FLIGHT) return item;
        return null;
    }
    public Item next() {
        if (state != State.RUNNING || current()!=null) return null;
        if (!stopRequested) for (Item item:items) if (item.outcome == Outcome.PENDING) {
            item.outcome=Outcome.IN_FLIGHT; item.requestId=UUID.randomUUID().toString(); return item;
        }
        state=State.FINISHED; return null;
    }
    public boolean reply(String camera, String request, String path, boolean ok, String reason) {
        Item item=current();
        if (state!=State.RUNNING || item==null || !cameraId.equals(camera)
                || !item.requestId.equals(request) || !item.path.equals(path)) return false;
        item.outcome=ok?Outcome.SUCCESS:Outcome.FAILED; item.reason=ok?"":reason;
        return true;
    }
    public void requestStop() { stopRequested=true; if (current()==null && state==State.RUNNING) state=State.FINISHED; }
    public void pause(String reason) {
        if (state!=State.RUNNING) return;
        Item item=current();
        if (item!=null) { item.outcome=Outcome.UNKNOWN; item.reason=reason; }
        stopRequested=true; state=State.PAUSED;
    }
    public void reconcile(String camera, Set<String> existingPaths) {
        if (!cameraId.equals(camera) || state==State.RUNNING) return;
        for (Item item:items) if (item.outcome==Outcome.UNKNOWN) {
            item.outcome=existingPaths.contains(item.path)?Outcome.FAILED:Outcome.SUCCESS;
            item.reason=item.outcome==Outcome.SUCCESS?"":"核对后比赛仍在相机中";
        }
        if (state==State.PAUSED) state=State.FINISHED;
    }
    public boolean hasUnknown() { for(Item i:items) if(i.outcome==Outcome.UNKNOWN)return true; return false; }
    public int count(Outcome outcome) { int count=0; for(Item i:items)if(i.outcome==outcome)count++;return count; }
    public Set<String> remainingPaths() {
        Set<String> paths=new LinkedHashSet<>();for(Item i:items)if(i.outcome!=Outcome.SUCCESS)paths.add(i.path);return paths;
    }
}
