package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.SharedPrefs;

@Keep
public class RecordStartRequestBean extends BaseBluetoothBean {
    // Snapshot the analysis choice with this recording, including offline receipts.
    public int analysis_requested;
    int auto_upload = -1, auto_upload_avs = -1, auto_upload_fec = -1;
    int auto_upload_log = -1, auto_upload_crop_buffer = -1;
    public com.xh.hotme.account.DailyAccessManager.OfflinePermit offline_permit;
    public boolean local_control;
    public long device_version, expected_device_version, schedule_version, stop_at, start_before;
    // Optional earlier local deadline; firmware still owns the quota and control version.
    public long record_until;
    public String session_id, operation_id;
    String cn ; //赛事名称
    String vn ; //视频名称
    long mbr;  //最大码率
    String au;  //拍摄者
    int vw;  //视频宽度
    int vh;  //视频高度
    String vs;   //场馆
    long vt;   //视频时间
    String city;   //城市
    String place_type;   //场地类型
    int motion_type;  //运动类型
    String host_team;   //主队名称
    String guest_team;  //客队名称
    int append;   //扩展录制
    int player_num;    //参赛人数
    int avs_mbr;
    String province_code;   //省份 code
    String city_code;    //市级编码
    int disable_avs;  // disable_avs 0/1, 默认或者不传是0, 所以默认不传就是avs录制打开的
    String live_cid;   //比赛id
    int ctype;  //比赛类型
    int live_mbr;  //直播码率
    int hide_osd;   //是否隐藏osd
    int no_preview;   //是否关闭预览

    int avsw;  //avs 宽
    int avsh;  //avs 高

    int pause_time;  //暂停计时

    public RecordStartRequestBean() {
        this.c = BleConstants.BLE_CMD_RECORDER_START;
        this.analysis_requested = CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS) ? 1 : 0;
        this.disable_avs = analysis_requested == 1 || CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_ENABLE_AVS) ? 0 : 1;
        if (analysis_requested == 1) {
            auto_upload = auto_upload_avs = auto_upload_fec = auto_upload_log = 1;
            auto_upload_crop_buffer = 0;
        }
        this.au = "";
        this.cn = "";
        this.live_cid = "";
        this.province_code = "";
        this.city_code = "";
        this.host_team ="";
        this.guest_team ="";
        this.vs ="";
        this.no_preview = 0;
        this.avsw = 7600;
        this.avsh = 1080;

        int index = SharedPrefs.getImpl().getInt(SharedPrefs.PREF_AVS_RESOLUTION);
        String[] resolutionArray = HotmeApplication.getContext().getResources().getStringArray(R.array.avs_resolution);
        if(index < resolutionArray.length){
            String resolutionStr = resolutionArray[index];
            String [] resolutions = resolutionStr.split("x");
            try{
                this.avsw =Integer.parseInt(resolutions[0]) ;
                this.avsh =  Integer.parseInt(resolutions[1]) ;

            } catch (Throwable e){

            }
        }
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getVn() {
        return vn;
    }

    public void setVn(String vn) {
        this.vn = vn;
    }

    public long getMbr() {
        return mbr;
    }

    public void setMbr(long mbr) {
        this.mbr = mbr;
    }

    public String getAu() {
        return au;
    }

    public void setAu(String au) {
        this.au = au;
    }

    public int getVw() {
        return vw;
    }

    public void setVw(int vw) {
        this.vw = vw;
    }

    public int getVh() {
        return vh;
    }

    public void setVh(int vh) {
        this.vh = vh;
    }

    public String getVs() {
        return vs;
    }

    public void setVs(String vs) {
        this.vs = vs;
    }

    public long getVt() {
        return vt;
    }

    public void setVt(long vt) {
        this.vt = vt;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPlace_type() {
        return place_type;
    }

    public void setPlace_type(String place_type) {
        this.place_type = place_type;
    }

    public int getMotion_type() {
        return motion_type;
    }

    public void setMotion_type(int motion_type) {
        this.motion_type = motion_type;
    }

    public String getHost_team() {
        return host_team;
    }

    public void setHost_team(String host_team) {
        this.host_team = host_team;
    }

    public String getGuest_team() {
        return guest_team;
    }

    public void setGuest_team(String guest_team) {
        this.guest_team = guest_team;
    }

    public int getAppend() {
        return append;
    }

    public void setAppend(int append) {
        this.append = append;
    }

    public int getPlayer_num() {
        return player_num;
    }

    public void setPlayer_num(int player_num) {
        this.player_num = player_num;
    }

    public int getAvs_mbr() {
        return avs_mbr;
    }

    public void setAvs_mbr(int avs_mbr) {
        this.avs_mbr = avs_mbr;
    }

    public String getProvince_code() {
        return province_code;
    }

    public void setProvince_code(String province_code) {
        this.province_code = province_code;
    }

    public String getCity_code() {
        return city_code;
    }

    public void setCity_code(String city_code) {
        this.city_code = city_code;
    }

    public int getDisable_avs() {
        return disable_avs;
    }

    public void setDisable_avs(int disable_avs) {
        this.disable_avs = disable_avs;
    }

    public String getLive_cid() {
        return live_cid;
    }

    public void setLive_cid(String live_cid) {
        this.live_cid = live_cid;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public int getLive_mbr() {
        return live_mbr;
    }

    public void setLive_mbr(int live_mbr) {
        this.live_mbr = live_mbr;
    }

    public int getHide_osd() {
        return hide_osd;
    }

    public void setHide_osd(int hide_osd) {
        this.hide_osd = hide_osd;
    }

    public int getNo_preview() {
        return no_preview;
    }

    public void setNo_preview(int no_preview) {
        this.no_preview = no_preview;
    }

    public int getPauseTime() {
        return pause_time;
    }

    public void setPauseTime(int pause_time) {
        this.pause_time = pause_time;
    }
}
