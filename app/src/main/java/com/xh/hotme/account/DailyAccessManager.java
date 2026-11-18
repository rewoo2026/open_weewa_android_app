package com.xh.hotme.account;

import android.os.SystemClock;
import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public final class DailyAccessManager {
    public static final String NETWORK_MESSAGE = "今日尚未完成服务器同步，请先断开相机热点，连接可上网的 Wi-Fi 或移动网络，配置网络后重试";
    @Keep public static final class DeviceAccess {
        public String deviceId, deviceType, recordingError;
        public boolean recordingAllowed;
        public OfflinePermit permit;
    }
    @Keep public static final class OfflinePermit { public String payload, signature; }
    @Keep public static final class Profile extends DailyAccessState {
        public String userId;
        public List<DeviceAccess> devices = new ArrayList<>();
    }
    @Keep private static final class Envelope extends CommonResponse { Profile data; }
    private static final List<IGenericApiCallback<Profile>> waiting = new ArrayList<>();
    private static String runningAccount;
    private static android.content.SharedPreferences prefs() {
        return HotmeApplication.getContext().getSharedPreferences("daily_server_access", 0);
    }
    private static String identity() {
        if (!LoginManager.isLogon()) return "";
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest((LoginManager.getUserId()+":"+LoginManager.getUserToken()).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(); for (byte b : bytes) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static Profile cached() {
        try { return new Gson().fromJson(prefs().getString("profile", ""), Profile.class); }
        catch (RuntimeException ignored) { return null; }
    }
    public static boolean ready() {
        Profile p = cached(); return p != null && p.ready(identity(), System.currentTimeMillis(), SystemClock.elapsedRealtime());
    }
    public static DeviceAccess device(String id) {
        if (!ready()) return null;
        Profile p = cached();
        if (p.devices != null) for (DeviceAccess d : p.devices) if (id != null && id.equals(d.deviceId)) return d;
        return null;
    }
    public static void invalidate() { prefs().edit().remove("profile").commit(); }
    public static void synchronize(boolean force, IGenericApiCallback<Profile> callback) {
        if (!force && ready()) { callback.onSuccess(cached()); callback.onFinish(); return; }
        final String account = identity();
        if (account.isEmpty()) { callback.onFail("401", "请先登录"); callback.onFinish(); return; }
        if (runningAccount != null) {
            if (runningAccount.equals(account)) waiting.add(callback);
            else { callback.onFail("-1", "登录账号正在变更，请重试"); callback.onFinish(); }
            return;
        }
        waiting.add(callback); runningAccount = account;
        com.xh.hotme.record.OfflineRecordingReports.flush(new IGenericApiCallback<Void>() {
            public void onSuccess(Void unused){fetch(account);}
            public void onFail(String code,String message){if("401".equals(code))LoginManager.clearLoginInfo();fail(code,message);}
            public void onFinish(){}
        });
    }
    private static void fetch(final String account) {
        com.xh.hotme.http.ApiUtil.getUserInfo(new IGenericApiCallback<com.xh.hotme.bean.UserInfoBean>() {
            public void onSuccess(com.xh.hotme.bean.UserInfoBean info){fetchProfile(account);}
            public void onFail(String code,String message){if("401".equals(code))LoginManager.clearLoginInfo();fail(code,NETWORK_MESSAGE);}
            public void onFinish(){}
        });
    }
    private static void fetchProfile(final String account) {
        RecordingApi.postJson(SdkApi.localCompetitionRecord("syncDaily", null), "{}", Envelope.class, 20, new IGenericApiCallback<Envelope>() {
            public void onSuccess(Envelope response) {
                Profile p = response.data;
                if (!account.equals(identity()) || p == null || !LoginManager.getUserId().equals(p.userId)
                        || p.refreshAt <= p.serverTime || p.refreshAt - p.serverTime > 86400000L) {
                    fail("-2", "服务器同步数据异常，请重新登录并配置网络"); return;
                }
                p.account = account; p.savedWall = System.currentTimeMillis(); p.savedElapsed = SystemClock.elapsedRealtime();
                if (!prefs().edit().putString("profile", new Gson().toJson(p)).commit()) { fail("-2", "无法保存今日同步数据，请重试"); return; }
                LoginManager.updateLoginTime();
                org.greenrobot.eventbus.EventBus.getDefault().post(new com.xh.hotme.bluetooth.event.LoginEvent());
                for (IGenericApiCallback<Profile> c : drain()) { c.onSuccess(p); c.onFinish(); }
            }
            public void onFail(String code, String message) {
                if ("401".equals(code)) LoginManager.clearLoginInfo();
                fail(code, "401".equals(code) ? "登录已失效，请联网重新登录" : NETWORK_MESSAGE);
            }
            public void onFinish() { }
        });
    }
    private static List<IGenericApiCallback<Profile>> drain() {
        List<IGenericApiCallback<Profile>> callbacks = new ArrayList<>(waiting); waiting.clear(); runningAccount = null; return callbacks;
    }
    private static void fail(String code, String message) {
        for (IGenericApiCallback<Profile> c : drain()) { c.onFail(code, message); c.onFinish(); }
    }
    private DailyAccessManager() { }
}
