package com.xh.hotme.me.setmeal.utils;

import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.CreateOrderBean;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;

import java.util.HashMap;
import com.xh.hotme.me.UserDeviceTypeManager;
import android.util.Log;
/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/17
 * @Description: 创建订单
 */
public class SetMealPayUtils {
    public static final boolean SERMEAL_DEV = false;

    public static void createOrder(String packageId, boolean baseToPro, IActionCallback commonListener) {
        createOrder(packageId,baseToPro,commonListener,"");

    }

    public static void createOrder(String packageId, boolean baseToPro, IActionCallback commonListener,String lastOrderID) {


        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceIdLast());
        paramsdata.put("packageId", packageId);
        paramsdata.put("clientPay", "1");

        String api;
        if(baseToPro) {
            api = SdkApi.createUpgradeOrder();
        } else {
            api = SdkApi.pay_createOrder();
        }

        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
        String deviveID = UserDeviceTypeManager.getInstance().getDeviceID();
        if (deviceType.equals("6") || deviceType.equals("7")) {
            api = SdkApi.pay_createOrder_V2();
            paramsdata = new HashMap<>();
            paramsdata.put("deviceId", deviveID);
            paramsdata.put("packageId", packageId);
            paramsdata.put("orderPoint", "2");
            if (lastOrderID == null || lastOrderID.isEmpty()) {
                paramsdata.put("orderType", "1");
                paramsdata.put("upgradeOrderId", "");
            }else {
                paramsdata.put("orderType", "2");
                paramsdata.put("upgradeOrderId", lastOrderID);
            }

        }

