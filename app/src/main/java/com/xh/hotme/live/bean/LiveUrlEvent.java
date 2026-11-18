package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import com.xh.hotme.utils.Constants;

import java.io.Serializable;

@Keep
public class LiveUrlEvent implements Serializable {
    // true表示使用第三方平台, 第三方平台是直接推送到第三方服务器, 不经过华为云
    public boolean third_party = false;
    public int third_plt = Constants.LIVE_PLATFORM_XQM;
    public String url_push = null;
    public String url_pull = null;
    public String url_pull_commentary = null;
    public String url_push_commentary = null;
    public boolean commentary = false;
}
