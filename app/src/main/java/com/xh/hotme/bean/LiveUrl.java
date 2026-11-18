package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class LiveUrl implements Serializable {
    // srs rtmp lib error code
    public static final int ERROR_OK = 0;
    public static final int ERROR_SOCKET_WRITE = 1009;
    public static final int ERROR_SOCKET_WAIT = 1010;
    public static final int ERROR_SOCKET_TIMEOUT = 1011;
    public static final int ERROR_SOCKET_CONNECT = 1012;
    public static final int ERROR_H264_DROP_BEFORE_SPS_PPS = 3043;
    public static final int ERROR_H264_DUPLICATED_SPS = 3044;
    public static final int ERROR_H264_DUPLICATED_PPS = 3045;

    public String url;
    public String key;
    public int plt;
    public int st;
    public int err_code;
}
