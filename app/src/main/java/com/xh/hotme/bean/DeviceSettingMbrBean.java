package com.xh.hotme.bean;


import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class DeviceSettingMbrBean implements Serializable {

    public int motion_type;
    public int place_type;
    public MbrBean mbr;

    /**
     * avs_video_mbr : 8192
     * fec_video_mbr : 4096
     * local_rtmp_mbr : 4096
     * live_mbr : 4096
     */

    public class MbrBean implements Serializable {
        public int avs_video_mbr;
        public int fec_video_mbr;
        public int local_rtmp_mbr;
        public int live_mbr;
    }

}
