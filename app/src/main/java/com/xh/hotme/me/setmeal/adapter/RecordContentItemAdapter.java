package com.xh.hotme.me.setmeal.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.databinding.AdapterSetmealRecordContentItemBinding;
import com.xh.hotme.me.setmeal.bean.UsageRecordBean;
import com.xh.hotme.me.setmeal.records.UsageRecordDetailsActivity;
import com.xh.hotme.utils.ClickGuard;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description: 使用记录
 */
public class RecordContentItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    List<UsageRecordBean.DataBean.RowsBean> list = new ArrayList<>();

    public void setList(List<UsageRecordBean.DataBean.RowsBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_setmeal_record_content_item, parent, false);

        return new VHRecord(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        VHRecord vhRecord = (VHRecord) holder;
        Context context = vhRecord.itemView.getContext();


        if(list.get(position).usedType == 1) {
            vhRecord.binding.tvStatus.setText(context.getString(R.string.competition_record));
            vhRecord.binding.tvStatus.setBackgroundResource(R.drawable.bg_f16c2f_raduis5);
        } else if(list.get(position).usedType == 2) {
            vhRecord.binding.tvStatus.setText(context.getString(R.string.cloud_storage));
            vhRecord.binding.tvStatus.setBackgroundResource(R.drawable.bg_f1b72f_raduis5);
        } else if(list.get(position).usedType == 3) {
            vhRecord.binding.tvStatus.setText(context.getString(R.string.competition_live));
            vhRecord.binding.tvStatus.setBackgroundResource(R.drawable.bg_2a387c_raduis5);
        } else if(list.get(position).usedType == 4) {
            vhRecord.binding.tvStatus.setText(context.getString(R.string.big_data));
            vhRecord.binding.tvStatus.setBackgroundResource(R.drawable.bg_0472e8_raduis5);
        }

        vhRecord.binding.tvName.setText(list.get(position).competitionName);
        vhRecord.binding.tvTime.setText(list.get(position).createTime.replaceAll("T", " "));
        vhRecord.itemView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                Intent intent = new Intent(context, UsageRecordDetailsActivity.class);

                intent.putExtra("bean", list.get(position));
                context.startActivity(intent);
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class VHRecord extends RecyclerView.ViewHolder {

        final AdapterSetmealRecordContentItemBinding binding;

        public VHRecord(@NonNull @NotNull View itemView) {
            super(itemView);
            binding = AdapterSetmealRecordContentItemBinding.bind(itemView);
        }
    }
}
