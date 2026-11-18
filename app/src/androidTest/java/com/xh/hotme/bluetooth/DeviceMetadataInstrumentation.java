package com.xh.hotme.bluetooth;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.bean.DeviceActiveSmsRequestBean;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleActiveSmsListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit live-camera acceptance; requires an already-bound same-account camera. */
public class DeviceMetadataInstrumentation extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments=args; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle(); int status=Activity.RESULT_OK;
        AtomicReference<DeviceInfo> info=new AtomicReference<>(), weewa=new AtomicReference<>();
        AtomicReference<String> activation=new AtomicReference<>();
        CountDownLatch activated=new CountDownLatch(1);
        IBleDeviceInfoNotifyListener deviceListener=new IBleDeviceInfoNotifyListener() {
            public void onDeviceInfo(DeviceInfo d) { if(d!=null) info.set(d); }
            public void onDeviceInfoFail() {}
        };
        IBleWeewaInfoNotifyListener weewaListener=new IBleWeewaInfoNotifyListener() {
            public void onWeewaInfo(WeewaInfo d) { if(d!=null) weewa.set(d.d); }
            public void onWeewaInfoFail() {}
        };
        IBleActiveSmsListener activeListener=new IBleActiveSmsListener() {
            public void onSendActiveSms(String code) { activation.set(code); activated.countDown(); }
            public void onSendActiveSmsFail(String msg) { activation.set("failed"); activated.countDown(); }
        };
        try {
            String address=arguments.getString("address");
            require(address!=null,"Camera address argument required");
            runOnMainSync(() -> {
                BluetoothHandle.addDeviceInfoNotifyListener(deviceListener);
                BluetoothHandle.addWeewaInfoNotifyListener(weewaListener);
                BluetoothHandle.addDeviceActiveSmsNotifyListener(activeListener);
                BluetoothManager.getImpl().connectToBle(new Device(address,0,"weewa520"));
            });
            for(int attempt=0;attempt<25 && info.get()==null;attempt++) {
                Thread.sleep(1000);
                runOnMainSync(() -> { BaseBluetoothBean b=new BaseBluetoothBean(); b.c=BleConstants.BLE_CMD_DEVICE_INFO; BluetoothManager.getImpl().sendCmdSimpleObj(b); });
            }
            validate(info.get());
            for(int attempt=0;attempt<15 && weewa.get()==null;attempt++) {
                runOnMainSync(() -> { BaseBluetoothBean b=new BaseBluetoothBean(); b.c=BleConstants.BLE_CMD_WEEWA_INFO; BluetoothManager.getImpl().sendCmdSimpleObj(b); });
                Thread.sleep(1000);
            }
            validate(weewa.get());
            DeviceInfo d=info.get();
            require(d.active_status==3 && d.account!=null && !d.account.isEmpty(),"Camera must already be bound to the verification account");
            runOnMainSync(() -> require(BluetoothManager.getImpl().sendCmdSimpleObj(new DeviceActiveSmsRequestBean(d.account,address)),"BLE activation request failed"));
            require(activated.await(20,TimeUnit.SECONDS),"Activation response timed out");
            require("2".equals(activation.get()),"Expected already-bound reply without sending SMS");
            result.putString("stream","\nOK: deviceinfo + weewa_info parsed WEEWA01 / 1.2.0 / 20261005; BLE activation reply=2\n");
            result.putInt("passed",3);
        } catch(Throwable e) {
            status=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+e.getClass().getSimpleName()+": "+e.getMessage()+"\n");
        } finally {
            runOnMainSync(() -> {
                BluetoothHandle.removeDeviceInfoNotifyListener(deviceListener);
                BluetoothHandle.removeWeewaInfoNotifyListener(weewaListener);
                BluetoothHandle.removeDeviceActiveSmsNotifyListener(activeListener);
                BluetoothManager.getImpl().disconnectBle();
            });
        }
        finish(status,result);
    }
    private static void validate(DeviceInfo d) {
        require(d!=null,"No camera device info received");
        require("zzg20230bde63f46f0".equals(d.device_id),"Unexpected camera");
        require("WEEWA01".equals(d.device_model),"Missing or incorrect model");
        require("1.2.0".equals(d.ota_version) && d.ota_version_code==20261005,"Incorrect software version");
    }
    private static void require(boolean ok,String reason) { if(!ok) throw new AssertionError(reason); }
}
