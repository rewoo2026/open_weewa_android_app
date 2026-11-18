package com.xh.hotme.camera;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.event.DeviceOnlineEvent;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.NetUtil;

import org.greenrobot.eventbus.EventBus;

public class CameraOnlineThread extends Thread{
    private final static String TAG = "CameraOnlineThread";
    private Handler _handler;
    private Context mContext;

    private final int period = 60000;  //定时1分钟

    private boolean run;


    private final Runnable _onlineRunnable = new Runnable() {
        @Override
        public void run() {
            //
            if(run) {
                requestOnlineStatus();

                _handler.postDelayed(_onlineRunnable, period);
            }
        }
    };


    /**
     * 构造方法，初始化参数
     * @param mContext
     */
    public CameraOnlineThread(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * 启动线程
     */
    public void startThread() {
        AppTrace.d("startThread");
        if(run){
            AppTrace.d("startThread is running");
            return;
        }
        run = true;
        _handler  = new Handler();
        _handler.postDelayed(_onlineRunnable, period);
        AppTrace.d("PushThread run");
    }

    /**
     * 停止线程
     */
    public void stopThread() {
        AppTrace.d("stopThread");
        run = false;
        if(_handler != null) {
            _handler.removeCallbacks(_onlineRunnable);
        }
    }

    public boolean isRun(){
        return run;
    }

    private void requestOnlineStatus(){

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
            AppTrace.d(TAG, "requestOnlineStatus fail: only remote model support to request online status");
            return;
        }

        if(!NetUtil.isConnectedAvailableNetwork()){
            AppTrace.d(TAG, "requestOnlineStatus fail: the network is disconnect");
            return;
        }

        if(CameraManager.getCameraInfo("") == null){
            AppTrace.d(TAG, "requestOnlineStatus fail: the device is disconnect");
            return;
        }

        String deviceId = CameraManager.getDeviceId();
        if(!TextUtils.isEmpty(deviceId)) {
            ApiUtil.getDeviceInfo(deviceId, new IGenericApiCallback<BindDeviceBean>() {
                @Override
                public void onSuccess(BindDeviceBean data) {
                    if(data != null){
                        boolean isOnline = data.getOnlineStatus() == 1 ? true: false;

                        EventBus.getDefault().post(new DeviceOnlineEvent(data.getDeviceId(), isOnline));
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "requestOnlineStatus fail: code=" + code + ", message=" + message);
                }

                @Override
                public void onFinish() {

                }
            });
        }
    }
}
