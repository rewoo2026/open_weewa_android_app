//
// Copyright (c) 2017, ledong.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice, this
// list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation
// and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//


package com.xh.hotme.utils;


import androidx.annotation.Keep;

@Keep
public class Constants {
    public static final String SOFTAP_REGEX = "^HotmeCam.*";

    public static String userToken = "";
    public static String HEADER_TOKEN = "";
    public static final String HEADER_TOKEN_NAME = "Authorization";

    public static final String TEST_TOKEN = "00000000";

    public static final boolean enableOTA = true;

    public static boolean TEST_SERVER = false;
    public static final int REQUEST_CODE_SCAN = 0x1010;
    public static String ROLE = "user";

    // app类型
    public static final int APP_TYPE_REWOO_CAMERA_USER = 1; // 热我相机普通用户版
    public static final int APP_TYPE_REWOO_TRANSFER = 2; // 热我快传
    public static final int APP_TYPE_OTA = 3;
    public static final int APP_TYPE_REWOO_CAMERA_SERVICE = 4; // 热我相机赛事服务版
    public static final int APP_TYPE_REWOO_CAMERA_COLLECTOR = 5; // 热我相机数据采集版
    public static final int APP_TYPE_REWOO_CAMERA_GREENKEEP = 6; // 热我相机球场管理员版本

    public static boolean shouldShowCameraTest() {
        return !"user".equals(ROLE);
    }

    public static boolean shouldCheckOTAAlways() {
        return "tester".equals(ROLE) || isGztCrew();
    }

    public static boolean isGztCrew() {
        return "gztTestr".equals(ROLE);
    }

    public static int getAppType() {
        if("service".equals(ROLE)) {
            return APP_TYPE_REWOO_CAMERA_SERVICE;
        } else if("collector".equals(ROLE)) {
            return APP_TYPE_REWOO_CAMERA_COLLECTOR;
        } else if("collector".equals(ROLE)) {
            return APP_TYPE_REWOO_CAMERA_COLLECTOR;
        }  else if("greenKeeper".equals(ROLE)) {
            return APP_TYPE_REWOO_CAMERA_GREENKEEP;
        }  else {
            return APP_TYPE_REWOO_CAMERA_USER;
        }
    }

    public static final String REQUEST_CODE = "request_code";
    public static final String REQUEST_DEVICE = "request_device";
    public static final String REQUEST_VIDEO_BEAN = "request_video_bean";

    public static final String REQUEST_FACTORY_TEST = "request_factory_test";

    public static final String REQUEST_DEVICE_INFO = "request_device_INFO";
    public static final String REQUEST_ACTIVE_STEP = "request_active_step";
    //进入直播前检查网络
    public static final String REQUEST_ACTIVE_LIVE = "request_active_live";
    public static final String REQUEST_MOBILE = "request_mobile";
    public static final String REQUEST_NETWORK_INFO = "request_network_info";


    public static final String INTENT_SSID = "ssid";
    public static final String INTENT_PASSWORD = "password";
    public static final String INTENT_BLE = "ble_address";

    public static final int REQUEST_CODE_LOGIN_TAB_ME = 0x1020;
    public static final int REQUEST_CODE_LOGIN_TAB_LIVE = 0x1022;
    public static final int REQUEST_CODE_LOGIN_HOME_DEVICE = 0x1021;
    public static final int REQUEST_CODE_WIFI_SETUP = 0x1101;


    public static final int REQUEST_CODE_WIFI_PREVIEW = 0x2101;
    public static final int REQUEST_CODE_WIFI_CAMERA_VIDEO = 0x2102;

    public static final int REQUEST_CODE_WIFI_FACTORY_TEST = 0x2103;

    public static final int REQUEST_CODE_NETWORK_UPGRADE = 0x4001;

    // other flag
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

    public static final String PREF_EXTRA = "PREF_EXTRA";

    public static final String UMENG_APP_ID = "";

    public static final String WEIXIN_APP_ID = "";
    public static final String WEIXIN_SECRECT_KEY = "";

    public final static String WINDOW_TYPE = "window_type";
    public final static String TITLE_NAME = "titleName";
    public final static String REQUEST_TYPE = "REQUEST_TYPE";
    public final static String ORIENTATION_TYPE = "ORIENTATION_TYPE";
    public final static String APP_TYPE = "APP_TYPE";
    public static final String SHOW_LOADING = "show_loading";
    public final static String URL = "url";
    public final static String MODIFY_TITLE = "modify_title";
    public final static String VIDEO_URL = "video_url";
    public final static String VIDEO_BEAN = "video_bean";
    public final static String COMPETITION_BEAN = "competition_bean";
    public final static String CONSTANT_TOKEN = "";


    public final static int ME_MODULE_PROFILE = 1;

