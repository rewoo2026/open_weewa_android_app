package com.xh.hotme.record;

import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.utils.MainHandler;
import java.text.SimpleDateFormat;
import java.util.*;

/** A durable outbox. Recording never waits for HTTP; synchronization runs before daily admission. */
public final class OfflineRecordingReports {
    @Keep private static final class Entry {
        String user, operation, payload, signature, id, session, permitId;
        long counter, startTime, stopAt;
        boolean started=true, attached, uploadedStart, uploadedStop;
        boolean closeLease;
        Map<String,String> metadata;
    }
    @Keep private static final class Meter {
        String user,device,permitId,id,session;long counter,observedAt;int active;
    }
    private static void meter(RecordStatusResultBean status) {
        if(status.offlineDeviceId==null||status.offlineDeviceId.isEmpty())return;
        Meter m=new Meter();m.user=LoginManager.getUserId();m.device=status.offlineDeviceId;m.permitId=status.offlinePermitId;m.id=status.offlineCompetitionId;m.session=status.offlineSessionId;
        m.counter=status.offlineCounter;m.observedAt=status.observedAt;m.active=status.offlineActive;
        prefs().edit().putString("meter:"+m.user+":"+m.device,new Gson().toJson(m)).commit();
    }
    private static void flushMeters(IGenericApiCallback<Void> callback) {
        List<Meter> meters=new ArrayList<>();String prefix="meter:"+LoginManager.getUserId()+":";
        for(Map.Entry<String,?> p:prefs().getAll().entrySet())if(p.getKey().startsWith(prefix)&&p.getValue() instanceof String) {
            try{Meter m=new Gson().fromJson((String)p.getValue(),Meter.class);if(m!=null)meters.add(m);}catch(RuntimeException ignored){}
        }
        if(meters.isEmpty()){callback.onSuccess(null);callback.onFinish();return;}
        RecordingApi.postJson(SdkApi.localCompetitionRecord("syncOfflineMeters",null),new Gson().toJson(meters),CommonResponse.class,20,new IGenericApiCallback<CommonResponse>(){
            public void onSuccess(CommonResponse r){callback.onSuccess(null);callback.onFinish();}
            public void onFail(String code,String message){callback.onFail(code,"相机用量同步失败，请配置网络后重试");callback.onFinish();}
            public void onFinish(){}
        });
    }
    private static android.content.SharedPreferences prefs() { return HotmeApplication.getContext().getSharedPreferences("offline_recording_outbox",0); }
    private static List<Entry> read() {
        String raw=prefs().getString("entries","[]");
        List<Entry> entries=new Gson().fromJson(raw,new TypeToken<List<Entry>>(){}.getType());
        if(entries==null)throw new IllegalStateException("离线录制用量数据损坏，请联系技术支持");return entries;
    }
    private static void save(List<Entry> entries) {
        if(!prefs().edit().putString("entries",new Gson().toJson(entries)).commit())throw new IllegalStateException("无法保存离线录制用量");
    }
    static void pending(RecordStartRequestBean request,DailyAccessManager.DeviceAccess access,String operation) {
        Entry entry=new Entry();entry.user=LoginManager.getUserId();entry.operation=operation;entry.payload=access.permit.payload;entry.signature=access.permit.signature;
        String permission=new String(android.util.Base64.decode(entry.payload,android.util.Base64.URL_SAFE),java.nio.charset.StandardCharsets.UTF_8);
        entry.permitId=new Gson().fromJson(permission,com.google.gson.JsonObject.class).get("permit_id").getAsString();
        entry.startTime=request.getVt();if(entry.startTime<=0)entry.startTime=System.currentTimeMillis();
        entry.metadata=new LinkedHashMap<>();entry.metadata.put("competitionName",request.getVn());
        entry.metadata.put("enableAnalyse",String.valueOf(request.analysis_requested));
        entry.metadata.put("motionType",String.valueOf(request.getMotion_type()));entry.metadata.put("placeType",request.getPlace_type());
        entry.metadata.put("place",request.getVs());entry.metadata.put("homeTeamName",request.getHost_team());entry.metadata.put("awayTeamName",request.getGuest_team());
        entry.metadata.put("cityCode",request.getCity_code());entry.metadata.put("provinceCode",request.getProvince_code());
        List<Entry> entries=read();entries.add(entry);save(entries);
    }
    public static void observe(RecordStatusResultBean status) {
        if(status==null||!"ok".equalsIgnoreCase(status.getR()))return;
        MainHandler.runOnUiThread(()->meter(status));
        if(status.offlinePermitId==null||status.offlinePermitId.isEmpty())return;
        MainHandler.runOnUiThread(()->{
            List<Entry> entries=read();boolean changed=false;
            for(Entry entry:entries) {
                if(entry.permitId==null)try{String raw=new String(android.util.Base64.decode(entry.payload,android.util.Base64.URL_SAFE),java.nio.charset.StandardCharsets.UTF_8);entry.permitId=new Gson().fromJson(raw,com.google.gson.JsonObject.class).get("permit_id").getAsString();}catch(RuntimeException ignored){}
                boolean matching=entry.attached?entry.id.equals(status.offlineCompetitionId)&&entry.session.equals(status.offlineSessionId)
                    :entry.operation.equals(status.offlineOperationId)&&status.offlinePermitId.equals(entry.permitId);
                if(!matching)continue;
                if(!entry.attached) {
                    if(status.offlineRecorded!=1)continue;
                    entry.id=status.offlineCompetitionId;entry.session=status.offlineSessionId;entry.stopAt=status.stopAt;entry.attached=true;
                }
                entry.counter=Math.max(entry.counter,status.offlineCounter);
                if(status.offlineActive==0&&"idle".equals(status.deviceState))entry.started=false;
                changed=true;
            }
            if(changed)save(entries);
        });
    }
    public static void install() {
        BluetoothHandle.addPlayNotifyListener(new IRecordControlListener(){
            public void onRecordVideoStart(){} public void onRecordVideoStop(){} public void onRecordException(String message){}
            public void onRecordVideoStatusUpdate(RecordStatusResultBean status){observe(status);}
        });
    }
    public static void flush(IGenericApiCallback<Void> callback) {
        final List<Entry> entries;
        try {entries=read();}catch(RuntimeException e){callback.onFail("-2",e.getMessage());callback.onFinish();return;}
        Entry candidate=null;
        for(Entry e:entries) if(LoginManager.getUserId().equals(e.user)&&e.attached&&((!e.started&&(!e.uploadedStop||expired(e)))||!e.uploadedStart)){candidate=e;break;}
        if(candidate==null){flushMeters(callback);return;}
        final Entry entry=candidate;
        entry.closeLease=!entry.started&&expired(entry)&&entries.stream().noneMatch(e->e!=entry&&e.payload.equals(entry.payload)&&e.attached&&e.counter>entry.counter);
        RecordingApi.postJson(SdkApi.localCompetitionRecord("reportOffline",null),new Gson().toJson(entry),CommonResponse.class,20,new IGenericApiCallback<CommonResponse>() {
            public void onSuccess(CommonResponse response) {
                List<Entry> current=read();for(Entry e:current)if(entry.operation.equals(e.operation)){if(entry.started)e.uploadedStart=true;else {e.uploadedStart=true;e.uploadedStop=true;}}
                if(!entry.started&&expired(entry))current.removeIf(e->entry.operation.equals(e.operation));save(current);flush(callback);
            }
            public void onFail(String code,String message){callback.onFail(code,"离线录制用量同步失败，请配置网络后重试；持续失败时请联系技术支持");callback.onFinish();}
            public void onFinish(){}
        });
    }
    private static boolean expired(Entry entry) {
        try {
            String raw=new String(android.util.Base64.decode(entry.payload,android.util.Base64.URL_SAFE),java.nio.charset.StandardCharsets.UTF_8);
            return System.currentTimeMillis()>=new Gson().fromJson(raw,com.google.gson.JsonObject.class).get("stop_before").getAsLong();
        }catch(RuntimeException e){return false;}
    }
    private OfflineRecordingReports(){}
}
