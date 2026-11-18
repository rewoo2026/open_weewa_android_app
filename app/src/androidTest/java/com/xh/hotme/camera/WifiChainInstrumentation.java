package com.xh.hotme.camera;

import android.app.*;
import android.content.*;
import android.os.*;
import com.xh.hotme.R;
import com.xh.hotme.bluetooth.*;
import com.xh.hotme.camera.wifi.*;
import com.xh.hotme.utils.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the production Wi-Fi add dialog. Credentials are consumed from a temporary private app file. */
public class WifiChainInstrumentation extends Instrumentation {
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();int code=Activity.RESULT_OK;WifiSettingActivity page=null;WifiAddDialog[] dialog=new WifiAddDialog[1];
        try {
            try(ParcelFileDescriptor p=getUiAutomation().executeShellCommand("am start -n com.xh.hotme/.main.MainActivity");FileInputStream in=new FileInputStream(p.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}
            Thread.sleep(2500);Constants.ACCESS_MODEL=Constants.ACCESS_MODEL_LOCAL;
            runOnMainSync(()->{CameraManager.newCamera("3A:A3:0A:04:00:A5");BluetoothManager.getImpl().connectToBle(new Device("3A:A3:0A:04:00:A5",0,"weewa520"));});
            long end=System.currentTimeMillis()+30000;while(!BluetoothManager.getImpl().isConnected()&&System.currentTimeMillis()<end)Thread.sleep(300);
            if(!BluetoothManager.getImpl().isConnected())throw new AssertionError("Bluetooth did not connect");Thread.sleep(3000);
            File credential=new File(getTargetContext().getFilesDir(),"analysis-wifi.private");
            String raw=new String(java.nio.file.Files.readAllBytes(credential.toPath()),java.nio.charset.StandardCharsets.UTF_8);credential.delete();
            com.google.gson.JsonObject config=new com.google.gson.Gson().fromJson(raw,com.google.gson.JsonObject.class);
            String ssid=config.get("ssid").getAsString(),password=config.get("password").getAsString();
            Intent intent=new Intent(getTargetContext(),WifiSettingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Constants.REQUEST_DEVICE,GsonUtils.GsonString(BluetoothManager.getConnectDevice()));
            intent.putExtra(Constants.REQUEST_TYPE,WifiSettingActivity.REQUEST_NETWORK_CONFIG);
            page=(WifiSettingActivity)startActivitySync(intent);final WifiSettingActivity host=page;
            AtomicReference<Boolean> success=new AtomicReference<>();
            runOnMainSync(()->{dialog[0]=new WifiAddDialog(host);dialog[0].setCallback(new WifiAddDialog.ICallback(){public void onCancel(){}public void onConfirm(String s,String p){success.set(true);dialog[0].dismiss();}});dialog[0].show();dialog[0].setDefaultSsidAndPassword(ssid,password);});
            Thread.sleep(3000);runOnMainSync(()->dialog[0].findViewById(R.id.tv_refresh).performClick());
            end=System.currentTimeMillis()+90000;while(success.get()==null&&System.currentTimeMillis()<end)Thread.sleep(500);
            if(!Boolean.TRUE.equals(success.get()))throw new AssertionError("Wi-Fi dialog did not report a connected camera");
            result.putString("stream","\nPASS: production Android Wi-Fi dialog -> BLE configure -> camera connected; SSID="+ssid+"\n");
        }catch(Throwable e){code=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+e+"\n");}
        finally{final WifiSettingActivity host=page;runOnMainSync(()->{if(dialog[0]!=null&&dialog[0].isShowing())dialog[0].dismiss();if(host!=null)host.finish();});finish(code,result);}
    }
}
