package com.xh.hotme.http;


import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.liulishuo.filedownloader.upload.backend.obs.ObsInfo;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.bean.AIEnableAnalyseBean;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.BindDeviceRequestBean;
import com.xh.hotme.bean.BindDeviceResultBean;
import com.xh.hotme.bean.BindResultBean;
import com.xh.hotme.bean.CompetitionListRequestBean;
import com.xh.hotme.bean.CompetitionListResultBean;
import com.xh.hotme.bean.DeviceSerialNumber;
import com.xh.hotme.bean.GetObsInfoResultBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.CompetitionResultBean;
import com.xh.hotme.bean.CompetitionVideoRequestBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceSettingRequestBean;
import com.xh.hotme.bean.DeviceSettingResultBean;
import com.xh.hotme.bean.LoginMobileRequestBean;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bean.SmsSendRequestBean;
import com.xh.hotme.bean.UnbindRequestBean;
import com.xh.hotme.bean.UserAvatarBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bean.VideoDurationUpdateBean;
import com.xh.hotme.bean.VideoNameUploadBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.HmVideoUploadProgress;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.bean.GetVideoUrlBean;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonBooleanResponse;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.live.bean.CommonStringResponse;
import com.xh.hotme.live.bean.CompetitionMarkBean;
import com.xh.hotme.live.bean.CompetitionMarkListBean;
import com.xh.hotme.live.bean.CompetitionStatBean;
import com.xh.hotme.live.bean.CompetitionStatListBean;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.HmCompetitionPoint;
import com.xh.hotme.live.bean.HmLivePlatform;
import com.xh.hotme.live.bean.LiveLoginCode;
import com.xh.hotme.live.bean.LiveStreamAddress;
import com.xh.hotme.live.bean.LiveTaskBean;
import com.xh.hotme.live.bean.LiveTaskListBean;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.record.bean.HmCompetitionMark;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.utils.AESTools;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.RegExpUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.Utils;
import com.xh.hotme.utils.VideoUtil;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;

public class ApiUtil {
    private final static String TAG = "ApiUtil";

    // Protocol 3 recording commands must be authorized and fenced by the server.
    // This endpoint returns an envelope with null data, including when code is 202.
    public static void startCompetitionLive(String competitionId, IGenericApiCallback<CommonResponse> listener) {
        okhttp3.Request.Builder request = new okhttp3.Request.Builder()
            .url(SdkApi.startCompetitionLive(competitionId))
            .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json;charset=utf-8"), "{}"));
        if(!TextUtils.isEmpty(Constants.HEADER_TOKEN)) {
            request.header(Constants.HEADER_TOKEN_NAME, SdkApi.TOKEN_PREFIX + Constants.HEADER_TOKEN);
        }
        OkHttpUtil.enqueue(request.build(), new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException error) {
                MainHandler.runOnUiThread(() -> {
                    listener.onFail("-1", error.getMessage());
                    listener.onFinish();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try(okhttp3.Response closedResponse = response) {
                    CommonResponse result = response.body() == null ? null
                        : new Gson().fromJson(response.body().string(), CommonResponse.class);
                    if(!response.isSuccessful() || result == null || (result.code != 200 && result.code != 202)) {
                        String code = result == null ? String.valueOf(response.code()) : String.valueOf(result.code);
                        String message = result == null || TextUtils.isEmpty(result.msg) ? "启动直播失败，请检查设备状态" : result.msg;
                        MainHandler.runOnUiThread(() -> {
                            listener.onFail(code, message);
                            listener.onFinish();
                        });
                    } else {
                        MainHandler.runOnUiThread(() -> {
                            listener.onSuccess(result);
                            listener.onFinish();
                        });
                    }
                } catch(Exception error) {
                    MainHandler.runOnUiThread(() -> {
                        listener.onFail("-2", "启动直播响应异常");
                        listener.onFinish();
                    });
                }
            }
        }, 120);
    }