    public final static int ME_MODULE_SETTING = 2;
    public final static int ME_MODULE_OUT = 3;

    // 设备类型
    public final static int DEVICE_TYPE_TEST = 1;
    public final static int DEVICE_TYPE_NORMAL = 2;


    public final static int DEVICE_STATUS_OFFLINE = 0;
    public final static int DEVICE_STATUS_ONLINE = 1;
    public final static int DEVICE_STATUS_CONNECT = 2;
    public final static int DEVICE_STATUS_CONNECT_FAIL = 3;

    public final static int LIVE_TYPE_FOOTBALL = 0;
    public final static int LIVE_TYPE_BASKETBALL = 1;
    public final static int LIVE_TYPE_TRAINING = 2;


    public final static String VIDEO_CATEGORY = "type";
    public final static String SKIP_CAMERA_INFO = "skip_load_camera_info";
    public final static int VIDEO_CATEGORY_TOP = 1;
    public final static int VIDEO_CATEGORY_GALLERY = 2;

    public final static String VIDEO_PLAY_MODE = "play_mode";


    public final static String ACTIVE_LOGIN = "active";
    public final static int ACTIVE_TYPE_LOGIN = 1;
    public final static int ACTIVE_TYPE_ACTIVE = 2;
    public final static int ACTIVE_TYPE_BIND = 3;

    public final static String SSID = "ssid";

    public final static String TYPE_BIND = "BIND";

    public final static String TYPE_UNBIND = "UN_BIND";
    public final static String TYPE_LOGIN = "LOGIN";
    public final static String TYPE_UPDATE_PHONE = "UPDATE_PHONE";
    public final static String REQUEST_INTENT_USAGE_INFO = "USAGE_INFO";
    public final static String REQUEST_INTENT_DEVICE_INFO = "DEVICE_INFO";

    public final static int ACTIVE_STEP_SETUP_WIFI = 0;
    public final static int ACTIVE_STEP_ACTIVE = 1;

    public final static int ACTIVE_STEP_BIND_FAIL = 2;
    public final static int ACTIVE_STEP_BIND_SUCCESS = 3;

    public final static int ACTIVE_STEP_BOUND = 4;

    public final static int ACTIVE_STEP_ACTIVE_SUCCESS = 5;

    public final static int ACTIVE_STEP_ACTIVE_FAIL = 6;

    public final static int BIND_DEVICE_PAGE_NUMBER = 100;
    public final static int COMPETITION_PAGE_NUMBER = 20;

    public final static String LOGIN_TYPE = "login_type";
    public final static int LOGIN_TYPE_LOGIN = 0;
    public final static int LOGIN_TYPE_CHANGE_MOBILE = 1;

    public final static int sms_count_down = 60;

    public final static int DEVICE_ACTION_CLICK = 0;
    public final static int DEVICE_ACTION_OPEN = 1;
    public final static int DEVICE_ACTION_REMOVE = 2;
    public final static int DEVICE_ACTION_DISCONNECT = 3;

    public final static int FEC_VIDEO_MAX_RATE = 4096;
    public final static int FEC_VIDEO_WIDTH = 1920;
    public final static int FEC_VIDEO_HEIGHT = 1080;


    public final static String CAMERA_SETTING_ENABLE_AVS = "avs_video_disable";
    public final static String CAMERA_SETTING_AUTO_UPLOAD = "rewoo_auto_upload";
    public final static String CAMERA_SETTING_AUTO_UPLOAD_AVS = "rewoo_auto_upload_avs";
    public final static String CAMERA_SETTING_VIDEO_ANALYSIS = "rewoo_video_analysis";
    public final static String CAMERA_SETTING_ADB_ENABLE = "rewoo_adb_enable";
    public final static String CAMERA_SETTING_AVS_MBR = "rewoo_avs_mbr";

    public final static String CAMERA_SETTING_MBR = "rewoo_mbr";


    public final static int MOTION_TYPE_FOOTBALL = 1;
    public final static int MOTION_TYPE_BASKETBALL = 2;


    public final static int COMPETITION_UNSET = 0; //未开始
    public final static int COMPETITION_START = 1;  //开始，进行中
    public final static int COMPETITION_STOP = 2;  //结束
    public final static int COMPETITION_PAUSE = 3;  //暂停

    // 比赛类型
    public final static int COMPETITION_TYPE_UNKNOWN = 0;
    public final static int COMPETITION_TYPE_RECORD = 1;
    public final static int COMPETITION_TYPE_LIVE = 2;

    public static final int SPORT_UNKNOWN = 0;
    public static final int SPORT_FOOTBALL = 1;
    public static final int SPORT_BASKETBALL = 2;

    // 上传方式
    public final static int UPLOAD_AUTO = 1;
    public final static int UPLOAD_MANUAL = 2;


