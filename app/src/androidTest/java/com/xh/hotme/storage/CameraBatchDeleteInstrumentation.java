package com.xh.hotme.storage;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.xh.hotme.R;
import com.xh.hotme.account.*;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.*;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.main.*;
import com.xh.hotme.utils.*;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;

/** Operates the production storage page; destructive actions are restricted to host-created fixtures. */
public class CameraBatchDeleteInstrumentation extends Instrumentation {
    private Bundle args;private MainActivity activity;private CameraVideoFragment fragment;private CameraBatchDeleteModel model;
    private String prefix,pathA,pathB,pathC,pathD;private int passed;
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;int oldMode=Constants.ACCESS_MODEL;
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("daily_server_access",0);String previous=prefs.getString("profile",null);
        try {
            prefix=args.getString("prefix");pathA=args.getString("pathA");pathB=args.getString("pathB");pathC=args.getString("pathC");pathD=args.getString("pathD");
            check(prefix!=null&&prefix.startsWith("批量删除验收_")&&pathA!=null,"Missing safe fixture parameters");
            await(()->com.xh.hotme.HotmeApplication.getInstance()!=null,10000,"App initialized");
            check(LoginManager.isLogon(),"Sign in before running this test");
            DailyAccessManager.Profile profile=new DailyAccessManager.Profile();
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((LoginManager.getUserId()+":"+LoginManager.getUserToken()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder identity=new StringBuilder();for(byte b:digest)identity.append(String.format("%02x",b&255));
            profile.account=identity.toString();profile.serverTime=System.currentTimeMillis();profile.refreshAt=profile.serverTime+3600000;profile.savedWall=profile.serverTime;profile.savedElapsed=SystemClock.elapsedRealtime();
            prefs.edit().putString("profile",new Gson().toJson(profile)).commit();
            runOnMainSync(()->{Constants.ACCESS_MODEL=Constants.ACCESS_MODEL_LOCAL;CameraManager.newCamera("3A:A3:0A:04:00:A5");BluetoothManager.getImpl().connectToBle(new Device("3A:A3:0A:04:00:A5",0,"weewa520"));});
            await(()->BluetoothManager.getImpl().isConnected(),30000,"Real Bluetooth connected");
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);shell("am start -n com.xh.hotme/.main.MainActivity");
            activity=(MainActivity)monitor.waitForActivityWithTimeout(15000);check(activity!=null,"Main activity opened");
            runOnMainSync(()->activity.findViewById(R.id.tab_icloud).performClick());Thread.sleep(1500);
            selectCameraPage();runOnMainSync(()->{model.attachCamera(CameraManager.getDeviceId());model.refresh();});
            await(()->model.protocol==1&&model.videos.stream().anyMatch(v->pathA.equals(v.path)),60000,"Correlated camera list");
            if("true".equals(args.getString("restoreOnly"))){
                runOnMainSync(()->{model.enterSelection();model.toggle(pathA);});
                CameraBatchDeleteModel retained=model;
                ActivityMonitor recreation=addMonitor(MainActivity.class.getName(),null,false);
                runOnMainSync(activity::recreate);activity=(MainActivity)recreation.waitForActivityWithTimeout(15000);check(activity!=null,"Activity recreated");
                selectCameraPage();check(model==retained&&model.selecting&&model.selected.contains(pathA),"Selection survives activity recreation");passed++;
                await(()->!model.loading,60000,"Restored list refreshed");
                screenshot("selection-restored");
                runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());screenshot("confirmation");
                final Dialog preview=(Dialog)field(fragment,"batchConfirm");runOnMainSync(()->preview.findViewById(R.id.dlg_left_btn).performClick());
                result.putInt("passed",passed);result.putString("stream","\nPASS: real camera storage selection survives Activity recreation; confirmation preview captured without deletion.\n");
                return;
            }
            runOnMainSync(()->activity.findViewById(R.id.batch_manage).performClick());
            check(!activity.findViewById(R.id.batch_delete).isEnabled(),"Empty selection disables deletion");passed++;
            runOnMainSync(()->activity.findViewById(R.id.batch_select_all).performClick());
            int expected=0;for(CameraVideoListBean.VideosBean v:model.videos)if(model.canSelect(v))expected++;
            check(model.selected.size()==expected,"Select all uses complete list");check(model.videos.stream().filter(v->!model.canSelect(v)).noneMatch(v->model.selected.contains(v.path)),"Busy rows excluded");passed++;
            runOnMainSync(()->{model.cancelSelection();model.enterSelection();});
            selectRow(pathA);selectRow(pathB);check(model.selected.size()==2,"Two rows selected");
            runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());
            com.xh.hotme.widget.ModalDialog confirmation=(com.xh.hotme.widget.ModalDialog)field(fragment,"batchConfirm");
            runOnMainSync(()->confirmation.findViewById(R.id.dlg_left_btn).performClick());check(model.session.state==BatchDeleteSession.State.IDLE,"Cancel has no deletion side effect");passed++;
            // Simulate a stale match identity between list and confirmed deletion.
            runOnMainSync(()->{for(CameraVideoListBean.VideosBean v:model.videos)if(pathB.equals(v.path))v.delete_identity="stale-fixture";activity.findViewById(R.id.batch_delete).performClick();});
            clickConfirm();await(()->model.session.state==BatchDeleteSession.State.FINISHED&&!model.loading,65000,"Partial deletion and list refresh");
            check(model.session.count(BatchDeleteSession.Outcome.SUCCESS)==1&&model.session.count(BatchDeleteSession.Outcome.FAILED)==1,"Per-match result isolation");passed++;
            com.xh.hotme.widget.ModalDialog outcome=(com.xh.hotme.widget.ModalDialog)field(fragment,"batchResult");
            final com.xh.hotme.widget.ModalDialog retryDialog=outcome;
            runOnMainSync(()->retryDialog.findViewById(R.id.dlg_right_btn).performClick());clickConfirm();
            await(()->model.session.state==BatchDeleteSession.State.FINISHED&&!model.loading&&model.session.count(BatchDeleteSession.Outcome.SUCCESS)==1,65000,"Explicit retry succeeded");passed++;
            outcome=(com.xh.hotme.widget.ModalDialog)field(fragment,"batchResult");final com.xh.hotme.widget.ModalDialog success=outcome;
            runOnMainSync(()->success.findViewById(R.id.dlg_right_btn).performClick());
            runOnMainSync(()->{model.cancelSelection();model.enterSelection();});selectRow(pathC);selectRow(pathD);
            runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());
            final com.xh.hotme.widget.ModalDialog stopConfirm=(com.xh.hotme.widget.ModalDialog)field(fragment,"batchConfirm");
            runOnMainSync(()->{stopConfirm.findViewById(R.id.dlg_right_btn).performClick();((Dialog)field(fragment,"batchProgress")).findViewById(R.id.batch_progress_stop).performClick();});
            await(()->model.session.state==BatchDeleteSession.State.FINISHED&&!model.loading,65000,"Stopped queue");
            check(model.session.count(BatchDeleteSession.Outcome.SUCCESS)==1&&model.session.count(BatchDeleteSession.Outcome.PENDING)==1,"Only current match finished after stop");passed++;
            screenshot("stopped-result");
            result.putString("stream","\nPASS: real BLE camera list; production select-all and protected rows; row selection; cancellation; partial deletion; explicit retry; stop after current match. Only named fixture matches deleted.\n");result.putInt("passed",passed);
        } catch(Throwable t){code=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+t+"\n");t.printStackTrace();}
        finally{Constants.ACCESS_MODEL=oldMode;if(previous==null)prefs.edit().remove("profile").commit();else prefs.edit().putString("profile",previous).commit();finish(code,result);}
    }
    private void selectCameraPage()throws Exception {
        CloudFragment cloud=null;for(Fragment f:activity.getSupportFragmentManager().getFragments())if(f instanceof CloudFragment)cloud=(CloudFragment)f;
        check(cloud!=null,"Cloud fragment exists");CloudFragment parent=cloud;
        runOnMainSync(()->((ViewPager)parent.getView().findViewById(R.id.view_pager)).setCurrentItem(2,false));Thread.sleep(800);
        for(Fragment f:parent.getChildFragmentManager().getFragments())if(f instanceof CameraVideoFragment)fragment=(CameraVideoFragment)f;
        check(fragment!=null,"Camera storage fragment exists");model=(CameraBatchDeleteModel)field(fragment,"batchModel");
    }
    private void selectRow(String path)throws Exception {
        CameraVideoListBean.VideosBean video=model.videos.stream().filter(v->path.equals(v.path)).findFirst().orElseThrow(()->new AssertionError("Fixture absent "+path));
        check(video.competition_name.startsWith(prefix),"Refusing non-fixture selection");int position=model.videos.indexOf(video);
        RecyclerView list=fragment.getView().findViewById(R.id.recycleView);runOnMainSync(()->list.scrollToPosition(position));Thread.sleep(500);
        runOnMainSync(()->{RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(position);check(holder!=null,"Row visible");holder.itemView.findViewById(R.id.select_match).performClick();});
    }
    private void clickConfirm()throws Exception {Thread.sleep(200);Dialog d=(Dialog)field(fragment,"batchConfirm");runOnMainSync(()->d.findViewById(R.id.dlg_right_btn).performClick());}
    private void screenshot(String name)throws Exception {Thread.sleep(500);android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot available");try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),"batch-"+name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
    private static Object field(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){throw new RuntimeException(e);}}
    private void shell(String command)throws Exception {try(ParcelFileDescriptor d=getUiAutomation().executeShellCommand(command);FileInputStream in=new FileInputStream(d.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}}
    private interface Condition {boolean ready()throws Exception;}
    private void await(Condition c,long ms,String label)throws Exception {long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){if(c.ready())return;Thread.sleep(200);}throw new AssertionError(label+" timed out");}
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
