package com.xh.hotme.me.setmeal.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.databinding.AdapterSermealListCardBinding;
import com.xh.hotme.databinding.AdapterSermealListServiceBinding;
import com.xh.hotme.databinding.AdapterSermealListStorageBinding;
import com.xh.hotme.databinding.AdapterSermealListTitleBinding;
import com.xh.hotme.databinding.AdapterSermealListUserBinding;
import com.xh.hotme.databinding.AdapterSermealListLiveCardBinding;
import com.xh.hotme.databinding.AdapterSermealListProServiceBinding;

import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.CreateOrderBean;
import com.xh.hotme.me.setmeal.PayDetailsActivity;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import com.xh.hotme.me.setmeal.bean.SetMealListBean;
import com.xh.hotme.me.setmeal.dialog.StartPayDialog;
import com.xh.hotme.me.setmeal.utils.SetMealPayUtils;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.ReTextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import com.xh.hotme.me.UserDeviceTypeManager;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description:
 */
public class SetMealListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<SetMealListBean> listBeans;
    final Context _ctx;

    public SetMealListAdapter(Context context) {
        _ctx = context;
    }

    public void setListBeans(List<SetMealListBean> listBeans) {
        this.listBeans = listBeans;
        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
//        View view = null;
//        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_storage, parent, false);
//
//        return new StorageHolder(view);

        View view = null;
        if(viewType == SetMealListBean.TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_user, parent, false);

            return new UserHolder(view);
        } else if(viewType == SetMealListBean.TYPE_TITLE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_title, parent, false);
            return new TitleHolder(view);
        } else if(viewType == SetMealListBean.TYPE_CONTENT_SERVICE) {
            //套餐
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_service, parent, false);
            return new ItemServiceHolder(view);
        } else if(viewType == SetMealListBean.TYPE_STORAGE) { // 添加新类型判断
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.adapter_sermeal_list_storage, parent, false);
            return new StorageHolder(view);
        } else if (viewType == SetMealListBean.TYPE_CONTENT_LIVE_CARD) { // 添加新类型判断
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.adapter_sermeal_list_live_card, parent, false);
            return new ItemLiveCardHolder(view);
        }  else if (viewType == SetMealListBean.TYPE_CONTENT_PRO_SERVICE) { // 添加新类型判断
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.adapter_sermeal_list_pro_service, parent, false);
            return new ItemProServiceHolder(view);
        } else {
            //次卡包
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_card, parent, false);
            return new ItemCardHolder(view);
        }

        // 这里需要添加直播次卡包 和 云存储

    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {

        SetMealListBean serMealListBean = listBeans.get(position);
        String cell = _ctx.getString(R.string.session);
        String unlimitStr = _ctx.getString(R.string.unlimited_time);
        if(holder instanceof UserHolder) {
            UserHolder userHolder = (UserHolder) holder;
            UserInfoBean loginInfo = LoginManager.getUserLoginInfo();
            if(TextUtils.isEmpty(loginInfo.avatar)) {
                GlideUtil.loadCircleWithBorder(_ctx, R.mipmap.default_avatar, userHolder.binding.avatar, DensityUtil.dip2px(2), Color.WHITE);
            } else {
                GlideUtil.loadCircleWithBorder(_ctx, loginInfo.avatar, userHolder.binding.avatar, DensityUtil.dip2px(2), Color.WHITE);
            }
            String mobile = loginInfo.phone;
            String nickName = loginInfo.nickName;
            if(TextUtils.isEmpty(nickName)) {
                if(mobile.length() == 11) {
                    mobile = mobile.substring(0, 3) + "****" + mobile.substring(7);
                } else {
                    mobile = mobile.substring(0, 11);
                }
                userHolder.binding.name.setText(mobile);
            } else {
                userHolder.binding.name.setText(nickName);
            }

            if(serMealListBean.packageType == 0) {
                userHolder.binding.vipService.setImageResource(R.mipmap.me_service_normal);
            } else if(serMealListBean.packageType == 1) {
                userHolder.binding.vipService.setImageResource(R.mipmap.me_service_base);
            } else if(serMealListBean.packageType == 2) {
                userHolder.binding.vipService.setImageResource(R.mipmap.me_service_pro);
            }

            userHolder.binding.vipExpiredTime.setText(serMealListBean.title);
        } else if(holder instanceof TitleHolder) {
            TitleHolder titleHolder = (TitleHolder) holder;
            titleHolder.binding.tvTitle.setText(serMealListBean.title);

        } else if(holder instanceof ItemServiceHolder) {
            ItemServiceHolder itemServiceHolder = (ItemServiceHolder) holder;
            itemServiceHolder.binding.tvBuy.setText(serMealListBean.btn);

            int color = ContextCompat.getColor(_ctx, R.color.orange);


            if(serMealListBean.dataBean.packageType.equals("2")) {
                itemServiceHolder.binding.ivLeft11.setVisibility(View.VISIBLE);
                itemServiceHolder.binding.tvBigRewo.setVisibility(View.VISIBLE);
                itemServiceHolder.binding.tvBigRewo.setText(serMealListBean.dataBean.bigDataCount +  cell +  _ctx.getString(R.string.rewoo_big_data));
                ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvBigRewo,
                    serMealListBean.dataBean.bigDataCount + cell, color);
            } else {
                itemServiceHolder.binding.ivLeft11.setVisibility(View.GONE);
                itemServiceHolder.binding.tvBigRewo.setVisibility(View.GONE);
            }

            itemServiceHolder.binding.tvTitle.setText(serMealListBean.dataBean.packageName);
            itemServiceHolder.binding.tvMoney2.setText(getPrice(serMealListBean.dataBean.packagePrice));
            if(serMealListBean.dataBean.packageType.equals("2")) {
                if(serMealListBean.dataBean.videoCount == null) {
                    itemServiceHolder.binding.tvRecord.setText(_ctx.getString(R.string.service_unlimit_yimdc));
                    ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvRecord,
                        itemServiceHolder.binding.tvRecord.getText().toString(), _ctx.getString(R.string.service_unlimit_yimdc_btn), color);
                } else {
                    itemServiceHolder.binding.tvRecord.setText(serMealListBean.dataBean.videoCount + cell + _ctx.getString(R.string.competition_record));
                    ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvRecord,
                        itemServiceHolder.binding.tvRecord.getText().toString(), serMealListBean.dataBean.videoCount + cell, color);
                }
            } else {

                if(serMealListBean.dataBean.videoCount == null) {
                    itemServiceHolder.binding.tvRecord.setText(unlimitStr +  _ctx.getString(R.string.competition_record));
                    ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvRecord,
                        itemServiceHolder.binding.tvRecord.getText().toString(),
                            unlimitStr, color);
                } else {
                    itemServiceHolder.binding.tvRecord.setText(serMealListBean.dataBean.videoCount + cell + _ctx.getString(R.string.competition_record));
                    ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvRecord,
                        itemServiceHolder.binding.tvRecord.getText().toString(), serMealListBean.dataBean.videoCount + cell, color);
                }
            }

            if(serMealListBean.dataBean.liveCount == null) {
                itemServiceHolder.binding.tvLive.setText(unlimitStr +  _ctx.getString(R.string.competition_live));
                ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvLive,
                    itemServiceHolder.binding.tvLive.getText().toString(), unlimitStr, color);

            } else {
                itemServiceHolder.binding.tvLive.setText(serMealListBean.dataBean.liveCount + cell + _ctx.getString(R.string.competition_live));
                ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvLive,
                    itemServiceHolder.binding.tvLive.getText().toString(), serMealListBean.dataBean.liveCount + cell, color);

            }


            if(serMealListBean.dataBean.storeCount == null) {

                itemServiceHolder.binding.tvSave.setText(unlimitStr +  _ctx.getString(R.string.cloud_storage));
                ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvSave,
                    itemServiceHolder.binding.tvSave.getText().toString(), unlimitStr, color);

            } else {

                itemServiceHolder.binding.tvSave.setText(serMealListBean.dataBean.storeCount  + cell + _ctx.getString(R.string.cloud_storage));
                ReTextUtils.changeLastTextColor(itemServiceHolder.binding.tvSave,
                    itemServiceHolder.binding.tvSave.getText().toString(), serMealListBean.dataBean.storeCount + cell, color);

            }


            itemServiceHolder.binding.tvBuy.setOnClickListener(view -> {

                StartPayDialog startPayDialog = new StartPayDialog(view.getContext());
                startPayDialog.show();

                SetMealPayUtils.createOrder(serMealListBean.dataBean.packageId, serMealListBean.isBaseToPro, (code, objects) -> {

                    startPayDialog.dismiss();

                    if(code == IActionCallback.CALLBACK_ACTION_OK) {
                        CreateOrderBean createOrderBean = (CreateOrderBean) objects[0];
                        Intent intent = new Intent(view.getContext(), PayDetailsActivity.class);
                        intent.putExtra("bean", createOrderBean);
                        view.getContext().startActivity(intent);
                    }


                });

            });

        } else if(holder instanceof ItemCardHolder) {
            ItemCardHolder itemCardHolder = (ItemCardHolder) holder;

            itemCardHolder.binding.tvTitle.setText(serMealListBean.dataBean.packageName);
            itemCardHolder.binding.tvMoney2.setText(getPrice(serMealListBean.dataBean.packagePrice));
            itemCardHolder.binding.tvRecord.setText(serMealListBean.dataBean.videoCount + cell + _ctx.getString(R.string.competition_record));

            int color = ContextCompat.getColor(_ctx, R.color.orange);
            ReTextUtils.changeLastTextColor(itemCardHolder.binding.tvRecord,
                itemCardHolder.binding.tvRecord.getText().toString(), serMealListBean.dataBean.videoCount + cell, color);

            if(serMealListBean.dataBean.liveCount != null && !serMealListBean.dataBean.liveCount.equals("0")) {
                itemCardHolder.binding.ivLeft3.setVisibility(View.VISIBLE);
                itemCardHolder.binding.constraintLayoutLive.setVisibility(View.VISIBLE);
                itemCardHolder.binding.tvLive.setText(serMealListBean.dataBean.liveCount + cell + _ctx.getString(R.string.competition_live));
                ReTextUtils.changeLastTextColor(itemCardHolder.binding.tvLive,
                    itemCardHolder.binding.tvLive.getText().toString(), serMealListBean.dataBean.liveCount + cell, color);

            } else {
                itemCardHolder.binding.constraintLayoutLive.setVisibility(View.GONE);
            }


            itemCardHolder.binding.tvSave.setText(serMealListBean.dataBean.storeCount + cell + _ctx.getString(R.string.cloud_storage));
            ReTextUtils.changeLastTextColor(itemCardHolder.binding.tvSave,
                itemCardHolder.binding.tvSave.getText().toString(), serMealListBean.dataBean.storeCount + cell, color);

            itemCardHolder.binding.tvBuy.setOnClickListener(view -> {

                StartPayDialog startPayDialog = new StartPayDialog(view.getContext());
                startPayDialog.show();
                SetMealPayUtils.createOrder(serMealListBean.dataBean.packageId, serMealListBean.isBaseToPro,
                    (code, objects) -> {
                        startPayDialog.dismiss();
                        if(code == IActionCallback.CALLBACK_ACTION_OK) {
                            CreateOrderBean createOrderBean = (CreateOrderBean) objects[0];
                            Intent intent = new Intent(view.getContext(), PayDetailsActivity.class);
                            intent.putExtra("bean", createOrderBean);
                            view.getContext().startActivity(intent);
                        }
                    });
            });
        } else if(holder instanceof StorageHolder) {
            StorageHolder storageHolder = (StorageHolder) holder;
            SetMealListStorageItemAdapter adapter = new SetMealListStorageItemAdapter(_ctx);
            storageHolder.binding.rvStorageList.setLayoutManager(new LinearLayoutManager(_ctx));
            storageHolder.binding.rvStorageList.setAdapter(adapter);

            adapter.setData(serMealListBean.storageBeans);



        } else if(holder instanceof ItemLiveCardHolder) {
            ItemLiveCardHolder itemLiveCardHolder = (ItemLiveCardHolder) holder;
            // 设置数据
            itemLiveCardHolder.binding.tvTitle.setText(serMealListBean.dataBean2.packageName);
           itemLiveCardHolder.binding.tvMoney2.setText(getPrice(String.valueOf(serMealListBean.dataBean2.packagePrice)));
//            // 设置直播次数
           itemLiveCardHolder.binding.tvLive.setText(String.valueOf(serMealListBean.dataBean2.liveCount));
           String packageId = serMealListBean.dataBean2.packageId;

            

            // 添加按钮点击事件
            itemLiveCardHolder.binding.tvBuy.setOnClickListener(view -> {
                StartPayDialog startPayDialog = new StartPayDialog(view.getContext());
                startPayDialog.show();
                SetMealPayUtils.createOrder(packageId, serMealListBean.isBaseToPro, (code, objects) -> {
                    startPayDialog.dismiss();

                    if(code == IActionCallback.CALLBACK_ACTION_OK) {
                        CreateOrderBean createOrderBean = (CreateOrderBean) objects[0];
                        Intent intent = new Intent(view.getContext(), PayDetailsActivity.class);
                        intent.putExtra("bean", createOrderBean);
                        view.getContext().startActivity(intent);
                    }
                });
            });
        
        }else if (holder instanceof ItemProServiceHolder) {
            ItemProServiceHolder itemProServiceHolder = (ItemProServiceHolder) holder;
            // 设置数据
            itemProServiceHolder.binding.tvTitle.setText(serMealListBean.dataBean2.packageName);
            itemProServiceHolder.binding.tvMoney2.setText(getPrice(String.valueOf(serMealListBean.dataBean2.packagePrice)));
            // 设置直播次数
            // tv_record

            itemProServiceHolder.binding.tvLive.setText(String.valueOf(serMealListBean.dataBean2.liveCount));
            itemProServiceHolder.binding.tvLiveText.setText(_ctx.getString(R.string.hour)+ _ctx.getString(R.string.competition_live));
            // 设置按钮
//            if (serMealListBean.dataBean2.enabled == 1) {
//                itemProServiceHolder.binding.tvBuy.setVisibility(View.VISIBLE);
//                itemProServiceHolder.binding.tvNotBuy.setVisibility(View.GONE);
//                // 添加按钮点击事件
//                itemProServiceHolder.binding.tvBuy.setOnClickListener(view -> {
//                    StartPayDialog startPayDialog = new StartPayDialog(view.getContext());
//                    startPayDialog.show();
//                    SetMealPayUtils.createOrder(serMealListBean.dataBean2.packageId, serMealListBean.isBaseToPro, (code, objects) -> {
//                        startPayDialog.dismiss();
//
//                        if(code == IActionCallback.CALLBACK_ACTION_OK) {
//                            CreateOrderBean createOrderBean = (CreateOrderBean) objects[0];
//                            Intent intent = new Intent(view.getContext(), PayDetailsActivity.class);
//                            intent.putExtra("bean", createOrderBean);
//                            view.getContext().startActivity(intent);
//                        }
//                    });
//                });
//            } else {
//                itemProServiceHolder.binding.tvBuy.setVisibility(View.GONE);
//                itemProServiceHolder.binding.tvNotBuy.setVisibility(View.VISIBLE);
//            }
            


        }
    }

    public String getPrice(String price) {
        return com.xh.hotme.me.setmeal.utils.OrderAmount.packagePrice(price);
    }


    @Override
    public int getItemViewType(int position) {
        return listBeans.get(position).type;
    }

    public class UserHolder extends RecyclerView.ViewHolder {

        final AdapterSermealListUserBinding binding;

        public UserHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListUserBinding.bind(itemView);
        }

    }

    public class TitleHolder extends RecyclerView.ViewHolder {

        final AdapterSermealListTitleBinding binding;

        public TitleHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListTitleBinding.bind(itemView);
        }

    }

    public class ItemServiceHolder extends RecyclerView.ViewHolder {
        final AdapterSermealListServiceBinding binding;


        public ItemServiceHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListServiceBinding.bind(itemView);
        }

    }

    public class ItemCardHolder extends RecyclerView.ViewHolder {
        final AdapterSermealListCardBinding binding;


        public ItemCardHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListCardBinding.bind(itemView);
        }

    }

    // 下面是新版的
    public class ItemLiveCardHolder extends RecyclerView.ViewHolder {
        final AdapterSermealListLiveCardBinding binding;

        public ItemLiveCardHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListLiveCardBinding.bind(itemView);
        }

    }

    public class ItemProServiceHolder extends RecyclerView.ViewHolder {
        final AdapterSermealListProServiceBinding binding;

        public ItemProServiceHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListProServiceBinding.bind(itemView);
        }

    }



    public class StorageHolder extends RecyclerView.ViewHolder {
        final AdapterSermealListStorageBinding binding;

        public StorageHolder(View itemView) {
            super(itemView);
            binding = AdapterSermealListStorageBinding.bind(itemView);
        }
    }

    @Override
    public int getItemCount() {
        if(listBeans == null) {
            return 0;
        }
        return listBeans.size();
    }
}
