package com.xh.hotme.http;

import static com.xh.hotme.utils.Constants.TEST_SERVER;

import android.util.Log;

import androidx.annotation.Keep;

import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;

/**
 * Created by liu hong liang on 2016/11/9.
 */
@Keep
public class SdkApi {
    private static final String TAG = SdkApi.class.getSimpleName();

    private static final String HOST_DOMAIN = "https://weewa.rewoo360.com";
    private static final String HOST_DOMAIN_TEST = "https://weewa.test.rewoo360.com";
    private static final String HOST_DOMAIN_GZT = "http://223.109.239.30:61004";
    public static final String TOKEN_PREFIX = "Bearer ";


    public static final String endpoints_login = "/hotme-api/app/user/mobileLogin";

    // 登录注册模块 新增
    public static String mobile_pwd_login() { return getRequestUrl() + "/hotme-api/login/mobile"; } //手机号密码登录 json
    public static String email_pwd_login() { return getRequestUrl() + "/hotme-api/login/email"; } //邮箱密码登录 json

    public static String email_get_code() { return getRequestUrl() + "/hotme-api/system/message/email/send"; } //邮箱获取验证码 x-www
    public static String mobile_get_code() { return getRequestUrl() + "/hotme-api/system/shortMessage/sendSmsCode"; } //手机获取验证码 x-www

    public static String mobile_register() { return getRequestUrl() + "/hotme-api/system/user/regist/mobile"; } //手机密码注册 x-www put

    public static String email_register() { return getRequestUrl() + "/hotme-api/system/user/regist/email"; } //邮箱密码注册 x-www

    public static String mobile_reset_pwd() { return getRequestUrl() + "/hotme-api/system/user/reset/mobile"; } //手机忘记重置密码 x-www

    public static String email_reset_pwd() { return getRequestUrl() + "/hotme-api/system/user/reset/email"; } //邮箱忘记重置密码 x-www
    public static String check_userLogin_status() { return getRequestUrl() + "/hotme-api/app/user/checkUserLoginStatus"; }
    public static final String endpoints_userinfo_get = "/hotme-api/app/user/userInfo";
    public static final String endpoints_userinfo_update = "/hotme-api/app/user";
    public static final String endpoints_avatar = "/hotme-api/system/user/profile/avatar";
    public static final String endpoints_update_phone = "/hotme-api/system/user/profile/updatePhone";

    public static final String endpoints_logout = "/hotme-api/logout";
    public static final String endpoints_user_delete = "/hotme-api/app/user";

    public static final String endpoints_send_sms_code = "/hotme-api/hm/device/sendSmsCode";
    public static final String endpoints_check_sms_code = "/hotme-api/hm/device/checkSmsCode";
    public static final String endpoints_device_unbind = "/hotme-api/hm/device/unbind";
    public static final String endpoints_device_unbind_novalidate = "/hotme-api/hm/device/unBindNoValidate";
    public static final String endpoints_device_info = "/hotme-api/hm/device/find";
    public static final String endpoints_device_list = "/hotme-api/hm/device/query/page";
    public static final String endpoints_video_list = "/hotme-api/hm/video/list";
    public static final String endpoints_video_preload = "/hotme-api/hm/video/preUpload";
    public static final String endpoints_video_upload_progress = "/hotme-api/hm/video/uploadProgress";
    public static final String endpoints_video_upload_progress_add = "/hotme-api/hm/videoUploadProgress/add";
    public static final String endpoints_video_upload_progress_update = "/hotme-api/hm/videoUploadProgress/update";
    public static String video_upload_progress_query_all() { return getRequestUrl() + "/hotme-api/hm/videoUploadProgress/query/all"; }
    public static final String endpoints_get_ota_version = "/hotme-api/hm/appVersion/getOTAByVersion";

    public static final String endpoints_competition_add = "/hotme-api/hm/competition/add";
    public static final String endpoints_competition_update = "/hotme-api/hm/competition/update";
    public static final String endpoints_competition_query_page = "/hotme-api/hm/competition/query/page";
    public static final String endpoints_competition_delete = "/hotme-api/hm/competition/delete/{id}";
    public static final String endpoints_competition_view_query_page = "/hotme-api/hm/competitionView/query/page";
    public static final String endpoints_competition_view_query = "/hotme-api/hm/competitionView/find/{id}";
    public static final String endpoints_competition_view_enableAnalyseTask = "/hotme-api/hm/competitionView/enableAnalyseTask";
    public static final String endpoints_competition_view_canEnableAnalyseTask = "/hotme-api/hm/competitionView/canEnableAnalyseTask";

