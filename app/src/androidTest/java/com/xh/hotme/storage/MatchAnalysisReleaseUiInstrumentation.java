package com.xh.hotme.storage;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Exercises the signed release through real views; no app reflection or test profiles. */
public class MatchAnalysisReleaseUiInstrumentation extends Instrumentation {
    private Bundle args;
    private volatile Activity main;
    private volatile Activity resumed;
    private String matchName;
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;
        try{
            matchName=args.getString("matchName","AI分析验收_已上传复用");
            boolean observeOnly="true".equals(args.getString("observeOnly"));
            check(matchName.startsWith("AI分析验收_")||(observeOnly&&(matchName.equals("测试1006")||matchName.equals("测试1006-02"))),"Only acceptance matches or read-only original match observation may be used");
            main=openMain();Thread.sleep(1000);
            View cloud=main.findViewById(id("tab_icloud"));check(cloud!=null,"Release main navigation exists");runOnMainSync(cloud::performClick);
            await(()->findText(main.getWindow().getDecorView(),"相机存储")!=null,20000,"Actual cloud page");
            View camera=findText(main.getWindow().getDecorView(),"相机存储");while(camera!=null&&!camera.hasOnClickListeners())camera=camera.getParent() instanceof View?(View)camera.getParent():null;
            check(camera!=null,"Camera storage tab is clickable");View tab=camera;runOnMainSync(tab::performClick);
            View selector=main.findViewById(id("photo_select"));check(selector!=null,"Actual camera storage selector");runOnMainSync(selector::performClick);
            await(()->resumed!=null&&"com.xh.hotme.camera.CameraListActivity".equals(resumed.getClass().getName()),15000,"Actual camera chooser opened");
            await(()->findText(resumed.getWindow().getDecorView(),"weewa520(00:A5)")!=null,20000,"Bound hardware camera visible");
            await(this::cameraActuallyOnline,30000,"Fresh Bluetooth scan reports the named hardware online");
            View device=findText(resumed.getWindow().getDecorView(),"weewa520(00:A5)");View parent=device;View connect=null;
            while(parent!=null){connect=parent.findViewById(id("btn_device_connect"));if(connect!=null)break;parent=parent.getParent() instanceof View?(View)parent.getParent():null;}
            check(connect!=null,"Actual camera chooser connect action");View button=connect;runOnMainSync(button::performClick);
            await(()->resumed==main&&main.hasWindowFocus(),45000,"Camera chooser connected and returned normally");
            if(observeOnly){
                await(()->main.findViewById(id("match_ai_action"))!=null&&main.findViewById(id("match_ai_action")).isShown(),45000,"Actual camera metadata loaded");
                for(int attempt=0;attempt<8&&!originalRowVisible();attempt++){scrollMatchList();Thread.sleep(600);}
                await(this::originalRowVisible,15000,"Original match visible");
                await(()->((ProgressBar)row().findViewById(id("ai_upload_bar"))).getProgress()==100,25000,"Original upload already complete");
                if(matchName.equals("测试1006"))await(()->((ProgressBar)row().findViewById(id("ai_analysis_bar"))).getProgress()>0,25000,"Actual original analysis preparation percentage restored");
                String status=((TextView)row().findViewById(id("ai_status"))).getText().toString();
                int percent=((ProgressBar)row().findViewById(id("ai_analysis_bar"))).getProgress();
                check(status.contains("准备视频")||status.contains("AI分析中")||status.contains("前方有")||status.contains("分析完成")||status.contains("结果生成中")||status.contains("正在生成结果"),"Actual server stage explanation displayed: "+status);
                result.putInt("uploadPercent",100);result.putInt("analysisPercent",percent);screenshot("original-"+matchName);
                Thread.sleep(6500);check(originalRowVisible(),"Original row survives background refresh");
                if("true".equals(args.getString("verifyResult"))){
                    await(()->((ProgressBar)row().findViewById(id("ai_analysis_bar"))).getProgress()==100,300000,"Original result actually complete");
                    View resultAction=row().findViewById(id("ai_status"));runOnMainSync(resultAction::performClick);
                    await(()->nodeId("dlg_right_btn")!=null,10000,"Original result action offered");
                    check("查看分析结果".contentEquals(nodeId("dlg_right_btn").getText()),"Original task offers actual result");
                    check(nodeId("dlg_right_btn").performAction(AccessibilityNodeInfo.ACTION_CLICK),"Original result action accepted");
                    await(()->nodeText("赛事AI分析结果")!=null&&nodeTextPrefix("已分析视频帧")!=null,45000,"Original parsed result loaded");
                    String actualFrames=nodeTextPrefix("已分析视频帧").getText().toString();String expected=args.getString("expectedFrames");
                    check(expected!=null&&actualFrames.replaceAll("\\s+","").equals("已分析视频帧"+expected+"/"+expected),"Original actual frame counts: "+actualFrames);
                    String actualEvents=nodeTextPrefix("轨迹帧").getText().toString();
                    String expectedEvents=args.getString("expectedEvents"),expectedTrajectory=args.getString("expectedTrajectory");
                    check(expectedEvents!=null&&expectedTrajectory!=null&&actualEvents.replaceAll("\\s+","").equals("轨迹帧"+expectedTrajectory+"识别事件"+expectedEvents),"Original complete parsed data: "+actualEvents);
                    screenshot("original-result-"+matchName);
                    result.putString("stream","\nPASS: signed release, real camera, original match "+matchName+" completed Upload=100 AI=100; parsed result "+actualFrames+"; "+actualEvents+"\n");return;
                }
                result.putString("stream","\nPASS: signed release, real camera, read-only original match "+matchName+", Upload=100 AI="+percent+" status="+status+"\n");return;
            }
            await(()->main.hasWindowFocus()&&main.findViewById(id("match_ai_action"))!=null&&main.findViewById(id("match_ai_action")).isShown()&&findText(main.getWindow().getDecorView(),matchName)!=null&&findText(main.getWindow().getDecorView(),matchName).isShown(),45000,"Release visible real camera list and durable tasks");
            if ("true".equals(args.getString("submit"))) {
                check(matchName.startsWith("AI分析验收_"), "Only the named acceptance match may be submitted");
                View action=main.findViewById(id("match_ai_action"));runOnMainSync(action::performClick);
                await(()->main.findViewById(id("batch_bottom")).isShown(),10000,"Analysis selection mode");
                await(()->row()!=null,15000,"Named row survives selection mode refresh");
                View selectedRow=row();
                while(selectedRow!=null&&selectedRow.findViewById(id("select_match"))==null)
                    selectedRow=selectedRow.getParent() instanceof View?(View)selectedRow.getParent():null;
                check(selectedRow!=null,"Named real recording has a selectable row");
                View checkbox=selectedRow.findViewById(id("select_match"));runOnMainSync(checkbox::performClick);
                TextView submit=(TextView)main.findViewById(id("batch_delete"));
                check("开始分析（1）".contentEquals(submit.getText())&&submit.isEnabled(),"Exactly one acceptance match selected");
                runOnMainSync(submit::performClick);
                await(()->nodeText("提交分析任务")!=null,10000,"Real analysis confirmation dialog");
                check(nodeText("提交分析任务").performAction(AccessibilityNodeInfo.ACTION_CLICK),"Real task submission accepted");
                await(()->main.hasWindowFocus()&&!main.findViewById(id("batch_bottom")).isShown(),30000,"Analysis submission completed");
                progress("Submitted the named real camera recording: "+matchName);
                await(()->row()!=null&&((ProgressBar)row().findViewById(id("ai_upload_bar"))).getProgress()==100
                        &&((ProgressBar)row().findViewById(id("ai_analysis_bar"))).getProgress()>0,
                        1200000,"Real camera upload and GPU analysis admission");
            }
            View entry=main.findViewById(id("match_ai_action"));View manage=main.findViewById(id("batch_manage"));int[] entryPosition=new int[2],managePosition=new int[2];entry.getLocationOnScreen(entryPosition);manage.getLocationOnScreen(managePosition);check(entryPosition[0]<managePosition[0],"AI entry is left of manage");
            runOnMainSync(entry::performClick);await(()->main.findViewById(id("batch_bottom")).isShown(),10000,"Release selection mode visible without hotspot");
            View all=main.findViewById(id("batch_select_all"));runOnMainSync(all::performClick);check(main.findViewById(id("batch_delete")).isEnabled(),"Release selects available matches");screenshot("release-selection");
            runOnMainSync(manage::performClick);await(()->entry.isShown()&&!main.findViewById(id("batch_bottom")).isShown(),10000,"Release cancels selection without submitting");
            await(()->row()!=null&&row().findViewById(id("ai_upload_bar"))!=null,25000,"Release percentage row loaded");
            ProgressBar upload=(ProgressBar)row().findViewById(id("ai_upload_bar"));ProgressBar analysis=(ProgressBar)row().findViewById(id("ai_analysis_bar"));
            await(()->upload.getProgress()==100&&analysis.getProgress()>0,30000,"Release restores actual server percentage fields after R8");
            result.putInt("uploadPercent",upload.getProgress());result.putInt("analysisPercent",analysis.getProgress());screenshot("release-progress");
            if("true".equals(args.getString("previewDialog"))){View container=row();while(container!=null&&container.findViewById(id("match_row"))==null)container=container.getParent() instanceof View?(View)container.getParent():null;View match=container==null?null:container.findViewById(id("match_row"));check(match!=null,"Actual local preview action");runOnMainSync(match::performClick);await(()->nodeId("dlg_left_btn")!=null,10000,"Explicit video preview offers phone hotspot connection");Thread.sleep(6500);check(nodeId("dlg_left_btn")!=null,"Explicit preview dialog survives AI polling");screenshot("release-preview-dialog");check(nodeId("dlg_left_btn").performAction(AccessibilityNodeInfo.ACTION_CLICK),"Preview prompt cancelled");await(()->main.hasWindowFocus(),10000,"Returns to actual AI task list");}
            if(!"true".equals(args.getString("resultOnly"))){result.putString("stream","\nPASS: signed release, normal account, real BLE camera list, AI entry and server percentage fields. Upload="+upload.getProgress()+" AI="+analysis.getProgress()+"\n");return;}
            await(()->((ProgressBar)row().findViewById(id("ai_analysis_bar"))).getProgress()==100,1200000,"Actual server analysis complete");
            await(()->((TextView)main.findViewById(id("match_ai_hint"))).getText().toString().contains("已结束"),20000,"Release terminal-state summary");
            screenshot("release-complete");View status=row().findViewById(id("ai_status"));runOnMainSync(status::performClick);
            await(()->nodeId("dlg_right_btn")!=null,10000,"Result action dialog");check("查看分析结果".contentEquals(nodeId("dlg_right_btn").getText()),"Completed task offers result viewing");
            check(nodeId("dlg_right_btn").performAction(AccessibilityNodeInfo.ACTION_CLICK),"Result button action accepted");
            await(()->nodeText("赛事AI分析结果")!=null&&nodeTextPrefix("已分析视频帧")!=null,30000,"Release reads actual parsed result DTO");
            String frames=nodeTextPrefix("已分析视频帧").getText().toString();check(frames.matches("已分析视频帧\\s+[1-9][0-9]*\\s*/\\s*[1-9][0-9]*"),"Actual completed frame counts survived release shrinking");screenshot("release-result");
            ActivityMonitor videoMonitor=addMonitor("com.xh.hotme.storage.CloudVideoListDetailActivity",null,false);
            AccessibilityNodeInfo videos=nodeText("查看赛事视频");check(videos!=null,"Release result has video action");videos.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());Thread.sleep(500);check(videos.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Video action accepted");
            check(videoMonitor.waitForActivityWithTimeout(30000)!=null,"Release opens actual cloud video list");Thread.sleep(1000);screenshot("release-result-videos");
            result.putString("stream","\nPASS: signed release, no injected profile, real camera -> both percentages -> terminal summary -> parsed result "+frames+" -> actual cloud videos.\n");
        }catch(Throwable t){code=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+t+"\n");t.printStackTrace();try{screenshot("failure");}catch(Exception ignored){}}
        finally{finish(code,result);}
    }
    @Override public void callActivityOnResume(Activity activity){super.callActivityOnResume(activity);resumed=activity;progress("Resumed: "+activity.getClass().getName());if("com.xh.hotme.main.MainActivity".equals(activity.getClass().getName()))main=activity;}
    private boolean cameraActuallyOnline(){
        if(Looper.myLooper()!=Looper.getMainLooper()){
            java.util.concurrent.atomic.AtomicBoolean available=new java.util.concurrent.atomic.AtomicBoolean();
            runOnMainSync(()->available.set(cameraActuallyOnline()));return available.get();
        }
        if(resumed==null)return false;
        View parent=findText(resumed.getWindow().getDecorView(),"weewa520(00:A5)");
        while(parent!=null){
            View status=parent.findViewById(id("tv_device_status"));
            if(status instanceof TextView)return ((TextView)status).getText().toString().contains("在线");
            parent=parent.getParent() instanceof View?(View)parent.getParent():null;
        }
        return false;
    }
    private Activity openMain()throws Exception{progress("Opening signed release main");try(InputStream launch=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("am start -W -f 0x14000000 -n com.xh.hotme/com.xh.hotme.main.MainActivity"))){ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int read;while((read=launch.read(buffer))>=0)output.write(buffer,0,read);progress(output.toString("UTF-8"));}await(()->main!=null&&main.hasWindowFocus(),20000,"Release main resumed (activity="+(main==null?"null":main.getClass().getName())+")");return main;}
    private void progress(String message){Bundle update=new Bundle();update.putString("stream","\n"+message+"\n");sendStatus(1,update);}
    private int id(String name){return getTargetContext().getResources().getIdentifier(name,"id","com.xh.hotme");}
    private View findText(View view,String text){if(Looper.myLooper()!=Looper.getMainLooper()){java.util.concurrent.atomic.AtomicReference<View> result=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->result.set(findText(view,text)));return result.get();}if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private View row(){if(Looper.myLooper()!=Looper.getMainLooper()){java.util.concurrent.atomic.AtomicReference<View> result=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->result.set(row()));return result.get();}View title=findMatchTitle(main.getWindow().getDecorView());View parent=title;while(parent!=null){if(parent.findViewById(id("ai_progress_group"))!=null&&parent.findViewById(id("match_title"))==title)return parent;parent=parent.getParent() instanceof View?(View)parent.getParent():null;}return null;}
    private View findMatchTitle(View view){if(view instanceof TextView&&view.getId()==id("match_title")&&matchName.contentEquals(((TextView)view).getText())&&view.getGlobalVisibleRect(new android.graphics.Rect()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);View found=child==null?null:findMatchTitle(child);if(found!=null)return found;}}return null;}
    private boolean originalRowVisible(){if(Looper.myLooper()!=Looper.getMainLooper()){java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();runOnMainSync(()->result.set(originalRowVisible()));return result.get();}View target=row();if(target==null)return false;View title=target.findViewById(id("match_title"));return title instanceof TextView&&matchName.contentEquals(((TextView)title).getText())&&title.getGlobalVisibleRect(new android.graphics.Rect());}
    private void scrollMatchList()throws Exception{View list=main.findViewById(id("recycleView"));if(list!=null&&list.isShown()){runOnMainSync(()->list.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null));return;}android.util.DisplayMetrics metrics=getTargetContext().getResources().getDisplayMetrics();int x=metrics.widthPixels/2;try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("input swipe "+x+" "+(metrics.heightPixels*3/4)+" "+x+" "+(metrics.heightPixels/3)+" 350"))){while(in.read()!=-1){}}}
    private AccessibilityNodeInfo nodeId(String name){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByViewId("com.xh.hotme:id/"+name);return nodes.isEmpty()?null:nodes.get(0);}
    private AccessibilityNodeInfo nodeText(String text){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(n.getText()))return n;return null;}
    private AccessibilityNodeInfo nodeTextPrefix(String text){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(n.getText()!=null&&n.getText().toString().startsWith(text))return n;return null;}
    private interface Condition{boolean ready()throws Exception;}
    private void await(Condition condition,long timeout,String message)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(condition.ready())return;Thread.sleep(250);}throw new AssertionError(message);}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void screenshot(String name)throws Exception{Thread.sleep(500);Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot available");try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"batch-"+name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
