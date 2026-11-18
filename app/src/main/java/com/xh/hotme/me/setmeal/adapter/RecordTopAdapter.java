package com.xh.hotme.me.setmeal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.databinding.AdapterSetmealRecordBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.me.setmeal.bean.RecordTopBean;
import com.xh.hotme.utils.ClickGuard;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description: 使用记录顶部
 */
public class RecordTopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    List<RecordTopBean> list = new ArrayList<>();

    public void setList(List<RecordTopBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }


    public void changeSelect(int position) {

        for(int i = 0; i < list.size(); i++) {
            list.get(i).isSelect = false;
        }
        list.get(position).isSelect = true;

        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_setmeal_record, parent, false);

        return new VHRecord(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        VHRecord vhRecord = (VHRecord) holder;

        vhRecord.binding.tv.setText(list.get(position).title);
        vhRecord.binding.tv.setSelected(list.get(position).isSelect);
        vhRecord.binding.tv.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(commonListener != null) {
                    commonListener.onReturn(position);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class VHRecord extends RecyclerView.ViewHolder {

        final AdapterSetmealRecordBinding binding;

        public VHRecord(@NonNull @NotNull View itemView) {
            super(itemView);
            binding = AdapterSetmealRecordBinding.bind(itemView);
        }
    }

    IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }
}