    public static void checkDyPushUrl(String pushUrlNoEncoding, IGenericApiCallback<Boolean> listener) {
        String encUrl = pushUrlNoEncoding;
        try {
            encUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.checkDyPushUrl(encUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonBooleanResponse>(CommonBooleanResponse.class) {
            @Override
            public void onDataSuccess(CommonBooleanResponse data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String msg) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, msg);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void checkWxPushUrl(String pushUrlNoEncoding, IGenericApiCallback<Boolean> listener) {
        String encUrl = pushUrlNoEncoding;
        try {
            encUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.checkWxPushUrl(encUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonBooleanResponse>(CommonBooleanResponse.class) {
            @Override
            public void onDataSuccess(CommonBooleanResponse data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String msg) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, msg);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getDyLiveUrl(String competitionName, String token, IGenericApiCallback<LiveStreamAddress> listener) {
        String url = SdkApi.getDyLiveUrl(competitionName, token);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<LiveStreamAddress>(null, new TypeToken<LiveStreamAddress>() {
        }.getType()) {
            @Override
            public void onDataSuccess(LiveStreamAddress data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getWxLiveUrl(String competitionName, String token, String coverUrl, IGenericApiCallback<LiveStreamAddress> listener) {
        String url = SdkApi.getWxLiveUrl(competitionName, token, OkHttpUtil.urlEncoded(coverUrl));
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<LiveStreamAddress>(null, new TypeToken<LiveStreamAddress>() {
        }.getType()) {
            @Override
            public void onDataSuccess(LiveStreamAddress data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getWxLiveUrl fail: code=" + code + ", messsage=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getDyLoginCode(IGenericApiCallback<LiveLoginCode> listener) {
        OkHttpUtil.postData(SdkApi.get_dy_login_code(), "{}", null, new OkHttpCallbackDecode<LiveLoginCode>(null, new TypeToken<LiveLoginCode>() {
        }.getType()) {
            @Override
            public void onDataSuccess(LiveLoginCode data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getDyLoginCode fail: code=" + code + ", messsage=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getWxLoginCode(IGenericApiCallback<LiveLoginCode> listener) {
        OkHttpUtil.postData(SdkApi.get_wx_login_code(), "{}", null, new OkHttpCallbackDecode<LiveLoginCode>(null, new TypeToken<LiveLoginCode>() {
        }.getType()) {
            @Override
            public void onDataSuccess(LiveLoginCode data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getWxLoginCode fail: code=" + code + ", messsage=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void updateLivePlatform(HmLivePlatform plt, IGenericApiCallback<HmLivePlatform> listener) {
        String url = SdkApi.live_platform_update();
        OkHttpUtil.postData(url, new Gson().toJson(plt), null, new OkHttpCallbackDecode<HmLivePlatform>(null, new TypeToken<HmLivePlatform>() {
        }.getType()) {
            @Override
            public void onDataSuccess(HmLivePlatform data) {
                if (data != null) {

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateLivePlatform fail: code=" + code + ", messsage=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getLivePlatform(String id, IGenericApiCallback<HmLivePlatform> listener) {
        String url = SdkApi.queryLivePlatform(id);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<HmLivePlatform>(null, new TypeToken<HmLivePlatform>() {
        }.getType()) {
            @Override
            public void onDataSuccess(HmLivePlatform data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getLivePlatform fail: code=" + code + ", messsage=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void addLivePlatform(int pltType, IGenericApiCallback<HmLivePlatform> listener) {
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        if (deviceInfo == null) {
            MainHandler.runOnUiThread(() -> {
                //接口回调通知
                if (listener != null) {
                    listener.onFail("-1", "尚未连接到相机");
                }
            });
            return;
        }

        String url = SdkApi.live_platform_add();
        HashMap<String, String> params = new HashMap<>();
        params.put("platformType", String.valueOf(pltType));
        params.put("deviceId", deviceInfo.device_id);
        OkHttpUtil.postData(url, params, null, new OkHttpCallbackDecode<HmLivePlatform>(null, new TypeToken<HmLivePlatform>() {
        }.getType()) {
            @Override
            public void onDataSuccess(HmLivePlatform data) {
                if (data != null) {

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getOTAVersion(IGenericApiCallback<OTAVersionInfo> listener) {
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        if (deviceInfo == null) {
            MainHandler.runOnUiThread(() -> {
                //接口回调通知
                if (listener != null) {
                    listener.onFail("-1", "尚未连接到相机");
                }
            });
            return;
        }

        String url = SdkApi.getOTAVersion(deviceInfo.device_id, deviceInfo.ota_version_code);

        AppTrace.d(TAG, "url=" + url);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        if (TextUtils.isEmpty(Constants.HEADER_TOKEN)) {
            headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);
        }

        OkHttpUtil.get(url, headers, new OkHttpCallbackDecode<OTAVersionInfo>(null, new TypeToken<OTAVersionInfo>() {
        }.getType()) {
            @Override
            public void onDataSuccess(OTAVersionInfo data) {

                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d("getOTAVersion", "onFailure:  code = " +code +", message=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getDeviceInfo(String deviceId, IGenericApiCallback<BindDeviceBean> listener) {

        if (TextUtils.isEmpty(deviceId)) {
            return;
        }
        String url = SdkApi.getDeviceInfo() + "/" + deviceId;

        AppTrace.d(TAG, "url=" + url);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);

        OkHttpUtil.postData(url, "", headers, new OkHttpCallbackDecode<BindDeviceBean>(null, new TypeToken<BindDeviceBean>() {
        }.getType()) {
            @Override
            public void onDataSuccess(BindDeviceBean data) {
                if (data != null) {

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void deleteCompetition(String competitionId, IApiCallback listener) {
        if (competitionId.isEmpty()) return;
        String url = SdkApi.deleteCompetition(competitionId);
        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void deleteCompetitionLive(String competitionId, IApiCallback listener) {
        if (competitionId.isEmpty()) return;
        String url = SdkApi.deleteCompetitionLive(competitionId);

        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getCompetitionLive(String competitionId, IGenericApiCallback<HmCompetitionLive> listener) {
        String url = SdkApi.queryCompetitionLive(competitionId);
        OkHttpUtil.postData(url, new HashMap<>(), null, new OkHttpCallbackDecode<HmCompetitionLive>() {
            @Override
            public void onDataSuccess(HmCompetitionLive data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getCompetitionView(String competitionId, IGenericApiCallback<HmCompetition> listener) {
        String url = SdkApi.queryCompetitionView(competitionId);
        OkHttpUtil.postData(url, new HashMap<>(), null, new OkHttpCallbackDecode<HmCompetition>() {
            @Override
            public void onDataSuccess(HmCompetition data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getCompetitionViewList(CompetitionListRequestBean videoBean, IGenericApiCallback<CompetitionListResultBean> listener) {

        if (videoBean == null) {
            return;
        }
        String url = SdkApi.queryCompetitionViewList();

        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(url, new Gson().toJson(videoBean), null, new OkHttpCallbackDecode<CompetitionListResultBean>() {
            @Override
            public void onDataSuccess(CompetitionListResultBean data) {
                if (data != null) {

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getCompetitionVideo(CompetitionVideoRequestBean videoBean, IGenericApiCallback<List<HmVideo>> listener) {
        if (videoBean == null) {
            return;
        }
        String url = SdkApi.queryAllCompetitionVideo();

        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(SdkApi.queryAllCompetitionVideo(), new Gson().toJson(videoBean), null, new OkHttpCallbackDecode<List<HmVideo>>(null, new TypeToken<List<HmVideo>>() {
        }.getType()) {
            @Override
            public void onDataSuccess(List<HmVideo> data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void updateCompetitionVideoName(VideoNameUploadBean videoBean, IApiCallback listener) {
        if (videoBean == null) {
            return;
        }
        String url = SdkApi.updateCompetitionVideo();
        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(url, new Gson().toJson(videoBean), null, new OkHttpCallbackDecode<HmVideo>() {
            @Override
            public void onDataSuccess(HmVideo data) {
                if (data != null) {
                    AppTrace.d("updateCompetitionVideoName", "videoId = " + data.getVideoId());

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void deleteCompetitionVideo(List<String> videoList, IApiCallback listener) {
        if (videoList == null) {
            return;
        }
        String url = SdkApi.deleteCompetitionVideo();
        AppTrace.d(TAG, "url=" + url);
        FormBody.Builder builder = new FormBody.Builder();
        for (String id : videoList) {
            builder.add("ids", id);
        }

        OkHttpUtil.postFormData(url, builder, null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                AppTrace.d("deleteCompetitionVideo", "delete ok");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getDeviceConfig(String deviceId, int deviceType, IGenericApiCallback<List<DeviceSettingResultBean>> listener) {

        String url = SdkApi.device_config_query_all();
        AppTrace.d(TAG, "url=" + url);

        DeviceSettingRequestBean requestBean = new DeviceSettingRequestBean(deviceId, Constants.ROLE, deviceType);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);

        OkHttpUtil.postData(url, new Gson().toJson(requestBean), headers, new OkHttpCallbackDecode<List<DeviceSettingResultBean>>(null, new TypeToken<List<DeviceSettingResultBean>>() {
        }.getType()) {
            @Override
            public void onDataSuccess(List<DeviceSettingResultBean> data) {
                AppTrace.d("getDeviceSetting", "getDeviceSetting");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void updateDeviceSetting(DeviceSettingResultBean deviceSettingResultBean, IApiCallback listener) {

        String url = SdkApi.update_device_setting();
        AppTrace.d(TAG, "url=" + url);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);

        OkHttpUtil.postData(url, new Gson().toJson(deviceSettingResultBean), headers, new OkHttpCallbackDecode<DeviceSettingResultBean>() {
            @Override
            public void onDataSuccess(DeviceSettingResultBean data) {
                AppTrace.d("getDeviceSetting", "getDeviceSetting");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }


    public static void updateDeviceName(String deviceId, String deviceName, IApiCallback listener) {
        String url = SdkApi.update_device_info();
        AppTrace.d(TAG, "url=" + url);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("deviceId", deviceId);
        jsonObject.addProperty("deviceName", deviceName);

        OkHttpUtil.postData(url, new Gson().toJson(jsonObject), headers, new OkHttpCallbackDecode<BindDeviceBean>() {
            @Override
            public void onDataSuccess(BindDeviceBean data) {
                AppTrace.d("updateDeviceName", "updateDeviceName");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void reportCompetitionStatus(String competitionId, String status) {
        String url = SdkApi.competition_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("competitionStatus", status);

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionResultBean>(CompetitionResultBean.class) {
            @Override
            public void onDataSuccess(CompetitionResultBean data) {
                AppTrace.d(TAG, "reportCompetitionStatus: ok, competitionId= " + competitionId + ", status=" + status);

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "onFailure: ");
            }
        });
    }

    public static void reportCompetitionScore(String competitionId, int hostScore, int awayScore) {
        String url = SdkApi.competition_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("homeTeamScore", String.valueOf(hostScore));
        paramsdata.put("awayTeamScore", String.valueOf(awayScore));

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionResultBean>(CompetitionResultBean.class) {
            @Override
            public void onDataSuccess(CompetitionResultBean data) {
                AppTrace.d(TAG, "reportCompetitionScore: ok, competitionId= " + competitionId);

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "reportCompetitionScore onFailure: " + message);
            }
        });
    }

    public static void getObsInfo(IApiCallback listener) {
        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        final String deviceId;
        final String mac;
        if(deviceInfo == null) {
            deviceId = Utils.generateMixedString(19);  // 生成随机19位设备ID
            mac = Utils.generateMixedString(17);  // 生成设随机17位mac
        } else {
            deviceId = deviceInfo.device_id;
            mac = deviceInfo.mac;
        }
        int nonce = (int)(System.currentTimeMillis() % 10000);
        String url = SdkApi.getObsInfo(deviceId, mac, nonce);
        OkHttpUtil.postData(url, "", null, new OkHttpCallbackDecode<GetObsInfoResultBean>() {
            @Override
            public void onDataSuccess(GetObsInfoResultBean data) {
                AppTrace.d(TAG, "onDataSuccess success");
                String key = null;
                try {
                    key = Base64.encodeToString(String.format("%s&%04X", deviceId, (nonce + 2024) / 2).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                    key = key.trim();
                } catch(Exception e) {
                    AppTrace.e(TAG, "getObsInfo parse fail");
                    e.printStackTrace();
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onFail("500", "failed to get key");
                        }
                    });
                    return;
                }

                // decrypt
                String host = AESTools.decrypt(data.accessDomainName, key);
                String bucket = AESTools.decrypt(data.bucketName, key);
                String akey = AESTools.decrypt(data.accessKey, key);
                String skey = AESTools.decrypt(data.secretKey, key);
                String endPoint = AESTools.decrypt(data.endpoint, key);
                if(TextUtils.isEmpty(host) ||
                    TextUtils.isEmpty(bucket) ||
                    TextUtils.isEmpty(akey) ||
                    TextUtils.isEmpty(skey) ||
                    TextUtils.isEmpty(endPoint)) {
                    MainHandler.runOnUiThread(() -> {

                        AppTrace.d("getObsInfo Exception", "failed to decrypt");
                        //接口回调通知
                        if (listener != null) {
                            listener.onFail("500", "failed to decrypt");
                        }
                    });
                    return;
                }

                // save
                ObsInfo obsInfo = new ObsInfo();
                obsInfo.hostName = host;
                obsInfo.bucketName = bucket;
                obsInfo.accessKeyId = akey;
                obsInfo.secretAccessKey = skey;
                obsInfo.endPoint = endPoint;
                FileDownloadUtils.setObsConfig(obsInfo);
                // success
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d("getObsInfo", "code=" + code + ", message=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void addCompetition(HmCompetition videoBean, IGenericApiCallback<HmCompetition> listener) {
        if (videoBean == null) {
            return;
        }
        String url = SdkApi.addCompetition();

        AppTrace.d("", "url=" + url);

        OkHttpUtil.postData(url, new Gson().toJson(videoBean), null, new OkHttpCallbackJson<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject respData) {
                if (respData != null) {
                    AppTrace.d("addCompetition", "respData = " + respData.toString());

                    String data = respData.optString("data");
                    Integer code = respData.optInt("code");
                    String msg = respData.optString("msg");

                    if (code == 200 || code == 201) {

                        if (TextUtils.isEmpty(data) || "null".equals(data)) {//数据是null的
                            if (listener != null) {
                                listener.onSuccess(null);
                            }
                            return;
                        }
                        MainHandler.runOnUiThread(() -> {
                            HmCompetition dataObject = GsonUtils.GsonToBean(data, HmCompetition.class);
                            if (listener != null) {
                                listener.onSuccess(dataObject);
                            }
                        });
                    } else if (code == 401) {
                        MainHandler.runOnUiThread(() -> {
                            MobileLoginActivity.start(ActivityStackManager.impl.getCurrentActivity());
                        });

                    } else {
                        MainHandler.runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onFail("" + code, msg);
                            }
                        });
                    }

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void queryVideoStatus(String videoId, IGenericApiCallback<List<HmVideoUploadProgress>> listener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("videoId", videoId);
        OkHttpUtil.postData(SdkApi.video_upload_progress_query_all(), params, null, new OkHttpCallbackDecode<List<HmVideoUploadProgress>>(null, new TypeToken<List<HmVideoUploadProgress>>() {
        }.getType()) {
            @Override
            public void onDataSuccess(List<HmVideoUploadProgress> data) {
                if (data != null && !data.isEmpty()) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("602", "data is empty");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void queryVideo(String videoId, IGenericApiCallback<HmVideo> listener) {
        if (videoId.isEmpty()) return;
        String url = SdkApi.queryCompetitionVideo(videoId);
        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<HmVideo>() {
            @Override
            public void onDataSuccess(HmVideo data) {
                if (data != null) {
                    AppTrace.d(TAG, "video id = " + data.getVideoId());

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void addVideo(HmVideo videoBean, IGenericApiCallback<HmVideo> listener) {
        if (videoBean == null) {
            return;
        }
        String url = SdkApi.addVideo();

        AppTrace.d("", "url=" + url);

        OkHttpUtil.postData(SdkApi.addVideo(), new Gson().toJson(videoBean), null, new OkHttpCallbackDecode<HmVideo>() {
            @Override
            public void onDataSuccess(HmVideo data) {
                if (data != null) {
                    AppTrace.d("LoginInteract", "token = " + data.getVideoId());
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void updateCompetitionImage(String competitionId, String thumbUrl) {
        ApiUtil.getCompetitionView(competitionId, new IGenericApiCallback<HmCompetition>() {
            @Override
            public void onSuccess(HmCompetition data) {
                HashMap<String, String> params = new HashMap<>();
                params.put("competitionId", competitionId);
                params.put("competitionImage", thumbUrl);
                String url = data.getCompetitionType() == Constants.COMPETITION_TYPE_RECORD ? SdkApi.competition_update() : SdkApi.competitionLive_update();
                OkHttpUtil.postData(url, params, null, new OkHttpCallbackDecode<CompetitionResultBean>() {
                    @Override
                    public void onDataSuccess(CompetitionResultBean data) {
                    }
                });
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "failed to get competition view: " + message);
            }

            @Override
            public void onFinish() {
            }
        });
    }

    public static void updateVideoThumbUrl(String competitionId, String videoId, String thumbUrl, IApiCallback listener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("competitionId", competitionId);
        params.put("videoId", videoId);
        params.put("videoImage", thumbUrl);

        OkHttpUtil.postData(SdkApi.updateCompetitionVideo(), params, null, new OkHttpCallbackDecode<HmVideoUploadProgress>() {
            @Override
            public void onDataSuccess(HmVideoUploadProgress data) {
                AppTrace.i("updateVideoThumbUrl", "ok");
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void addVideoUploadStatus(String videoId, IGenericApiCallback<HmVideoUploadProgress> listener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("videoId", videoId);
        params.put("uploadStatus", String.valueOf(Constants.STATUS_STARTING));
        params.put("uploadSpeed", "0");
        params.put("uploadProgress", "0");
        params.put("uploadType", String.valueOf(VideoUtil.VIDEO_UPLOAD_TYPE_MANUAL)); // 2表示手动上传

        OkHttpUtil.postData(SdkApi.addVideoUploadProgress(), params, null, new OkHttpCallbackDecode<HmVideoUploadProgress>() {
            @Override
            public void onDataSuccess(HmVideoUploadProgress data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void updateVideoUploadStatus(String videoId, String domainId, int status, double speed, long progress, String url, IApiCallback listener) {
        HmVideoUploadProgress progressBean = new HmVideoUploadProgress();
        progressBean.setDomainId(domainId);
        progressBean.setUploadStatus(status);
        progressBean.setUploadProgress(progress);
        progressBean.setVideoId(videoId);
        progressBean.setUploadSpeed(speed);
        progressBean.setVideoUrl(url);

        AppTrace.i("uploadStatus", "upload = " + GsonUtils.GsonString(progressBean));

        OkHttpUtil.postData(SdkApi.updateVideoUploadProgress(), new Gson().toJson(progressBean), null, new OkHttpCallbackDecode<HmVideoUploadProgress>() {
            @Override
            public void onDataSuccess(HmVideoUploadProgress data) {
                AppTrace.i("uploadStatus", "ok");
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }


    //获取推流地址
    public static void getPushStreamWithoutCommentary(String competitionId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.getPushStreamWithoutCommentary(competitionId);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getPushStreamWithCommentary(String competitionId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.getPushStreamWithCommentary(competitionId);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    //获取拉流地址
    public static void getPullStreamUrlWithoutCommentary(String competitionId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.getPullStreamWithoutCommentary(competitionId);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getPullStreamUrlWithCommentary(String competitionId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.getPullStreamWithCommentary(competitionId);

        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getPullStreamUrlWithCommentaryPassword(String competitionId, String pwd, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.getPullStreamWithCommentaryPassword(competitionId, pwd);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getPullStreamUrlWithDeviceId(String deviceId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.device_get_pull_stream();

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("deviceId", deviceId);

        OkHttpUtil.postFormData(url, builder, null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getPullStreamUrlWithDeviceId onFailure: code= " + code +", message = " +message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getPullStreamUrlWithDeviceIdV2(String deviceId,String competitionId, IGenericApiCallback<PullStreamAddrBean> listener) {
        String url = SdkApi.device_get_pull_stream_v2();

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("deviceId", deviceId);
        builder.add("competitionId", competitionId);

        OkHttpUtil.postFormData(url, builder, null, new OkHttpCallbackDecode<PullStreamAddrBean>(PullStreamAddrBean.class) {
            @Override
            public void onDataSuccess(PullStreamAddrBean data) {
                if (data.code == 200) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });
                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(String.valueOf(data.code), data.msg);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "getPullStreamUrlWithDeviceId onFailure: code= " + code +", message = " +message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static boolean hasTodayTask(List<HmCompetitionLive> list) {
        // 只显示今天的直播
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (HmCompetitionLive bean : list) {
            try {
                Date date = format.parse(bean.competitionTime);
                if (date != null) {
                    cal.setTime(date);
                    if (year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH) && day == cal.get(Calendar.DAY_OF_MONTH)) {
                        return true;
                    }
                }
            } catch (ParseException e) {
            }
        }
        return false;
    }

    //是否有直播任务
    public static void getLiveTaskList(IActionCallback commonListener) {

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("competitionBeginTime", TimeUtil.getTodayString());
        paramsdata.put("competitionEndTime", TimeUtil.getTodayEndString());

        OkHttpUtil.postData(SdkApi.competitionLive_query_all(), paramsdata, null,
                new OkHttpCallbackDecode<LiveTaskListBean>(LiveTaskListBean.class) {
                    @Override
                    public void onDataSuccess(LiveTaskListBean data) {

                        MainHandler.runOnUiThread(() -> {
                            try {
                                boolean hasTask = hasTodayTask(data.data);
                                commonListener.onReturn(hasTask ? 1 : 0);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });


                    }

                    @Override
                    public void onFailure(String code, String message) {
                        AppTrace.d(TAG, "getLiveTaskList fail: code=" + code + ", message=" + message);
                        MainHandler.runOnUiThread(() -> {
                            ToastUtil.s(message);
                        });
                    }
                });
    }

    public static void reportCompetitionLiveScore(String competitionId, int hostScore, int awayScore) {
        String url = SdkApi.competitionLive_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("homeTeamScore", String.valueOf(hostScore));
        paramsdata.put("awayTeamScore", String.valueOf(awayScore));

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                AppTrace.d(TAG, "reportCompetitionLiveScore: ok, competitionId= " + competitionId);

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "reportCompetitionLiveScore onFailure: " + message);
            }
        });
    }

    public static void comprehensiveReportCompetitionLiveStatus(String competitionId, String pushUrlNoEncoding, int thirdPlt, int status) {
        reportCompetitionLiveStatus(competitionId, status);
        if (thirdPlt != -1) {
            switch (thirdPlt) {
                case Constants.LIVE_PLATFORM_WX_CHANNEL:
                    switch (status) {
                        case Constants.COMPETITION_START:
                            startWxLive(pushUrlNoEncoding);
                            break;
                        case Constants.COMPETITION_STOP:
                            stopWxLive(pushUrlNoEncoding);
                            break;
                    }
                    break;
                case Constants.LIVE_PLATFORM_DY_CHANNEL:
                    switch (status) {
                        case Constants.COMPETITION_START:
                            startDyLive(pushUrlNoEncoding);
                            break;
                        case Constants.COMPETITION_STOP:
                            stopDyLive(pushUrlNoEncoding);
                            break;
                    }
                    break;
            }
        }
    }

    public static void stopWxLive(String pushUrlNoEncoding) {
        String encPushUrl = pushUrlNoEncoding;
        try {
            encPushUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.wxCloseLive(encPushUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonResponse>(CommonResponse.class) {
            @Override
            public void onDataSuccess(CommonResponse data) {
            }
        });
    }

    public static void stopDyLive(String pushUrlNoEncoding) {
        String encPushUrl = pushUrlNoEncoding;
        try {
            encPushUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.dyCloseLive(encPushUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonResponse>(CommonResponse.class) {
            @Override
            public void onDataSuccess(CommonResponse data) {
            }
        });
    }

    public static void startWxLive(String pushUrlNoEncoding) {
        String encPushUrl = pushUrlNoEncoding;
        try {
            encPushUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.wxStartLive(encPushUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonResponse>(CommonResponse.class) {
            @Override
            public void onDataSuccess(CommonResponse data) {
            }
        });
    }

    public static void startDyLive(String pushUrlNoEncoding) {
        String encPushUrl = pushUrlNoEncoding;
        try {
            encPushUrl = URLEncoder.encode(pushUrlNoEncoding, "utf-8");
        } catch (UnsupportedEncodingException e) {
        }
        String url = SdkApi.dyStartLive(encPushUrl);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonResponse>(CommonResponse.class) {
            @Override
            public void onDataSuccess(CommonResponse data) {
            }
        });
    }

    public static void sendDyCode(String token, IGenericApiCallback<Boolean> listener) {
        String url = SdkApi.dySendCode(token);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonBooleanResponse>(CommonBooleanResponse.class) {
            @Override
            public void onDataSuccess(CommonBooleanResponse data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String msg) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, msg);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void validateDyCode(String token, String code, IGenericApiCallback<Boolean> listener) {
        String url = SdkApi.dyValidateCode(token, code);
        OkHttpUtil.postData(url, "{}", null, new OkHttpCallbackDecode<CommonBooleanResponse>(CommonBooleanResponse.class) {
            @Override
            public void onDataSuccess(CommonBooleanResponse data) {
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String msg) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, msg);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void reportCompetitionLiveStatus(String competitionId, int status) {
        String url = SdkApi.competitionLive_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("competitionStatus", String.valueOf(status));

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                AppTrace.d(TAG, "reportCompetitionLiveStatus: ok, competitionId= " + competitionId + ", status=" + status);

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "reportCompetitionLiveStatus onFailure, code: " + code + ", msg: " + message + ", competition id: " + competitionId);
            }
        });
    }


    public static void updateCompetitionLiveName(String competitionId, String competitionName, String hostName, String guestName) {
        String url = SdkApi.competitionLive_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("competitionName", competitionName);
        paramsdata.put("homeTeamName", hostName);
        paramsdata.put("awayTeamName", guestName);

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                AppTrace.d(TAG, "updateCompetitionName: ok, competitionId= " + competitionId + ", competitionName=" + competitionName + ", hostTeamName=" + hostName + ",guestTeamName=" + guestName);

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "onFailure, code: " + code + ", msg: " + message + ", competition id: " + competitionId);
            }
        });
    }

    public static void deleteUser(final IApiCallback onLoginListener) {

        String url = SdkApi.deleteUser();

        OkHttpUtil.delete(url, null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                AppTrace.d("DeleteInteract", "");
                //保存登录信息
                LoginManager.clearLoginInfo();
                EventBus.getDefault().post(new LoginEvent());
                CameraManager.release();
                BluetoothManager.getImpl().disconnectBle();
                DeviceManager.getImpl().clearDeviceList();
                CameraManager.setDeviceId("");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onLoginListener != null) {
                        onLoginListener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFinish();
                    }
                });
            }
        });
    }


    public static void checkSmsCode(final String account, String smscode, final IGenericApiCallback<BindResultBean> onLoginListener) {
        if (!RegExpUtil.isMobileNumber(account)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-1", HotmeApplication.getContext().getString(R.string.login_error_phone_format));
            }
            return;
        }
        if (TextUtils.isEmpty(smscode)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_error_verify_code_null));
            }
            return;
        }

        LoginMobileRequestBean loginMobileRequestBean = new LoginMobileRequestBean();
        loginMobileRequestBean.setMobile(account);
        loginMobileRequestBean.setSmscode(smscode);

        String url = SdkApi.checkDeviceSmsCode() + "?mobile=" + account + "&code=" + smscode;

        OkHttpUtil.get(url, null, new OkHttpCallbackDecode<BindResultBean>() {
            @Override
            public void onDataSuccess(BindResultBean data) {
                if (data != null) {
                    AppTrace.d("BindInteract", "deviceId = " + data.getMac());
                    //保存登录信息

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (onLoginListener != null) {
                            onLoginListener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFinish();
                    }
                });
            }
        });
    }

    // 这个可以做一个通用的吧，然后账户的发送和注册和遗忘 6个接口
    public static void submitLogin(final String account, String smscode, final IGenericApiCallback<LoginResultBean> onLoginListener) {
        submitLogin(account,smscode,"sms",onLoginListener);
    }

    public static void submitLogin(final String account, String smscode,String type, final IGenericApiCallback<LoginResultBean> onLoginListener) {
        String url = "";
        if (type.equals("sms")) {
            if (!RegExpUtil.isMobileNumber(account)) {
                if (onLoginListener != null) {
                    onLoginListener.onFail("-1", HotmeApplication.getContext().getString(R.string.login_error_phone_format));
                }
                return;
            }
            if (TextUtils.isEmpty(smscode)) {
                if (onLoginListener != null) {
                    onLoginListener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_error_verify_code_null));
                }
                return;
            }

            url = SdkApi.getLoginRegister() + "?mobile=" + account + "&code=" + smscode;

            OkHttpUtil.get(url, null, new OkHttpCallbackDecode<LoginResultBean>() {
                @Override
                public void onDataSuccess(LoginResultBean data) {
                    if (data != null) {
                        AppTrace.d("LoginInteract", "login token received");

                        LoginManager.saveUserToken(data.getToken());
                        Constants.HEADER_TOKEN = data.getToken();

                        MainHandler.runOnUiThread(() -> {
                            //接口回调通知
                            if (onLoginListener != null) {
                                onLoginListener.onSuccess(data);
                            }
                        });

                    } else {
                        MainHandler.runOnUiThread(() -> {
                            if (onLoginListener != null) {
                                onLoginListener.onFail("-1", "data is null");
                            }
                        });
                    }
                }

                @Override
                public void onFailure(String code, String message) {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFail(code, message);
                        }
                    });
                }

                @Override
                public void onFinish() {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFinish();
                        }
                    });
                }
            });
        }else{

            if (TextUtils.isEmpty(smscode)) {
                if (onLoginListener != null) {
                    onLoginListener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_input_password));
                }
                return;
            }
            // 需要区分当前是手机还是邮箱
            String phoneRegex = "^1[0-9]{10}$";
            // 邮箱正则：简单验证
            String emailRegex = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$";

            if (account.matches(phoneRegex)) {
                // 是手机号
                // ... 处理手机号逻辑
//                ToastUtil.s("是手机");
                url = SdkApi.mobile_pwd_login();

            } else if (account.matches(emailRegex)) {
                // 是邮箱
                // ... 处理邮箱逻辑
//                ToastUtil.s("是邮箱");
                url = SdkApi.email_pwd_login();
            } else {
                // 都不是，提示用户
                ToastUtil.s(R.string.input_phone_or_email);
                return;
            }

            HashMap<String, String> paramsdata = new HashMap<>();
            paramsdata.put("username", account);
            paramsdata.put("password",smscode);

            OkHttpUtil.postData(url,paramsdata,null, new OkHttpCallbackDecode<LoginResultBean>() {
                @Override
                public void onDataSuccess(LoginResultBean data) {
                    if (data != null) {
                        AppTrace.d("LoginInteract", "login token received");

                        LoginManager.saveUserToken(data.getToken());
                        Constants.HEADER_TOKEN = data.getToken();

                        MainHandler.runOnUiThread(() -> {
                            //接口回调通知
                            if (onLoginListener != null) {
                                onLoginListener.onSuccess(data);
                            }
                        });

                    } else {
                        MainHandler.runOnUiThread(() -> {
                            if (onLoginListener != null) {
                                onLoginListener.onFail("-1", "data is null");
                            }
                        });
                    }
                }

                @Override
                public void onFailure(String code, String message) {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFail(code, message);
                        }
                    });
                }

                @Override
                public void onFinish() {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFinish();
                        }
                    });
                }
            });
        }

//        LoginMobileRequestBean loginMobileRequestBean = new LoginMobileRequestBean();
//        loginMobileRequestBean.setMobile(account);
//        loginMobileRequestBean.setSmscode(smscode);

    }


    public static void submitUpdateMobile(final String account, String smscode, final IApiCallback onLoginListener) {
        if (!RegExpUtil.isMobileNumber(account)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-1", HotmeApplication.getContext().getString(R.string.login_error_phone_format));
            }
            return;
        }
        if (TextUtils.isEmpty(smscode)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_error_verify_code_null));
            }
            return;
        }

        LoginMobileRequestBean loginMobileRequestBean = new LoginMobileRequestBean();
        loginMobileRequestBean.setMobile(account);
        loginMobileRequestBean.setSmscode(smscode);

        String url = SdkApi.updateMobile() + "?phone=" + account + "&code=" + smscode;

        OkHttpUtil.get(url, null, new OkHttpCallbackDecode<LoginResultBean>() {
            @Override
            public void onDataSuccess(LoginResultBean data) {
                AppTrace.d("LoginInteract", "token = " + data.getToken());
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFinish();
                    }
                });
            }
        });

    }


    public static void logout(final IApiCallback onLoginListener) {
        OkHttpUtil.postData(SdkApi.logout(), "", null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                LoginManager.clearLoginInfo();

                if (onLoginListener != null) {
                    onLoginListener.onSuccess();
                }
            }

            @Override
            public void onFailure(String code, String message) {

                if (onLoginListener != null) {
                    onLoginListener.onFail(code, message);
                }
            }

            @Override
            public void onFinish() {

            }
        });

    }


    public static void updateNickname(final String nickName, final IApiCallback listener) {

        if (TextUtils.isEmpty(nickName)) {
            if (listener != null) {
                listener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_error_verify_code_null));
            }
            return;
        }

        String url = SdkApi.setNickName() + "?nickName=" + nickName;

        OkHttpUtil.put(url, null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });

            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    // 账号登录的发送验证码，注册，


    public static void sendLoginSMS(String account, final IApiCallback onSendSmsListener) {
        sendSMS(account, Constants.TYPE_LOGIN, onSendSmsListener);
    }


    public static void sendUpdateMobileSMS(String account, final IApiCallback onSendSmsListener) {
        sendSMS(account, Constants.TYPE_UPDATE_PHONE, onSendSmsListener);
    }

    public static void sendSMS(String account, String smsType, final IApiCallback onSendSmsListener) {
        if (!RegExpUtil.isMobileNumber(account)) {
            if (onSendSmsListener != null) {
                onSendSmsListener.onFail("-1", HotmeApplication.getContext().getString(R.string.login_error_phone_format));
            }
            return;
        }

        SmsSendRequestBean requestBean = new SmsSendRequestBean();
        requestBean.setSmsType(smsType);
        requestBean.setMobile(account);

        OkHttpUtil.postData(SdkApi.getSmsSend(), new Gson().toJson(requestBean), null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {

                //开始计时控件
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (null != onSendSmsListener) {
                        onSendSmsListener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onSendSmsListener != null) {
                        onSendSmsListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                super.onFinish();
                MainHandler.runOnUiThread(() -> {
                    if (onSendSmsListener != null) {
                        onSendSmsListener.onFinish();
                    }
                });

            }
        });

    }


    public static void submitUnbind(final String account, String smscode, String mac, final IGenericApiCallback<LoginResultBean> onLoginListener) {
        if (!RegExpUtil.isMobileNumber(account)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-1", HotmeApplication.getContext().getString(R.string.login_error_phone_format));
            }
            return;
        }
        if (TextUtils.isEmpty(smscode)) {
            if (onLoginListener != null) {
                onLoginListener.onFail("-2", HotmeApplication.getContext().getString(R.string.login_error_verify_code_null));
            }
            return;
        }

        UnbindRequestBean loginMobileRequestBean = new UnbindRequestBean();
        loginMobileRequestBean.setMobile(account);
        loginMobileRequestBean.setCode(smscode);
        loginMobileRequestBean.setMac(mac);

        OkHttpUtil.postData(SdkApi.unbindDevice(), new Gson().toJson(loginMobileRequestBean), null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                if (data != null) {
                    AppTrace.d("UnbindInteract", "onDataSuccess");

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (onLoginListener != null) {
                            onLoginListener.onSuccess(null);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFinish();
                    }
                });
            }
        });

    }

    public static void unbindValidate(String deviceId, IApiCallback listener) {

        String url = SdkApi.device_unbind_novalidate() + "/" + deviceId;
        AppTrace.d(TAG, "url=" + url);

        //使用固定token去访问接口
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_TOKEN_NAME, Constants.CONSTANT_TOKEN);

        OkHttpUtil.postData(url, "", headers, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                AppTrace.d("getDeviceSetting", "getDeviceSetting");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (listener != null) {
                        listener.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }


    public static void getUserInfo(final IGenericApiCallback<UserInfoBean> onLoginListener) {
        OkHttpUtil.get(SdkApi.getUserInfo(), new OkHttpCallbackDecode<UserInfoBean>() {
            @Override
            public void onDataSuccess(UserInfoBean data) {
                if (data != null) {
                    //保存登录信息
                    data.userToken = Constants.HEADER_TOKEN;
                    LoginManager.saveLoginInfo(data);

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (onLoginListener != null) {
                            onLoginListener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (onLoginListener != null) {
                            onLoginListener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (onLoginListener != null) {
                        onLoginListener.onFinish();
                    }
                });
            }
        });
    }


    public static void modifyPortrait(File file, IGenericApiCallback<UserAvatarBean> listener) {
        OkHttpUtil.postFile(SdkApi.setAvatar(), new OkHttpCallbackDecode<UserAvatarBean>() {
            @Override
            public void onDataSuccess(UserAvatarBean data) {
                if (data != null) {

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        }, file);
    }

    public static void getDeviceList(int pageNumber, int pageSize, final IGenericApiCallback<BindDeviceResultBean> onListener) {
        UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
        if (userInfoBean == null) {
            if (onListener != null) {
                onListener.onFail("-100", "未登录");
            }
            return;
        }
        BindDeviceRequestBean requestBean = new BindDeviceRequestBean();
        requestBean.bindingPhone = userInfoBean.phone;
        requestBean.pageNum = pageNumber;
        requestBean.pageSize = pageSize;

        AppTrace.d("getDeviceList url:" + SdkApi.getDeviceList());

        OkHttpUtil.postData(SdkApi.getDeviceList(), GsonUtils.GsonString(requestBean), null, new OkHttpCallbackDecode<BindDeviceResultBean>(null, new TypeToken<BindDeviceResultBean>() {
        }.getType()) {

            @Override
            public void onDataSuccess(BindDeviceResultBean data) {
                if (pageNumber == 1) {
                    AppFileUtil.saveBindDeviceInfo(userInfoBean.phone, new Gson().toJson(data));
                }
                if (onListener != null) {
                    onListener.onSuccess(data);
                }
            }

            @Override
            public void onFailure(String code, String message) {
                super.onFailure(code, message);
                if (onListener != null) {
                    onListener.onFail(code, message);
                }
            }

            @Override
            public void onFinish() {
                if (onListener != null) {
                    onListener.onFinish();
                }
            }
        });
    }

    public static void queryAllRecordTask(String deviceId, final IGenericApiCallback<List<HmCompetitionRecordBean>> onListener) {

        HashMap<String, String> queryBean = new HashMap<>();
        queryBean.put("competitionType", "1");
        queryBean.put("deviceId", deviceId);
        queryBean.put("competitionBeginTime", TimeUtil.getTodayString());
        queryBean.put("competitionEndTime", TimeUtil.getTodayEndString());

        AppTrace.d("queryAllCompetition=" + SdkApi.queryAllCompetition() + ": " + GsonUtils.GsonString(queryBean));
        OkHttpUtil.postData(SdkApi.queryAllCompetition(), GsonUtils.GsonString(queryBean), null, new OkHttpCallbackDecode<List<HmCompetitionRecordBean>>(null, new TypeToken<List<HmCompetitionRecordBean>>() {
        }.getType()) {

            @Override
            public void onDataSuccess(List<HmCompetitionRecordBean> data) {


                if (data != null) {
                    AppFileUtil.saveString(GsonUtils.GsonString(data), deviceId + "_record_task_" + TimeUtil.getTodayString());
                }

                if (onListener != null) {
                    onListener.onSuccess(data);
                }
            }

            @Override
            public void onFailure(String code, String message) {
                super.onFailure(code, message);
                if (onListener != null) {
                    onListener.onFail(code, message);
                }
            }

            @Override
            public void onFinish() {
                if (onListener != null) {
                    onListener.onFinish();
                }
            }
        });
    }

    public static void addCompetitionLivePoint(String competitionId, String videoName, String teamName, int pointTime, String pointType, String markId, IGenericApiCallback<HmCompetitionPoint> onListener) {
        String url = SdkApi.competitionPoint_add();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("pointTeam", teamName);
        paramsdata.put("pointTimeOffset", String.valueOf(pointTime));
        paramsdata.put("pointType", pointType);
        paramsdata.put("pointSource", String.valueOf(Constants.DOT_SOURCE_MANUAL));
        paramsdata.put("markId", markId);

        if(!TextUtils.isEmpty(videoName)) {
            paramsdata.put("videoName", videoName);
        }

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionStatBean>(CompetitionStatBean.class) {
            @Override
            public void onDataSuccess(CompetitionStatBean data) {
                AppTrace.d(TAG, "addCompetitionLivePoint: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data.data);
                    }
                });

            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "addCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void updateCompetitionLivePoint(String competitionId, String pointId, String teamName, int pointTime, String pointType, String subPointType, IGenericApiCallback<HmCompetitionPoint> onListener) {
        String url = SdkApi.competitionPoint_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("pointId", pointId);
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("pointTeam", teamName);
        paramsdata.put("pointTimeOffset", String.valueOf(pointTime));
        paramsdata.put("pointType", pointType);
        paramsdata.put("subPointType", subPointType);

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionStatBean>(CompetitionStatBean.class) {
            @Override
            public void onDataSuccess(CompetitionStatBean data) {
                AppTrace.d(TAG, "updateCompetitionLivePoint: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void updateCompetitionLivePointTime(String competitionId, String pointId, int pointTime, IGenericApiCallback<HmCompetitionPoint> onListener) {
        String url = SdkApi.competitionPoint_update();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("pointId", pointId);
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("pointTimeOffset", String.valueOf(pointTime));

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionStatBean>(CompetitionStatBean.class) {
            @Override
            public void onDataSuccess(CompetitionStatBean data) {
                AppTrace.d(TAG, "updateCompetitionLivePointTime: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateCompetitionLivePointTime onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void deleteCompetitionLivePoint(String competitionId, String pointId, IGenericApiCallback<Object> onListener) {
        String url = SdkApi.getDeleteCompetitionPoint(pointId);
        HashMap<String, String> paramsdata = new HashMap<>();
        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<Object>(Object.class) {
            @Override
            public void onDataSuccess(Object data) {
                AppTrace.d(TAG, "updateCompetitionLivePoint: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void deleteAllCompetitionLivePoint(String competitionId, List<String> pointId, IGenericApiCallback<Object> onListener) {
        String url = SdkApi.competitionPoint_delete_all();

        AppTrace.d(TAG, "url=" + url);
        FormBody.Builder builder = new FormBody.Builder();
        for (String id : pointId) {
            builder.add("ids", id);
        }
        OkHttpUtil.postFormData(url, builder, null, new OkHttpCallbackDecode<Object>() {

            @Override
            public void onDataSuccess(Object data) {
                AppTrace.d(TAG, "deleteAllCompetitionLivePoint: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }


    public static void queryCompetitionLivePoint(String competitionId, String videoName, IGenericApiCallback<CompetitionStatListBean> onListener) {
        String url = SdkApi.competitionPoint_query_all();

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("videoName", videoName);
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("competitionId", competitionId);

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionStatListBean>(CompetitionStatListBean.class) {
            @Override
            public void onDataSuccess(CompetitionStatListBean data) {
                AppTrace.d(TAG, "queryCompetitionLivePoint: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "queryCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }


    public static void addCompetitionMark(String competitionId, String videoName, String markName, int videoTime, IGenericApiCallback<CompetitionMarkBean> onListener) {
        String url = SdkApi.competitionMark_add();
        AppTrace.d(TAG, "url=" + url);
        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("videoName", videoName);
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("markName", markName);
        paramsdata.put("markTimeOffset", String.valueOf(videoTime));
        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionMarkBean>(CompetitionMarkBean.class) {

            @Override
            public void onDataSuccess(CompetitionMarkBean data) {
                AppTrace.d(TAG, "addCompetitionMark: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "updateCompetitionLivePoint onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void queryCompetitionMark(String competitionId, String videoName, String markName, IGenericApiCallback<CompetitionMarkListBean> onListener) {
        String url = SdkApi.competitionMark_query();
        AppTrace.d(TAG, "url=" + url);
        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("videoName", videoName);
        paramsdata.put("competitionId", competitionId);
        paramsdata.put("markName", markName);
        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionMarkListBean>(CompetitionMarkListBean.class) {

            @Override
            public void onDataSuccess(CompetitionMarkListBean data) {
                AppTrace.d(TAG, "queryCompetitionMark: ok, competitionId= " + competitionId +", mark=" + markName);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "queryCompetitionMark onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }


    public static void queryCompetitionMark(String competitionId, String videoName, IGenericApiCallback<CompetitionMarkListBean> onListener) {
        String url = SdkApi.competitionMark_query();
        AppTrace.d(TAG, "url=" + url);
        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("videoName", videoName);
        paramsdata.put("competitionId", competitionId);
        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<CompetitionMarkListBean>(CompetitionMarkListBean.class) {

            @Override
            public void onDataSuccess(CompetitionMarkListBean data) {
                AppTrace.d(TAG, "queryCompetitionMark: ok, competitionId= " + competitionId);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "queryCompetitionMark onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }


    public static void addCompetitionPointBatch(List<HmCompetitionPoint> data, IGenericApiCallback<CompetitionStatListBean> onListener) {
        String url = SdkApi.competitionPoint_add_batch();
        AppTrace.d(TAG, "url=" + url);

        String dataStr = GsonUtils.GsonString(data);
        AppTrace.d(TAG, "addCompetitionMarkBatch = " + dataStr);

        OkHttpUtil.postData(url, dataStr, null, new OkHttpCallbackDecode<CompetitionStatListBean>(CompetitionStatListBean.class) {

            @Override
            public void onDataSuccess(CompetitionStatListBean data) {
                AppTrace.d(TAG, "addCompetitionMarkBatch: ok");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "addCompetitionMarkBatch onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void addCompetitionMarkBatch(List<HmCompetitionMark> data, IGenericApiCallback<CompetitionMarkListBean> onListener) {
        String url = SdkApi.competitionMark_add_batch();
        AppTrace.d(TAG, "url=" + url);

        String dataStr = GsonUtils.GsonString(data);
        AppTrace.d(TAG, "addCompetitionMarkBatch = " + dataStr);
        OkHttpUtil.postData(url, dataStr, null, new OkHttpCallbackDecode<CompetitionMarkListBean>(CompetitionMarkListBean.class) {

            @Override
            public void onDataSuccess(CompetitionMarkListBean data) {
                AppTrace.d(TAG, "addCompetitionMarkBatch: ok");
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onSuccess(data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "addCompetitionMarkBatch onFailure: " + message);
                MainHandler.runOnUiThread(() -> {
                    //接口回调通知
                    if (onListener != null) {
                        onListener.onFail(code, message);
                    }
                });
            }
        });
    }

    public static void uploadCover(File file, String wxToken, IGenericApiCallback<CommonStringResponse> listener) {

        try {
            FileInputStream fis = new FileInputStream(file.getPath());//转换成输入流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            while((count = fis.read(buffer)) >= 0){
                baos.write(buffer, 0, count);//读取输入流并写入输出字节流中
            }
            fis.close();//关闭文件输入流
            String uploadBuffer = new String(Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP));  //进行Base64编码

            FormBody.Builder builder = new FormBody.Builder();
            builder.add("coverImg", "data:image/jpeg;base64," + uploadBuffer);
            builder.add("token", wxToken);

            OkHttpUtil.postFormData(SdkApi.getWxUploadCoverUrl(), builder, null, new OkHttpCallbackDecode<CommonStringResponse>(CommonStringResponse.class) {
                @Override
                public void onDataSuccess(CommonStringResponse data) {
                    AppTrace.d("uploadCover onDataSuccess");
                    if (data != null) {

                        MainHandler.runOnUiThread(() -> {
                            //接口回调通知
                            if (listener != null) {
                                listener.onSuccess(data);
                            }
                        });

                    } else {
                        MainHandler.runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onFail("-1", "data is null");
                            }
                        });
                    }
                }

                @Override
                public void onFailure(String code, String message) {
                    AppTrace.d("uploadCover fail: code="+code+ ", message");
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail(code, message);
                        }
                    });
                }

                @Override
                public void onFinish() {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFinish();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateCompetitionVideoTime(VideoDurationUpdateBean videoBean, IApiCallback listener) {
        if (videoBean == null) {
            return;
        }
        String url = SdkApi.updateCompetitionVideo();
        AppTrace.d(TAG, "url=" + url);

        OkHttpUtil.postData(url, new Gson().toJson(videoBean), null, new OkHttpCallbackDecode<HmVideo>() {
            @Override
            public void onDataSuccess(HmVideo data) {
                if (data != null) {
                    AppTrace.d("updateCompetitionVideoTime", "videoDuration = " + videoBean.getVideoDuration());

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d("updateCompetitionVideoTime", "code = " + code +", message=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void queryDeviceSerialNumber(String deviceId, IGenericApiCallback listener) {

        String url = SdkApi.findDeviceSerialNumber(deviceId);
        AppTrace.d(TAG, "url=" + url);
        HashMap<String, String> paramsdata = new HashMap<>();
        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<DeviceSerialNumber>(DeviceSerialNumber.class) {
            @Override
            public void onDataSuccess(DeviceSerialNumber data) {
                if (data != null) {
                    AppTrace.d("queryDeviceSerialNumber", "deviceId = " + deviceId);

                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d("queryDeviceSerialNumber", "code = " + code +", message=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }


    public static void canEnableAnalyseTask(String competitionId, IGenericApiCallback<AIEnableAnalyseBean> listener) {

        String url = SdkApi.canEnableAnalyseTask();

        AppTrace.d(TAG, "url=" + url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("competitionId", competitionId);

        OkHttpUtil.postFormData(SdkApi.canEnableAnalyseTask(), builder, null, new OkHttpCallbackDecode<AIEnableAnalyseBean>(AIEnableAnalyseBean.class) {
            @Override
            public void onDataSuccess(AIEnableAnalyseBean data) {
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void enableAnalyseTask(String competitionId, IGenericApiCallback<AIEnableAnalyseBean> listener) {

        String url = SdkApi.enableAnalyseTask();

        AppTrace.d(TAG, "url=" + url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("competitionId", competitionId);
        OkHttpUtil.postFormData(SdkApi.enableAnalyseTask(), builder, null, new OkHttpCallbackDecode<AIEnableAnalyseBean>(AIEnableAnalyseBean.class) {
            @Override
            public void onDataSuccess(AIEnableAnalyseBean data) {
                AppTrace.d(TAG, "enableAnalyseTask ok");
                if (data != null) {
                    MainHandler.runOnUiThread(() -> {
                        //接口回调通知
                        if (listener != null) {
                            listener.onSuccess(data);
                        }
                    });

                } else {
                    MainHandler.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onFail("-1", "data is null");
                        }
                    });
                }
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "enableAnalyseTask onFailure: code=" + code + ", message=" + message);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, message);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }

    public static void getVideoPlayUrl(String videoID, final IGenericApiCallback<String> listener) {
        String apiUrl = SdkApi.encodeVideoUrl(videoID);
        HashMap<String, String> paramsdata = new HashMap<>();
        OkHttpUtil.postData(apiUrl, paramsdata, null, new OkHttpCallbackDecode<GetVideoUrlBean>(GetVideoUrlBean.class) {
            @Override
            public void onDataSuccess(GetVideoUrlBean data) {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onSuccess(data.data);
                    }
                });
            }

            @Override
            public void onFailure(String code, String msg) {
                AppTrace.d(TAG, "error: code=" + code + ", msg=" + msg);
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFail(code, msg);
                    }
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onFinish();
                    }
                });
            }
        });
    }
}