    public static final String endpoints_video_add = "/hotme-api/hm/video/add";
    public static final String endpoints_video_query = "/hotme-api/hm/video/find/{id}";
    public static final String endpoints_video_query_all = "/hotme-api/hm/video/query/all";
    public static final String endpoints_video_update = "/hotme-api/hm/video/update";
    public static final String endpoints_video_delete_by_batch = "/hotme-api/hm/video/delete/batch";

    public static final String endpoints_competition_query_all = "/hotme-api/hm/competition/query/all";

    public static boolean isTestServer;


    public static String getDomain() {
        return getRequestUrl();
    }

    public static String getRequestUrl() {
        return com.xh.hotme.environment.RuntimeEnvironment.get().apiOrigin();
    }

    public static String getSmsSend() {
        return getRequestUrl() + endpoints_send_sms_code;
    }

    //一键登陆or注册
    public static String getLoginRegister() {
        printUrl(endpoints_login);
        return getRequestUrl() + endpoints_login;
    }

    //获取用户信息
    public static String getUserInfo() {
        printUrl(endpoints_userinfo_get);
        return getRequestUrl() + endpoints_userinfo_get;
    }

    //设置昵称
    public static String setNickName() {
        printUrl(endpoints_userinfo_update);
        return getRequestUrl() + endpoints_userinfo_update;
    }

    //设置用户头像
    public static String setAvatar() {
        return getRequestUrl() + endpoints_avatar;
    }

    //修改手机号
    public static String updateMobile() {
        return getRequestUrl() + endpoints_update_phone;
    }

    //退出登陆
    public static String logout() {
        return getRequestUrl() + endpoints_logout;
    }

    //注销账号
    public static String deleteUser() {
        return getRequestUrl() + endpoints_user_delete;
    }


    //用户收到绑定验证码之后向服务端发送校验验证码请求，并且获取设备mac地址
    public static String checkDeviceSmsCode() {
        return getRequestUrl() + endpoints_check_sms_code;
    }

    //解绑设备
    public static String unbindDevice() {
        return getRequestUrl() + endpoints_device_unbind;
    }

    //绑定的设备列表
    public static String getDeviceList() {
        return getRequestUrl() + endpoints_device_list;
    }


    public static String getDeviceInfo() {
        return getRequestUrl() + endpoints_device_info;
    }

    //绑定的设备列表
    public static String getVideoList() {
        return getRequestUrl() + endpoints_video_list;
    }


    //添加比赛
    public static String addCompetition() {
        return getRequestUrl() + endpoints_competition_add;
    }

    //查询比赛列表
    public static String queryCompetitionList() {
        return getRequestUrl() + endpoints_competition_query_page;
    }

    public static String queryCompetitionViewList() {
        return getRequestUrl() + endpoints_competition_view_query_page;
    }

    public static String queryCompetitionView(String competitionId) {
        return getRequestUrl() + endpoints_competition_view_query.replace("{id}", competitionId);
    }

    public static String deleteCompetition(String competitionId) {
        return getRequestUrl() + endpoints_competition_delete.replace("{id}", competitionId);
    }

    public static String deleteCompetitionLive(String competitionId) {
        return getRequestUrl() + endpoints_competitionLive_delete.replace("{id}", competitionId);
    }

    public static String queryCompetitionLive(String competitionId) {
        return getRequestUrl() + endpoints_competitionLive_query.replace("{id}", competitionId);
    }

    public static String addVideo() {
        return getRequestUrl() + endpoints_video_add;
    }


    public static String queryCompetitionVideo(String videoId) {
        return getRequestUrl() + endpoints_video_query.replace("{id}", videoId);
    }

    public static String queryAllCompetitionVideo() {
        return getRequestUrl() + endpoints_video_query_all;
    }

