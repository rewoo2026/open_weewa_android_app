package com.xh.hotme.me.setmeal.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.databinding.AdapterSetmealRecordContentBinding;
import com.xh.hotme.me.setmeal.bean.RecordContentBean;
import com.xh.hotme.utils.ClickGuard;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description: 使用记录
 */
public class RecordContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    List<RecordContentBean> list = new ArrayList<>();

    public void setList(List<RecordContentBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }


    public void retract(int position) {
        list.get(position).retract = !list.get(position).retract;
        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_setmeal_record_content, parent, false);

        return new VHRecord(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        VHRecord vhRecord = (VHRecord) holder;
        Context context = vhRecord.itemView.getContext();

        String cell = context.getString(R.string.times);
        String use = context.getString(R.string.use);

        if(list.get(position).retract) {
            Drawable drawable = context.getResources().getDrawable(R.mipmap.icon_arrow_bottom);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            vhRecord.binding.tvMonth.setCompoundDrawables(null, null, drawable, null);
            vhRecord.binding.rv.setVisibility(View.GONE);
        } else {
            Drawable drawable = context.getResources().getDrawable(R.mipmap.icon_arrow_bottom2);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            vhRecord.binding.tvMonth.setCompoundDrawables(null, null, drawable, null);
            vhRecord.binding.rv.setVisibility(View.VISIBLE);
        }

        vhRecord.binding.tvMonth.setText(list.get(position).title + context.getString(R.string.month));


        int count1 = 0;
        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        for(int i = 0; i < list.get(position).list.size(); i++) {
            if(list.get(position).list.get(i).usedType == 1) {
                count1++;
            } else if(list.get(position).list.get(i).usedType == 2) {
                count2++;
            } else if(list.get(position).list.get(i).usedType == 3) {
                count3++;
            } else if(list.get(position).list.get(i).usedType == 4) {
                count4++;
            }
        }
        StringBuilder str = new StringBuilder();
        if(count1 != 0) {
            str.append(use).append(context.getString(R.string.competition_record)).append(count1).append(cell);
        }

        if(count2 != 0) {
            if(str.length() == 0) {
                str.append(use);
            } else {
                str.append(",");
            }
            str.append(context.getString(R.string.cloud_storage)).append(count2).append(cell);
        }

        if(count3 != 0) {
            if(str.length() == 0) {
                str.append(use);
            } else {
                str.append(",");
            }
            str.append(context.getString(R.string.competition_live)).append(count3).append(cell);
        }

        if(count4 != 0) {
            if(str.length() == 0) {
                str.append(use);
            } else {
                str.append(",");
            }
            str.append(context.getString(R.string.big_data)).append(count4).append(context.getString(R.string.times));
        }

        vhRecord.binding.tvHint.setText(str.toString());


        vhRecord.binding.tvMonth.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                retract(position);
                return true;
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        vhRecord.binding.rv.setLayoutManager(linearLayoutManager);
        RecordContentItemAdapter recordContentItemAdapter = new RecordContentItemAdapter();
        vhRecord.binding.rv.setAdapter(recordContentItemAdapter);
        recordContentItemAdapter.setList(list.get(position).list);


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class VHRecord extends RecyclerView.ViewHolder {

        final AdapterSetmealRecordContentBinding binding;

        public VHRecord(@NonNull @NotNull View itemView) {
            super(itemView);
            binding = AdapterSetmealRecordContentBinding.bind(itemView);
        }
    }
}
