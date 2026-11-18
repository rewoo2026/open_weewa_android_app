package com.xh.hotme.account;

import android.bluetooth.BluetoothGatt;

import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.SetTokenRequestBean;
import com.xh.hotme.bluetooth.listener.IBleLoginTokenListener;
import com.xh.hotme.http.DeviceHttpManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.MainHandler;

/** Shares phone login credentials only with the currently connected local camera. */
public final class CameraTokenManager {
    private static final String TAG = "CameraTokenManager";
    private static final CameraTokenSynchronizer sync = new CameraTokenSynchronizer(
            new CameraTokenSynchronizer.Scheduler() {
                public void postDelayed(Runnable task, long delayMillis) {
                    MainHandler.getImpl().postDelayed(task, delayMillis);
                }
                public void remove(Runnable task) {
                    MainHandler.getImpl().removeCallbacks(task);
                }
            });
    private static BluetoothGatt bluetooth;
    private static CameraTokenSynchronizer.Callback bluetoothResponse;
    private static boolean softApConnected;
    private static Object currentConnection;

    private CameraTokenManager() { }

    public static void onLoginChanged() {
        MainHandler.runOnUiThread(() -> sync.setToken(LoginManager.getUserToken()));
    }

    public static void onBluetoothReady(BluetoothGatt gatt) {
        MainHandler.runOnUiThread(() -> {
            bluetooth = gatt;
            refreshConnection();
        });
    }

    public static void onBluetoothDisconnected(BluetoothGatt gatt) {
        MainHandler.runOnUiThread(() -> {
            if (bluetooth != gatt) return;
            bluetooth = null;
            bluetoothResponse = null;
            refreshConnection();
        });
    }

    public static void onSoftApConnectionChanged(boolean connected) {
        MainHandler.runOnUiThread(() -> {
            softApConnected = connected;
            refreshConnection();
        });
    }

    private static void refreshConnection() {
        Object next = bluetooth != null ? bluetooth : softApConnected ? "soft-ap" : null;
        if (currentConnection != null && currentConnection != next) {
            sync.disconnected(currentConnection);
        }
        currentConnection = next;
        // Disconnect the previous camera before applying a changed login token.
        sync.setToken(LoginManager.getUserToken());
        if (next == null) return;
        final BluetoothGatt gatt = bluetooth;
        sync.connected(next, (token, callback) -> {
            AppTrace.i(TAG, "set_token requested via " + (gatt != null ? "bluetooth" : "http"));
            if (gatt != null) {
                bluetoothResponse = callback;
                return BluetoothManager.getImpl().sendCmdSimpleObj(new SetTokenRequestBean(token));
            }
            return DeviceHttpManager.sendToken(token, success -> MainHandler.runOnUiThread(() -> {
                AppTrace.i(TAG, "set_token HTTP response success=" + success);
                callback.onResult(success);
            }));
        });
    }

    public static void onBluetoothTokenResponse(BluetoothGatt gatt, JSONObject response) {
        boolean success = BleConstants.BLE_RESULT_SUCCESS.equalsIgnoreCase(
                response.getString(BleConstants.BLE_KEY_RESULT));
        MainHandler.runOnUiThread(() -> {
            if (bluetooth != gatt || bluetoothResponse == null) return;
            CameraTokenSynchronizer.Callback callback = bluetoothResponse;
            bluetoothResponse = null;
            AppTrace.i(TAG, "set_token bluetooth response success=" + success);
            callback.onResult(success);
        });
    }

    public static void syncCurrentToken(IBleLoginTokenListener listener) {
        MainHandler.runOnUiThread(() -> {
            sync.setToken(LoginManager.getUserToken());
            sync.sync(success -> {
                if (success) listener.onSyncToken();
                else listener.onSyncTokenFail("token sync failed");
            });
        });
    }
}