    // 热我自己定义的上传状态常量
    public final static int STATUS_STARTING = 1;
    public final static int STATUS_PROCESSING = 2;
    public final static int STATUS_STOP = 3;
    public final static int STATUS_COMPLETE = 4;
    public final static int STATUS_FAIL = 5;

    // 服务器那边的视频类型
    public static final int VIDEO_CLASSIFY_FULL = 0;
    public static final int VIDEO_CLASSIFY_PLAYBACK = 1;
    public static final int VIDEO_CLASSIFY_HIGHLIGHTS = 2;
    public static final int VIDEO_CLASSIFY_HOST_GOAL = 3;
    public static final int VIDEO_CLASSIFY_GUEST_GOAL = 4;

    public static final int VIDEO_CLASSIFY_HIGHLIGHTS_AI = 5;
    public static final int VIDEO_CLASSIFY_LIVE_PLAYBACK = 99;

    // 第三方平台id
    public static final int LIVE_PLATFORM_XQM = 0;
    public static final int LIVE_PLATFORM_WX_CHANNEL = 1;
    public static final int LIVE_PLATFORM_DY_CHANNEL = 2;
    public static final int LIVE_PLATFORM_KUAISHOU= 3;
    public static final int LIVE_PLATFORM_OTHER = 99;


    public static final int STAT_GOAL_0 =0; // 进球 - 进球
    public static final int STAT_SHOOT_0 = 1; // 射门 - 射正
    public static final int STAT_DEFENSE_0 = 2; // 防守 - 扑救
    public static final int STAT_PENALTY_0 = 3; // 犯规 - 普通
    public static final int STAT_GOAL_1 = 4; // 进球 - 任意球
    public static final int STAT_CORNER_BALL = 5;  // 角球
    public static final int STAT_ATTACK_0 = 6;// 进攻 - 过人
    public static final int STAT_FOUL_0 = 7; // 犯规 - 普通
    public static final int STAT_GOAL_2 = 8; // 进球 - 乌龙
    public static final int STAT_GOAL_3 = 9; // 进球 - 头球
    public static final int STAT_GOAL_4 = 10; // 进球 - 点球
    public static final int STAT_GOAL_5 = 11; // 进球 - 点球射失
    public static final int STAT_SHOOT_1 = 12; // 射门 - 门框
    public static final int STAT_SHOOT_2 = 13; // 射门 - 射偏
    public static final int STAT_FOUL_1 = 14; // 犯规 - 红牌
    public static final int STAT_FOUL_2 = 15; // 犯规 - 黄牌
    public static final int STAT_FOUL_3 = 16; // 犯规 - 越位
    public static final int STAT_EXCHANGE = 17; // 换人
    public static final int STAT_CONTROL = 18; // 控球
    public static final int STAT_PASS_0 = 19; // 传球 - 传球
    public static final int STAT_PASS_1 = 20; // 传球 - 直塞
    public static final int STAT_PASS_2 = 21; // 传球 - 传中
    public static final int STAT_PASS_3 = 22; // 传球 - 传球失败
    public static final int STAT_ATTACK_1 = 23; // 进攻 - 尝试过人
    public static final int STAT_ATTACK_2 = 24; // 进攻 - 危险进攻
    public static final int STAT_ATTACK_3 = 25; // 进攻 - 普通进攻
    public static final int STAT_DEFENSE_1 = 26; // 防守 - 抢断
    public static final int STAT_DEFENSE_2 = 27; // 防守 - 拦截
    public static final int STAT_DEFENSE_3 = 28; // 防守 - 解围
    public static final int STAT_DEFENSE_4 = 29; // 防守 - 封堵

    public static final int STAT_CUSTOM_1 = 100; // 自定义1
    public static final int STAT_CUSTOM_2 = 101; // 自定义2
    public static final int STAT_CUSTOM_3 = 102; // 自定义3


    //视频打点
    public final static int INT_STATE_FILTER_TYPE_ALL_TEAM = -1;
    public final static int INT_STATE_FILTER_TYPE_HOST_TEAM = 0;
    public final static int INT_STATE_FILTER_TYPE_GUEST_TEAM = 1;


    //打点场景
    public final static int DOT_SCENE_RECORDING = 0;   //录制打点
    public final static int DOT_SCENE_LIVING = 1;      //直播打点
    public final static int DOT_SCENE_VIDEO = 2;       //视频打点

    public final static int DOT_SOURCE_MANUAL = 1;       //人工打点


    //
    public final static int ACCESS_MODEL_LOCAL = 0;  //本地模式
    public final static int ACCESS_MODEL_REMOTE = 1;  //远程模式

    public static int ACCESS_MODEL = ACCESS_MODEL_LOCAL;  //本地模式

}
