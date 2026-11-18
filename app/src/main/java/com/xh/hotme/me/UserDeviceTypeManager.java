package com.xh.hotme.me;

import android.util.Log;

import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.me.setmeal.utils.StoragePurchasePolicy;
import java.util.List;
public class UserDeviceTypeManager {
    private static String deviceType;

    private static String deviceID;

    //    目前是云存储的升级
    private static String lastOrderID;
    private static double userSaveCount;
    private static UserPackageCountBean.UserPackageData storagePackage;
    private static String storageScope;

    private static UserDeviceTypeManager instance;

    private UserDeviceTypeManager() {}

    public static UserDeviceTypeManager getInstance() {
        if (instance == null) {
            synchronized (UserDeviceTypeManager.class) {
                if (instance == null) {
                    instance = new UserDeviceTypeManager();
                }
            }
        }
        return instance;
    }

    public void setDeviceType(String type) {
        deviceType = type;
    }

    public String getDeviceType() {
        List<BindDeviceBean> bindDeviceList = DeviceManager.getBindDeviceList();

        String deviceType = "";


        for(BindDeviceBean deviceBean : bindDeviceList) {
            if(deviceBean.getDeviceId().equals(CameraManager.getDeviceIdLast())) {
                deviceType = deviceBean.getDeviceType();
            }
        }
        return deviceType;
//        return "6";
    }


    public void setLastOrderID(String id) {
        lastOrderID = id;
    }

    public String getLastOrderID() {
        if (!currentStorageScope().equals(storageScope)
                || StoragePurchasePolicy.upgradeOrderId(storagePackage, System.currentTimeMillis()).isEmpty()) return "";
        return lastOrderID;
    }

    private String currentStorageScope() {
        return CameraManager.getDeviceIdLast() + "|" + com.xh.hotme.environment.RuntimeEnvironment.cacheScope();
    }

    public void setStoragePackage(UserPackageCountBean.UserPackageData storage) {
        storageScope = currentStorageScope();
        if (storage != null && storage.deviceId != null && !storage.deviceId.equals(CameraManager.getDeviceIdLast())) {
            storage = null;
        }
        storagePackage = storage;
        userSaveCount = storage == null ? 0 : storage.totalCount;
        lastOrderID = StoragePurchasePolicy.upgradeOrderId(storage, System.currentTimeMillis());
    }

    public void setDeviceID(String id) {
        deviceID = id;
    }

    public String getDeviceID() {
        deviceID = CameraManager.getDeviceIdLast();
        return deviceID;
//        return "zzg20230ebe2552c8a";
    }


    public void setUserSaveCount(double count) {
        userSaveCount = count;
    }

    public double getUserSaveCount() {
        if (!currentStorageScope().equals(storageScope)
                || !StoragePurchasePolicy.hasActiveStorage(storagePackage, System.currentTimeMillis())) return 0;
        return userSaveCount;
    }
}
