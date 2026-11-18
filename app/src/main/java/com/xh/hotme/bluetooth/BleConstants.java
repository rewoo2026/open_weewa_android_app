/******************************************************************************
 *
 *  Copyright (C) 2013-2014 Cypress Semiconductor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.xh.hotme.bluetooth;

import java.util.UUID;

/**
 * Contains the UUID of services, characteristics, and descriptors
 */
public class BleConstants {
    public static final String TAG_PREFIX = "Home."; //used for debugging

    /**
     * Transmit packet size
     */
    public static final int SIZE_TRANSMIT_PACKET = 134;


    public static final UUID BLE_WRITE_SERVICE_UUID = UUID
        .fromString("0000ffe5-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of Wifi Characteristic
     */
//    public static final UUID WIFI_CHARACTERISTIC_UUID = UUID
//            .fromString("00009999-0000-1000-8000-00805F9B34FB");
    public static final UUID BLE_WRITE_CHARACTERISTIC_UUID = UUID
        .fromString("0000ffe9-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of the client configuration descriptor
     */
    public static final UUID CLIENT_CONFIG_DESCRIPTOR_UUID = UUID
        .fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID BLE_READ_SERVICE_UUID = UUID
        .fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    public static final UUID BLE_READ_CHARACTERISTIC_UUID = UUID
        .fromString("0000ffe4-0000-1000-8000-00805f9b34fb");

    public static final String SOFT_AP_IP = "10.201.126.1";
    public static final byte BLE_CMD_CHAR_START = 0x01;
    public static final byte BLE_CMD_CHAR_END = 0x04;

    public static final String BLE_CMD = "c";
    public static final String BLE_KEY_SSID = "s";
    public static final String BLE_KEY_PASSWORD = "p";
    public static final String BLE_KEY_RESULT = "r";
    public static final String BLE_KEY_HIDE_SCORE = "hide_score";
    public static final String BLE_KEY_HIDE_TEAM = "hide_team";

    public static final String BLE_KEY_FREE = "free";

    public static final String BLE_KEY_TOTAL = "total";
    public static final String BLE_KEY_USED = "used";

    public static final String BLE_KEY_REACHABLE = "reachable";
    public static final String BLE_KEY_WIFI = "wifi";
    public static final String BLE_KEY_ETH = "eth";
    public static final String BLE_KEY_SIM = "sim";
    public static final String BLE_KEY_HTTP = "http";

    public static final String BLE_KEY_ST = "st";
    public static final String BLE_KEY_IP = "ip";

    public static final String BLE_KEY_DNS = "dns";

    public static final String BLE_KEY_NETMASK = "netmask";

    public static final String BLE_KEY_GATEWAY = "gateway";
    public static final String BLE_KEY_DHCP = "dhcp";


    public static final String BLE_KEY_HAS_CARD = "has_card";
    public static final String BLE_KEY_CONNECTED = "connected";

    public static final String BLE_KEY_SOFTAP = "ap";
    public static final String BLE_KEY_SOFTAP_SSID = "s";
    public static final String BLE_KEY_SOFTAP_PASSWORD = "p";

    public static final String BLE_RESULT_SUCCESS = "ok";
    public static final String BLE_KEY_WIFILIST = "wl";
    public static final String BLE_KEY_ERROR_CODE = "ec";
    public static final String BLE_KEY_RSSI_LEVEL = "rssi_lv";
    public static final String BLE_KEY_IMEI = "imei";
    public static final String BLE_KEY_ICCID = "iccid";
    public static final String BLE_KEY_APN = "apn";

    public static final String BLE_KEY_DEVICE_NAME = "dn";
    public static final String BLE_KEY_DEVICE_ID = "did";
    public static final String BLE_KEY_DEVICE_MAC = "mac";
    public static final String BLE_KEY_DEVICE_ACTIVE_STATUS = "act";
    public static final String BLE_KEY_VOLUME = "volume";
    public static final String BLE_KEY_DEVICE_ACCOUNT = "acc";
    public static final String BLE_KEY_DEFAULT_SETTINGS_SYNCED = "def_synced";
    public static final String BLE_KEY_DEVICE_VERSION = "v";
    public static final String BLE_KEY_OTA_VERSION = "ota_version";
    public static final String BLE_KEY_OTA_VERSION_CODE = "ota_version_code";
    public static final String BLE_KEY_AUTO_UPLOAD = "auto_upload";
    public static final String BLE_KEY_NONCE = "nonce";
    public static final String BLE_KEY_ADB_ENABLE = "adb";
    public static final String BLE_KEY_ID = "id";

    public static final String BLE_KEY_DEVICE_ACTIVE_TIME = "bind_time";

    public static final String BLE_KEY_UPDATE_NAME = "n";
    public static final String BLE_KEY_UPDATE_SN = "sn";

    public static final String BLE_CMD_POWER_ON = "poweron";
    public static final String BLE_CMD_POWER_OFF = "poweroff";
    public static final String BLE_CMD_RESTART = "restart";
    public static final String BLE_CMD_WIFILIST = "wifilist";
    public static final String BLE_CMD_WIFI_SETUP = "wifisetup";

    public static final String BLE_CMD_WIFI_TURN_OFF = "turn_off_wifi";
    public static final String BLE_CMD_SOFTAP_OPEN = "softap_open";
    public static final String BLE_CMD_SOFTAP_CLOSE = "softap_close";

    public static final String BLE_CMD_SET_FAN_SPEED = "set_fan_speed";

    public static final String BLE_CMD_SOFTAP_STATUS = "softap_status";

    public static final String BLE_CMD_NETWORK_STATUS = "net_status";
    public static final String BLE_CMD_NETWORK_CHANGE = "net_changed";
    public static final String BLE_CMD_SIM_OPEN = "turn_on_4g";
    public static final String BLE_CMD_SIM_CLOSE = "turn_off_4g";
    public static final String BLE_CMD_START_AUTO_UPLOAD = "start_auto_upload";
    public static final String BLE_CMD_STOP_AUTO_UPLOAD = "stop_auto_upload";
    public static final String BLE_CMD_AUTO_UPLOAD_STATUS = "auto_upload_status";
    public static final String BLE_CMD_SET_APN = "set_apn";
    public static final String BLE_CMD_UPLOAD_LOG = "upload_log";

    public static final String BLE_CMD_CAMERA_SETUP = "camera_setup";
    public static final String BLE_CMD_CAMERA_START = "camera_start";
    public static final String BLE_CMD_CAMERA_STOP = "camera_stop";
    public static final String BLE_CMD_CAMERA_STATUS = "camera_status";
    public static final String BLE_CMD_SOFTWARE_INFO = "software_info";
    public static final String BLE_KEY_IDLE_VERSION = "idle_v";
    public static final String BLE_KEY_DAEMON_VERSION = "daemon_v";
    public static final String BLE_KEY_RKIPC_VERSION = "rkipc_v";

    public static final String BLE_CMD_SCENE_SETUP = "scene_setup";

    public static final String BLE_CMD_UPDATE_SETTINGS = "update_settings";
    public static final String BLE_CMD_UPDATE_SETTINGS_EX = "update_settings_ex";
    public static final String BLE_CMD_GET_SETTINGS = "get_settings";
    public static final String BLE_CMD_GET_SETTINGS_EX = "get_settings_ex";

    public static final String BLE_CMD_GET_OTA_STATUS = "get_ota_status";
    public static final String BLE_CMD_START_OTA_DOWNLOAD = "start_ota_download";
    public static final String BLE_CMD_START_OTA = "start_ota";
    public static final String BLE_CMD_OTA_UPGRADED = "ota_upgraded";  //相机主动推送

    public static final String BLE_CMD_RKIPC_START = "rkipc_start";
    public static final String BLE_CMD_RKIPC_STOP = "rkipc_stop";
    public static final String BLE_CMD_RECORDER_START = "record_start";
    public static final String BLE_CMD_RECORDER_STOP = "record_stop";
    public static final String BLE_CMD_STOP_RECORD_COZ_LOW_STORAGE = "stop_record_coz_low_storage";
    public static final String BLE_CMD_RECORDER_STATUS = "record_status";
    public static final String BLE_CMD_ROI_OP = "roi_op";
    public static final String BLE_CMD_SET_TRAINING_MODE = "set_training_mode";

    public static final String BLE_CMD_SET_TEAM_NAME = "set_team_name";

    public static final String BLE_CMD_LIVE_STATUS = "live_status";

    public static final String BLE_CMD_AVS_RTMP_URL_SET = "set_avs_rtmp_url";
    public static final String BLE_CMD_FEC_LIVE_URL_ADD = "add_fec_live_url";
    public static final String BLE_CMD_FEC_LIVE_URL_REMOVE = "remove_fec_live_url";
    public static final String BLE_CMD_FEC_LIVE_URL_REMOVE_ALL = "remove_all_fec_live";
    public static final String BLE_CMD_FEC_PREVIEW_START = "fec_preview_start";
    public static final String BLE_CMD_FEC_PREVIEW_STOP = "fec_preview_stop";

    public static final String BLE_CMD_WEEWA_INFO = "weewa_info";
    public static final String BLE_CMD_SET_VOLUME = "set_volume";
    public static final String BLE_CMD_USAGE_INFO = "usage_info";
    public static final String BLE_CMD_GET_PLAN_USAGE = "get_plan_usage";

    // 这个命令没有蓝牙方式, 因为水印图片比较大通过蓝牙发有问题, 即使分片发也有问题
    public static final String BLE_CMD_SET_WATERMARK = "set_watermark";

    public static final String BLE_CMD_DELETE_WATERMARK = "delete_watermark";
    public static final String BLE_CMD_BEEP = "beep";

    public static final String BLE_CMD_FACTORY_TEST_LIGHT = "factory_test_light";
    public static final String BLE_CMD_FACTORY_TEST_FAN = "factory_test_fan";
    public static final String BLE_CMD_FACTORY_TEST_AUDIO_START = "factory_test_audio_start";
    public static final String BLE_CMD_FACTORY_TEST_AUDIO_STOP = "factory_test_audio_stop";

    public static final String BLE_CMD_FACTORY_TEST_ARECORD_START = "factory_test_arecord_start";
    public static final String BLE_CMD_FACTORY_TEST_ARECORD_STOP = "factory_test_arecord_stop";

//    public static final String BLE_CMD_FACTORY_TEST_AUDIO = "factory_test_audio";
    public static final String BLE_CMD_FACTORY_TEST_PING = "factory_test_ping";
    public static final String BLE_CMD_FACTORY_TEST_FAST_CHARGE = "factory_test_fast_charge";

    public static final String BLE_CMD_DEVICE_INFO = "deviceinfo";
    public static final String BLE_CMD_ENERGY = "energy_info";
    public static final String BLE_CMD_STORAGE = "storage_info";

    public static final String BLE_CMD_UPDATE_NAME = "update_dev_name";
    public static final String BLE_CMD_UPDATE_SN = "update_sn";

    public static final String BLE_CMD_DEVICE_ACTIVE_SMS = "active_sms";

    public static final String BLE_CMD_DEVICE_ACTIVE_BIND = "active_bind";
    public static final String BLE_CMD_DEVICE_UNBIND_SMS = "unbind_sms";
    public static final String BLE_CMD_DEVICE_UNBIND = "device_unbind";
    public static final String BLE_CMD_SET_TOKEN = "set_token";

    public static final String BLE_CMD_VIDEO_DIR_UPLOAD = "video_dir_upload";
    public static final String BLE_CMD_VIDEO_LIST = "videolist";
    public static final String BLE_CMD_VIDEO_LIST_URL = "video_list";

    public static final String BLE_CMD_VIDEO_DETAIL_LIST = "video_detail_list";
    public static final String BLE_CMD_VIDEO_FILTER = "video_filter";

    //删除目录下的单个或多个文件
    public static final String BLE_CMD_VIDEO_DELETE_SUB = "video_delete_sub";
    public static final String BLE_CMD_VIDEO_DELETE = "video_delete";
    public static final String BLE_CMD_HIGH_TEMP = "high_temp";

    public static final String BLE_CMD_WEEWA_START = "weewa_start";

    public static final String BLE_CMD_LOW_POWER_OFF = "low_poweroff";

    public static final String BLE_CMD_WEEWA_CONNECTED = "weewa_connected";
    public static final String BLE_CMD_POST_PROC_STATE = "postproc_state";
    public static final String BLE_CMD_START_POST_PROC = "start_postproc";
    public static final String BLE_CMD_POST_PROC_PROGRESS = "postproc_progress";
    public static final String BLE_CMD_STAT_LIST = "get_stat_list";
    public static final String BLE_CMD_COMPETITION_LABEL = "set_label";
    public static final String BLE_CMD_SET_VIDEO_NAME = "set_video_name";
    public static final String BLE_CMD_OSD_CONTROL = "osd_control";
    public static final String BLE_CMD_GET_OSD_SETTINGS = "get_osd_settings";
    public static final String BLE_CMD_START_USB = "start_usb";
    public static final String BLE_CMD_STOP_USB = "stop_usb";

    public static final String BLE_CMD_COMPETITION_ADD_SCORE_STAT = "add_score_stat";    //增加比分和打点
    public static final String BLE_CMD_COMPETITION_ADD_SCORE = "add_score";    //增加比分
    public static final String BLE_CMD_COMPETITION_REMOVE_SCORE = "remove_score";  //减少比分
    public static final String BLE_CMD_COMPETITION_CLEAR_SCORE = "clear_score";   //清空比分
    public static final String BLE_CMD_COMPETITION_ADD_STAT = "add_stat";
    public static final String BLE_CMD_COMPETITION_REMOVE_STAT = "remove_stat";
    public static final String BLE_CMD_COMPETITION_EDIT_STAT = "edit_stat";


    public static final String BLE_CMD_MATCH_TIME_PAUSE = "pause_match_time";
    public static final String BLE_CMD_MATCH_TIME_RESUME = "resume_match_time";
    public static final String BLE_CMD_MATCH_TIME_RESET = "reset_match_time";

    public static final String BLE_CMD_UPLOAD_CALIB = "upload_calib";

    // asx新增
    public static final String BLE_CMD_SET_ETHERNET_DHCP = "ethsetup";

    // 4G测速命令
    public static final String BLE_CMD_4G_TEST_SPEED = "speedtest";

    // exec
    public static final String BLE_CMD_EXEC = "exec";
    public static final String BLE_KEY_COMMAND_LINE = "cmdline";
    public static final String BLE_KEY_BACKGROUND = "bg";

    public static final String BLE_KEY_COMMAND = "c";
    public static final String BLE_KEY_CHALLENGE = "_c";
    public static final String BLE_KEY_TIMESTAMP = "_t";
    public static final String BLE_KEY_AUTHOR = "au";
    public static final String BLE_KEY_MAX_RATE = "mbr";
    public static final String BLE_KEY_CAMPAIGN_NAME = "cn";
    public static final String BLE_KEY_VIDEO_NAME = "vn";
    public static final String BLE_KEY_VIDEO_WIDTH = "vw";
    public static final String BLE_KEY_VIDEO_HEIGHT = "vh";
    public static final String BLE_KEY_VIDEO_TIME = "vt";
    public static final String BLE_KEY_VIDEO_ADDRESS = "vs";
    public static final String BLE_KEY_VIDEO_CITY = "city";
    public static final String BLE_KEY_VIDEO_PLACE_TYPE = "place_type";
    public static final String BLE_KEY_HOST_TEAM = "host_team";
    public static final String BLE_KEY_GUSET_TEAM = "guest_team";
    public static final String BLE_KEY_MOTION_TYPE = "motion_type";
    public static final String BLE_KEY_APPEND = "append";
    public static final String BLE_KEY_PLAYER_NUM = "player_num";
    public static final String BLE_KEY_AVS_MBR = "avs_mbr";
    public static final String BLE_KEY_PROVINCE_CODE = "province_code";
    public static final String BLE_KEY_CITY_CODE = "city_code";
    public static final String BLE_KEY_DISABLE_AVS = "disable_avs";
    public static final String BLE_KEY_HIDE_OSD = "hide_osd";

    // 该字段用于表示比赛id, 不再仅限于直播, 为了兼容和方便, 名称不变!!
    public static final String BLE_KEY_LIVE_COMPETITION_ID = "live_cid";
    public static final String BLE_KEY_COMPETITION_TYPE = "ctype";
    public static final String BLE_KEY_LIVE_RATE = "live_mbr";

    public static final String BLE_KEY_AUDIO = "audio";
    public static final String BLE_KEY_SETTINGS = "settings";
    public static final String BLE_KEY_RESTART = "restart";

    public static final String BLE_KEY_MOBILE = "mb";

    public static final String BLE_KEY_BLE_MAC = "ble";

    public static final String BLE_KEY_MAC = "mac";
    public static final String BLE_KEY_TOKEN = "t";
    public static final String BLE_KEY_CODE = "code";

    public static final String BLE_SMS_SUCCESS_BIND = "0";

    public static final String BLE_SMS_SUCCESS_LOGIN = "1";

    public static final String BLE_SMS_SUCCESS_DEVICE_BOUND = "2";

    public static final String BLE_KEY_PATH = "p";
    public static final String BLE_KEY_VIDEOS = "videos";
    public static final String BLE_KEY_VIDEO = "video";
    public static final String BLE_KEY_GROUP = "groups";

    public static final String BLE_KEY_FILTER = "filter";

    public static final String BLE_KEY_WAIT = "wait";
    public static final String BLE_KEY_ONGOING = "ongoing";
    public static final String BLE_KEY_DONE = "done";
    public static final String BLE_KEY_ERROR = "error";

    public static final String BLE_KEY_PATH_FULL = "path";
    public static final String BLE_KEY_THRESHOLD = "threshold";
    public static final String BLE_KEY_DURATION = "duration";
    public static final String BLE_KEY_PROGRESS = "progress";
    public static final String BLE_KEY_VERSION = "version";
    public static final String BLE_KEY_VERSION_CODE = "version_code";
    public static final String BLE_KEY_SUB = "sub";
    public static final String BLE_KEY_CHARGE = "charge";

    public static final String BLE_KEY_FORCE_END = "force_end";

    public static final String BLE_KEY_HAS_UPLOAD = "has_upload";
    public static final String BLE_KEY_UPLOAD_FAILED = "failed";


    public static final String BLE_CMD_EXT = "ext";
    public static final String BLE_CMD_EXT_TOTAL = "total";
    public static final String BLE_CMD_EXT_ORDER = "order";
    public static final String BLE_CMD_EXT_DATA = "data";
    public static final String BLE_CMD_EXT_BATCH = "batch";

    public static final String BLE_CMD_ADD_GOAL = "add_goal";
    public static final String BLE_CMD_REMOVE_GOAL = "remove_goal";

    public static final String BLE_KEY_GOAL_HOME = "host_team";
    public static final String BLE_KEY_GOAL_HOST = "host_goal";
    public static final String BLE_KEY_GOAL_GUEST = "guest_goal";
    public static final String BLE_KEY_TYPE = "type";
    public static final String BLE_KEY_REMOVE = "remove";

    public static final String BLE_KEY_STATS = "stats";
    public static final String BLE_KEY_GOALS = "goals";
    public static final String BLE_KEY_HOST = "host";
    public static final String BLE_KEY_TIME = "time";

    public static final String BLE_KEY_BITMAP = "bmp";
    public static final String BLE_KEY_FLAG = "flag";

    public static final String BLE_KEY_4G_UPLOAD_SPEED = "upload_speed";

//    download_speed
    public static final String BLE_KEY_4G_DOWNLOAD_SPEED = "download_speed";

    public static final int BLE_CONNECTED_DELAY = 2000;
}
