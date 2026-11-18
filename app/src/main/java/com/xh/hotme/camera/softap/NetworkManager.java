package com.xh.hotme.camera.softap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.weewa.lib.WeewaLib;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.account.CameraTokenManager;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.NetUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * desc   :
 */
public class NetworkManager implements IBleSoftApNotifyListener {
    private final WifiManager _sysWifiManager;

    public static final int NETWORK_CONNECT = 1;
    public static final int STATUS_NO_NETWORK = 0;
    public static final int STATUS_WIFI = 1;
    public static final int STATUS_MOBILE = 2;
    private static final String TAG = "NetworkManager";
    private ConnectivityManager mConnectivityManager;
    private final CopyOnWriteArrayList<OnNetworkStatusChangedListener> mListeners = new CopyOnWriteArrayList<>();

    private String _softap_ssid = com.xh.hotme.camera.softap.WifiManager.WEEWA_SOFTAP_NAME;
    private String _softap_password = com.xh.hotme.camera.softap.WifiManager.WEEWA_SOFTAP_PWD;
    private int _softap_status;
    private boolean _deviceSoftApLaunched = false;

    public interface OnNetworkStatusChangedListener {
        void onStatusChanged(int newStatus, int oldStatus);
    }

    private NetworkManager() {
        if(mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) HotmeApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        }
        mNetStatus = populateNetworkStatus();
        _sysWifiManager = (WifiManager) HotmeApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        Log.i(TAG, "registerNetworkStatusChangeListener start");
        try {
            HotmeApplication.getContext().registerReceiver(mReceiver, filter);
        } catch(Exception e) {
            unregisterReceivers(HotmeApplication.getContext());
            try {
                HotmeApplication.getContext().registerReceiver(mReceiver, filter);
            } catch(Exception e1) {

            }
        }

