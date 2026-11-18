package com.xh.hotme.me.setmeal.adapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.databinding.AdapterSermealListCardBinding;
import com.xh.hotme.databinding.AdapterSermealListServiceBinding;
import com.xh.hotme.databinding.AdapterSermealListStorageBinding;
import com.xh.hotme.databinding.AdapterSermealListTitleBinding;
import com.xh.hotme.databinding.AdapterSermealListUserBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.CreateOrderBean;
import com.xh.hotme.me.setmeal.PayDetailsActivity;
import com.xh.hotme.me.setmeal.bean.SetMealListBean;
import com.xh.hotme.me.setmeal.bean.SetMealListPackageBean;
import com.xh.hotme.me.setmeal.dialog.StartPayDialog;
import com.xh.hotme.me.setmeal.utils.SetMealPayUtils;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.ReTextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import com.xh.hotme.me.UserDeviceTypeManager;


public class SetMealListStorageItemAdapter extends RecyclerView.Adapter<SetMealListStorageItemAdapter.ViewHolder> {
    private List<SetMealListPackageBean.DataBean> items;
    private Context context;

    public SetMealListStorageItemAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<SetMealListPackageBean.DataBean> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_sermeal_list_storage_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SetMealListPackageBean.DataBean item = items.get(position);

        holder.tvTitle.setText(item.packageName);
        holder.tvPrice.setText(com.xh.hotme.me.setmeal.utils.OrderAmount.packagePrice(String.valueOf(item.packagePrice))
                + context.getString(R.string.price_per_year));

        // 如果存储小于当前，按钮变灰，不能点击
        double userCount = UserDeviceTypeManager.getInstance().getUserSaveCount();
        double packageStorage = (double) item.storeCount;
        if (packageStorage <= userCount) {
            holder.btnBuy.setEnabled(false);
            holder.btnBuy.setAlpha(0.5f);

        } else {
            holder.btnBuy.setEnabled(true);
            holder.btnBuy.setAlpha(1.0f);
        }

        holder.btnBuy.setOnClickListener(v -> {
            StartPayDialog startPayDialog = new StartPayDialog(context);
            startPayDialog.show();
            SetMealPayUtils.createOrder(item.packageId, false, (code, objects) -> {
                startPayDialog.dismiss();
                if(code == IActionCallback.CALLBACK_ACTION_OK) {
                    CreateOrderBean createOrderBean = (CreateOrderBean)  objects[0];
                    Intent intent = new Intent(context, PayDetailsActivity.class);
                    intent.putExtra("bean", createOrderBean);
                    context.startActivity(intent);
                }
            },UserDeviceTypeManager.getInstance().getLastOrderID());
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvPrice;
        TextView btnBuy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_storage_icon);
            tvTitle = itemView.findViewById(R.id.tv_storage_title);
            tvPrice = itemView.findViewById(R.id.tv_storage_price);
            btnBuy = itemView.findViewById(R.id.tv_storage_buy);
        }
    }
}
