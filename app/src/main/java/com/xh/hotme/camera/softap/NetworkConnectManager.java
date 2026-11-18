package com.xh.hotme.camera.softap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;

//监听CONNECTIVITY_ACTION
public class NetworkConnectManager {


    public static final int NETWORK_CONNECT = 1;
    public static final int STATUS_NO_NETWORK = 0;
    public static final int STATUS_WIFI = 1;
    public static final int STATUS_MOBILE = 2;
    private static final String TAG = "NetworkConnectManager";
    private ConnectivityManager mConnectivityManager;
    private static volatile NetworkConnectManager manager;
    private final CopyOnWriteArrayList<OnNetworkStatusChangedListener> mListeners = new CopyOnWriteArrayList<>();

    public interface OnNetworkStatusChangedListener {
        void onStatusChanged(int newStatus, int oldStatus);
    }

    private NetworkConnectManager() {


    }

    public void init(Context context) {
        initNetStatus(context);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if(context != null) {
            Log.i(TAG, "registerNetworkStatusChangeListener start");
            try {

                context.registerReceiver(mReceiver, filter);
            } catch(Exception e) {
                unregisterReceivers(context);
                try {
                    context.registerReceiver(mReceiver, filter);
                } catch(Exception e1) {

                }

            }

        }

    }

    public void initNetStatus(Context context) {
        if(mConnectivityManager == null && context != null) {
            mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        }
        mNetStatus = populateNetworkStatus();
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
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive---action = " + action);
            int status = mNetStatus;
            if((ConnectivityManager.CONNECTIVITY_ACTION.equals(action) && isNetStatusChanged())) {
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
        }
    };

    public static NetworkConnectManager getInstance() {
        if(manager == null) {
            synchronized(NetworkConnectManager.class) {
                if(manager == null) {
                    manager = new NetworkConnectManager();
                }
            }
        }
        return manager;
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
}
