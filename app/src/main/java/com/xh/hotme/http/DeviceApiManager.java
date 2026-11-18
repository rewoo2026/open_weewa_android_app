package com.xh.hotme.http;

import android.util.Log;

import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.bean.DeleteWatermarkRequestBean;
import com.xh.hotme.bluetooth.listener.IBleVideoListNotifyListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.rabbitmq.RabbitMQ;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.NetUtil;

import java.util.ArrayList;

public class DeviceApiManager {
    private static final String TAG = "DeviceApiManager";

    public static boolean useBluetooth = true;
    public final static int api_auto = -1;
    public final static int api_bluetooth = 0;
    public final static int api_soft_ap = 1;

    private static boolean useWifiNetwork() {

        return !getApiType();
    }

    public static void setApiType(boolean isBluetooth) {
        useBluetooth = isBluetooth;
    }

    public static boolean getApiType() {
        return useBluetooth;
    }

    private static boolean isConnectedSoftAp() {
        return NetworkManager.getImpl().isConnectedSoftAp();
    }

    private static boolean isConnectedBluetooth() {
        return BluetoothManager.getImpl().isConnected();
    }

    private static boolean isConnectedNetwork() {
        return NetUtil.isConnectedAvailableNetwork();
    }

    public static boolean isConnected() {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (isConnectedSoftAp()) {
                return true;
            } else {
                return BluetoothManager.getImpl().isConnected();
            }
        } else {
            boolean hasNetwork = isConnectedNetwork();
            if(!hasNetwork){
                return false;
            }
            return CameraMonitor.getImpl().isOnline();
        }
    }

    public static boolean sendCmdPowerOn() {
        AppTrace.t(TAG, "sendCmdPowerOn ...");
        if(useWifiNetwork()) {
            return false;
        } else {
            return BluetoothManager.getImpl().sendCmdPowerOn();
        }
    }


    public static boolean sendCmdSetWatermark(String b64Bmp) {
        // 这个api只能通过http访问
        AppTrace.d(TAG, "sendCmdSetWatermark ...");
        if(!isConnectedSoftAp()) {
            AppTrace.d(TAG, "wifi disconnected");
            return false;
        }
        return DeviceHttpManager.sendCmdSetWatermark(b64Bmp);
    }

    public static boolean sendCmdDeleteWatermark() {
        AppTrace.d(TAG, "sendCmdDeleteWatermark ...");
        if(useWifiNetwork()) {
            if(!isConnectedSoftAp()) {
                AppTrace.d(TAG, "wifi disconnected");
                return false;
            }
            return DeviceHttpManager.sendCmdDeleteWatermark();
        } else {
            return BluetoothManager.getImpl().sendCmdSimpleObj(new DeleteWatermarkRequestBean());
        }
    }


    public static boolean sendCmdVideoList(int apiType, IBleVideoListNotifyListener listener) {
        AppTrace.d(TAG, "sendCmdVideoList ...");
        if(apiType == -1) {
            apiType = !isConnectedSoftAp() ? api_bluetooth : api_soft_ap;
        }
        if(apiType == api_soft_ap) {
            if(!isConnectedSoftAp()) {
                AppTrace.d(TAG, "wifi disconnected");
                return false;
            }
            return DeviceHttpManager.sendCmdVideoList(listener);
        } else if(apiType == api_bluetooth) {
            return BluetoothManager.getImpl().sendCmdVideoList();
        } else {
            AppTrace.d(TAG, "api type is unknown");
            return false;
        }
    }

    public static boolean sendCmdSimpleObjBySoftAp(BaseBluetoothBean baseBluetoothBean) {
        AppTrace.t(TAG, "sendCmdSimpleObjBySoftAp:" + baseBluetoothBean.c);
        return DeviceHttpManager.sendCmdSimpleObj(baseBluetoothBean);
    }


    public static boolean sendCmdSimpleObj(BaseBluetoothBean baseBluetoothBean) {
        AppTrace.t(TAG, "sendCmdSimpleObj:" + baseBluetoothBean.c);
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            return BluetoothManager.getImpl().sendCmdSimpleObj(baseBluetoothBean);
        } else {
            if(!isConnectedNetwork()){
                AppTrace.t(TAG, "sendCmdSimpleObj fail: network disconnect");
                return false;
            }
            RabbitMQ.Companion.getImpl().sendCmdSimpleObj(CameraManager.getDeviceId(), baseBluetoothBean);
            return true;
        }
    }

    public static boolean sendCmdSimpleObj(String deviceId, BaseBluetoothBean baseBluetoothBean) {
        AppTrace.t(TAG, "sendCmdSimpleObj ...");
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            return BluetoothManager.getImpl().sendCmdSimpleObj(baseBluetoothBean);
        } else {
            if(!isConnectedNetwork()){
                AppTrace.t(TAG, "sendCmdSimpleObj fail: network disconnect");
                return false;
            }
            RabbitMQ.Companion.getImpl().sendCmdSimpleObj(deviceId, baseBluetoothBean);
            return true;
        }
    }

    //释放连接信息
    public static void disConnect(){
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            //先释放连接信息
            CameraManager.release();

            if(BluetoothManager.getImpl() != null) {
                BluetoothManager.getImpl().disconnectBle();
            }
        } else {
            //远程模式下，释放连接信息
            CameraManager.release();
        }
    }
}
