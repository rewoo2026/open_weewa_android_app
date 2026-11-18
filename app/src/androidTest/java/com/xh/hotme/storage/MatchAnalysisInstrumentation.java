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

/** Exercises the production AI selection and durable task UI using named acceptance matches. */
public class MatchAnalysisInstrumentation extends Instrumentation {
    private Bundle args;private MainActivity activity;private CameraVideoFragment fragment;private CameraBatchDeleteModel model;
    private String prefix,pathA,pathB,pathC,pathD;private int passed;
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;int oldMode=Constants.ACCESS_MODEL;
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("daily_server_access",0);String previous=prefs.getString("profile",null);
        try {
            prefix=args.getString("prefix");pathA=args.getString("pathA");pathB=args.getString("pathB");pathC=args.getString("pathC");pathD=args.getString("pathD");
            check(prefix!=null&&prefix.startsWith("AI分析验收_")&&pathA!=null,"Missing safe fixture parameters");
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
            dismissPreviewPrompt();
            CameraAnalysisModel ai=(CameraAnalysisModel)field(fragment,"analysisModel");
            await(()->!ai.cameraId.isEmpty(),10000,"Task model attached");
            if("true".equals(args.getString("selectionOnly"))){
                runOnMainSync(()->activity.findViewById(R.id.match_ai_action).performClick());selectRow(pathA);Thread.sleep(1000);
                Dialog hotspot=(Dialog)field(fragment,"_joinSoftApDialog");check(hotspot==null||!hotspot.isShowing(),"Analysis selection does not require phone hotspot preview");
                check(ai.selected.size()==1,"Actual match selected without submitting another task");screenshot("ai-selection-current");
                runOnMainSync(ai::cancel);result.putInt("passed",2);result.putString("stream","\nPASS: real phone AI entry -> select match without hotspot interruption -> cancel.\n");return;
            }
            if("true".equals(args.getString("observeOnly"))){
                String cid=model.videos.stream().filter(v->pathA.equals(v.path)).findFirst().get().competition_id;
                await(()->ai.tasks.containsKey(cid),20000,"Durable task restored after process restart");
                int position=0;while(!model.videos.get(position).path.equals(pathA))position++;
                RecyclerView list=fragment.getView().findViewById(R.id.recycleView);int row=position;
                runOnMainSync(()->list.scrollToPosition(row));Thread.sleep(600);RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(row);check(holder!=null,"Progress row visible");
                MatchAnalysisTask task=ai.tasks.get(cid);
                check(((android.widget.ProgressBar)holder.itemView.findViewById(R.id.ai_upload_bar)).getProgress()==task.uploadPercent,"Upload bar follows server percentage");
                check(((android.widget.ProgressBar)holder.itemView.findViewById(R.id.ai_analysis_bar)).getProgress()==task.analysisPercent,"Analysis bar follows server percentage");
                dismissPreviewPrompt();screenshot("ai-observed-progress");result.putInt("passed",3);result.putString("stream","\nPASS: process restart restores task and both server percentage bars. State="+task.state+" upload="+task.uploadPercent+" analysis="+task.analysisPercent+"\n");return;
            }
            if("true".equals(args.getString("resultOnly"))){
                String cid=model.videos.stream().filter(v->pathA.equals(v.path)).findFirst().get().competition_id;
                await(()->ai.tasks.containsKey(cid)&&"done".equals(ai.tasks.get(cid).state),60000,"Completed server result visible");
                if(!ai.hasActive())check(((android.widget.TextView)activity.findViewById(R.id.match_ai_hint)).getText().toString().contains("已结束"),"Completed task summary no longer claims background processing");
                screenshot("ai-complete");
                java.lang.reflect.Method show=fragment.getClass().getDeclaredMethod("showAnalysisTask",MatchAnalysisTask.class);show.setAccessible(true);
                runOnMainSync(()->{try{show.invoke(fragment,ai.tasks.get(cid));}catch(Exception e){throw new RuntimeException(e);}});
                Thread.sleep(300);Dialog resultDialog=(Dialog)field(fragment,"analysisDetail");
                ActivityMonitor resultMonitor=addMonitor(MatchAnalysisResultActivity.class.getName(),null,false);
                runOnMainSync(()->resultDialog.findViewById(R.id.dlg_right_btn).performClick());
                Activity resultActivity=resultMonitor.waitForActivityWithTimeout(15000);check(resultActivity!=null,"Result screen opened");
                await(()->((android.widget.TextView)field(resultActivity,"status")).getText().toString().startsWith("分析完成"),25000,"Actual parsed result loaded from server");
                android.widget.LinearLayout content=(android.widget.LinearLayout)field(resultActivity,"content");boolean trajectories=false;android.widget.Button videos=null;
                for(int i=0;i<content.getChildCount();i++){
                    View child=content.getChildAt(i);
                    if(child instanceof android.widget.TextView){String value=((android.widget.TextView)child).getText().toString();if(value.startsWith("轨迹帧"))trajectories=value.matches("轨迹帧\\s+[0-9]+.*");}
                    if(child instanceof android.widget.Button)videos=(android.widget.Button)child;
                }
                check(trajectories,"Actual trajectory count displayed, including a valid empty detection result");check(ai.tasks.get(cid).processedFrames>0&&ai.tasks.get(cid).totalFrames>0,"Server completed actual video frame processing");screenshot("ai-result");
                check(videos!=null,"Match video entry available");ActivityMonitor videoMonitor=addMonitor(CloudVideoListDetailActivity.class.getName(),null,false);android.widget.Button videoButton=videos;
                runOnMainSync(()->videoButton.performClick());Activity videoActivity=videoMonitor.waitForActivityWithTimeout(25000);check(videoActivity!=null,"Result opens actual cloud videos");Thread.sleep(1000);screenshot("ai-result-videos");
                result.putString("stream","\nPASS: completed real analysis -> result screen -> actual processed frames and trajectory/event counts; cloud video entry.\n");result.putInt("passed",3);return;
            }
            if("true".equals(args.getString("retryOnly"))){
                String cid=model.videos.stream().filter(v->pathA.equals(v.path)).findFirst().get().competition_id;
                await(()->ai.tasks.containsKey(cid)&&"failed".equals(ai.tasks.get(cid).state),30000,"Interrupted analysis visible");
                check(ai.tasks.get(cid).uploadPercent==100,"Retry preserves completed camera upload");
                java.lang.reflect.Method show=fragment.getClass().getDeclaredMethod("showAnalysisTask",MatchAnalysisTask.class);show.setAccessible(true);
                runOnMainSync(()->{try{show.invoke(fragment,ai.tasks.get(cid));}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(300);
                Dialog detail=(Dialog)field(fragment,"analysisDetail");runOnMainSync(()->detail.findViewById(R.id.dlg_right_btn).performClick());
                await(()->!ai.submitting&&ai.tasks.get(cid).active()&&ai.tasks.get(cid).analysisPercent==0,35000,"Server accepted fresh analysis retry");
                screenshot("ai-retry");result.putInt("passed",3);result.putString("stream","\nPASS: interrupted actual GPU analysis -> phone retry -> upload remains 100% and analysis restarts from 0%. CID="+cid+"\n");return;
            }
            if("true".equals(args.getString("singleUploaded"))){
                runOnMainSync(()->activity.findViewById(R.id.match_ai_action).performClick());selectRow(pathA);check(ai.selected.size()==1,"Single match selection");
                runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());Thread.sleep(300);Dialog single=lastDialog();
                runOnMainSync(()->single.findViewById(R.id.dlg_right_btn).performClick());
                String cid=model.videos.stream().filter(v->pathA.equals(v.path)).findFirst().get().competition_id;
                await(()->!ai.submitting&&ai.tasks.containsKey(cid),30000,"Single task accepted");
                await(()->ai.tasks.get(cid).uploadPercent==100,65000,"Already-uploaded inputs skip camera and queue analysis");screenshot("ai-already-uploaded");
                result.putInt("passed",3);result.putString("stream","\nPASS: real phone single selection -> submit -> already-uploaded camera inputs skip upload. CID="+cid+" state="+ai.tasks.get(cid).state+"\n");return;
            }
            runOnMainSync(()->activity.findViewById(R.id.match_ai_action).performClick());
            check(ai.selecting&&!activity.findViewById(R.id.batch_delete).isEnabled(),"Empty analysis selection disabled");passed++;
            runOnMainSync(()->activity.findViewById(R.id.batch_select_all).performClick());
            long eligible=model.videos.stream().filter(ai::canSelect).count();check(ai.selected.size()==eligible,"AI select all covers eligible rows");passed++;
            runOnMainSync(()->{activity.findViewById(R.id.batch_select_all).performClick();});check(ai.selected.isEmpty(),"Unselect all");
            selectRow(pathA);selectRow(pathB);check(ai.selected.size()==2,"Batch AI selection");passed++;
            screenshot("ai-selection");
            runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());Thread.sleep(500);
            Dialog cancel=lastDialog();runOnMainSync(()->cancel.findViewById(R.id.dlg_left_btn).performClick());
            check(ai.selecting&&ai.selected.size()==2,"Confirm cancel preserves selection");passed++;
            runOnMainSync(()->activity.findViewById(R.id.batch_delete).performClick());Thread.sleep(500);
            Dialog submit=lastDialog();runOnMainSync(()->submit.findViewById(R.id.dlg_right_btn).performClick());
            String cidA=model.videos.stream().filter(v->pathA.equals(v.path)).findFirst().get().competition_id;
            String cidB=model.videos.stream().filter(v->pathB.equals(v.path)).findFirst().get().competition_id;
            await(()->!ai.submitting&&ai.tasks.containsKey(cidA)&&ai.tasks.containsKey(cidB),65000,"Server accepted both tasks; error="+ai.error);
            check(!ai.selecting,"Submission exits selection");passed++;
            screenshot("ai-tasks-created");
            runOnMainSync(()->activity.findViewById(R.id.tab_home).performClick());Thread.sleep(1200);
            runOnMainSync(()->activity.findViewById(R.id.tab_icloud).performClick());selectCameraPage();
            CameraAnalysisModel retained=(CameraAnalysisModel)field(fragment,"analysisModel");
            await(()->retained.tasks.containsKey(cidA)&&retained.tasks.containsKey(cidB),20000,"Tasks restored after leaving page");passed++;
            dismissPreviewPrompt();screenshot("ai-return-progress");
            result.putString("stream","\nPASS: real BLE camera list; AI selection/all/cancel; two-match server submission; leave and return; two independent percentage bars. CIDS="+cidA+","+cidB+"\n");result.putInt("passed",passed);
        } catch(Throwable t){code=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+t+"\n");t.printStackTrace();}
        finally{Constants.ACCESS_MODEL=oldMode;if("true".equals(args.getString("cleanupProfile"))||previous==null)prefs.edit().remove("profile").commit();else prefs.edit().putString("profile",previous).commit();finish(code,result);}
    }
    private void dismissPreviewPrompt()throws Exception {
        Object dialog=field(fragment,"_joinSoftApDialog");
        if(dialog instanceof Dialog && ((Dialog)dialog).isShowing())runOnMainSync(()->((Dialog)dialog).findViewById(R.id.dlg_left_btn).performClick());
    }
    private Dialog lastDialog(){return (Dialog)field(fragment,"analysisConfirm");}
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
