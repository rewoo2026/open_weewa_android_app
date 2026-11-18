package com.xh.hotme.bluetooth.event;
public final class CameraVideoDeleteResultEvent {
    public final String requestId,deviceId,path,result;
    public CameraVideoDeleteResultEvent(String requestId,String deviceId,String path,String result) {
        this.requestId=requestId;this.deviceId=deviceId;this.path=path;this.result=result;
    }
}
