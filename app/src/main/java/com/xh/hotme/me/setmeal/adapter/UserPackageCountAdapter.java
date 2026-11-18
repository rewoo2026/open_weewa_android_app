package com.xh.hotme.me.setmeal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;

import java.util.ArrayList;
import java.util.List;

import com.xh.hotme.me.UserDeviceTypeManager;
import  com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.xh.hotme.utils.ReTextUtils;


public class UserPackageCountAdapter extends RecyclerView.Adapter<UserPackageCountAdapter.ViewHolder> {
    private List<UserPackageCountBean.UserPackageData> items;
    private Context context;

    public UserPackageCountAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setData(List<UserPackageCountBean.UserPackageData> items) {
        this.items = items;
        UserPackageCountBean.UserPackageData storage = null;
        if (items != null) {
            for (UserPackageCountBean.UserPackageData item : items) {
                if (item.countType == 2) { storage = item; break; }
            }
        }
        UserDeviceTypeManager.getInstance().setStoragePackage(storage);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_package_count, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserPackageCountBean.UserPackageData item = items.get(position);
        // 根据 countType 设置不同图标
        // 1 录制；2 云存储；3 直播；4 热我大数据
        switch (item.countType) {
            case 1:  // 录制
                holder.ivIcon.setImageResource(R.mipmap.icon_item_record);
                break;
            case 2:  // 云存储
                holder.ivIcon.setImageResource(R.mipmap.icon_item_save);
                break;
            case 3:  // 赛事直播
                holder.ivIcon.setImageResource(R.mipmap.icon_item_live);
                break;
            case 4:  // 热我大数据
                holder.ivIcon.setImageResource(R.mipmap.icon_item_record);
                break;
            default:
                holder.ivIcon.setImageResource(R.mipmap.icon_item_record);
                break;
        }

                // 设置名称
                holder.tvName.setText(item.countName);
        
                // 设置到期时间
                if (item.expireTime != null) {
                    holder.expiryInfo.setVisibility(View.VISIBLE);
                    holder.tvCount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                    String expireDate = item.expireTime.split(" ")[0]; 
                    holder.tvExpiredTime.setText(expireDate);
                    holder.tvExpiredTime.setVisibility(View.VISIBLE);
                    holder.tvNumber.setVisibility(View.VISIBLE);
                } else {
                    holder.expiryInfo.setVisibility(View.GONE);
                    holder.tvCount.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    holder.tvExpiredTime.setVisibility(View.GONE);
                    holder.tvNumber.setVisibility(View.GONE);
                }
                
                // 设置剩余次数/容量
                String countText = item.remainCount + "/" + item.totalCount;
                if (item.countUnit != null) {
                    countText += " " + item.countUnit;
                    holder.tvCount.setText(countText);
                    // 设置单位文字为黑色
                    int color = ContextCompat.getColor(context, R.color.color_3D3D3D);
                    ReTextUtils.changeLastTextColor(holder.tvCount, item.countUnit, color);
                } else {
                    holder.tvCount.setText(countText);
                }

                
                // 设置升级按钮
                if (item.buttonLabel != null) {
                    holder.btnBuy.setText(item.buttonLabel);
                    holder.btnBuy.setVisibility(View.VISIBLE);
                    holder.btnBuy.setOnClickListener(view -> {
                        if (item.countType == 2) {
                            UserDeviceTypeManager.getInstance().setStoragePackage(item);
                        } else {
                            UserDeviceTypeManager.getInstance().setLastOrderID("");
                        }
                        Intent intent = new Intent(view.getContext(), SetMealListActivity.class);
                        intent.putExtra("type", 1);
                        view.getContext().startActivity(intent);
                    });
                } else {
                    holder.btnBuy.setVisibility(View.GONE);
                }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber;
        TextView tvName;
        TextView tvExpiredTime;
        TextView tvCount;
        TextView btnBuy;
        ImageView ivIcon;
        View expiryInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_bottom_number);
            expiryInfo = itemView.findViewById(R.id.special_expiry_info);
            tvName = itemView.findViewById(R.id.special_tv_name1);
            tvExpiredTime = itemView.findViewById(R.id.special_expired_time1);
            tvCount = itemView.findViewById(R.id.special_tv_count1);
            btnBuy = itemView.findViewById(R.id.special_view_item1_buy);
            ivIcon = itemView.findViewById(R.id.special_iv_item_icon_1);
        }
    }
}
