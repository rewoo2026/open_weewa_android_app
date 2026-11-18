package com.xh.hotme.bean;

import androidx.annotation.Keep;

@Keep
public class UserInfoBean {
    public String nickName;
    public String phone;
    public String avatar;
    // 这里可能要加邮箱。还有其他显示手机号的地方也要换
    public  String email;
    public int userId;
    public String userToken;

    public String toString() {
        return "nickName=" + nickName + ", phone=" + phone + ", token=" + userToken;
    }
}
