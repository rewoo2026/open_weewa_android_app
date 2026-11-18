package com.xh.hotme.me.setmeal.utils;

import static com.xh.hotme.me.setmeal.utils.SetMealPayUtils.SERMEAL_DEV;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.live.LiveActivity;
import com.xh.hotme.live.bean.CheckSetMealBean;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.me.setmeal.dialog.ExpiredDialog;
import com.xh.hotme.me.setmeal.dialog.ExpiredServiceDialog;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;

import java.util.HashMap;

import okhttp3.FormBody;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.utils.ToastUtil;

import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/3/21
 * @Description: 进入应用提示用户套餐过期
 */
public class SetMealCheckManager {

    // 直播页面使用案例
//    SetMealCheckManager.checkSetMeal(requireContext(), new SetMealCheckManager.CheckSetMealCallback() {
//        @Override
//        public void onSuccess() {
//            Log.d("asx","asx+验证通过，进入直播");
//            startActivity(new Intent(getActivity(), LiveActivity.class));
//        }
//    },1);

    public interface CheckSetMealCallback {
        void onSuccess();
        default void onfail() {
            // 默认空实现
        }
    }

//    新的两个弹窗
    private static void showUserService(UserPackageCountBean data,Context context, CheckSetMealCallback callback,Integer checkType) {
//        checkType 0默认两个都检测 1检测直播 2检测录制 3检测带开关的录制

        // 解析云存储和直播可用量
        double storageRemain = 0;
        double liveRemain = 0;
        UserDeviceTypeManager.getInstance().setStoragePackage(null);

//        showStorageDialog(context);


        if (data != null && data.data != null && data.data.size() > 0) {
            List<UserPackageCountBean.UserPackageData> children = data.data.get(0).children;
            if (children != null) {
                for (UserPackageCountBean.UserPackageData info : children) {
                    if (info.countType == 2) { // 云存储
                        storageRemain = info.remainCount;
                        UserDeviceTypeManager.getInstance().setStoragePackage(info);
                    } else if (info.countType == 3) { // 直播
                        liveRemain = info.remainCount;
                    }
                }
            }
        }

        if (checkType == 0) {
            // 按顺序检查：先检查直播，再检查云存储
            if (liveRemain <= 0) {
                showLiveDialog(context);
                if (callback != null) {
                    callback.onfail();
                }
                return;
            }
            if (storageRemain <= 0) {
                showStorageDialog(context);
                if (callback != null) {
                    callback.onfail();
                }
                return;
            }
        } else if (checkType == 1 && liveRemain <= 0) {
            showLiveDialog(context);
            if (callback != null) {
                callback.onfail();
            }
            return;
        } else if (checkType == 2 && storageRemain <= 0) {
            showStorageDialog(context);
            if (callback != null) {
                callback.onfail();
            }
            return;
        } else if (checkType == 3 && storageRemain <= 0) {
            showStorageDialogWithSwith(context);
            if (callback != null) {
                callback.onfail();
            }
            return;
        }

        // 如果检查通过，执行回调
        if (callback != null) {
            callback.onSuccess();
        }

    }

    private static void showLiveDialog(Context context) {
        if (DeviceManager.getBindDeviceList().isEmpty()) {
            return;
        }
        ExpiredDialog expiredDialog = new ExpiredDialog(context);
        expiredDialog.setDialogText(context.getString(R.string.live_count_exhausted), context.getString(R.string.live_count_exhausted_desc)
                , context.getString(R.string.right_now_purchase), 1, 1);
        expiredDialog.showOnCenter(0);
    }

    private static void showStorageDialog(Context context) {
        if (DeviceManager.getBindDeviceList().isEmpty()) {
            return;
        }
        ExpiredDialog expiredDialog = new ExpiredDialog(context);
        expiredDialog.setDialogText(context.getString(R.string.cloud_storage_insufficient), context.getString(R.string.cloud_storage_insufficient_desc)
                , context.getString(R.string.right_now_purchase), 1, 2);
        expiredDialog.showOnCenter(0);
    }

    private static void showStorageDialogWithSwith(Context context) {
        if (DeviceManager.getBindDeviceList().isEmpty()) {
            return;
        }
        ExpiredDialog expiredDialog = new ExpiredDialog(context);
        expiredDialog.setDialogTextForSwithStrong(context.getString(R.string.cloud_storage_insufficient), context.getString(R.string.cloud_storage_insufficient_autoupload)
                , context.getString(R.string.right_now_purchase), context.getString(R.string.close_auto_upload), 1);
        expiredDialog.showOnCenter(0);
    }

