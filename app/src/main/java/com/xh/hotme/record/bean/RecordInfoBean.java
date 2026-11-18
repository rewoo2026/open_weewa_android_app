package com.xh.hotme.record.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.VideoUtil;

import java.io.Serializable;

@Keep
public class RecordInfoBean implements Serializable {
    public String place;
    public String campaignName = ""; // 赛事名称
    public String name;   //录制名称
    public long maxRate;  // 最大码率
    public int width;  // 视频宽度
    public int height;  // 视频高度
    public String author;  // 拍摄作者
    public long time;  //拍摄时间

    public String city = "";  //城市
    public int motionType;  //运动类型
    public String hostTeam = "";  //主队名称
    public String guestTeam = "";  //客队名称
    public int placeType;  //场地类型

    public int hostGoal;  // 主队进球数
    public int guestGoal;  // 客队进球数

    public boolean append;  //追加比赛

    public int player_num;
    public int avs_mbr;  //avs 码率
    public String provinceId;  //avs 码率
    public String cityId;  //avs 码率

    public final int disable_avs;  // disable_avs 0/1, 默认或者不传是0, 所以默认不传就是avs录制打开的

    // 该字段用于表示比赛id, 不再仅限于直播, 为了兼容和方便, 名称不变!!
    public String live_cid;

    // 比赛类型
    public int ctype;

    public int live_mbr;

    public String fec_fn;
    public String avs_fn;
    public String videoId;

    public int hide_osd = 0;  // hide_osd=1/0, 1是隐藏，0是不隐藏

    public int pause_time = 0;    //暂停计时

    public int avsw = 7600;
    public int avsh = 1080;

    public boolean isLive() {
        return ctype == Constants.COMPETITION_TYPE_LIVE;
    }

    public RecordInfoBean(String name, long rate) {
        this.name = name;
        this.maxRate = rate;
        this.disable_avs = CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_ENABLE_AVS) ? 0 : 1;
        this.live_cid = "";
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(long maxRate) {
        this.maxRate = maxRate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getHostGoal() {
        return hostGoal;
    }

    public void setHostGoal(int hostGoal) {
        this.hostGoal = hostGoal;
    }

    public int getGuestGoal() {
        return guestGoal;
    }

    public void setGuestGoal(int guestGoal) {
        this.guestGoal = guestGoal;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public int getPlayerNum() {
        return player_num;
    }

    public void setPlayerNum(int player_num) {
        this.player_num = player_num;
    }

    public int getAvsMbr() {
        return avs_mbr;
    }

    public void setAvs_mbr(int avs_mbr) {
        this.avs_mbr = avs_mbr;
    }

    public int getLive_mbr() {
        return live_mbr;
    }

    public void setLive_mbr(int live_mbr) {
        this.live_mbr = live_mbr;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public int getHide_osd() {
        return hide_osd;
    }

    public void setHide_osd(int hide_osd) {
        this.hide_osd = hide_osd;
    }

    public RecordStartRequestBean convertRecordStartRequestBean(){

        RecordStartRequestBean _recordInfoBean = new RecordStartRequestBean();
        _recordInfoBean.setVh(height);
        _recordInfoBean.setVw(width);
        _recordInfoBean.setVs(place);
        _recordInfoBean.setVn(name);
        _recordInfoBean.setCn(campaignName);
        _recordInfoBean.setMotion_type(motionType);
        _recordInfoBean.setPlace_type(""+placeType);
        _recordInfoBean.setCity(city==null?"":city);
        _recordInfoBean.setProvince_code(provinceId);
        _recordInfoBean.setCity_code(cityId);
        _recordInfoBean.setHost_team(hostTeam);
        _recordInfoBean.setGuest_team(guestTeam);

        _recordInfoBean.setVt(time);

        _recordInfoBean.setMbr(maxRate);
        _recordInfoBean.setAvs_mbr(avs_mbr);
        _recordInfoBean.setLive_mbr(live_mbr);
        _recordInfoBean.setCtype(ctype);
        _recordInfoBean.setHide_osd(hide_osd);
        _recordInfoBean.setPauseTime(pause_time);

        _recordInfoBean.setLive_cid(live_cid==null?"":live_cid);
        _recordInfoBean.setPlayer_num(player_num);

        _recordInfoBean.setAppend(append?1:0);

        return _recordInfoBean;
    }
}
