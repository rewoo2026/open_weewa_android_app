package com.xh.hotme.storage;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.annotation.Keep;
import com.xh.hotme.bean.*;
import com.xh.hotme.http.*;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.*;
import org.json.*;
import java.util.List;

/** Read the actual parsed server results for this competition. */
public class MatchAnalysisResultActivity extends Activity {
    private LinearLayout content;
    private String competitionId,deviceId;
    private TextView status;
    @Keep public static class Data { public String competitionName,homeTeamName,awayTeamName,summary;public long trajectoryFrames,eventCount,processedFrames,totalFrames; }
    @Keep public static class Response extends CommonResponse { public Data data; }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);competitionId=getIntent().getStringExtra("competitionId");deviceId=getIntent().getStringExtra("deviceId");
        getWindow().setStatusBarColor(Color.WHITE);getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.WHITE);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(24),dp(24),dp(32));scroll.addView(content);setContentView(scroll);
        TextView back=text("‹  返回",18,Color.rgb(225,107,49));back.setPadding(0,dp(12),0,dp(12));back.setOnClickListener(v->finish());
        text("赛事AI分析结果",26,Color.BLACK);status=text("正在获取分析结果…",16,Color.GRAY);
        JSONObject body=new JSONObject();try{body.put("competitionId",competitionId);body.put("deviceId",deviceId);}catch(JSONException e){status.setText("赛事信息无效");return;}
        OkHttpUtil.postData(SdkApi.getRequestUrl()+"/hotme-api/hm/matchAnalysis/result",body.toString(),null,new OkHttpCallbackDecode<Response>(Response.class){
            public void onDataSuccess(Response response){MainHandler.runOnUiThread(()->{if(isFinishing()||isDestroyed())return;if(response.data==null){status.setText("暂无分析结果");return;}render(response.data);});}
            public void onFailure(String code,String message){MainHandler.runOnUiThread(()->{if(!isFinishing())status.setText("获取失败："+message);});}
            public void onFinish(){}
        });
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(10),0,dp(10));content.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    private void render(Data data){
        status.setText("分析完成 · 相机上传 100% · AI分析 100%");status.setTextColor(Color.rgb(48,142,94));
        text(data.competitionName,22,Color.BLACK);text("已分析视频帧  "+data.processedFrames+" / "+data.totalFrames,17,Color.DKGRAY);text("轨迹帧  "+data.trajectoryFrames+"    识别事件  "+data.eventCount,17,Color.DKGRAY);
        if(data.eventCount==0)text("本次分析未识别到赛事事件。",15,Color.GRAY);
        text("双方技术统计",20,Color.BLACK);
        try{
            JSONObject summary=new JSONObject(data.summary==null||data.summary.isEmpty()?"{}":data.summary);
            JSONArray teams=summary.optJSONArray("teams"),stats=summary.optJSONArray("stats");
            text((teams==null?data.homeTeamName:teams.optString(0,"主队"))+"  /  "+(teams==null?data.awayTeamName:teams.optString(1,"客队")),17,Color.DKGRAY);
            if(stats==null||stats.length()==0)text("暂无双方技术统计。",15,Color.GRAY);
            else for(int i=0;i<stats.length();i++){
                JSONObject item=stats.getJSONObject(i);JSONArray values=item.optJSONArray("values");
                text(item.optString("name")+"    "+(values==null?"—":values.optString(0,"—"))+"  /  "+(values==null?"—":values.optString(1,"—")),17,Color.DKGRAY);
            }
        }catch(JSONException e){text("技术统计暂不可用，请稍后重试。",15,Color.GRAY);}
        Button video=new Button(this);video.setText("查看赛事视频");video.setTextColor(Color.WHITE);video.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(241,108,47)));content.addView(video);video.setOnClickListener(v->openVideos(video));
    }
    private void openVideos(Button button){
        button.setEnabled(false);
        ApiUtil.getCompetitionView(competitionId,new IGenericApiCallback<HmCompetition>(){
            public void onSuccess(HmCompetition competition){
                CompetitionVideoRequestBean request=new CompetitionVideoRequestBean();request.setCompetitionId(competitionId);
                ApiUtil.getCompetitionVideo(request,new IGenericApiCallback<List<HmVideo>>(){
                    public void onSuccess(List<HmVideo> videos){if(isFinishing()||isDestroyed())return;VideoManager.getImpl().setVideoList(videos);Intent intent=new Intent(MatchAnalysisResultActivity.this,CloudVideoListDetailActivity.class);intent.putExtra(CloudVideoListDetailActivity.EXTRA_COMPETITION,GsonUtils.GsonString(competition));startActivity(intent);}
                    public void onFail(String code,String message){ToastUtil.s(message);}public void onFinish(){button.setEnabled(true);}
                });
            }
            public void onFail(String code,String message){ToastUtil.s(message);button.setEnabled(true);}public void onFinish(){}
        });
    }
}
