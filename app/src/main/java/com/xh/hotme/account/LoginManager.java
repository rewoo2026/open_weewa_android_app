package com.xh.hotme.account;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Keep;

import com.xh.hotme.bean.CheckUserLoginStatus;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.LoginSpUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.TimeUtil;


/**
 * Create by zhaozhihui on 2019/3/9
 **/

/**
 * manage user data. file and user sharepreference
 */
@Keep
public class LoginManager {

    public static UserInfoBean loginUserInfo;
    public static long loginTime;
    public static final int loginExpiredTime = 1;  //单位1天

    public static void init() {
        LoginSpUtil.init();
        //loading user info file
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo) {
            LoginSpUtil.setUserId(String.valueOf(userInfo.userId));
            LoginSpUtil.saveUserToken(userInfo.userToken);
            Constants.HEADER_TOKEN = userInfo.userToken;
            loginUserInfo = userInfo;
        }

        loginTime = LoginSpUtil.getLoginTime();
    }

    public static UserInfoBean getUserLoginInfo() {
        if(loginUserInfo != null) {
            return loginUserInfo;
        }

        return AppFileUtil.loadUserInfo();
    }

    public static long getLoginTime() {

        if(loginTime == 0) {
            loginTime = LoginSpUtil.getLoginTime();
        }
        return loginTime;
    }

    public static void updateLoginTime() {

        //如果没有登录，则返回
        if(TextUtils.isEmpty(getUserToken())) {
            return;
        }

        long curTimeStamp = System.currentTimeMillis();
        loginTime = curTimeStamp;
        LoginSpUtil.saveLoginTime(curTimeStamp);

    }

    //判断是否过期，过期时间1天
    public static boolean isLoginExpired() {
        if(loginTime == 0) {
            return true;
        }

        boolean valid = TimeUtil.isOnTime(loginTime, loginExpiredTime);
        return !valid;

    }

    //判断是否登录过，无须判断是否过期
    public static boolean isLogon() {
        return !TextUtils.isEmpty(getUserToken());
    }

    public static boolean isSignedIn() {
        return validToken();
    }

    public static boolean validToken() {
        return !TextUtils.isEmpty(getUserToken()) && !isLoginExpired();
    }

    /*
     * leto里是以手机号或者是mgc_开始的临时账号，作为uid
     */
    public static String getUserId() {
        //
        LoginSpUtil.init();

        UserInfoBean userInfo = AppFileUtil.loadUserInfo();

        if(null != userInfo) {

            return "" + userInfo.userId;
        }

        return "";
    }


    public static String getUserToken() {
        LoginSpUtil.init();
        if(!TextUtils.isEmpty(Constants.HEADER_TOKEN)) {
            return Constants.HEADER_TOKEN;
        }

        return LoginSpUtil.getUserToken();
    }

    public static void saveLoginInfo(UserInfoBean data) {
        if(null == data) {
            return;
        }
        LoginSpUtil.saveLoginInfo(data);
        loginUserInfo = data;
    }

    public static String getNickname() {
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo && !TextUtils.isEmpty(userInfo.nickName)) {
            return userInfo.nickName;
        } else {
            LoginSpUtil.init();
            if(!TextUtils.isEmpty(LoginSpUtil.getNickname())) {
                return LoginSpUtil.getNickname();
            }
        }
        return "";
    }

    public static boolean setNickname(String nickname) {
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo) {
            userInfo.nickName = nickname;
        } else {
            return false;
        }
        return true;
    }

    public static String getMobile() {
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo && !TextUtils.isEmpty(userInfo.phone)) {
            return userInfo.phone;
        } else {
            LoginSpUtil.init();
            if(!TextUtils.isEmpty(LoginSpUtil.getMobile())) {
                return LoginSpUtil.getMobile();
            }
        }
        return "";
    }

    public static String getPortrait() {
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo && !TextUtils.isEmpty(userInfo.avatar)) {
            return userInfo.avatar;
        } else {
            LoginSpUtil.init();
            if(!TextUtils.isEmpty(LoginSpUtil.getPortrait())) {
                return LoginSpUtil.getPortrait();
            }
        }
        return "";
    }

    public static boolean setPortrait(String portrait) {
        UserInfoBean userInfo = AppFileUtil.loadUserInfo();
        if(null != userInfo) {
            userInfo.avatar = portrait;
        } else {
            return false;
        }
        return true;
    }

    public static void clearLoginInfo() {
        DailyAccessManager.invalidate();
        AppFileUtil.deleteUserInfo(AppFileUtil.DEFAULT_USER_INFO);
        LoginSpUtil.clearLogin();
        Constants.HEADER_TOKEN = "";
        loginUserInfo = null;

        loginTime = 0;
        LoginSpUtil.saveLoginTime(0);
        CameraTokenManager.onLoginChanged();
    }


    public static void saveUserToken(String token) {
        DailyAccessManager.invalidate();
        LoginSpUtil.saveUserToken(token);
        Constants.HEADER_TOKEN = token;
        CameraTokenManager.onLoginChanged();
    }


    public static void checkUserLoginStatus(Context context) {
        if(!LoginManager.isLogon()) {
            return;
        }
        OkHttpUtil.get(SdkApi.check_userLogin_status(), null,
            new OkHttpCallbackDecode<CheckUserLoginStatus>(CheckUserLoginStatus.class) {
                @Override
                public void onDataSuccess(CheckUserLoginStatus data) {

                    MainHandler.runOnUiThread(() -> {
                        if(data.code == 401) {
                            MobileLoginActivity.start(context);
                        }
                    });
                }

                @Override
                public void onFailure(String code, String message) {

                }

                @Override
                public void onFinish() {

                }
            });

    }
}
