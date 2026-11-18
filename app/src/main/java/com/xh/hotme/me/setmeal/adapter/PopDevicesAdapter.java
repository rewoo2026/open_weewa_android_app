package com.xh.hotme.me.setmeal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.databinding.AdapterPopDevicesBinding;
import com.xh.hotme.listener.IActionCallback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description: 设备列表
 */
public class PopDevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }

    List<BindDeviceBean> list = new ArrayList<>();

    public void setList(List<BindDeviceBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_pop_devices, parent, false);

        return new VHRecord(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        VHRecord vhRecord = (VHRecord) holder;
        Context context = vhRecord.itemView.getContext();

        vhRecord.binding.tvName.setText(list.get(position).getDeviceName());
        vhRecord.binding.tvName.setOnClickListener(view -> {
            if(commonListener != null) {
                commonListener.onReturn(1, list.get(position).getDeviceName());
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class VHRecord extends RecyclerView.ViewHolder {

        final AdapterPopDevicesBinding binding;

        public VHRecord(@NonNull @NotNull View itemView) {
            super(itemView);
            binding = AdapterPopDevicesBinding.bind(itemView);
        }
    }
}
