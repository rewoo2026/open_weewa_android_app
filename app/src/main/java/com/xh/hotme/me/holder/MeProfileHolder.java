package com.xh.hotme.me.holder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.GetVideoUrlBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.live.bean.UserSetMealBean;
import com.xh.hotme.live.dialog.AddPushPopUtils;
import com.xh.hotme.me.MeModuleBean;
import com.xh.hotme.me.profile.PurchaseHistoryActivity;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import com.xh.hotme.storage.CloudVideoListDetailActivity;
import com.xh.hotme.storage.VideoPlayActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ColorUtil;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ReTextUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import com.xh.hotme.me.setmeal.adapter.UserPackageCountAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import  com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import okhttp3.FormBody;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.utils.ToastUtil;

public class MeProfileHolder extends CommonViewHolder<MeModuleBean> {
    private static final Logger log = LoggerFactory.getLogger(MeProfileHolder.class);
    // views
    private final TextView _nameLabel,_ceshiView, _historyView, _purchaseView, _extendView, _upgradeView, _expiredStatusView;
    private final ImageView _avatarView;
    private final TextView tv_rewo;
    private final TextView tv_service;
    private final TextView tv_cord;
    private final TextView tv_record;
    private final TextView vip_expired_time;
    private final TextView tv_count1;
    private final TextView tv_count2;
    private final TextView tv_count3;
    private final TextView tv_count_big;
    private final TextView tv_select_device;
    private final TextView tv_select_device_left;
    private final androidx.constraintlayout.widget.Group group_big;
    //套餐状态
    private final ImageView iv_vip_service;
    final Context _ctx;

    private UserPackageCountAdapter userPackageCountAdapter;
    private RecyclerView rvUserPackageCounts;


    private String rewoTime = "";
    private String baseTime = "";
    private String recordTime = "";
    private int nowSelect = 0;
    private int packageRequestVersion = 0;

    public static MeProfileHolder create(Context ctx, ViewGroup parent) {
        View convertView = LayoutInflater.from(ctx).inflate(R.layout.me_layout_vip, parent, false);
        return new MeProfileHolder(convertView);
    }

