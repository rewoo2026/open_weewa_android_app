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
import java.util.concurrent.atomic.AtomicReference;

/** Explicit real-device acceptance: normal form, six-minute recording, BLE stop, durable receipt. */
public class AnalysisChainInstrumentation extends Instrumentation implements IRecordControlListener {
    private final AtomicReference<RecordStatusResultBean> status=new AtomicReference<>();
    private String cid,sid;
    private AddRecordTaskActivity form;
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){
        Bundle result=new Bundle();int outcome=Activity.RESULT_OK;
        try {
            await(()->com.xh.hotme.HotmeApplication.getInstance()!=null,10000,"Application initialization");
            try(android.os.ParcelFileDescriptor p=getUiAutomation().executeShellCommand("am start -n com.xh.hotme/.main.MainActivity");java.io.FileInputStream in=new java.io.FileInputStream(p.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}
            Thread.sleep(2500);
            AtomicReference<String> synced=new AtomicReference<>();
            runOnMainSync(()->DailyAccessManager.synchronize(true,new IGenericApiCallback<DailyAccessManager.Profile>(){
                public void onSuccess(DailyAccessManager.Profile p){synced.set("ok");}
                public void onFail(String c,String m){synced.set(m);}public void onFinish(){}
            }));
            await(()->synced.get()!=null,60000,"Daily access synchronization");
            check("ok".equals(synced.get()),"Daily access failed: "+synced.get());
            Constants.ACCESS_MODEL=Constants.ACCESS_MODEL_LOCAL;
            runOnMainSync(()->{CameraManager.newCamera("3A:A3:0A:04:00:A5");BluetoothHandle.addPlayNotifyListener(this);BluetoothManager.getImpl().connectToBle(new Device("3A:A3:0A:04:00:A5",0,"weewa520"));});
            await(()->BluetoothManager.getImpl().isConnected(),30000,"Bluetooth connection");
            check(query().getStatus()==0,"Camera is already recording");
            check(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS),"Enable video analysis in the app before running");
            form=(AddRecordTaskActivity)startActivitySync(new Intent(getTargetContext(),AddRecordTaskActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            String name="E2E_"+System.currentTimeMillis();
            runOnMainSync(()->{((EditText)form.findViewById(R.id.tv_game_name)).setText(name);((EditText)form.findViewById(R.id.home_team_name)).setText("E2E_HOME");((EditText)form.findViewById(R.id.guest_team_name)).setText("E2E_AWAY");form.findViewById(R.id.btn_record).performClick();});
            await(form::isFinishing,120000,"Form did not confirm recording");
            RecordStatusResultBean started=query();cid=started.competitionId;sid=started.sessionId;
            check(started.getStatus()==1&&started.firstFrame==1&&name.equals(started.getVideoName()),"No first frame or mismatched recording");
            Bundle progress=new Bundle();progress.putString("stream","\nRECORDING: cid="+cid+" name="+name+"\n");sendStatus(0,progress);
            for(int i=0;i<34;i++){Thread.sleep(10000);check(query().getStatus()==1,"Recording stopped before six-minute acceptance");}
            runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(cid,sid)));
            await(()->query().getStatus()==0,45000,"Stop did not complete");
            Thread.sleep(2000);RecordStatusResultBean ended=query();
            runOnMainSync(()->OfflineRecordingReports.observe(ended));Thread.sleep(1000);
            synced.set(null);
            runOnMainSync(()->OfflineRecordingReports.flush(new IGenericApiCallback<Void>(){
                public void onSuccess(Void v){synced.set("ok");}public void onFail(String c,String m){synced.set(m);}public void onFinish(){}
            }));
            await(()->synced.get()!=null,60000,"Receipt synchronization");
            check("ok".equals(synced.get()),"Receipt failed: "+synced.get());
            result.putString("stream","\nPASS: real Android form -> BLE first frame -> 340 seconds -> stopped -> server receipt. CID="+cid+"\n");
        }catch(Throwable e){outcome=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+e+" CID="+cid+"\n");}
        finally{
            if(cid!=null)try{RecordStatusResultBean s=query();if(s.getStatus()>0&&cid.equals(s.competitionId))runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(cid,sid)));}catch(Exception ignored){}
            runOnMainSync(()->{BluetoothHandle.removePlayNotifyListener(this);if(form!=null&&!form.isFinishing())form.finish();});
            finish(outcome,result);
        }
    }
    private RecordStatusResultBean query() throws Exception{status.set(null);long end=System.currentTimeMillis()+25000;while(status.get()==null&&System.currentTimeMillis()<end){runOnMainSync(()->BluetoothManager.getImpl().sendCmdSimpleObj(new RecordStatusRequestBean()));Thread.sleep(1500);}check(status.get()!=null,"BLE status timeout");return status.get();}
    public void onRecordVideoStart(){}public void onRecordVideoStop(){}public void onRecordException(String m){}
    public void onRecordVideoStatusUpdate(RecordStatusResultBean s){status.set(s);}
    private interface Condition{boolean get()throws Exception;}
    private void await(Condition c,long ms,String m)throws Exception{long end=System.currentTimeMillis()+ms;while(System.currentTimeMillis()<end){if(c.get())return;Thread.sleep(200);}throw new AssertionError(m);}
    private void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
