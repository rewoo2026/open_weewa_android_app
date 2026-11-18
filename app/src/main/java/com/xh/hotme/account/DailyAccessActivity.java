package com.xh.hotme.account;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.record.LocalRecordingReports;

/** Blocks entry, while retaining a safety stop for a recording already in progress. */
public class DailyAccessActivity extends AppCompatActivity implements IRecordControlListener {
    private TextView message;
    private Button retry, stop;
    private boolean stopping;
    private long stopStarted;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable stopPoll=new Runnable(){public void run(){
        if(!stopping)return;
        if(android.os.SystemClock.elapsedRealtime()-stopStarted>30000){stopping=false;stop.setEnabled(true);message.setText("停止尚未确认，请检查相机蓝牙连接后重试。录制仍受原截止时间限制。");return;}
        query();handler.postDelayed(this,1000);
    }};
    private RecordStatusResultBean current;
    private final Runnable statusPoll=new Runnable(){public void run(){if(!isFinishing()){query();handler.postDelayed(this,2000);}}};
    private void connectCamera() {
        final java.util.List<String> addresses=new java.util.ArrayList<>(),names=new java.util.ArrayList<>();
        for(java.util.Map.Entry<String,?> e:getSharedPreferences("com.xh.hotme",0).getAll().entrySet()) {
            if(!e.getKey().startsWith("device_")||!(e.getValue() instanceof String))continue;
            String address=e.getKey().substring(7);if(!address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}"))continue;
            try {com.google.gson.JsonObject data=new com.google.gson.Gson().fromJson((String)e.getValue(),com.google.gson.JsonObject.class);
                String name=data.has("device_id")?data.get("device_id").getAsString():address;addresses.add(address);names.add(name);
            }catch(RuntimeException ignored){}
        }
        if(addresses.isEmpty()){message.setText("没有已连接过的相机。请先配置网络并完成登录；已有录制仍受原截止时间限制。");return;}
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("连接相机以查看或停止录制")
            .setItems(names.toArray(new String[0]),(dialog,which)->{
                String address=addresses.get(which);com.xh.hotme.camera.CameraManager.newCamera(address);
                BluetoothManager.getImpl().connectToBle(new com.xh.hotme.bluetooth.Device(address,0,names.get(which)));
            }).setNegativeButton("取消",null).show();
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(24 * getResources().getDisplayMetrics().density); layout.setPadding(padding,padding,padding,padding);
        message = new TextView(this); message.setTextSize(18); layout.addView(message);
        Button network = new Button(this); network.setText("配置网络"); layout.addView(network);
        network.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        Button connect=new Button(this);connect.setText("连接相机以停止录制");layout.addView(connect);connect.setOnClickListener(v->connectCamera());
        retry = new Button(this); retry.setText("重新同步"); layout.addView(retry); retry.setOnClickListener(v -> synchronize());
        stop = new Button(this); stop.setText("停止当前录制"); stop.setVisibility(View.GONE); layout.addView(stop);
        stop.setOnClickListener(v -> {
            if (current != null && current.getStatus() > 0 && current.sessionId != null) {
                stopping = true;stopStarted=android.os.SystemClock.elapsedRealtime(); stop.setEnabled(false);handler.postDelayed(stopPoll,1000);
                BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(current.competitionId,current.sessionId));
                query();
            }
        });
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(layout);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            layout.setPadding(padding+bars.left,padding+bars.top,padding+bars.right,padding+bars.bottom);return insets;
        });
        androidx.core.view.WindowInsetsControllerCompat appearance=new androidx.core.view.WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView());
        appearance.setAppearanceLightStatusBars(true);appearance.setAppearanceLightNavigationBars(true);
        setContentView(scroll); BluetoothHandle.addPlayNotifyListener(this); query(); synchronize();
    }
    private void query() { BluetoothManager.getImpl().sendCmdSimpleObj(new RecordStatusRequestBean()); }
    private void synchronize() {
        if (!LoginManager.isLogon()) {
            message.setText("请配置可上网的网络后重新登录");
            retry.setText("前往登录"); retry.setOnClickListener(v -> MobileLoginActivity.start(this)); return;
        }
        retry.setEnabled(false); message.setText("正在同步今日账号、设备和套餐信息…");
        DailyAccessManager.synchronize(false, new IGenericApiCallback<DailyAccessManager.Profile>() {
            public void onSuccess(DailyAccessManager.Profile profile) { if (!isFinishing()) finish(); }
            public void onFail(String code, String text) { if (!isFinishing()) message.setText(text); }
            public void onFinish() { if (!isFinishing()) retry.setEnabled(true); }
        });
    }
    @Override public void onResume() { super.onResume(); if (DailyAccessManager.ready()) finish();else {handler.removeCallbacks(statusPoll);handler.post(statusPoll);} }
    @Override public void onPause(){handler.removeCallbacks(statusPoll);super.onPause();}
    @Override public void onBackPressed() { finishAffinity(); }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null);BluetoothHandle.removePlayNotifyListener(this); super.onDestroy(); }
    public void onRecordVideoStart() { query(); }
    public void onRecordVideoStop() { query(); }
    public void onRecordException(String text) { runOnUiThread(() -> { stopping=false; stop.setEnabled(true); message.setText(text); }); }
    public void onRecordVideoStatusUpdate(RecordStatusResultBean status) {
        runOnUiThread(() -> {
            if (!"ok".equalsIgnoreCase(status.getR())) return;
            if (status.getStatus() > 0) {
                current = status; stop.setVisibility(View.VISIBLE);
                
            } else if (stopping && "idle".equals(status.deviceState)) {
                if(current!=null&&status.recordingProtocolVersion<5)LocalRecordingReports.enqueue(current.competitionId,current.sessionId,status.deviceVersion,false);
                com.xh.hotme.record.OfflineRecordingReports.observe(status);
                stopping=false; current=null; stop.setVisibility(View.GONE); message.setText("录制已停止。"+DailyAccessManager.NETWORK_MESSAGE);
            }
        });
    }
}