    public static String updateCompetitionVideo() {
        return getRequestUrl() + endpoints_video_update;
    }

    public static String deleteCompetitionVideo() {
        return getRequestUrl() + endpoints_video_delete_by_batch;
    }

    public static String addVideoUploadProgress() {
        return getRequestUrl() + endpoints_video_upload_progress_add;
    }

    //上传视频
    public static String updateVideoUploadProgress() {
        return getRequestUrl() + endpoints_video_upload_progress_update;
    }

    public static String getOTAVersion(String deviceId, int curVersionCode) {
        return getRequestUrl() + endpoints_get_ota_version + "?deviceId=" + deviceId + "&versionCode=" + curVersionCode;
    }

    private static void printUrl(String path) {
        AppTrace.e(TAG, "http_url=" + getRequestUrl() + path);
    }

    public static String checkWxPushUrl(String url) {
        return getRequestUrl() + endpoint_check_wx_push_url.replace("{url}", url);
    }

    public static String checkDyPushUrl(String url) {
        return getRequestUrl() + endpoint_check_dy_push_url.replace("{url}", url);
    }

    public static String wxCloseLive(String url) {
        return getRequestUrl() + endpoint_wx_close_live.replace("{url}", url);
    }

    public static String dyCloseLive(String url) {
        return getRequestUrl() + endpoint_dy_close_live.replace("{url}", url);
    }

    public static String wxStartLive(String url) {
        return getRequestUrl() + endpoint_wx_start_live.replace("{url}", url);
    }

    public static String dyStartLive(String url) {
        return getRequestUrl() + endpoint_dy_start_live.replace("{url}", url);
    }

    public static String dySendCode(String token) {
        return getRequestUrl() + endpoint_dy_send_code.replace("{token}", token);
    }

    public static String dyValidateCode(String token, String code) {
        return getRequestUrl() + endpoint_dy_validate_code.replace("{token}", token).replace("{code}", code);
    }

    public static String getWxLiveUrl(String name, String token, String coverUrl) {
        return getRequestUrl() + endpoint_get_wx_live_url.replace("{name}", name).replace("{token}", token).replace("{coverUrl}", coverUrl);
    }

    public static String getWxUploadCoverUrl() {
        return getRequestUrl() + endpoint_upload_cover_image_url;
    }

    public static String getDyLiveUrl(String name, String token) {
        return getRequestUrl() + endpoint_get_dy_live_url.replace("{name}", name).replace("{token}", token);
    }

    public static String queryLivePlatform(String id) {
        return getRequestUrl() + endpoint_live_platform_query.replace("{id}", id);
    }

    public static String encodeVideoUrl(String id) {
        return getRequestUrl() + get_encode_video_url.replace("{id}", id);
    }


    public static String getPushStreamWithoutCommentary(String competitionId) {
        return getRequestUrl() + endpoint_get_push_stream_without_commentary.replace("{id}", competitionId);
    }

    public static String getPushStreamWithCommentary(String competitionId) {
        return getRequestUrl() + endpoint_get_push_stream_with_commentary.replace("{id}", competitionId);
    }

    public static String getPullStreamWithoutCommentary(String competitionId) {
        return getRequestUrl() + endpoint_get_pull_stream_without_commentary.replace("{id}", competitionId);
    }

    public static String getPullStreamWithCommentary(String competitionId) {
        return getRequestUrl() + endpoint_get_pull_stream_with_commentary.replace("{id}", competitionId);
    }

    public static String getPullStreamWithCommentaryPassword(String competitionId, String pwd) {
        return getRequestUrl() + endpoint_get_pull_stream_with_commentary_pwd
            .replace("{id}", competitionId)
            .replace("{pwd}", pwd);
    }

    public static String getAppVersion(int type, int versionCode) {
        return getRequestUrl() + endpoint_app_version_url.replace("{type}", String.valueOf(type)).replace("{code}", String.valueOf(versionCode));
    }


    public static String getDeleteCompetitionPoint(String pointId) {
        return getRequestUrl() + endpoint_competitionPoint_delete.replace("{id}", pointId);
    }



    public static String findDeviceSerialNumber(String pointId) {
        return getRequestUrl() + endpoint_device_serialNumber_query + pointId;
    }

