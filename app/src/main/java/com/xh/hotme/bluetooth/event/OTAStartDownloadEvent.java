package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

@Keep
public class OTAStartDownloadEvent {
    public int status;
    public int errorCode;   //1:未知错误 2:相机无网，3：已是最新；4:已经下载完成

    public OTAStartDownloadEvent(int status, int ec) {
        this.status = status;
        this.errorCode = ec;
    }
}
