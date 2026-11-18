package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class LiveStatusResultBean implements Serializable {
    public int avs_live;
    public int avs_timeout;
    public int fec_live;
    public int fec_timeout;
    public List<LiveUrl> avs_live_urls;
    public List<LiveUrl> fec_live_urls;
}