    public static String getObsInfo(String id, String mac, int nonce) {
        return getRequestUrl() + endpoint_get_obs_info.replace("{deviceId}", id)
            .replace("{deviceMac}", mac)
            .replace("{nonce}", String.valueOf(nonce));
    }

    //用户协议
    public static final String user_agreement = "https://weewa.rewoo360.com/hotme-api/app/userproxy/rewoo";
    //隐私政策
    public static final String user_privacy = "https://weewa.rewoo360.com/hotme-api/app/private/rewoo";
    //如何获取推流地址
    public static String user_pushUrl() { return getRequestUrl() + "/hotme-api/app/getPushUrl"; }
    //如何使用热我
    public static String user_useCamera() { return getRequestUrl() + "/hotme-api/app/getPushUrl"; }
    //最新版本
    public static String upgrade() { return getRequestUrl() + "/hotme-api/app/upgrade"; }
    //获取直播推流信息
    public static String getPushAddress() { return getRequestUrl() + "/hotme-api/hm/deviceLive/getPushAddress"; }
    //保存推流地址
    public static String savePushAddress() { return getRequestUrl() + "/hotme-api/hm/deviceLive/savePushAddress"; }

    //获取版本信息
    public static final String endpoint_app_version_url = "/hotme-api/hm/appVersion/getLatestAppByVersion?appType={type}&versionCode={code}";


    //根据组合条件进行全部查询接口 推流设置
    public static String live_platform_query_all() { return getRequestUrl() + "/hotme-api/hm/livePlatform/query/all"; }
    //增加对象接口
    public static String live_platform_add() { return getRequestUrl() + "/hotme-api/hm/livePlatform/add"; }
    public static final String endpoint_live_platform_query = "/hotme-api/hm/livePlatform/find/{id}";
    //更新
    public static String live_platform_update() { return getRequestUrl() + "/hotme-api/hm/livePlatform/update"; }
    //删除
    public static String live_platform_delete() { return getRequestUrl() + "/hotme-api/hm/livePlatform/delete"; }

    public static String get_wx_login_code() { return getRequestUrl() + "/hotme-api/wx/channels/wxAuthLoginCode"; }
    public static final String endpoint_check_wx_push_url = "/hotme-api/wx/channels/wxCheckPushStreamAddress?pushStreamAddress={url}";
    public static final String endpoint_wx_start_live = "/hotme-api/wx/channels/wxBeginLive?pushStreamAddress={url}";
    public static final String endpoint_wx_close_live = "/hotme-api/wx/channels/wxCloseLive?pushStreamAddress={url}";
    public static final String endpoint_get_wx_live_url = "/hotme-api/wx/channels/wxPushStreamAddress?description={name}&token={token}&coverImgUrl={coverUrl}";
    public static final String endpoint_upload_cover_image_url = "/hotme-api/wx/channels/wxUploadCoverImage";

    public static String get_dy_login_code() { return getRequestUrl() + "/hotme-api/dy/streamingtool/dyGetQrcode"; }
    public static final String endpoint_check_dy_push_url = "/hotme-api/dy/streamingtool/dyChckPushStreamAddress?pushStreamAddress={url}";
    public static final String endpoint_dy_start_live = "/hotme-api/dy/streamingtool/dyBeginLive?pushStreamAddress={url}";
    public static final String endpoint_dy_close_live = "/hotme-api/dy/streamingtool/dyCloseLive?pushStreamAddress={url}";
    public static final String endpoint_dy_send_code = "/hotme-api/dy/streamingtool/dySendCode?token={token}";
    public static final String endpoint_dy_validate_code = "/hotme-api/dy/streamingtool/dyValidateCode?token={token}&validateCode={code}";
    public static final String endpoint_get_dy_live_url = "/hotme-api/dy/streamingtool/dyPushStreamAddress?description={name}&token={token}";

    public static final String get_encode_video_url = "/hotme-api/hm/video/playAddress/{id}";

    public static String get_encode_videos_url() { return getRequestUrl() + "/hotme-api/hm/video/playAddress"; }

    public static String canEnableAnalyseTask() {
        return getRequestUrl() + endpoints_competition_view_canEnableAnalyseTask;
    }

