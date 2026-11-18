package com.xh.hotme.bean;

import androidx.annotation.Keep;

import com.xh.hotme.record.bean.RecordInfoBean;

import java.io.Serializable;

/*
{"c":"record_status","au":"","audio":1,"city":"广东省深圳市","duration":252,"guest_team":"巴西","host_team":"阿根廷","mbr":8192,"motion_type":"足球","place_type":"11人场","r":"ok","st":1,"vh":1080,"vn":"世界杯预选赛","vs":"桃园","vt":1701932743487,"vw":1920}
 */
@Keep
public class RecordStatusResultBean implements Serializable {
    public int recordingProtocolVersion;
    public int localControlSupported, localControl, firstFrame;
    public long offlineCounter, observedAt;
    public int offlineActive, offlineRecorded;
    public String offlineDeviceId, offlineCompetitionId, offlineSessionId, offlinePermitId, offlineOperationId;
    public long deviceVersion, stopAt;
    public String deviceState, sessionId, competitionId, operationId;
    private String c;
    private String au;
    private int audio;
    private String city;
    private int duration;
    private String guest_team;
    private String host_team;
    private int mbr;
    private int live_mbr;
    private int avs_mbr;
    private int motion_type;
    private int place_type;
    private String r;
    private int st;
    private int vh;
    private String vn;
    private String vs;
    private long vt;
    private int vw;

    private int guest_goal;
    private int host_goal;

    private int player_num;
    private int stitch_distance;
    private String label;
    private String live_cid;
    private int ctype;
    private int training_mode;
    public String fec_fn;
    public String avs_fn;
    public int match_time;
    private int match_time_paused; // 0表示未暂停，1表示已暂停

    public int isTrainingMode() {
        return training_mode;
    }

    public void setTrainingMode(int training_mode) {
        this.training_mode = training_mode;
    }

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public String getAu() {
        return au;
    }

    public void setAu(String au) {
        this.au = au;
    }

    public int getAudio() {
        return audio;
    }

    public void setAudio(int audio) {
        this.audio = audio;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getGuestTeam() {
        return guest_team;
    }

    public void setGuestTeam(String guestTeam) {
        this.guest_team = guestTeam;
    }

    public String getHostTeam() {
        return host_team;
    }

    public void setHostTeam(String hostTeam) {
        this.host_team = hostTeam;
    }

    public Integer getMbr() {
        return mbr;
    }

    public void setMbr(Integer mbr) {
        this.mbr = mbr;
    }

    public int getMotionType() {
        return motion_type;
    }

    public void setMotionType(int motionType) {
        this.motion_type = motionType;
    }

    public int getPlaceType() {
        return place_type;
    }

    public void setPlaceType(int placeType) {
        this.place_type = placeType;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public int getStatus() {
        return st;
    }

    public void setStatus(int st) {
        this.st = st;
    }

    public int getVideoHeight() {
        return vh;
    }

    public void setVideoHeight(int vh) {
        this.vh = vh;
    }

    public String getVideoName() {
        return vn;
    }

    public void setVideoName(String vn) {
        this.vn = vn;
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

    public int getVideoWidth() {
        return vw;
    }

    public void setVideoWidth(int vw) {
        this.vw = vw;
    }

    public int getGuest_goal() {
        return guest_goal;
    }

    public void setGuest_goal(int guest_goal) {
        this.guest_goal = guest_goal;
    }

    public int getHost_goal() {
        return host_goal;
    }

    public void setHost_goal(int host_goal) {
        this.host_goal = host_goal;
    }

    public int getPlayer_num() {
        return player_num;
    }

    public void setPlayer_num(int player_num) {
        this.player_num = player_num;
    }

    public int getStitch_distance() {
        return stitch_distance;
    }

    public void setStitch_distance(int stitch_distance) {
        this.stitch_distance = stitch_distance;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }


    public String getLiveCId() {
        return live_cid;
    }

    public void setLiveCId(String cid) {
        this.live_cid = cid;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public String getFec_fn() {
        return fec_fn;
    }

    public void setFec_fn(String fec_fn) {
        this.fec_fn = fec_fn;
    }

    public String getAvs_fn() {
        return avs_fn;
    }

    public void setAvs_fn(String avs_fn) {
        this.avs_fn = avs_fn;
    }

    public int getMatchTime() {
        return match_time;
    }

    public void setMatchTime(int match_time) {
        this.match_time = match_time;
    }
    
    public int getMatchTimePaused() {
        return match_time_paused;
    }
    
    public void setMatchTimePaused(int match_time_paused) {
        this.match_time_paused = match_time_paused;
    }

    public RecordInfoBean convertRecordInfoBean() {
        RecordInfoBean recordInfoBean = new RecordInfoBean(this.vn, this.mbr);
        recordInfoBean.setAuthor(au);
        recordInfoBean.setHeight(vh);
        recordInfoBean.setWidth(vw);
        recordInfoBean.time = vt;
        recordInfoBean.guestTeam = guest_team;
        recordInfoBean.hostTeam = host_team;
        recordInfoBean.city = city;
        recordInfoBean.place = vs;
        recordInfoBean.motionType = motion_type;
        recordInfoBean.placeType = place_type;
        recordInfoBean.hostGoal = host_goal;
        recordInfoBean.guestGoal = guest_goal;
        recordInfoBean.player_num = player_num;
        recordInfoBean.live_cid = live_cid;
        recordInfoBean.ctype = ctype;
        recordInfoBean.live_mbr = live_mbr;
        recordInfoBean.avs_mbr = avs_mbr;
        recordInfoBean.avs_fn = avs_fn;
        recordInfoBean.fec_fn = fec_fn;
        return recordInfoBean;
    }
}
