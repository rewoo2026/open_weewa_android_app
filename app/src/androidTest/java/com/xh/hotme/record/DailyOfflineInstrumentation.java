package com.xh.hotme.record;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import com.xh.hotme.R;
import com.xh.hotme.account.*;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.*;
import com.xh.hotme.bluetooth.bean.*;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.*;
import okhttp3.*;

/** Exercises the production form and daily gate against a real Bluetooth camera. */
public class DailyOfflineInstrumentation extends Instrumentation implements IRecordControlListener {
    private final AtomicReference<RecordStatusResultBean> status=new AtomicReference<>();
    private AddRecordTaskActivity form;
    private DailyAccessActivity gate;
    private String cid,sid;
    private boolean autoStop;
    public void onCreate(Bundle args){super.onCreate(args);autoStop=args!=null&&"true".equals(args.getString("autoStop"));start();}
    public void onStart(){
        Bundle result=new Bundle();int outcome=Activity.RESULT_OK;Field client=null;Object savedClient=null;String savedProfile=null;
        int savedMode=Constants.ACCESS_MODEL;
        try {
            await(()->com.xh.hotme.HotmeApplication.getInstance()!=null,10000,"App init");
            check(LoginManager.isLogon(),"Please sign in before testing");
            AtomicReference<String> sync=new AtomicReference<>();
            runOnMainSync(()->DailyAccessManager.synchronize(true,new IGenericApiCallback<DailyAccessManager.Profile>(){
                public void onSuccess(DailyAccessManager.Profile p){sync.set("ok");} public void onFail(String c,String m){sync.set(m);}public void onFinish(){}
            }));
            await(()->sync.get()!=null,40000,"Daily sync timeout");check("ok".equals(sync.get()),"Daily sync failed: "+sync.get());
            savedProfile=getTargetContext().getSharedPreferences("daily_server_access",0).getString("profile",null);
            String address="3A:A3:0A:04:00:A5";
            Constants.ACCESS_MODEL=Constants.ACCESS_MODEL_LOCAL;
            runOnMainSync(()->{CameraManager.newCamera(address);BluetoothHandle.addPlayNotifyListener(this);BluetoothManager.getImpl().connectToBle(new Device(address,0,"weewa520"));});
            await(()->BluetoothManager.getImpl().isConnected(),30000,"Bluetooth connect");
            RecordStatusResultBean before=query();
            if(before.getStatus()>0&&before.getVideoName()!=null&&before.getVideoName().startsWith("每日离线验收")) {
                final RecordStatusResultBean old=before;
                runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(old.competitionId,old.sessionId)));
                await(()->query().getStatus()==0,45000,"Previous test cleanup stop");Thread.sleep(1500);before=query();
            }
            check(before.getStatus()==0&&before.recordingProtocolVersion>=5,"Camera must be idle on protocol 5");
            client=OkHttpUtil.class.getDeclaredField("CLIENT");client.setAccessible(true);savedClient=client.get(null);
            AtomicInteger forbidden=new AtomicInteger();
            client.set(null,new OkHttpClient.Builder().addInterceptor(chain->{
                String path=chain.request().url().encodedPath();
                if(path.contains("/addLocal")||path.contains("/prepareLocal")||path.contains("/startRecord")||path.contains("/submitUseNumber"))forbidden.incrementAndGet();
                throw new IOException("Test: both phone and camera are offline");
            }).build());
            shell("am start -n com.xh.hotme/.main.MainActivity");Thread.sleep(1800);
            Constants.ACCESS_MODEL=Constants.ACCESS_MODEL_LOCAL;
            shell("svc wifi disable");shell("svc data disable");Thread.sleep(1500);
            Intent intent=new Intent(getTargetContext(),AddRecordTaskActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            form=(AddRecordTaskActivity)startActivitySync(intent);
            String name="每日离线验收"+System.currentTimeMillis();
            runOnMainSync(()->{((EditText)form.findViewById(R.id.tv_game_name)).setText(name);((EditText)form.findViewById(R.id.home_team_name)).setText("主队");((EditText)form.findViewById(R.id.guest_team_name)).setText("客队");form.findViewById(R.id.btn_record).performClick();});
            await(form::isFinishing,120000,"Offline form start not confirmed");
            RecordStatusResultBean started=query();cid=started.competitionId;sid=started.sessionId;
            final long counterBefore=before.offlineCounter;
            check(started.getStatus()==1&&started.firstFrame==1&&started.localControl==1&&name.equals(started.getVideoName()),"Wrong recording metadata or no first frame");
            check(cid!=null&&cid.startsWith("900")&&sid!=null,"Device did not assign identity");
            check(forbidden.get()==0,"Start attempted live quota/creation/permission HTTP");
            long deadline=started.stopAt;
            runOnMainSync(DailyAccessManager::invalidate);
            gate=(DailyAccessActivity)startActivitySync(new Intent(getTargetContext(),DailyAccessActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Thread.sleep(2500);RecordStatusResultBean continuing=query();
            check(continuing.getStatus()==1&&continuing.stopAt==deadline&&sid.equals(continuing.sessionId),"Failed daily sync interrupted or extended recording");
            Field message=DailyAccessActivity.class.getDeclaredField("message");message.setAccessible(true);
            await(()->((TextView)message.get(gate)).getText().toString().contains("配置网络"),30000,"Network setup prompt missing");
            Field stop=DailyAccessActivity.class.getDeclaredField("stop");stop.setAccessible(true);
            if(autoStop) {
                check(deadline-System.currentTimeMillis()<=65000,"Fixture must have at most one minute left");
                await(()->{try{return query().getStatus()==0;}catch(Exception e){return false;}},90000,"Offline automatic deadline stop failed");
            } else {
                runOnMainSync(()->((Button)value(stop,gate)).performClick());
                await(()->{try{return query().getStatus()==0;}catch(Exception e){return false;}},45000,"Gate safety stop failed");
            }
            Thread.sleep(2000);RecordStatusResultBean idle=query();check(idle.offlineActive==0&&idle.offlineCounter>0,"Offline usage not persisted");
            check(!DailyAccessManager.ready(),"Failed synchronization admitted account");
            if(autoStop)check(idle.offlineCounter-counterBefore==60000,"One minute quota was not metered exactly once");
            client.set(null,savedClient);savedClient=null;
            shell("svc wifi enable");Thread.sleep(10000);
            sync.set(null);runOnMainSync(()->DailyAccessManager.synchronize(true,new IGenericApiCallback<DailyAccessManager.Profile>(){
                public void onSuccess(DailyAccessManager.Profile p){sync.set("ok");}public void onFail(String c,String m){sync.set(m);}public void onFinish(){}
            }));
            await(()->sync.get()!=null,50000,"Reconnect sync timeout");check("ok".equals(sync.get()),"Reconnect receipt sync failed: "+sync.get());
            if(autoStop)check(!DailyAccessManager.device("zzg20230bde63f46f0").recordingAllowed,"Exhausted camera must not obtain another start grant");
            result.putString("stream","\nPASS ("+(autoStop?"one-minute automatic stop":"gate safety stop")+"): daily server sync; real BLE offline form start without quota/create/prepare HTTP; device-assigned session/fence; failed daily gate retains recording and deadline; network setup prompt; gate safety stop; persistent usage; reconnect receipt sync. CID="+cid+"\n");
        }catch(Throwable error){outcome=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+error+" CID="+cid+"\n");}
        finally {
            try{shell("svc wifi enable");}catch(Exception ignored){}
            if(client!=null&&savedClient!=null)try{client.set(null,savedClient);}catch(Exception ignored){}
            try {RecordStatusResultBean s=query();if(cid!=null&&cid.equals(s.competitionId)&&s.getStatus()>0){runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(cid,sid)));await(()->{try{return query().getStatus()==0;}catch(Exception e){return false;}},45000,"Cleanup stop");}}catch(Throwable ignored){}
            runOnMainSync(()->{BluetoothHandle.removePlayNotifyListener(this);if(form!=null&&!form.isFinishing())form.finish();if(gate!=null&&!gate.isFinishing())gate.finish();});
            Constants.ACCESS_MODEL=savedMode;
        }
        finish(outcome,result);
    }
    private void shell(String command)throws Exception {
        try(ParcelFileDescriptor descriptor=getUiAutomation().executeShellCommand(command);FileInputStream input=new FileInputStream(descriptor.getFileDescriptor())){byte[] b=new byte[1024];while(input.read(b)!=-1){}}
    }
    private static Object value(Field f,Object object){try{return f.get(object);}catch(Exception e){throw new RuntimeException(e);}}
    private RecordStatusResultBean query()throws Exception{status.set(null);long end=SystemClock.elapsedRealtime()+20000;while(status.get()==null&&SystemClock.elapsedRealtime()<end){runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new RecordStatusRequestBean()));Thread.sleep(1500);}check(status.get()!=null,"BLE status timeout");return status.get();}
    private interface Condition{boolean ready()throws Exception;}
    private void await(Condition c,long timeout,String message)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(c.ready())return;Thread.sleep(250);}throw new AssertionError(message);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public void onRecordVideoStart(){}public void onRecordVideoStop(){}public void onRecordException(String message){}
    public void onRecordVideoStatusUpdate(RecordStatusResultBean s){if("ok".equalsIgnoreCase(s.getR()))status.set(s);}
}
