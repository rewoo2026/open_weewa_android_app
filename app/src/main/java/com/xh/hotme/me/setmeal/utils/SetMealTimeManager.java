package com.xh.hotme.me.setmeal.utils;

import static com.xh.hotme.me.setmeal.utils.SetMealPayUtils.SERMEAL_DEV;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.GetPlanUsageRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.event.GetPlanUsageEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsEvent;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.UserSetMealBean;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.me.setmeal.dialog.NotAvailableDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/3/6
 * @Description: 直播时间查询 上传
 */
public class SetMealTimeManager {
    private static final String TAG = SetMealTimeManager.class.getSimpleName();
    private static final Logger log = LoggerFactory.getLogger(SetMealTimeManager.class);

    private SetMealTimeManager() {
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public static SetMealTimeManager getInstance() {
        return SetMealTimeManager.SetMealTimeManagerHolder.instance;
    }

    private static class SetMealTimeManagerHolder {
        private final static SetMealTimeManager instance = new SetMealTimeManager();
    }

    //某一时刻只有一个listener
    private OnPlanUsageResultListener onPlanUsageResultListener;
    private Context context;
    private int countType;
    private String usageDeviceId, usageDeviceType;
    private String usageCacheKey, usageTimeKey;

    /**
     * 请求设备已使用获取时长 上传服务器校验套餐
     *
     * @param countType 1:录制；2:云存储；3:直播
     */
    public void sendCmdGetPlanUsage(int countType, Context context,
                                    OnPlanUsageResultListener onPlanUsageResultListener) {

        try {

            String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
            Log.d("asx","asx+" + deviceType);


            // Apply the camera's quota exemption before checking local recording allowance.
            if (!DeviceQuotaPolicy.requiresUsageUpload(deviceType, countType)) {
                onPlanUsageResultListener.onResult(true);
                return;
            }

            if (countType == 1 && Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                final DailyAccessManager.DeviceAccess access = DailyAccessManager.device(CameraManager.getDeviceIdLast());
                onPlanUsageResultListener.onResult(access != null && access.recordingAllowed && access.permit != null);
                return;
            }


            if(SERMEAL_DEV) {
                onPlanUsageResultListener.onResult(true);
                return;
            }

            this.countType = countType;
            this.usageDeviceId = CameraManager.getDeviceIdLast();
            this.usageDeviceType = deviceType;
            this.usageCacheKey = DeviceQuotaPolicy.cacheKey(usageDeviceId, countType);
            this.usageTimeKey = usageCacheKey + ":time";
            this.context = context;
            this.onPlanUsageResultListener = onPlanUsageResultListener;


            String planUsage = SharedPrefs.getImpl().getString(usageCacheKey, "");
            String planUsageTime = SharedPrefs.getImpl().getString(usageTimeKey, "");



            if(!TextUtils.isEmpty(planUsage) && !TextUtils.isEmpty(planUsageTime)) {
                GetPlanUsageEvent event = new Gson().fromJson(planUsage, GetPlanUsageEvent.class);
                // Flush the old receipt, then read usage accrued since it was cached.
                sendApi(planUsageTime, event, true);
            } else {
                sendCmd();
            }
        } catch(Exception e) {
            e.printStackTrace();
            sendCmd();
        }
    }

    private void sendCmd() {
        AppTrace.d(TAG, "检查使用时长");
        if(countType == 1) {
            DeviceApiManager. sendCmdSimpleObj(new GetPlanUsageRequestBean(GetPlanUsageRequestBean.FLAG_RECORD_ONLY));
        } else if(countType == 3) {
            DeviceApiManager. sendCmdSimpleObj(new GetPlanUsageRequestBean(GetPlanUsageRequestBean.FLAG_RTMP_ONLY));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getPlanUsageEvent(GetPlanUsageEvent data) {
        AppTrace.d(TAG, "结果返回 " + data.recordUsed + ",," + data.rtmpUsed);
        if(context == null) {
            DialogUtil.dismissDialog();
            return;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = simpleDateFormat.format(new Date(System.currentTimeMillis()));


        SharedPrefs.getImpl().putString(usageCacheKey, new Gson().toJson(data));
        SharedPrefs.getImpl().putString(usageTimeKey, time);


        sendApi(time, data, false);
    }

    private void sendApi(String time, GetPlanUsageEvent data, boolean refreshAfterUpload) {
        final Context requestContext = context;
        final OnPlanUsageResultListener requestListener = onPlanUsageResultListener;
        final String requestDeviceId = usageDeviceId;
        final String requestCacheKey = usageCacheKey;
        final String requestTimeKey = usageTimeKey;
        final String requestPayload = new Gson().toJson(data);
        int useNumber = 0;
        if(countType == 1) {
            useNumber = data.recordUsed;
        } else if(countType == 3) {
            useNumber = data.rtmpUsed;
        }

        useNumber = DeviceQuotaPolicy.billableMinutes(useNumber);
        DialogUtil.showDialog("");
        AppTrace.d(TAG, "上传使用时长 " + countType + ",," + useNumber + "分钟");
        submitUseNumberForDevice(context, usageDeviceId, usageDeviceType, countType, time,
            useNumber, (code, objects) -> {
                if(requestContext != null) {
                    MainHandler.runOnUiThread(() -> {
                        if(code == IActionCallback.CALLBACK_ACTION_OK) {
                            AppTrace.d(TAG, "还有次数 ");
                            // A later request may already have cached newer usage for this camera.
                            if (time.equals(SharedPrefs.getImpl().getString(requestTimeKey, ""))
                                    && requestPayload.equals(SharedPrefs.getImpl().getString(requestCacheKey, ""))) {
                                SharedPrefs.getImpl().putString(requestCacheKey, "");
                                SharedPrefs.getImpl().putString(requestTimeKey, "");
                            }
                            if (refreshAfterUpload) {
                                if (requestListener == onPlanUsageResultListener && requestDeviceId.equals(CameraManager.getDeviceIdLast())) sendCmd();
                                else requestListener.onResult(false);
                            } else requestListener.onResult(true);
                        } else {
//                            SharedPrefs.getImpl().putString(usageCacheKey, "");
//                            SharedPrefs.getImpl().putString(usageTimeKey, "");
                            requestListener.onResult(false);
                        }
                        DialogUtil.dismissDialog();
                    });

                }

            });
    }


    /**
     * 提交使用量
     *
     * @param countType 1:录制；2:云存储；3:直播
     */
    public static void submitUseNumber(Context context, int countType, String startTime,
                                       int useNumber, IActionCallback commonListener) {
        submitUseNumberForDevice(context, CameraManager.getDeviceIdLast(), UserDeviceTypeManager.getInstance().getDeviceType(), countType, startTime, useNumber, commonListener);
    }
    private static void submitUseNumberForDevice(Context context, String deviceId, String deviceType, int countType,
            String startTime, int useNumber, IActionCallback commonListener) {
        if(SERMEAL_DEV) {
            if(commonListener != null) {
                commonListener.onReturn(1);
            }
            return;
        }

        String submitApi = SdkApi.submitUseNumber();




        Log.d("asx", "asx + v2咋了" + deviceType + deviceType.equals("6"));
        if (DeviceQuotaPolicy.usesV2(deviceType)) {

            submitApi = SdkApi.submitUseNumber_V2();

            // 录制的去掉
            if (countType != 1) {
                String ss = "deviceId=" + deviceId + "&countType=" + countType
                        + "&startTime=" + startTime + "&useNumber=" + useNumber;
                OkHttpUtil.postDataUrlencoded(submitApi, ss, null,
                        new OkHttpCallbackDecode<UserPackageCountBean>(UserPackageCountBean.class) {
                            @Override
                            public void onDataSuccess(UserPackageCountBean data) {
                                try {
                                    MainHandler.runOnUiThread(() -> {
                                        DialogUtil.dismissDialog();
                                        if(data != null && data.code == 200) {
                                            if(commonListener != null) {
                                                commonListener.onReturn(1);
                                            }
                                        } else {
                                            if(CameraManager.isTestDevice()) {
                                                if(commonListener != null) {
                                                    commonListener.onReturn(1);
                                                }
                                            } else {
                                                if(commonListener != null) {
                                                    commonListener.onReturn(2);
                                                }
                                            }
                                        }
                                    });
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onFailure(String code, String message) {
                                MainHandler.runOnUiThread(() -> {
                                    ToastUtil.s(message);
                                    if(commonListener != null) {
                                        commonListener.onReturn(2);
                                    }
                                    DialogUtil.dismissDialog();
                                    if(code.equals("1001")) {
                                        queryAll(context);
                                    }
                                    // 这个601还需要沟通下。是判断还是直接200了
                                    if (code.equals("602")) {
                                        SetMealCheckManager.checkSetMeal(context,null,1);
                                    }
                                });
                            }

                            @Override
                            public void onFinish() {

                            }
                        });
            }else{
                if(commonListener != null) {
                    commonListener.onReturn(1);
                }
            }




        }else {
            String ss = "deviceId=" + deviceId + "&countType=" + countType
                    + "&startTime=" + startTime + "&useNumber=" + useNumber;
            OkHttpUtil.postDataUrlencoded(submitApi, ss, null,
                    new OkHttpCallbackDecode<UserSetMealBean>(UserSetMealBean.class) {
                        @Override
                        public void onDataSuccess(UserSetMealBean data) {
                            try {
                                MainHandler.runOnUiThread(() -> {
                                    DialogUtil.dismissDialog();
                                    if(data != null && data.code == 200) {
                                        if(commonListener != null) {
                                            commonListener.onReturn(1);
                                        }
                                    } else {
                                        if(CameraManager.isTestDevice()) {
                                            if(commonListener != null) {
                                                commonListener.onReturn(1);
                                            }
                                        } else {
                                            if(commonListener != null) {
                                                commonListener.onReturn(2);
                                            }
                                        }
                                    }
                                });
                            } catch(Exception e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                ToastUtil.s(message);
                                if(commonListener != null) {
                                    commonListener.onReturn(2);
                                }
                                DialogUtil.dismissDialog();
                                if(code.equals("1001")) {
                                    queryAll(context);
                                }
                            });
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
        }


    }


    //提示用户不可用
    public static void queryAll(Context context) {
        if(SERMEAL_DEV) {
            return;
        }

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getSaveDeviceIdLast());

        OkHttpUtil.postData(SdkApi.package_query_all(), paramsdata, null,
            new OkHttpCallbackDecode<UserSetMealBean>(UserSetMealBean.class) {
                @Override
                public void onDataSuccess(UserSetMealBean data) {

                    MainHandler.runOnUiThread(() -> {
                        try {
                            boolean hasBase = false;//有基础服务
                            if(data != null && data.data != null) {
                                for(int i = 0; i < data.data.size(); i++) {
                                    if(data.data.get(i).packageType == 1) {
                                        hasBase = true;
                                    }
                                }
                            }
                            AppTrace.d(TAG, "基础服务" + hasBase);
                            NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
                            notAvailableDialog.showOnCenter(0);
                            if(hasBase) {
                                notAvailableDialog.setText(context, "升级", 2);
                            } else {
                                notAvailableDialog.setText(context, "立即购买", 1);
                            }
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

    public void clear() {
        context = null;
        onPlanUsageResultListener = null;
    }

    public interface OnPlanUsageResultListener {

        void onResult(boolean success);
    }

}
