package com.xh.hotme.http;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.record.bean.LocalRecordingGrant;
import java.util.HashMap;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Protocol 3 commands are authorized and versioned by the server. */
public final class RecordingApi {
    private RecordingApi() { }

    @Keep
    private static class RecordResponse extends CommonResponse {
        HmCompetitionRecordBean data;
    }

    public static void start(String id, IGenericApiCallback<CommonResponse> callback) {
        control(SdkApi.startCompetitionRecord(id), callback);
    }

    public static void finish(String id, IGenericApiCallback<CommonResponse> callback) {
        control(SdkApi.finishCompetitionRecord(id), callback);
    }

    public static void pause(String id, IGenericApiCallback<CommonResponse> callback) {
        control(SdkApi.pauseCompetitionRecord(id), callback);
    }

    @Keep
    private static class GrantResponse extends CommonResponse { LocalRecordingGrant data; }

    public static void prepareLocal(String id, long deviceVersion, String requestId, IGenericApiCallback<LocalRecordingGrant> callback) {
        HashMap<String,Object> body = new HashMap<>(); body.put("deviceVersion", deviceVersion); body.put("requestId", requestId);
        body.put("cloudUploadRequested", com.xh.hotme.camera.CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD));
        post(SdkApi.localCompetitionRecord("prepareLocal", id), new Gson().toJson(body), GrantResponse.class, new IGenericApiCallback<GrantResponse>() {
            public void onSuccess(GrantResponse response) {
                if (response.data == null) callback.onFail("-2", "本地录制授权响应异常"); else callback.onSuccess(response.data);
            }
            public void onFail(String code, String message) { callback.onFail(code, message); }
            public void onFinish() { callback.onFinish(); }
        });
    }
    public static void reportLocal(String id, String session, long deviceVersion, boolean started, IGenericApiCallback<CommonResponse> callback) {
        localReport("reportLocal", id, session, deviceVersion, started, null, callback);
    }
    public static void cancelLocal(LocalRecordingGrant grant, IGenericApiCallback<CommonResponse> callback) {
        localReport("cancelLocal", grant.live_cid, grant.session_id, grant.device_version, false, grant.operation_id, callback);
    }
    private static void localReport(String action, String id, String session, long version, boolean started, String operation, IGenericApiCallback<CommonResponse> callback) {
        HashMap<String,Object> body = new HashMap<>(); body.put("sessionId", session); body.put("deviceVersion", version);
        body.put("started", started); body.put("operationId", operation);
        post(SdkApi.localCompetitionRecord(action, id), new Gson().toJson(body), CommonResponse.class, callback);
    }

    private static void control(String url, IGenericApiCallback<CommonResponse> callback) {
        post(url, CommonResponse.class, callback);
    }

    public static void query(String id, IGenericApiCallback<HmCompetitionRecordBean> callback) {
        post(SdkApi.queryCompetitionRecord(id), RecordResponse.class, new IGenericApiCallback<RecordResponse>() {
            public void onSuccess(RecordResponse response) {
                if (response.data == null) callback.onFail("-2", "录制任务状态异常");
                else callback.onSuccess(response.data);
            }
            public void onFail(String code, String message) { callback.onFail(code, message); }
            public void onFinish() { callback.onFinish(); }
        });
    }

    private static <T extends CommonResponse> void post(String url, Class<T> type,
                                                       IGenericApiCallback<T> callback) {
        post(url, "{}", type, callback);
    }
    private static <T extends CommonResponse> void post(String url, String body, Class<T> type, IGenericApiCallback<T> callback) {
        postJson(url, body, type, 120, callback);
    }
    public static <T extends CommonResponse> void postJson(String url, String body, Class<T> type, int timeoutSeconds, IGenericApiCallback<T> callback) {
        Request.Builder request = new Request.Builder().url(url).post(RequestBody.create(
                MediaType.parse("application/json;charset=utf-8"), body));
        if (Constants.HEADER_TOKEN != null && !Constants.HEADER_TOKEN.isEmpty()) {
            request.header(Constants.HEADER_TOKEN_NAME, SdkApi.TOKEN_PREFIX + Constants.HEADER_TOKEN);
        }
        // Control endpoints return data=null for both confirmed (200) and
        // pending (202) starts. Decode the envelope, not its data field.
        OkHttpUtil.enqueue(request.build(), new Callback() {
            public void onFailure(Call call, IOException error) {
                deliverFailure("-1", error.getMessage());
            }
            public void onResponse(Call call, Response response) {
                try (Response closed = response) {
                    T result = response.body() == null ? null
                            : new Gson().fromJson(response.body().string(), type);
                    if (!response.isSuccessful() || result == null
                            || (result.code != 200 && result.code != 202)) {
                        deliverFailure(result == null ? String.valueOf(response.code()) : String.valueOf(result.code),
                                result == null ? "录制服务响应异常" : result.msg);
                    } else {
                        MainHandler.runOnUiThread(() -> { callback.onSuccess(result); callback.onFinish(); });
                    }
                } catch (Exception error) {
                    deliverFailure("-2", "录制服务响应异常");
                }
            }
            private void deliverFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> { callback.onFail(code, message); callback.onFinish(); });
            }
        }, timeoutSeconds);
    }
}