    public MeProfileHolder(View itemView) {
        super(itemView);

        // find views
        _ctx = itemView.getContext();
        _nameLabel = itemView.findViewById(R.id.name);
        _avatarView = itemView.findViewById(R.id.avatar);
        _expiredStatusView = itemView.findViewById(R.id.vip_expired_status);
        _ceshiView = itemView.findViewById(R.id.asx_ceshi);
        _historyView = itemView.findViewById(R.id.tv_purchase_history);
        _purchaseView = itemView.findViewById(R.id.vip_purchase);
        _extendView = itemView.findViewById(R.id.vip_extend);
        _upgradeView = itemView.findViewById(R.id.vip_upgrade);
        tv_service = itemView.findViewById(R.id.tv_service);
        tv_rewo = itemView.findViewById(R.id.tv_rewo);
        tv_cord = itemView.findViewById(R.id.tv_cord);
        tv_record = itemView.findViewById(R.id.tv_record);
        iv_vip_service = itemView.findViewById(R.id.iv_vip_service);
        vip_expired_time = itemView.findViewById(R.id.vip_expired_time);
        tv_count1 = itemView.findViewById(R.id.tv_count1);
        tv_count2 = itemView.findViewById(R.id.tv_count2);
        tv_count3 = itemView.findViewById(R.id.tv_count3);
        tv_count_big = itemView.findViewById(R.id.tv_count_big);
        group_big = itemView.findViewById(R.id.group_big);
        tv_select_device = itemView.findViewById(R.id.tv_select_device);
        tv_select_device_left = itemView.findViewById(R.id.tv_select_device_left);

        _ceshiView.setText(SdkApi.getRequestUrl());

        tv_select_device.setOnClickListener(view ->

            AddPushPopUtils.showDeviceSelectPop(_ctx, view, (code, objects) -> {

                List<BindDeviceBean> bindDeviceList = DeviceManager.getBindDeviceList();

                for(BindDeviceBean deviceBean : bindDeviceList) {
                    if(deviceBean.getDeviceName().equals(objects[0])) {
                        tv_select_device.setText(deviceBean.getDeviceName());

                        CameraManager.setDeviceId(deviceBean.getDeviceId());

                        DialogUtil.showDialog("");
                        nowSelect = 0;
                        initData();
                        break;
                    }
                }

            }));
        _nameLabel.setOnClickListener(view -> {
            if(!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(_ctx);
            }
        });
        _avatarView.setOnClickListener(view -> {
            if(!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(_ctx);
            }
        });
        //购买记录
        _historyView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                PurchaseHistoryActivity.start(_ctx);
                return true;
            }
        });


        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        rvUserPackageCounts = itemView.findViewById(R.id.rv_user_package_count);
        rvUserPackageCounts.setLayoutManager(new LinearLayoutManager(_ctx, LinearLayoutManager.VERTICAL, false));
        userPackageCountAdapter = new UserPackageCountAdapter(_ctx);
        rvUserPackageCounts.setAdapter(userPackageCountAdapter);

    }


    private void showService(UserSetMealBean data) {

        tv_select_device.setVisibility(View.VISIBLE);
        tv_select_device_left.setVisibility(View.VISIBLE);

//        int status = 0;
//        int status = 1;//没有服务只有大礼包
//        int status = 2;//基础服务
//        int status = 3;//PRO服务

        if(data.data.isEmpty()) {
            itemView.findViewById(R.id.vip_time_info).setVisibility(View.VISIBLE);
            vip_expired_time.setText(_ctx.getString(R.string.no_meal));
            _purchaseView.setVisibility(View.VISIBLE);
            _extendView.setVisibility(View.GONE);
            _upgradeView.setVisibility(View.GONE);
            _purchaseView.setOnClickListener(view -> {
                //购买
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 1);
                view.getContext().startActivity(intent);
            });
            return;
        }

        boolean hasPack = false;//有大礼包
        boolean packExpired = false;//大礼包过期

        boolean hasBase = false;//有基础服务
        boolean baseExpired = false;//过期

        boolean hasPro = false;//有pro服务
        boolean proExpired = false;//pro过期

        boolean hasCard = false;//有次卡包
        boolean hasRecordPack = false;//有录制大礼包
        boolean recordPackExpired = false;//过期

        for(int i = 0; i < data.data.size(); i++) {

            if(data.data.get(i).packageType == 0) {

                hasPack = true;
                packExpired = data.data.get(i).expired;
                try {
                    rewoTime = data.data.get(i).expireTime.
                        substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else if(data.data.get(i).packageType == 1) {

                hasBase = true;
                baseExpired = data.data.get(i).expired;
                try {
                    baseTime = data.data.get(i).expireTime.
                        substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else if(data.data.get(i).packageType == 2) {

                hasPro = true;
                proExpired = data.data.get(i).expired;
                try {
                    baseTime = data.data.get(i).expireTime.
                        substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else if(data.data.get(i).packageType == 3) {
                hasCard = true;
            } else if(data.data.get(i).packageType == 4) {
                hasRecordPack = true;
                recordPackExpired = data.data.get(i).expired;
                try {
                    recordTime = data.data.get(i).expireTime.
                        substring(0, data.data.get(i).expireTime.lastIndexOf(" "));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        itemView.findViewById(R.id.vip_time_info).setVisibility(View.VISIBLE);
        itemView.findViewById(R.id.service_item).setVisibility(View.VISIBLE);


        iv_vip_service.setVisibility(View.VISIBLE);

        String cell = _ctx.getString(R.string.session);

        if(hasPro) {
            iv_vip_service.setImageResource(R.mipmap.me_service_pro);
            vip_expired_time.setText(baseTime);
            tv_service.setText(_ctx.getString(R.string.pro_service));

        } else if(hasBase) {
            iv_vip_service.setImageResource(R.mipmap.me_service_base);
            vip_expired_time.setText(baseTime);
            tv_service.setText(_ctx.getString(R.string.base_service));

        } else if(hasPack) {
            iv_vip_service.setImageResource(R.mipmap.me_service_normal);
            vip_expired_time.setText(rewoTime);

        } else {
            iv_vip_service.setVisibility(View.GONE);

            _purchaseView.setVisibility(View.VISIBLE);
            _extendView.setVisibility(View.GONE);
            _upgradeView.setVisibility(View.GONE);

            _purchaseView.setOnClickListener(view -> {
                //购买
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 1);
                view.getContext().startActivity(intent);
            });
        }


        boolean finalHasBase = hasBase;
        boolean finalHasPro = hasPro;
        boolean finalPackExpired = packExpired;
        boolean finalRecordExpired = recordPackExpired;

        //大礼包
        tv_rewo.setOnClickListener(view -> {
            nowSelect = 1;
            tv_rewo.setTextColor(ColorUtil.parseColor("#3D3D3D"));
            tv_service.setTextColor(ColorUtil.parseColor("#999999"));
            tv_cord.setTextColor(ColorUtil.parseColor("#999999"));
            tv_record.setTextColor(ColorUtil.parseColor("#999999"));
            tv_rewo.setTextSize(TypedValue.COMPLEX_UNIT_SP , 18);
            tv_service.setTextSize(16);
            tv_cord.setTextSize(16);
            tv_record.setTextSize(16);

            tv_rewo.setTypeface(null, Typeface.BOLD_ITALIC);
            tv_service.setTypeface(null, Typeface.NORMAL);
            tv_cord.setTypeface(null, Typeface.NORMAL);
            tv_record.setTypeface(null, Typeface.NORMAL);

            int color = ContextCompat.getColor(_ctx, R.color.color_3D3D3D);

            for(int i = 0; i < data.data.size(); i++) {
                if(data.data.get(i).packageType == 0 && data.data.get(i).countType == 1) {

                    if(data.data.get(i).totalCount == -1) {
                        tv_count1.setText(_ctx.getString(R.string.unlimited_time));
                    } else {
                        tv_count1.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                        ReTextUtils.changeLastTextColor(tv_count1, cell, color);
                    }

                } else if(data.data.get(i).packageType == 0 && data.data.get(i).countType == 2) {

                    if(data.data.get(i).totalCount == -1) {
                        tv_count2.setText(_ctx.getString(R.string.unlimited_time));
                    } else {
                        tv_count2.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                        ReTextUtils.changeLastTextColor(tv_count2, cell, color);
                    }
                } else if(data.data.get(i).packageType == 0 && data.data.get(i).countType == 3) {

                    if(data.data.get(i).totalCount == -1) {
                        tv_count3.setText(_ctx.getString(R.string.unlimited_time));
                    } else {
                        tv_count3.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                        ReTextUtils.changeLastTextColor(tv_count3, cell, color);
                    }
                }
            }


            group_big.setVisibility(View.GONE);
            vip_expired_time.setText(rewoTime);

            if(finalHasBase || finalHasPro) {
                _purchaseView.setVisibility(View.GONE);
            } else {
                _purchaseView.setVisibility(View.VISIBLE);
            }
            _extendView.setVisibility(View.GONE);
            _upgradeView.setVisibility(View.GONE);

            _purchaseView.setOnClickListener(view2 -> {
                //购买
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 1);
                view.getContext().startActivity(intent);
            });

            _extendView.setOnClickListener(view2 -> {
                //续费
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 5);
                view.getContext().startActivity(intent);
            });

            _upgradeView.setOnClickListener(view4 -> {
                //升级
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 6);
                view.getContext().startActivity(intent);
            });

            if(finalPackExpired) {
                _purchaseView.setVisibility(View.VISIBLE);
                _expiredStatusView.setVisibility(View.VISIBLE);
                _extendView.setVisibility(View.GONE);
                _upgradeView.setVisibility(View.GONE);

                _purchaseView.setOnClickListener(view2 -> {
                    //购买
                    Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                    intent.putExtra("type", SetMealListActivity.TYPE_BASE);
                    view.getContext().startActivity(intent);
                });
            }

        });

        //基础 pro服务
        boolean finalBaseExpired = baseExpired;
        boolean finalProExpired = proExpired;
        tv_service.setOnClickListener(view -> {
            nowSelect = 2;

            tv_rewo.setTextSize(16);
            tv_service.setTextSize(18);
            tv_cord.setTextSize(16);
            tv_record.setTextSize(16);

            tv_rewo.setTypeface(null, Typeface.NORMAL);
            tv_service.setTypeface(null, Typeface.BOLD_ITALIC);
            tv_cord.setTypeface(null, Typeface.NORMAL);
            tv_record.setTypeface(null, Typeface.NORMAL);


            tv_service.setTextColor(ColorUtil.parseColor("#3D3D3D"));
            tv_cord.setTextColor(ColorUtil.parseColor("#999999"));
            tv_rewo.setTextColor(ColorUtil.parseColor("#999999"));
            tv_record.setTextColor(ColorUtil.parseColor("#999999"));
            int color = ContextCompat.getColor(_ctx, R.color.color_3D3D3D);

            if(finalHasPro) {
                //pro

//                ReTextUtils.changeLastTextColor(tv_count1, "不限次", color);
                group_big.setVisibility(View.VISIBLE);
                for(int i = 0; i < data.data.size(); i++) {
                    if(data.data.get(i).packageType == 2 && data.data.get(i).countType == 1) {
                        if(data.data.get(i).totalCount == -1) {
                            tv_count1.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count1.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count1, _ctx.getString(R.string.session), color);
                        }
                    } else if(data.data.get(i).packageType == 2 && data.data.get(i).countType == 4) {
                        if(data.data.get(i).totalCount == -1) {
                            tv_count_big.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count_big.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count_big, _ctx.getString(R.string.session), color);
                        }
                    } else if(data.data.get(i).packageType == 2 && data.data.get(i).countType == 2) {


                        if(data.data.get(i).totalCount == -1) {
                            tv_count2.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count2.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count2, cell, color);
                        }
                    } else if(data.data.get(i).packageType == 2 && data.data.get(i).countType == 3) {
                        if(data.data.get(i).totalCount == -1) {
                            tv_count3.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count3.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count3, cell, color);
                        }
                    }
                }

                if(finalProExpired) {
                    _purchaseView.setVisibility(View.VISIBLE);
                    _expiredStatusView.setVisibility(View.VISIBLE);
                    _extendView.setVisibility(View.GONE);
                    _upgradeView.setVisibility(View.GONE);

                    _purchaseView.setOnClickListener(view2 -> {
                        //购买
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", SetMealListActivity.TYPE_BASE);
                        view.getContext().startActivity(intent);
                    });
                } else {
                    _purchaseView.setVisibility(View.GONE);
                    _extendView.setVisibility(View.VISIBLE);
                    _upgradeView.setVisibility(View.GONE);

                    _extendView.setOnClickListener(view2 -> {
                        //续费
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", SetMealListActivity.TYPE_RENEW);
                        view.getContext().startActivity(intent);
                    });
                }

            } else if(finalHasBase) {
                group_big.setVisibility(View.GONE);

                for(int i = 0; i < data.data.size(); i++) {
                    if(data.data.get(i).packageType == 1 && data.data.get(i).countType == 1) {

                        if(data.data.get(i).totalCount == -1) {
                            tv_count1.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count1.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count1, cell, color);
                        }
                    } else if(data.data.get(i).packageType == 1 && data.data.get(i).countType == 2) {
                        if(data.data.get(i).totalCount == -1) {
                            tv_count2.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count2.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count2, cell, color);
                        }
                    } else if(data.data.get(i).packageType == 1 && data.data.get(i).countType == 3) {

                        if(data.data.get(i).totalCount == -1) {
                            tv_count3.setText(_ctx.getString(R.string.unlimited_time));
                        } else {
                            tv_count3.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                            ReTextUtils.changeLastTextColor(tv_count3, cell, color);
                        }
                    }
                }


                if(finalBaseExpired) {
                    _purchaseView.setVisibility(View.VISIBLE);
                    _expiredStatusView.setVisibility(View.VISIBLE);
                    _extendView.setVisibility(View.GONE);
                    _upgradeView.setVisibility(View.GONE);

                    _purchaseView.setOnClickListener(view2 -> {
                        //购买
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", SetMealListActivity.TYPE_BASE);
                        view.getContext().startActivity(intent);
                    });

                } else {
                    _purchaseView.setVisibility(View.GONE);
                    _extendView.setVisibility(View.VISIBLE);
                    _upgradeView.setVisibility(View.VISIBLE);

                    _extendView.setOnClickListener(view2 -> {
                        //续费
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", SetMealListActivity.TYPE_BASE);
                        view.getContext().startActivity(intent);
                    });

                    _upgradeView.setOnClickListener(view2 -> {
                        //升级
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", SetMealListActivity.TYPE_UPDATE);
                        view.getContext().startActivity(intent);
                    });
                }


            }

            vip_expired_time.setText(baseTime);
        });

        //次卡包
        tv_cord.setOnClickListener(view ->

        {
            nowSelect = 3;

            tv_rewo.setTextSize(16);
            tv_service.setTextSize(16);
            tv_cord.setTextSize(18);
            tv_record.setTextSize(16);

            tv_rewo.setTypeface(null, Typeface.NORMAL);
            tv_service.setTypeface(null, Typeface.NORMAL);
            tv_cord.setTypeface(null, Typeface.BOLD_ITALIC);
            tv_record.setTypeface(null, Typeface.NORMAL);

            tv_cord.setTextColor(ColorUtil.parseColor("#3D3D3D"));
            tv_service.setTextColor(ColorUtil.parseColor("#999999"));
            tv_rewo.setTextColor(ColorUtil.parseColor("#999999"));
            tv_record.setTextColor(ColorUtil.parseColor("#999999"));
            for(int i = 0; i < data.data.size(); i++) {
                if(data.data.get(i).packageType == 3 && data.data.get(i).countType == 1) {
                    tv_count1.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                } else if(data.data.get(i).packageType == 3 && data.data.get(i).countType == 2) {
                    tv_count2.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                } else if(data.data.get(i).packageType == 3 && data.data.get(i).countType == 3) {
                    tv_count3.setText((data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell);
                }
            }
            int color = ContextCompat.getColor(_ctx, R.color.color_3D3D3D);
            ReTextUtils.changeLastTextColor(tv_count1, cell, color);
            ReTextUtils.changeLastTextColor(tv_count2, cell, color);
            ReTextUtils.changeLastTextColor(tv_count3, cell, color);

            vip_expired_time.setText(_ctx.getString(R.string.unlimited_duration));
            group_big.setVisibility(View.GONE);

            _expiredStatusView.setVisibility(View.GONE);
            if(finalHasBase || finalHasPro) {
                _purchaseView.setVisibility(View.GONE);
                _extendView.setVisibility(View.GONE);
                _upgradeView.setVisibility(View.GONE);
            }
        });

        tv_record.setOnClickListener(view ->
        {
            nowSelect = 4;
            tv_rewo.setTextSize(16);
            tv_service.setTextSize(16);
            tv_cord.setTextSize(16);
            tv_record.setTextSize(18);
            tv_rewo.setTypeface(null, Typeface.NORMAL);
            tv_service.setTypeface(null, Typeface.NORMAL);
            tv_cord.setTypeface(null, Typeface.NORMAL);
            tv_record.setTypeface(null, Typeface.BOLD_ITALIC);
            tv_cord.setTextColor(ColorUtil.parseColor("#999999"));
            tv_service.setTextColor(ColorUtil.parseColor("#999999"));
            tv_rewo.setTextColor(ColorUtil.parseColor("#999999"));
            tv_record.setTextColor(ColorUtil.parseColor("#3D3D3D"));
            for(int i = 0; i < data.data.size(); i++) {
                if(data.data.get(i).packageType == 4) {
                    String count = (data.data.get(i).totalCount - data.data.get(i).useCount) + "/" + data.data.get(i).totalCount + cell;
                    if(data.data.get(i).countType == 1) {
                        tv_count1.setText(count);
                    } else if(data.data.get(i).countType == 2) {
                        tv_count2.setText(count);
                    } else if(data.data.get(i).countType == 3) {
                        tv_count3.setText(count);
                    }
                }
            }
            int color = ContextCompat.getColor(_ctx, R.color.color_3D3D3D);
            ReTextUtils.changeLastTextColor(tv_count1, cell, color);
            ReTextUtils.changeLastTextColor(tv_count2, cell, color);
            ReTextUtils.changeLastTextColor(tv_count3, cell, color);

            vip_expired_time.setText(recordTime);
            group_big.setVisibility(View.GONE);

            _expiredStatusView.setVisibility(View.GONE);
            if(finalHasBase || finalHasPro) {
                _purchaseView.setVisibility(View.GONE);
                _extendView.setVisibility(View.GONE);
                _upgradeView.setVisibility(View.GONE);
            }
        });

        tv_rewo.setVisibility(View.GONE);
        tv_service.setVisibility(View.GONE);
        tv_cord.setVisibility(View.GONE);
        tv_record.setVisibility(View.GONE);


        if(hasPack) {//有大礼包 没有基础服务
            tv_rewo.setVisibility(View.VISIBLE);
            if(nowSelect == 0 || nowSelect == 1) {
                tv_rewo.setTextColor(ColorUtil.parseColor("#3D3D3D"));
                nowSelect = 1;
                tv_rewo.performClick();
            }
        }


        if(hasBase || hasPro) {//有基础服务
            tv_service.setVisibility(View.VISIBLE);
            if(nowSelect == 0 || nowSelect == 2) {
                tv_service.setTextColor(ColorUtil.parseColor("#3D3D3D"));
                nowSelect = 2;
                tv_service.performClick();
            }

            if(nowSelect == 1 && !hasPack) {
                tv_service.setTextColor(ColorUtil.parseColor("#3D3D3D"));
                nowSelect = 2;
                tv_service.performClick();
            }
        }


        if(hasCard) {//有次卡包
            tv_cord.setVisibility(View.VISIBLE);

            if(nowSelect == 0 || nowSelect == 3) {
                tv_cord.setTextColor(ColorUtil.parseColor("#3D3D3D"));
                nowSelect = 3;
                tv_cord.performClick();
            }

            if(nowSelect == 1 && !hasPack) {
                if(!hasBase) {
                    nowSelect = 3;
                    tv_cord.performClick();
                }
            } else if(nowSelect == 2 && !hasBase) {
                nowSelect = 3;
                tv_cord.performClick();
            }
        }
        if(hasRecordPack) {//有录制大礼包
            tv_record.setVisibility(View.VISIBLE);

            if(nowSelect == 0 || nowSelect == 4) {
                tv_record.setTextColor(ColorUtil.parseColor("#3D3D3D"));
                nowSelect = 4;
                tv_record.performClick();
            }

            if(nowSelect == 1 && !hasPack) {
                if(!hasBase) {
                    nowSelect = 4;
                    tv_record.performClick();
                }
            } else if(nowSelect == 2 && !hasBase) {
                nowSelect = 4;
                tv_record.performClick();
            }
        }
    }

    private void showUserService(UserPackageCountBean data) {
        tv_select_device.setVisibility(View.VISIBLE);
        tv_select_device_left.setVisibility(View.VISIBLE);
        if (data == null || data.data == null || data.data.isEmpty() || data.data.get(0) == null) {
            userPackageCountAdapter.setData(null);
            return;
        }

        // 先设置时间
        if (data.data.get(0).expireTime == null) {
            itemView.findViewById(R.id.special_vip_time_info).setVisibility(View.GONE);
        }else{
            itemView.findViewById(R.id.special_vip_time_info).setVisibility(View.VISIBLE);
            TextView tvTimeInfo = itemView.findViewById(R.id.special_vip_expired_time);
            tvTimeInfo.setText(data.data.get(0).expireTime);
            itemView.findViewById(R.id.special_vip_upgrade).setOnClickListener(view -> {
                //续费
                UserDeviceTypeManager.getInstance().setLastOrderID("");
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 1);
                view.getContext().startActivity(intent);
            });
        }

        // 设置服务名称
        itemView.findViewById(R.id.special_tv_base).setVisibility(View.VISIBLE);
        TextView tvBase = (TextView) itemView.findViewById(R.id.special_tv_base);
        tvBase.setText(data.data.get(0).countName);
        // 设置服务升级
        if ("1".equals(data.data.get(0).packageType)) {
            itemView.findViewById(R.id.special_pro_upgrade).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.special_pro_upgrade).setOnClickListener(view -> {
                //续费
                UserDeviceTypeManager.getInstance().setLastOrderID("");
                Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                intent.putExtra("type", 1);
                view.getContext().startActivity(intent);
            });

        }else{
            itemView.findViewById(R.id.special_pro_upgrade).setVisibility(View.GONE);
        }

        if (data.data.get(0).children != null) {
            itemView.findViewById(R.id.special_service_item).setVisibility(View.VISIBLE);
            List<UserPackageCountBean.UserPackageData> children = data.data.get(0).children;
            userPackageCountAdapter.setData(children);
        }else{
            itemView.findViewById(R.id.special_service_item).setVisibility(View.GONE);
            userPackageCountAdapter.setData(null);
        }
    
    }



    private void initData() {
        final int requestVersion = ++packageRequestVersion;
        initDefaultView();
        String deviceId = UserDeviceTypeManager.getInstance().getDeviceID();
        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();


        Log.d("asx","asx+" + deviceId);
        //  6 移动基础/专业版；7 固定商用版 当前这个设备是6
        tv_select_device.setVisibility(View.VISIBLE);
        tv_select_device_left.setVisibility(View.VISIBLE);



        List<BindDeviceBean> bindDeviceList = DeviceManager.getBindDeviceList();

        if (deviceId.isEmpty() && !bindDeviceList.isEmpty()) {
            deviceId = bindDeviceList.get(0).getDeviceId();
        }

        String currentName = "";
        for(BindDeviceBean deviceBean : bindDeviceList) {
            if (deviceBean.getDeviceId().equals(deviceId)) {
                deviceType = deviceBean.getDeviceType();
                tv_select_device.setText(deviceBean.getDeviceName());
                if (deviceBean.getDeviceName() != null) {
                    currentName = deviceBean.getDeviceName();
                }
            }
        }

        // 固定场馆不展示套餐，也不请求套餐数据。
        if (currentName.isEmpty() || "4".equals(deviceType)) {
            DialogUtil.dismissDialog();
            return;
        }

        if ("6".equals(deviceType) || "7".equals(deviceType)) {
            FormBody.Builder builder = new FormBody.Builder();
            builder.add("deviceId", deviceId);
            OkHttpUtil.postFormData(SdkApi.package_query_all_v2(), builder, null,
                    new OkHttpCallbackDecode<UserPackageCountBean>(UserPackageCountBean.class) {
                        @Override
                        public void onDataSuccess(UserPackageCountBean data) {

                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                try {
                                    DialogUtil.dismissDialog();
                                    initDefaultView();
                                    showUserService(data);
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                DialogUtil.dismissDialog();
                                initDefaultView();
                                tv_select_device.setVisibility(View.VISIBLE);
                                tv_select_device_left.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onFinish() {
                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                DialogUtil.dismissDialog();
                            });
                        }
                    });

        }else{
            HashMap<String, String> paramsdata = new HashMap<>();
            paramsdata.put("deviceId", deviceId);
            OkHttpUtil.postData(SdkApi.package_query_all(), paramsdata, null,
                    new OkHttpCallbackDecode<UserSetMealBean>(UserSetMealBean.class) {
                        @Override
                        public void onDataSuccess(UserSetMealBean data) {

                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                try {
                                    DialogUtil.dismissDialog();
                                    initDefaultView();
                                    showService(data);


                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                DialogUtil.dismissDialog();
                                initDefaultView();
                                tv_select_device.setVisibility(View.VISIBLE);
                                tv_select_device_left.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onFinish() {
                            MainHandler.runOnUiThread(() -> {
                                if (requestVersion != packageRequestVersion) return;
                                DialogUtil.dismissDialog();
                            });
                        }
                    });

        }


    }

    private void initDefaultView() {
        itemView.findViewById(R.id.vip_time_info).setVisibility(View.GONE);
        _purchaseView.setVisibility(View.GONE);
        _extendView.setVisibility(View.GONE);
        _upgradeView.setVisibility(View.GONE);
        itemView.findViewById(R.id.service_item).setVisibility(View.GONE);
        iv_vip_service.setVisibility(View.GONE);
        group_big.setVisibility(View.GONE);
        _expiredStatusView.setVisibility(View.GONE);
        tv_rewo.setVisibility(View.GONE);
        tv_service.setVisibility(View.GONE);
        tv_cord.setVisibility(View.GONE);
        tv_record.setVisibility(View.GONE);
        tv_select_device.setVisibility(View.GONE);
        tv_select_device_left.setVisibility(View.GONE);

        // 设置特殊
        itemView.findViewById(R.id.special_vip_time_info).setVisibility(View.GONE);
        itemView.findViewById(R.id.special_tv_base).setVisibility(View.GONE);
        itemView.findViewById(R.id.special_tv_pro).setVisibility(View.GONE);
        itemView.findViewById(R.id.special_tv_fix).setVisibility(View.GONE);
        itemView.findViewById(R.id.special_pro_upgrade).setVisibility(View.GONE);
        
        itemView.findViewById(R.id.special_service_item).setVisibility(View.GONE);
        userPackageCountAdapter.setData(null);
//        itemView.findViewById(R.id.group_special_view_item1).setVisibility(View.VISIBLE);
//        itemView.findViewById(R.id.group_special_view_item2).setVisibility(View.VISIBLE);
//        itemView.findViewById(R.id.group_special_view_item3).setVisibility(View.VISIBLE);
//
//
    

    }

    @Override
    public void onDestroy() {
        ++packageRequestVersion;
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReload(LoginEvent event) {
        updateProfile();
    }


    @Override
    public void onBind(final MeModuleBean model, int position) {

        // update user profile
        updateProfile();
    }

    private void updateProfile() {
        ++packageRequestVersion;
        // get login info
        UserInfoBean loginInfo = LoginManager.getUserLoginInfo();
        if(loginInfo != null) {
            // if temp account, use default avatar builtin
            // if not, load avatar
            if(!LoginManager.isSignedIn()) {
                // avatar
                GlideUtil.loadCircleWithBorder(_ctx, R.mipmap.default_avatar, _avatarView, DensityUtil.dip2px(2), Color.WHITE);

                // name
                _nameLabel.setText(_ctx.getString(R.string.click_login));
                initDefaultView();
            } else {
                // avatar
                if(TextUtils.isEmpty(loginInfo.avatar)) {
                    GlideUtil.loadCircleWithBorder(_ctx, R.mipmap.default_avatar, _avatarView, DensityUtil.dip2px(2), Color.WHITE);
                } else {
                    GlideUtil.loadCircleWithBorder(_ctx, loginInfo.avatar, _avatarView, DensityUtil.dip2px(2), Color.WHITE);
                }
                // name
                String mobile = loginInfo.phone;
                String nickName = loginInfo.nickName;
                if(TextUtils.isEmpty(nickName)) {
                    if(mobile.length() == 11) {
                        mobile = mobile.substring(0, 3) + "****" + mobile.substring(7);
                    } else {
                        mobile = mobile.substring(0, 11);
                    }
                    _nameLabel.setText(mobile);
                } else {
                    _nameLabel.setText(nickName);
                }

                String deviceIdLast = CameraManager.getSaveDeviceIdLast();

                List<BindDeviceBean> list = DeviceManager.getBindDeviceList();

                AppTrace.d("me", "deviceIdLast " + deviceIdLast);
                if(!TextUtils.isEmpty(deviceIdLast)) {
                    initData();
                } else {
                    if(ListUtils.isValid(list)) {
                        CameraManager.setDeviceId(list.get(0).getDeviceId());
                        initData();
                    } else {
                        initDefaultView();
                    }
                }
            }
        } else {
            // avatar
            GlideUtil.loadCircleWithBorder(_ctx, R.mipmap.default_avatar, _avatarView, DensityUtil.dip2px(2), Color.WHITE);
            // name
            _nameLabel.setText(_ctx.getString(R.string.click_login));
            initDefaultView();
        }
    }

}
