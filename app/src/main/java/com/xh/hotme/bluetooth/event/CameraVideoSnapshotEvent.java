package com.xh.hotme.bluetooth.event;
import com.xh.hotme.bean.CameraVideoListBean;
public final class CameraVideoSnapshotEvent {
    public final CameraVideoListBean snapshot;
    public CameraVideoSnapshotEvent(CameraVideoListBean snapshot) { this.snapshot=snapshot; }
}