    public static String enableAnalyseTask() {
        return getRequestUrl() + endpoints_competition_view_enableAnalyseTask;
    }

    //获取所有省份列表
    public static String area_province() { return getRequestUrl() + "/hotme-api/hm/area/province"; }
    public static String area_city() { return getRequestUrl() + "/hotme-api/hm/area/city/"; }

    public static String competition_update() { return getRequestUrl() + endpoints_competition_update; }

    //比赛直播任务
    public static String competitionLive_add() { return getRequestUrl() + "/hotme-api/hm/competitionLive/add"; }
    //查询全部
    public static String competitionLive_query_all() { return getRequestUrl() + "/hotme-api/hm/competitionLive/query/all"; }
    //删除
    public static final String endpoints_competitionLive_delete = "/hotme-api/hm/competitionLive/delete/{id}";
    public static final String endpoints_competitionLive_query = "/hotme-api/hm/competitionLive/find/{id}";
    //更新
    public static String competitionLive_update() { return getRequestUrl() + "/hotme-api/hm/competitionLive/update"; }

    public static String startCompetitionLive(String competitionId) {
        return getRequestUrl() + "/hotme-api/hm/competitionLive/startLive/" + competitionId;
    }

    // 录制的
    public static String competitionRecord_update() { return getRequestUrl() + "/hotme-api/hm/competitionRecord/update"; }
    public static String competitionRecord_add() { return getRequestUrl() + "/hotme-api/hm/competitionRecord/add"; }
    public static String localCompetitionRecord(String action, String id) {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/" + action + (id == null ? "" : "/" + id);
    }
    public static String addLocalCompetitionRecord() {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/addLocal";
    }
    public static String startCompetitionRecord(String id) {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/startRecord/" + id;
    }
    public static String finishCompetitionRecord(String id) {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/finishRecord/" + id;
    }
    public static String pauseCompetitionRecord(String id) {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/stopRecord/" + id;
    }
    public static String queryCompetitionRecord(String id) {
        return getRequestUrl() + "/hotme-api/hm/competitionRecord/find/" + id;
    }
    //获取拉流地址
    public static String competitionLive_pull_stream_addr() { return getRequestUrl() + "/hotme-api/hm/competitionLive/pullStreamAddr"; }

    //获取推流地址
    public static final String endpoint_get_pull_stream_without_commentary = "/hotme-api/hm/competitionLive/pullStreamAddr/playClient?competitionId={id}";
    public static final String endpoint_get_pull_stream_with_commentary = "/hotme-api/hm/competitionLive/pullStreamAddr/commentary?competitionId={id}";
    public static final String endpoint_get_pull_stream_with_commentary_pwd = "/hotme-api/hm/competitionLive/pullStreamAddr/playClientV2?competitionId={id}&encryptText={pwd}";
    public static final String endpoint_get_push_stream_without_commentary = "/hotme-api/hm/competitionLive/pushStreamAddr/camera?competitionId={id}";
    public static final String endpoint_get_push_stream_with_commentary = "/hotme-api/hm/competitionLive/pushStreamAddr/commentary?competitionId={id}";

    //获得设备的拉流地址
    public static String device_get_pull_stream() { return getRequestUrl() + "/hotme-api/hm/device/pullStreamAddr"; }
    public static String device_get_pull_stream_v2() { return getRequestUrl() + "/hotme-api/hm/device/pullStreamAddr/v2"; }


    //打点地址
    public static String competitionPoint_add() { return getRequestUrl() + "/hotme-api/hm/competitionPoint/add"; }
    public static String competitionPoint_add_batch() { return getRequestUrl() + "/hotme-api/hm/competitionPoint/add/batch"; }
    public static String competitionPoint_update() { return getRequestUrl() + "/hotme-api/hm/competitionPoint/update"; }
    public static final String endpoint_competitionPoint_delete = "/hotme-api/hm/competitionPoint/delete/{id}";
    public static String competitionPoint_delete_all() { return getRequestUrl() + "/hotme-api/hm/competitionPoint/delete/batch"; }
    public static String competitionPoint_query_all() { return getRequestUrl() + "/hotme-api/hm/competitionPoint/query/all"; }


