package com.xh.hotme.me.profile;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.PurchaseInfoBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityPurchaseHistoryListBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setmeal.bean.SetMealHistoryBean;
import com.xh.hotme.me.setmeal.bean.SetMealHistoryPackageBean;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;

import okhttp3.FormBody;


public class PurchaseHistoryActivity extends BaseActivity<ActivityPurchaseHistoryListBinding> {
    private static final String TAG = PurchaseHistoryActivity.class.getSimpleName();

    final List<PurchaseInfoBean> _dataList = new ArrayList<>();


    PurchaseHistoryItemAdapter _adapter;

    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, PurchaseHistoryActivity.class);
            context.startActivity(intent);
        }
    }


    @Override
    protected void initView() {
        // find views
        TitleLayoutHelper.initTitle(viewBinding.title, getString(R.string.purchase_history));

    }

    @Override
    protected void initData() {


        _adapter = new PurchaseHistoryItemAdapter(PurchaseHistoryActivity.this, _dataList);

        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(PurchaseHistoryActivity.this));
//        viewBinding.recyclerView.addItemDecoration(new GridSpacingItemDecoration(1,
//                DensityUtil.dip2px(PurchaseHistoryActivity.this, 10), false));

        View emptyView = LayoutInflater.from(PurchaseHistoryActivity.this).inflate(R.layout.purchase_history_empty_view, null);
        _adapter.setEmptyView(emptyView);

        viewBinding.recyclerView.setAdapter(_adapter);

        viewBinding.refreshLayout.setOnRefreshListener(() -> new Handler(Looper.getMainLooper()).post(() -> loadData()));

        loadData();
    }

    private void loadData() {

        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
        String deviceId = UserDeviceTypeManager.getInstance().getDeviceID();
        if (deviceType.equals("6") || deviceType.equals("7")) {
            HashMap<String, String> paramsdata = new HashMap<>();
            paramsdata.put("deviceId", deviceId);
            paramsdata.put("orderStatus","2");
            OkHttpUtil.postData(SdkApi.renewRecord_list_V2(), paramsdata, null,
                    new OkHttpCallbackDecode<SetMealHistoryPackageBean>(SetMealHistoryPackageBean.class) {
                        @Override
                        public void onDataSuccess(SetMealHistoryPackageBean data) {

                            MainHandler.runOnUiThread(() -> {
                                try {
                                    DialogUtil.dismissDialog();
                                    viewBinding.refreshLayout.setRefreshing(false);
                                    _dataList.clear();


                                    for (SetMealHistoryPackageBean.DataBean order : data.data) {

                                        PurchaseInfoBean purchaseInfoBean = new PurchaseInfoBean();
                                        purchaseInfoBean.service_type = 10000 + Integer.parseInt(order.packageSnapshot.packageCategory);
                                        purchaseInfoBean.description = String.valueOf(order.packageSnapshot.packagePrice) + "元";
                                        purchaseInfoBean.start_time = order.createTime;
                                        purchaseInfoBean.name = order.packageSnapshot.packageName;
                                        purchaseInfoBean.purchase_type = order.orderPoint == 1 ? "网页端" : "APP";
                                        _dataList.add(purchaseInfoBean);
                                    }


                                    _adapter.notifyDataSetChanged();
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                DialogUtil.dismissDialog();
                                viewBinding.refreshLayout.setRefreshing(false);
                                _dataList.clear();
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

            OkHttpUtil.get(SdkApi.renewRecord_list() + "?deviceId=" + CameraManager.getDeviceIdLast(), null,
                    new OkHttpCallbackDecode<SetMealHistoryBean>(SetMealHistoryBean.class) {
                        @Override
                        public void onDataSuccess(SetMealHistoryBean data) {

                            runOnUiThread(() -> {
                                try {

                                    viewBinding.refreshLayout.setRefreshing(false);
                                    _dataList.clear();

                                    for(int i = 0; i < data.data.rows.size(); i++) {
                                        SetMealHistoryBean.DataBean.RowsBean rowsBean = data.data.rows.get(i);
                                        PurchaseInfoBean purchaseInfoBean = new PurchaseInfoBean();
                                        purchaseInfoBean.service_type = Integer.parseInt(rowsBean.packageType);
                                        purchaseInfoBean.description = rowsBean.packagePrice + "元";

                                        if(rowsBean.payChanneltype.equals("0")) {
                                            purchaseInfoBean.purchase_type = "赠送";
                                            purchaseInfoBean.description = "赠送";
                                        } else if(rowsBean.payChanneltype.equals("12")) {
                                            purchaseInfoBean.purchase_type = "支付宝";
                                        } else if(rowsBean.payChanneltype.equals("13")) {
                                            purchaseInfoBean.purchase_type = "微信";
                                        } else if(rowsBean.payChanneltype.equals("27")) {
                                            purchaseInfoBean.purchase_type = "云闪付";
                                        } else if(rowsBean.payChanneltype.equals("36")) {
                                            purchaseInfoBean.purchase_type = "花呗分期";
                                        } else if(rowsBean.payChanneltype.equals("37")) {
                                            purchaseInfoBean.purchase_type = "信用卡分期";
                                        } else {
                                            purchaseInfoBean.purchase_type = "其他";
                                        }

                                        purchaseInfoBean.start_time = rowsBean.createTime;
                                        purchaseInfoBean.end_time = rowsBean.endTime;
                                        purchaseInfoBean.name = rowsBean.packageName;
                                        _dataList.add(purchaseInfoBean);
                                    }

                                    _adapter.notifyDataSetChanged();
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            runOnUiThread(() -> {
                                viewBinding.refreshLayout.setRefreshing(false);
                            });
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
        }

    }
}