    private static void checkUserSetMeal(Context context, CheckSetMealCallback callback,Integer checkType) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("deviceId", UserDeviceTypeManager.getInstance().getDeviceID());
        OkHttpUtil.postFormData(SdkApi.package_query_all_v2(), builder, null,
                new OkHttpCallbackDecode<UserPackageCountBean>(UserPackageCountBean.class) {
                    @Override
                    public void onDataSuccess(UserPackageCountBean data) {

                        MainHandler.runOnUiThread(() -> {
                            try {
                                DialogUtil.dismissDialog();
                                showUserService(data,context,callback,checkType);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        });

                    }

                    @Override
                    public void onFailure(String code, String message) {
                        MainHandler.runOnUiThread(() -> {
                            DialogUtil.dismissDialog();

                        });
                    }

                    @Override
                    public void onFinish() {
                        MainHandler.runOnUiThread(() -> {
                            DialogUtil.dismissDialog();
                        });
                    }
                });
    }


    //进入应用提示用户套餐过期

    public static void checkSetMeal(Context context) {
        checkSetMeal(context, null);
    }
    public static void checkSetMeal(Context context, CheckSetMealCallback callback) {
        checkSetMeal(context, callback, 0);
    }
    public static void checkSetMeal(Context context, CheckSetMealCallback callback, Integer checkType) {

        // 专门检测某项 0 按顺序 1 直播 2云存储

        if(SERMEAL_DEV) {
            return;
        }

        // 这个好像没必要？
        if (DeviceManager.getBindDeviceList().isEmpty()) {
            return;
        }


        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
        if (DeviceQuotaPolicy.isFullyExempt(deviceType)) {
            if (callback != null) callback.onSuccess();
            return;
        }
        if (DeviceQuotaPolicy.usesV2(deviceType)) {
            checkUserSetMeal(context,callback,checkType);
        }else{
            if (callback != null) {
                callback.onSuccess();
            }else{
                FormBody.Builder builder = new FormBody.Builder();
                builder.add("deviceId", CameraManager.getDeviceIdLast());
                OkHttpUtil.postFormData(SdkApi.package_packagecount_ountStatus(), builder, null,
                        new OkHttpCallbackDecode<CheckSetMealBean>(CheckSetMealBean.class) {
                            @Override
                            public void onDataSuccess(CheckSetMealBean data) {

                                MainHandler.runOnUiThread(() -> {
                                    try {
                                        String time = data.data.expireTime;

                                        String videoCount = "";
                                        String storeCount = "";
                                        String liveCount = "";
                                        String bigDataCount = "";

                                        if(data.data.videoCount == null) {
                                            videoCount = context.getString(R.string.unlimited_time);
                                        } else {
                                            videoCount = data.data.videoCount + context.getString(R.string.session);
                                        }

                                        if(data.data.storeCount == null) {
                                            storeCount =context.getString(R.string.unlimited_time);
                                        } else {
                                            storeCount = data.data.storeCount + context.getString(R.string.session);
                                        }

                                        if(data.data.liveCount == null) {
                                            liveCount = context.getString(R.string.unlimited_time);
                                        } else {
                                            liveCount = data.data.liveCount + context.getString(R.string.session);
                                        }

                                        if(data.data.bigDataCount == null) {
                                            bigDataCount = context.getString(R.string.unlimited_time);
                                        } else {
                                            bigDataCount = data.data.bigDataCount + context.getString(R.string.session);
                                        }


                                        if(data.data.code.equals("1")) {

                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.setText(context.getString(R.string.expired_annual_vip),
                                                    time, context.getString(R.string.right_now_purchase), 1);
                                            expiredDialog.showOnCenter(0);

                                        } else if(data.data.code.equals("2")) {
                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.setText(context.getString(R.string.expired_annual_vip),
                                                    time, context.getString(R.string.upgrade), 2);
                                            expiredDialog.showOnCenter(0);
                                        } else if(data.data.code.equals("3")) {
                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.setText(context.getString(R.string.expired_annual_vip),
                                                    time, context.getString(R.string.right_now_purchase), 1);
                                            expiredDialog.showOnCenter(0);
                                        } else if(data.data.code.equals("4")) {
                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.setText(context.getString(R.string.info_uses_exhausted_package), context.getString(R.string.info_purchase_meal)
                                                    , context.getString(R.string.right_now_purchase), 1);
                                            expiredDialog.showOnCenter(0);
                                        } else if(data.data.code.equals("5")) {
                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.setText(context.getString(R.string.info_uses_exhausted_package), context.getString(R.string.info_purchase_meal)
                                                    , context.getString(R.string.upgrade), 2);
                                            expiredDialog.showOnCenter(0);
                                        } else if(data.data.code.equals("6")) {
                                            ExpiredDialog expiredDialog = new ExpiredDialog(context);
                                            expiredDialog.showOnCenter(0);
                                            expiredDialog.setText(context.getString(R.string.info_uses_exhausted_package), context.getString(R.string.info_purchase_meal)
                                                    , context.getString(R.string.right_now_purchase), 1);
                                        } else if(data.data.code.equals("7")) {
                                            ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
                                            expiredDialog.showOnCenter(0);
                                            expiredDialog.setText(context.getString(R.string.rewoo_gift), videoCount,
                                                    storeCount, liveCount, context.getString(R.string.right_now_purchase));
                                        } else if(data.data.code.equals("8")) {
                                            ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
                                            expiredDialog.setText(context.getString(R.string.base_service), videoCount,
                                                    storeCount, liveCount, context.getString(R.string.upgrade));
                                            expiredDialog.showOnCenter(0);
                                        } else if(data.data.code.equals("9")) {
                                            ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
                                            expiredDialog.setText(context.getString(R.string.pro_service), videoCount,
                                                    storeCount, liveCount, context.getString(R.string.vip_renew));
                                            expiredDialog.showOnCenter(0);
                                        }
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String code, String message) {
                                MainHandler.runOnUiThread(() -> {
//                            DialogUtil.dismissDialog();
                                });
                            }

                            @Override
                            public void onFinish() {
                            }
                        });
            }

        }

    }

    // 专门检测不同相机的云存储
    public  static  void checkByDeviceId(Context context, CheckSetMealCallback callback, String deviceId) {
        String deviceType = "";
        List<BindDeviceBean> bindDeviceList = DeviceManager.getBindDeviceList();
        for(BindDeviceBean deviceBean : bindDeviceList) {
            if (deviceBean.getDeviceId().equals(deviceId)) {
                deviceType = deviceBean.getDeviceType();
            }
        }
//        deviceType = "";
        if (deviceType.equals("")) {
            ToastUtil.s(context.getString(R.string.device_invalid));
        }

        if (DeviceQuotaPolicy.usesV2(deviceType)) {
            FormBody.Builder builder = new FormBody.Builder();
            builder.add("deviceId", deviceId);
            OkHttpUtil.postFormData(SdkApi.package_query_all_v2(), builder, null,
                    new OkHttpCallbackDecode<UserPackageCountBean>(UserPackageCountBean.class) {
                        @Override
                        public void onDataSuccess(UserPackageCountBean data) {

                            MainHandler.runOnUiThread(() -> {
                                try {
                                    DialogUtil.dismissDialog();
                                    showUserDeviceIdService(data,context,callback);
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                DialogUtil.dismissDialog();

                            });
                        }

                        @Override
                        public void onFinish() {
                            MainHandler.runOnUiThread(() -> {
                                DialogUtil.dismissDialog();
                            });
                        }
                    });

        }else{
            if (callback != null) {
                callback.onSuccess();
            }
        }

    }

    private static void showUserDeviceIdService(UserPackageCountBean data,Context context, CheckSetMealCallback callback) {

        // 解析云存储和直播可用量
        double storageRemain = 0;

        if (data != null && data.data != null && data.data.size() > 0) {
            List<UserPackageCountBean.UserPackageData> children = data.data.get(0).children;
            if (children != null) {
                for (UserPackageCountBean.UserPackageData info : children) {
                    if (info.countType == 2) { // 云存储
                        storageRemain = info.remainCount;
                    }
                }
            }
        }

        if (storageRemain <= 0) {
            ToastUtil.s(context.getString(R.string.upload_Choose_device_cloud_storage_insufficient));
            return;
        }


        // 如果检查通过，执行回调
        if (callback != null) {
            callback.onSuccess();
        }

    }

}