    //比赛标记（阶段）
    public static String competitionMark_add() { return getRequestUrl() + "/hotme-api/hm/competitionMark/add"; }
    public static String competitionMark_add_batch() { return getRequestUrl() + "/hotme-api/hm/competitionMark/add/batch"; }
    public static String competitionMark_update() { return getRequestUrl() + "/hotme-api/hm/competitionMark/update"; }
    public static String competitionMark_query() { return getRequestUrl() + "/hotme-api/hm/competitionMark/query/all"; }

    //分享地址
    public static String share_url() { return getRequestUrl() + "/LiveH5/app/liveShare?raceId="; }


    public static final String endpoint_device_serialNumber_query = "/hotme-api/hm/deviceSerialNumber/find/";

    //------------------------套餐 start--------------------------
    //套餐list
    public static String package_list() { return getRequestUrl() + "/hotme-api/hm/package/list"; }
    //录制/直播剩余次数验证
    public static String package_validate() { return getRequestUrl() + "/hotme-api/hm/packageCount/validate"; }
    //提交 录制/直播/云存储 使用量
    public static String submitUseNumber() { return getRequestUrl() + "/hotme-api/hm/packageCount/submitUseNumber"; }
    //根据组合条件进行全部查询接口
    public static String package_query_all() { return getRequestUrl() + "/hotme-api/hm/packageCount/query/all"; }

    public static String package_packagecount_ountStatus() { return getRequestUrl() + "/hotme-api/hm/packageCount/ountStatus"; }

    //创建订单
    public static String pay_createOrder() { return getRequestUrl() + "/hotme-api/hm/pcgPay/createOrder"; }
    public static String createUpgradeOrder() { return getRequestUrl() + "/hotme-api/hm/pcgPay/createUpgradeOrder"; }
    //通知后台url
    public static String pay_notify() { return getRequestUrl() + "/hotme-api/hm/pcgPay/notify"; }
    //续费记录列表
    public static String renewRecord_list() { return getRequestUrl() + "/hotme-api/hm/renewRecord/list"; }
    //套餐使用记录
    public static String packageUsed_list() { return getRequestUrl() + "/hotme-api/hm/packageUsed/list"; }
    //套餐详情
    public static String packageUsed_packageUsedInfo() { return getRequestUrl() + "/hotme-api/hm/packageUsed/packageUsedInfo"; }

    // v2版 新版套餐
    public static String package_query_all_v2() { return getRequestUrl() + "/hotme-api/hm/packageCount/v2/query/deviceId"; }

    public static String package_list_V2() { return getRequestUrl() + "/hotme-api/hm/package/v2/query/deviceId"; }
    public static String pay_createOrder_V2() { return getRequestUrl() + "/hotme-api/hm/packageOrder/v2/create"; }
    public static String createUpgradeOrder_V2() { return getRequestUrl() + "/hotme-api/hm/packageOrder/v2/upgrade"; }
    public static String pay_notify_V2() { return getRequestUrl() + "/hotme-api/hm/packageOrder/v2/notify"; }
    public static String renewRecord_list_V2() { return getRequestUrl() + "/hotme-api/hm/packageOrder/v2/query/all"; }

    public static String submitUseNumber_V2() { return getRequestUrl() + "/hotme-api/hm/packageCount/v2/submitUseNumber"; }

    public static String update_store_V2() { return getRequestUrl() + "/hotme-api/hm/packageCount/v2/update/store/deviceId"; }

    //------------------------套餐 end --------------------------


    public static String device_config_query_all() { return getRequestUrl() + "/hotme-api/hm/deviceConfig/query/all"; }
    public static String update_device_setting() { return getRequestUrl() + "/hotme-api/hm/deviceConfig/update"; }
    public static String update_device_info() { return getRequestUrl() + "/hotme-api/hm/device/update"; }


    public static String device_unbind_novalidate() { return getRequestUrl() + endpoints_device_unbind_novalidate; }
    public static String queryAllCompetition() { return getRequestUrl() + endpoints_competition_query_all; }

    public static final String endpoint_get_obs_info = "/hotme-api/hw/obs/obsInfo?deviceId={deviceId}&nonce={nonce}&deviceMac={deviceMac}";
}