        BluetoothHandle.addSoftApNotifyListener(this);
        CameraTokenManager.onSoftApConnectionChanged(isConnectedSoftAp());
    }

    public boolean isConnectedSoftAp() {
        String wifiSsid = com.xh.hotme.camera.softap.WifiManager.getImpl().getConnectWifiSsid();
        AppTrace.d(TAG, "当前wifi:" + wifiSsid + ",,," + _softap_ssid);
        if(!TextUtils.isEmpty(wifiSsid) && !TextUtils.isEmpty(_softap_ssid) && wifiSsid.startsWith(_softap_ssid)) {
            if(!WeewaLib.Companion.shared().isOnline(BleConstants.SOFT_AP_IP)) {
                AppTrace.d(TAG, "file server is not online, relaunch it");
//                WeewaLib.Companion.shared().relaunch();
            }

            String localIp = NetUtil.getLocalIpAddress();
            if(localIp.startsWith("10.201.126")){
                return true;
            }
            return !localIp.equals("0.0.0.0");
        }
        return false;
    }

    public boolean isConnectedWeewa() {
        String wifiSsid = com.xh.hotme.camera.softap.WifiManager.getImpl().getConnectWifiSsid();
        AppTrace.d(TAG, "当前wifi:" + wifiSsid + ",,," + _softap_ssid);
        if(!TextUtils.isEmpty(wifiSsid) && (wifiSsid.startsWith("weewa_") || wifiSsid.startsWith("W_") || wifiSsid.startsWith("w_"))) {
           return true;
        }
        return false;
    }

    public void updateNetworkStatus() {
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
    }

    public void updateWithNetworkInfo(NetworkBean networkInfo) {
        _softap_ssid = networkInfo.getAp().ssid;
        _softap_password = networkInfo.getAp().password;
        _softap_status = networkInfo.getAp().st;
        _deviceSoftApLaunched = _softap_status == 1;
        CameraTokenManager.onSoftApConnectionChanged(isConnectedSoftAp());
    }

    public String getSoftApSsid() {
        return _softap_ssid;
    }

    public String getSoftApPassword() {
        return _softap_password;
    }

    public int getSoftApStatus() {
        return _softap_status;
    }

    public boolean isDeviceSoftApLaunched() {
        return _deviceSoftApLaunched;
    }

    public void registerNetworkStatusChangeListener(OnNetworkStatusChangedListener listener, boolean isNotHasNetwork) {
        if(isNotHasNetwork) {
            mNetStatus = STATUS_NO_NETWORK;
        }
        if(!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeNetworkStatusChangeListener(Context context, OnNetworkStatusChangedListener listener) {
        Log.i(TAG, "removeNetworkStatusChangeListener------>listener = " + listener);
        if(listener != null) {
            mListeners.remove(listener);

            if(mListeners.isEmpty() && context != null) {
                unregisterReceivers(context);
            }
        }
    }

    public void unregisterReceivers(Context context) {
        try {
            context.unregisterReceiver(mReceiver);
        } catch(Exception e) {

        }
    }

    public int getNetworkState() {
        return mNetStatus;
    }

    public boolean isNotHasNetwork() {
        return mNetStatus == STATUS_NO_NETWORK;
    }

    private volatile int mNetStatus = STATUS_NO_NETWORK;

    private static final String WIFISSID_UNKNOWN = "<unknown ssid>";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive---action = " + action);
            int status = mNetStatus;
            if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(networkInfo != null && networkInfo.isConnected()) {
                    // 获取当前连接的 WiFi 信息
                    WifiInfo wifiInfo = _sysWifiManager.getConnectionInfo();
                    String ssid = wifiInfo.getSSID();
                    AppTrace.d(TAG, "WifiReceiver onReceive1 ssid : " + ssid);
                    // 去除 SSID 前后的引号（如果有的话）
                    if(ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    // 如果获取到的还是 空或者 <unknown ssid>，则遍历 wifi 列表来获取
                    if(TextUtils.isEmpty(ssid) || WIFISSID_UNKNOWN.equalsIgnoreCase(ssid.trim())) {
                        ssid = com.xh.hotme.camera.softap.WifiManager.getSSIDByNetworkId();
                    }
                    AppTrace.d(TAG, "WifiReceiver onReceive2 ssid : " + ssid);
                    if(!TextUtils.isEmpty(_softap_ssid) && TextUtils.equals(ssid, _softap_ssid)) {
                        AppTrace.d(TAG, "WifiReceiver postDelayed getVideoList");
                        EventBus.getDefault().post(new NetWorkForQEvent(NetWorkForQEvent.AVAILABLE));
                    }
                }
//                else {
//
//                    if (mListeners.size() > 0) {
//                        for (int i = 0/*, size = mListeners.size()*/; i < mListeners.size(); i++) {
//                            OnNetworkStatusChangedListener onNetworkStatusChangedListener = mListeners.get(i);
//                            Log.i(TAG, "onReceive---" + onNetworkStatusChangedListener);
//                            if (onNetworkStatusChangedListener == null) {
//                                continue;
//                            }
//                            onNetworkStatusChangedListener.onStatusChanged(wifiState, wifiState);
//                        }
//                    }
//                }

            } else if((ConnectivityManager.CONNECTIVITY_ACTION.equals(action))) {
                AppTrace.d(TAG, "CONNECTIVITY_ACTION");
                String ssid = com.xh.hotme.camera.softap.WifiManager.getImpl().getConnectWifiSsid();
                if(!TextUtils.isEmpty(_softap_ssid) && TextUtils.equals(ssid, _softap_ssid)) {
                    AppTrace.d(TAG, "WifiReceiver softap connected ");
                    EventBus.getDefault().post(new NetWorkForQEvent(NetWorkForQEvent.AVAILABLE));
                } else {
                    AppTrace.d(TAG, "Wifi is not soft ap : " + _softap_ssid);
                }

                if(!mListeners.isEmpty()) {
                    for(int i = 0/*, size = mListeners.size()*/; i < mListeners.size(); i++) {
                        OnNetworkStatusChangedListener onNetworkStatusChangedListener = mListeners.get(i);
                        Log.i(TAG, "onReceive---" + onNetworkStatusChangedListener);
                        if(onNetworkStatusChangedListener == null) {
                            continue;
                        }
                        onNetworkStatusChangedListener.onStatusChanged(mNetStatus, status);
                    }
                }
            }
            CameraTokenManager.onSoftApConnectionChanged(isConnectedSoftAp());
        }
    };

    private static final class HolderClass {
        private static final NetworkManager INSTANCE = new NetworkManager();
    }

    public static NetworkManager getImpl() {
        return HolderClass.INSTANCE;
    }

    private int populateNetworkStatus() {
        NetworkInfo info = null;
        if(mConnectivityManager != null) {
            info = mConnectivityManager.getActiveNetworkInfo();
        }
        if(info != null && info.isConnected()) {
            int type = info.getType();
            Log.i(TAG, "populateNetworkStatus------->type = " + type);
            switch(type) {
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                case ConnectivityManager.TYPE_MOBILE_HIPRI:
                case ConnectivityManager.TYPE_MOBILE_MMS:
                case ConnectivityManager.TYPE_MOBILE_SUPL:
                    return STATUS_MOBILE;
                default:
                    return STATUS_WIFI;
            }
        }
        Log.i(TAG, "populateNetworkStatus------->no netWork!");
        return STATUS_NO_NETWORK;
    }

    private boolean isNetStatusChanged() {
        int status = populateNetworkStatus();
        Log.i(TAG, "isNetStatusChanged------->status = " + status + "--->mNetStatus = " + mNetStatus);
        if(mNetStatus != status) {
            mNetStatus = status;
            return true;
        }
        return false;
    }

    private void updateSoftApInfo() {
        NetworkBean networkInfo = CameraManager.getNetworkInfo(null);
        if(networkInfo != null && networkInfo.getAp() != null && !TextUtils.isEmpty(networkInfo.getAp().ssid)) {
            _softap_ssid = networkInfo.getAp().ssid;
            _softap_password = networkInfo.getAp().password;
            _softap_status = networkInfo.getAp().st;
            _deviceSoftApLaunched = _softap_status == 1;
        } else {
            _softap_ssid = "";
            _softap_password = "";
            _softap_status = 0;
            _deviceSoftApLaunched = false;
        }
    }

    @Override
    public void onSoftApStart(String ssid, String password) {
        updateSoftApInfo();
    }

    @Override
    public void onSoftApStartFail(String message) {

    }

    @Override
    public void onSoftApStop() {
        updateSoftApInfo();
    }

    @Override
    public void onSoftApStopFail(String message) {

    }

    @Override
    public void onSoftApStatus(int status) {
        updateSoftApInfo();
    }

    @Override
    public void onSoftApConnectStart() {

    }

    @Override
    public void onSoftApConnectSuccess() {

    }

    @Override
    public void onSoftApConnectFail(String message) {

    }
}
