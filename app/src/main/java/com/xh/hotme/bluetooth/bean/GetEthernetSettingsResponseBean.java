package com.xh.hotme.bluetooth.bean;
import androidx.annotation.Keep;

import java.io.Serializable;
@Keep
public class GetEthernetSettingsResponseBean implements Serializable {

    public String ip;
    public String dns;
    public String netmask;
    public String gateway;
    public int dhcp;
}
