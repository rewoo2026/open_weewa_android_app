package com.xh.hotme.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.bean.GetEthernetSettingsResponseBean;

import java.io.Serializable;

@Keep
public class WeewaInfo implements Serializable {
    public DeviceInfo d;
    public NetworkBean n;
    public GetEthernetSettingsResponseBean ethbean;
    public long _t;
}