        OkHttpUtil.postData(api, paramsdata, null,
            new OkHttpCallbackDecode<CreateOrderBean>(CreateOrderBean.class) {
                @Override
                public void onDataSuccess(CreateOrderBean data) {
                    try {
                        String amount = OrderAmount.validate(data);
                        data.data.hmOrder.amt = amount;
                    } catch (IllegalArgumentException error) {
                        onFailure("INVALID_ORDER", error.getMessage());
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    MainHandler.runOnUiThread(() -> {
                        try {
                            commonListener.onReturn(1, data);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onFailure(String code, String message) {
                    MainHandler.runOnUiThread(() -> {
                        try {
                            ToastUtil.s(message);
                            commonListener.onReturn(2);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onFinish() {

                }
            });
    }


    //连接设备出现检查多次
//    private static boolean isQuery = false;

    /**
     * 查询次数并提示
     *
     * @param countType 1:录制；2:云存储；3:直播
     * @param type      1进入应用是检查过期就提示 2功能不可用样式
     */
//    public static void queryNumber(Context context, int countType, CommonListener commonListener, int type) {
//
//        if (SERMEAL_DEV) {
//            commonListener.onReturn(1);
//            return;
//        }
//
//        if (isQuery) {
//            return;
//        }
//
//        if (TextUtils.isEmpty(CameraManager.getDeviceIdLast())) {
//            return;
//        }
//        if (type == 2) {
//            DialogUtil.showDialog("");
//        }
//
//        isQuery = true;
//        HashMap<String, String> paramsdata = new HashMap<>();
//        paramsdata.put("deviceId", CameraManager.getDeviceIdLast());
//        OkHttpUtil.postData(SdkApi.package_query_all(), paramsdata, null,
//                new OkHttpCallbackDecode<UserSetMealBean>(UserSetMealBean.class) {
//                    @Override
//                    public void onDataSuccess(UserSetMealBean data) {
//                        try {
//                            isQuery = false;
//                            MainHandler.runOnUIThread(() -> DialogUtil.dismissDialog());
//
//                            checkUserBean(context, countType, commonListener, data, false, type);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(String code, String message) {
//                        MainHandler.runOnUIThread(() -> {
//                            ToastUtil.s(message);
//                            DialogUtil.dismissDialog();
//                        });
//                    }
//
//                    @Override
//                    public void onFinish() {
//                    }
//                });
//
//    }


//    public static void checkUserBean(Context context, int countType, CommonListener commonListener
//            , UserSetMealBean data, boolean showToast, int type) {
//        MainHandler.runOnUIThread(() -> {
//
//            //data.code==200  同时 data.data.size==0  证明这个设备没套餐
//            //除了测试设备设备必须有个套餐 最少也是个送的大礼包 没有说明有问题
//            if (data == null || (data.code == 200 && data.data == null)) {
//                commonListener.onReturn(2);
//                return;
//            }
//
//            //热我大礼包剩余次数
//            int rewoCount = -100;
//            //基础服务
//            int baseCount = -100;
//            int baseTotalCount = -100;
//            //PRO服务
//            int proCount = -100;
//            //次卡服务
//            int cardCount = -100;
//
//            for (int i = 0; i < data.data.size(); i++) {
//                if (data.data.get(i).countType == countType) {
//                    if (data.data.get(i).packageType == 0) {
//                        rewoCount = data.data.get(i).totalCount - data.data.get(i).useCount;
//                    } else if (data.data.get(i).packageType == 1) {
//                        baseTotalCount = data.data.get(i).totalCount;
//                        baseCount = data.data.get(i).totalCount - data.data.get(i).useCount;
//                    } else if (data.data.get(i).packageType == 2) {
//                        proCount = data.data.get(i).totalCount - data.data.get(i).useCount;
//                    } else if (data.data.get(i).packageType == 3) {
//                        cardCount = data.data.get(i).totalCount - data.data.get(i).useCount;
//                    }
//                }
//            }
//
//
//            AppTrace.e("套餐使用情况 countType=" + countType, "热我大礼包剩余次数 " + rewoCount);
//            AppTrace.e("套餐使用情况 countType=" + countType, "基础服务剩余次数 " + baseCount);
//            AppTrace.e("套餐使用情况 countType=" + countType, "PRO服务剩余次数 " + proCount);
//            AppTrace.e("套餐使用情况 countType=" + countType, "次卡服务剩余次数 " + cardCount);
//
//            //热我大礼包剩余次数
////            rewoCount = 0;
////            //基础服务
////            baseCount = -100;
////            //PRO服务
////            proCount = -100;
////            //次卡服务
////            cardCount = -100;
//
//
//            if (rewoCount == -100 && baseCount == -100 && proCount == -100 && cardCount == -100) {
//                if (showToast) {
//                    //不存在这种情况
//                    ToastUtil.s("未购买套餐");
//                }
//                commonListener.onReturn(2);
//            } else {
//
//                //其他剩余次数
//                int count1 = 0;
//                int count2 = 0;
//                int count3 = 0;
//                //过期
//                boolean expired = false;
//                String expireTime = "";
//
//                //录制
//                if (countType == 1) {
//
//                    for (int i = 0; i < data.data.size(); i++) {
//                        if (data.data.get(i).packageType == 2) {
//                            expired = data.data.get(i).expired;
//                            expireTime = data.data.get(i).expireTime;
//                        }
//                    }
//
//                    if (proCount != -100) {
//                        //有pro服务  录制不限次
//                        if (!expired) {
//                            //录制pro服务不限次
//                            commonListener.onReturn(1);
//                            return;
//                        } else {
//
//                            if (type == 1) {
//                                //判断PRO到期
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//
//                                return;
//                            } else if (type == 2 && cardCount <= 0 && rewoCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            }
//                        }
//                    }
//
//                    if (baseCount != -100) {
//                        //有基础服务服务
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 1) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//
//                        if (!expired && baseTotalCount == -1) {
//                            //基础服务不限次
//                            commonListener.onReturn(1);
//                            return;
//                        }
//
//
//                        //基础服务过期
//                        if (expired) {
//                            if (type == 2 && cardCount <= 0 && rewoCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            }
//                        } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                            if (type == 2 && cardCount <= 0 && rewoCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            }
//                        } else if (baseCount <= 0) {
//                            if (type == 2 && cardCount <= 0 && rewoCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showBaseDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            }
//
//                        } else {
//                            commonListener.onReturn(1);
//                            return;
//                        }
//
//                    }
//
//
//                    if (rewoCount != -100) {
//                        //有大礼包
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 0) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//                        if (expired) {
//                            if (type == 2 && cardCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            }
//                        } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                            if (type == 2 && cardCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            }
//                        } else if (rewoCount <= 0) {
//                            if (type == 2 && cardCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (type == 1) {
//                                SetMealDialogUtils.showBaseDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            }
//
//                        } else {
//                            commonListener.onReturn(1);
//                            return;
//                        }
//
//                    }
//                    if (cardCount > 0) {
//                        //还有次卡
//                        commonListener.onReturn(1);
//                    } else {
//                        if (showToast) {
//                            ToastUtil.s("套餐使用次数到期");
//                        }
//                    }
//                } else if (countType == 3) {
//
//                    //直播
//                    if (proCount != -100) {
//                        //有pro服务
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 2) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//                        if (type == 1) {
//
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (proCount <= 0) {
//                                SetMealDialogUtils.showProDialog(context, "不限次", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//                            NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//
//                            if (expired && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (proCount <= 0 && cardCount <= 0 && rewoCount <= 0) {
//                                //续费
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        }
//                    }
//
//                    if (baseCount != -100) {
//                        //有基础服务
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 1) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//
//                        if (type == 1) {
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (baseCount <= 0) {
//                                SetMealDialogUtils.showBaseDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//                            NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//
//                            if (expired && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (baseCount <= 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        }
//                    }
//
//                    if (rewoCount != -100) {
//                        //有大礼包
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 0) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//                        if (type == 1) {
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (rewoCount <= 0) {
//                                SetMealDialogUtils.showRwPackDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//                            if ((expired || rewoCount <= 0) && cardCount <= 0 && rewoCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else {
//                                //还有次数
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        }
//                    }
//
//                    if (cardCount > 0) {
//                        //还有次卡
//                        commonListener.onReturn(1);
//                    } else {
//                        if (showToast) {
//                            ToastUtil.s("套餐使用次数到期");
//                        }
//                    }
//
//
//                } else if (countType == 2) {//云存储
//
//                    if (proCount != -100) {
//                        //有pro服务
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 2) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//
//                        if (type == 1) {
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (proCount <= 0) {
//                                SetMealDialogUtils.showProDialog(context, "不限次", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//                            NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                            if (expired && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else if (proCount <= 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                //续费
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        }
//                    }
//
//
//                    if (baseCount != -100) {
//                        //有基础服务
//
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 1) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//                        if (type == 1) {
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (baseCount <= 0) {
//                                SetMealDialogUtils.showBaseDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//                            NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//
//                            if (expired && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else if (baseCount <= 0 && cardCount <= 0 && rewoCount <= 0) {
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "升级", 2);
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        }
//                    }
//
//                    if (rewoCount != -100) {
//                        //有大礼包
//                        for (int i = 0; i < data.data.size(); i++) {
//                            if (data.data.get(i).packageType == 0) {
//                                expired = data.data.get(i).expired;
//                                expireTime = data.data.get(i).expireTime;
//                                if (data.data.get(i).countType == 1) {
//                                    count1 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 2) {
//                                    count2 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                } else if (data.data.get(i).countType == 3) {
//                                    count3 = data.data.get(i).totalCount - data.data.get(i).useCount;
//                                }
//                            }
//                        }
//                        if (type == 1) {
//
//                            if (expired) {
//                                SetMealDialogUtils.showExpiredYearDialog(context, expireTime);
//                                return;
//                            } else if (count1 == 0 && count2 == 0 && count3 == 0) {
//                                SetMealDialogUtils.showExpiredDialog(context);
//                                return;
//                            } else if (rewoCount <= 0) {
//                                SetMealDialogUtils.showRwPackDialog(context, count1 + "场", count2 + "场", count3 + "场");
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//                        } else if (type == 2) {
//
//                            if ((expired || rewoCount <= 0) && cardCount <= 0) {
//                                NotAvailableDialog notAvailableDialog = new NotAvailableDialog(context);
//                                notAvailableDialog.show();
//                                notAvailableDialog.setText(context, "立刻购买", 1);
//                                return;
//                            } else {
//                                commonListener.onReturn(1);
//                                return;
//                            }
//
//                        }
//                    }
//                    if (cardCount > 0) {
//                        //还有次卡
//                        commonListener.onReturn(1);
//                    } else {
//                        if (showToast) {
//                            ToastUtil.s("套餐使用次数到期");
//                        }
//                    }
//                }
//
//            }
//
//
//        });
//    }


    //进入应用检查提示
//    public static void checkShow(Context context) {
//
//        if (SERMEAL_DEV) {
//            return;
//        }
//
//        if (!TextUtils.isEmpty(CameraManager.getDeviceIdLast()) && LoginManager.isLogon(context)) {
//            SetMealPayUtils.queryNumber(context, 1, (code, objects) -> {
//
//                if (code == 1) {
//                    SetMealPayUtils.queryNumber(context, 3, (code1, objects1) -> {
//                        if (code1 == 1) {
//                            SetMealPayUtils.queryNumber(context, 2, (code11, objects11) -> {
//                                if (code11 == 1) {
//
//                                }
//                            }, 1);
//                        }
//                    }, 1);
//                }
//            }, 1);
//        }
//    }
}
