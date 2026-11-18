package com.xh.hotme.me.setmeal;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.event.CommonFinishEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivitySetmealListBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.live.bean.UserSetMealBean;
import com.xh.hotme.me.setmeal.adapter.SetMealListAdapter;
import com.xh.hotme.me.setmeal.bean.PackageListBean;
import com.xh.hotme.me.setmeal.bean.SetMealListBean;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.xh.hotme.me.UserDeviceTypeManager;

import okhttp3.FormBody;
import  com.xh.hotme.me.setmeal.bean.SetMealListPackageBean;
import com.xh.hotme.me.UserDeviceTypeManager;

@IEventBus
public class SetMealListActivity extends BaseActivity<ActivitySetmealListBinding> {
    public static final int TYPE_REWO = 1;//热我套餐购买
    public static final int TYPE_UPDATE = 2;//BASE升级
    public static final int TYPE_RENEW = 3;//pro续费
    public static final int TYPE_BASE = 4;//BASE续费
    public static final int TYPE_BIG = 5;//大礼包续费
    public static final int TYPE_BIG_UPDATE = 6;//大礼包升级

    private SetMealListAdapter adapter;

    private List<SetMealListBean> listBeans;

    private int type;
    private boolean showBig = false;

    @Override
    protected void initView() {
        type = getIntent().getIntExtra("type", 1);

        switch(type) {
            case TYPE_REWO:
                viewBinding.tvTitle.setText(getString(R.string.rewoo_meal));
                break;
            case TYPE_UPDATE:
            case TYPE_BIG_UPDATE:
                viewBinding.tvTitle.setText(getString(R.string.upgrade));
                break;
            case TYPE_RENEW:
            case TYPE_BASE:
            case TYPE_BIG:
                viewBinding.tvTitle.setText(getString(R.string.vip_renew));
                break;
        }


        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return false;
            }
        });
        adapter = new SetMealListAdapter(this);
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(SetMealListActivity.this));
        viewBinding.recyclerView.setAdapter(adapter);


        getUser();
    }

    private  void  showUserService(SetMealListPackageBean data) {
        listBeans = new ArrayList<>();
        SetMealListBean serMealListBean1 = new SetMealListBean();
        serMealListBean1.type = SetMealListBean.TYPE_TITLE;
        serMealListBean1.title = getString(R.string.live_counts_gift_package);
        listBeans.add(serMealListBean1);

        for(int i = 0; i < data.data.size(); i++) {
            if(data.data.get(i).packageCategory.equals("2")) {
                SetMealListBean serMealListBean = new SetMealListBean();
                serMealListBean.type = SetMealListBean.TYPE_CONTENT_LIVE_CARD;
                serMealListBean.dataBean2 = data.data.get(i);
                listBeans.add(serMealListBean);
            }
        }

        SetMealListBean serMealListBean2 = new SetMealListBean();
        serMealListBean2.type = SetMealListBean.TYPE_TITLE;
        serMealListBean2.title = getString(R.string.cloud_storage);
        listBeans.add(serMealListBean2);


        SetMealListBean serMealListBean3 = new SetMealListBean();
        serMealListBean3.type = SetMealListBean.TYPE_STORAGE;
        List<SetMealListPackageBean.DataBean> testStorageBeans = new ArrayList<>();
        serMealListBean3.storageBeans = testStorageBeans;
       for(int i = 0; i < data.data.size(); i++) {
           if(data.data.get(i).packageCategory.equals("1")) {
            serMealListBean3.storageBeans.add(data.data.get(i));
           }
       }
         listBeans.add(serMealListBean3);

        SetMealListBean serMealListBean4 = new SetMealListBean();
        serMealListBean4.type = SetMealListBean.TYPE_TITLE;
        serMealListBean4.title = getString(R.string.rewoo_camera_package);
        listBeans.add(serMealListBean4);

        for(int i = 0; i < data.data.size(); i++) {
            if(data.data.get(i).packageCategory.equals("3")) {
                SetMealListBean serMealListBean = new SetMealListBean();
                serMealListBean.type = SetMealListBean.TYPE_CONTENT_PRO_SERVICE;
                serMealListBean.dataBean2 = data.data.get(i);
                listBeans.add(serMealListBean);
            }
        }


        adapter.setListBeans(listBeans);
    }

    private void getLiveList(String time, int packageType) {
//        listBeans = new ArrayList<>();
//        SetMealListBean serMealListBean = new SetMealListBean();
//        serMealListBean.type = 4;
//        serMealListBean.title = "123456";
//
//        serMealListBean.packageType = 5;
//        listBeans.add(serMealListBean);
//        adapter.setListBeans(listBeans);

        // 获取设备类型
        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();

        if (deviceType.equals("6") || deviceType.equals("7")) {
            FormBody.Builder builder = new FormBody.Builder();
            String deviceId = UserDeviceTypeManager.getInstance().getDeviceID();
            builder.add("deviceId", deviceId);
            OkHttpUtil.postFormData(SdkApi.package_list_V2(), builder, null,
                    new OkHttpCallbackDecode<SetMealListPackageBean>(SetMealListPackageBean.class) {
                        @Override
                        public void onDataSuccess(SetMealListPackageBean data) {

                            MainHandler.runOnUiThread(() -> {
                                try {
                                    DialogUtil.dismissDialog();

                                    showUserService(data);
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
            OkHttpUtil.get(SdkApi.package_list(),
            null, new OkHttpCallbackDecode<PackageListBean>(PackageListBean.class) {
                @Override
                public void onDataSuccess(PackageListBean data) {
 
                    MainHandler.runOnUiThread(() -> {
                        if(data != null && data.data != null) {
 
                            try {
 
                                listBeans = new ArrayList<>();
                                if(type != TYPE_REWO) {
                                    SetMealListBean serMealListBean = new SetMealListBean();
                                    serMealListBean.type = SetMealListBean.TYPE_USER;
                                    serMealListBean.title = time;
                                    serMealListBean.packageType = packageType;
                                    listBeans.add(serMealListBean);
                                }
 
                                SetMealListBean serMealListBean2 = new SetMealListBean();
                                serMealListBean2.type = SetMealListBean.TYPE_TITLE;
                                if(type == TYPE_REWO) {
                                    serMealListBean2.title = getString(R.string.rewoo_meal);
                                } else if(type == TYPE_UPDATE) {
                                    serMealListBean2.title = getString(R.string.update_to_pro_service);
                                } else if(type == TYPE_RENEW) {
                                    serMealListBean2.title = getString(R.string.vip_renew);
                                } else if(type == TYPE_BASE) {
                                    serMealListBean2.title = getString(R.string.vip_renew);
                                } else if(type == TYPE_BIG) {
                                    serMealListBean2.title = getString(R.string.vip_renew);
                                } else if(type == TYPE_BIG_UPDATE) {
                                    serMealListBean2.title = getString(R.string.update_service);
                                }
 
                                listBeans.add(serMealListBean2);
 
                                for(int i = 0; i < data.data.size(); i++) {
                                    PackageListBean.DataBean dataBean = data.data.get(i);
                                    if(dataBean.packageType.equals("1")) {
                                        SetMealListBean serMealListBean3 = new SetMealListBean();
                                        serMealListBean3.type = SetMealListBean.TYPE_CONTENT_SERVICE;
                                        if(type == TYPE_REWO) {
                                            serMealListBean3.btn = getString(R.string.purchase);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        } else if(type == TYPE_UPDATE) {
                                            serMealListBean3.btn = getString(R.string.upgrade);
                                        } else if(type == TYPE_RENEW) {
                                            serMealListBean3.btn = getString(R.string.vip_renew);
                                        } else if(type == TYPE_BASE) {
                                            serMealListBean3.btn = getString(R.string.vip_renew);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        } else if(type == TYPE_BIG_UPDATE) {
                                            serMealListBean3.btn = getString(R.string.upgrade);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        }
 
                                    } else if(dataBean.packageType.equals("2")) {
                                        SetMealListBean serMealListBean3 = new SetMealListBean();
                                        serMealListBean3.type = SetMealListBean.TYPE_CONTENT_SERVICE;
                                        if(type == TYPE_REWO) {
                                            serMealListBean3.btn = getString(R.string.purchase);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        } else if(type == TYPE_UPDATE) {
                                            serMealListBean3.btn = getString(R.string.upgrade);
                                            serMealListBean3.dataBean = dataBean;
                                            serMealListBean3.isBaseToPro = true;
                                            listBeans.add(serMealListBean3);
 
 
                                        } else if(type == TYPE_BIG_UPDATE) {
                                            serMealListBean3.btn = getString(R.string.upgrade);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        } else if(type == TYPE_RENEW) {
                                            serMealListBean3.btn = getString(R.string.vip_renew);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        } else if(type == TYPE_BASE) {
 
                                        }
 
                                    } else if(dataBean.packageType.equals("0")) {
                                        SetMealListBean serMealListBean3 = new SetMealListBean();
                                        serMealListBean3.type = SetMealListBean.TYPE_CONTENT_SERVICE;
                                        if(type == TYPE_BIG) {
                                            serMealListBean3.btn = getString(R.string.vip_renew);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        }
 
                                    } else if(dataBean.packageType.equals("4")) {  //录制大礼包
                                        SetMealListBean serMealListBean3 = new SetMealListBean();
                                        serMealListBean3.type = SetMealListBean.TYPE_CONTENT_SERVICE;
                                        if(type == TYPE_REWO) {
                                            serMealListBean3.btn = getString(R.string.purchase);
                                            serMealListBean3.dataBean = dataBean;
                                            listBeans.add(serMealListBean3);
                                        }
                                    }
                                }
                                SetMealListBean serMealListBean4 = new SetMealListBean();
                                serMealListBean4.type = SetMealListBean.TYPE_TITLE;
                                serMealListBean4.title = getString(R.string.counts_gift_package);
                                listBeans.add(serMealListBean4);
 
                                for(int i = 0; i < data.data.size(); i++) {
                                    if(data.data.get(i).packageType.equals("3")) {
                                        SetMealListBean serMealListBean3 = new SetMealListBean();
                                        serMealListBean3.type = SetMealListBean.TYPE_CONTENT_CARD;
                                        serMealListBean3.dataBean = data.data.get(i);
                                        listBeans.add(serMealListBean3);
                                    }
                                }
 
                                if(showBig) {
                                    for(int i = 0; i < data.data.size(); i++) {
                                        if(data.data.get(i).packageType.equals("0")) {
                                            SetMealListBean serMealListBean3 = new SetMealListBean();
                                            serMealListBean3.type = SetMealListBean.TYPE_CONTENT_CARD;
                                            serMealListBean3.dataBean = data.data.get(i);
                                            listBeans.add(serMealListBean3);
                                        }
                                    }
                                }
 
                                adapter.setListBeans(listBeans);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
 
                }
 
                @Override
                public void onFailure(String code, String message) {
                    MainHandler.runOnUiThread(() -> {
                        ToastUtil.s(message);
                    });
 
 
                }
 
                @Override
                public void onFinish() {
 
                }
            });
        }






    }

    public void getUser() {

        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceIdLast());

        OkHttpUtil.postData(SdkApi.package_query_all(), paramsdata, null,
            new OkHttpCallbackDecode<UserSetMealBean>(UserSetMealBean.class) {
                @Override
                public void onDataSuccess(UserSetMealBean data) {

                    MainHandler.runOnUiThread(() -> {
                        try {
                            String baseTime = "";
                            String rewoTime = "";
                            String cardTime = "";
                            int packageType = 0;
                            for(int i = 0; i < data.data.size(); i++) {

                                if(data.data.get(i).packageType == 0) {

                                    try {
                                        rewoTime = data.data.get(i).expireTime.
                                            substring(0, data.data.get(i).expireTime.lastIndexOf(" "));

                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                } else if(data.data.get(i).packageType == 1) {

                                    try {
                                        baseTime = data.data.get(i).expireTime.
                                            substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                                        packageType = 1;
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                } else if(data.data.get(i).packageType == 2) {

                                    try {
                                        baseTime = data.data.get(i).expireTime.
                                            substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                                        packageType = 2;
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                } else if(data.data.get(i).packageType == 3) {
                                    cardTime = getString(R.string.unlimited_duration);
                                }

                            }

                            if(data.data.isEmpty()) {
                                showBig = true;
                            }


                            if(type == TYPE_BIG || type == TYPE_BIG_UPDATE) {
                                getLiveList(rewoTime, 0);
                            } else {
                                if(TextUtils.isEmpty(baseTime)) {
                                    baseTime = cardTime;
                                }
                                getLiveList(baseTime, packageType);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    });

                }

                @Override
                public void onFailure(String code, String message) {

                }

                @Override
                public void onFinish() {

                }
            });

    }

    @Override
    protected void initData() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CommonFinishEvent event) {
        if(event.status == CommonFinishEvent.TYPE_PAY_SUCCESS) {
            finish();
        }
    }
}
