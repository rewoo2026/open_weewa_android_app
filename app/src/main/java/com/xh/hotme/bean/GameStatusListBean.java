package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/*{
            "host": 1,
            "type": 0,
            "time": 20
        }
 */
@Keep
public class GameStatusListBean implements Serializable {
    int type;
    int host;
    int time;
    int ori_seq;
    String name;
    String id;
    String mark_name;
    String mark_id;

    public GameStatusListBean(int type){
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getHost() {
        return host;
    }

    public void setHost(int host) {
        this.host = host;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOri_seq() {
        return ori_seq;
    }

    public void setOri_seq(int ori_seq) {
        this.ori_seq = ori_seq;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMark_name() {
        return mark_name;
    }

    public void setMark_name(String mark_name) {
        this.mark_name = mark_name;
    }

    public String getMark_id() {
        return mark_id;
    }

    public void setMark_id(String mark_id) {
        this.mark_id = mark_id;
    }
}
