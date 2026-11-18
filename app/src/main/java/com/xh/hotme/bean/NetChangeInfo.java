package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/**
 * 网络状态状态
 */
@Keep
public class NetChangeInfo implements Serializable {
    public int sim;
    public int wifi;

}
