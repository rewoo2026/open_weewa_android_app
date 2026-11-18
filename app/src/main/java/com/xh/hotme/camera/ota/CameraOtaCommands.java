package com.xh.hotme.camera.ota;

import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.Constants;

public final class CameraOtaCommands {
    private CameraOtaCommands() {}

    public static boolean send(BaseBluetoothBean command) {
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL
                && NetworkManager.getImpl().isConnectedSoftAp()) {
            return DeviceApiManager.sendCmdSimpleObjBySoftAp(command);
        }
        return DeviceApiManager.sendCmdSimpleObj(command);
    }
}
