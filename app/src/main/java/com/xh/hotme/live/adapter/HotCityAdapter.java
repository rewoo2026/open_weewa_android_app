package com.xh.hotme.live.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.HotCityBean;

import java.util.ArrayList;
import java.util.List;


/**
 * @Author: zhangtuan
 * @CreateDate: 2023/12/27
 * @Description: 添加直播推流设置
 */
public class HotCityAdapter extends RecyclerView.Adapter<HotCityAdapter.IViewHolder> {

    private final List<HotCityBean> mList = new ArrayList<>();


    public void setList(List<HotCityBean> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public IViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_item_hot_city, parent, false);

        return new HotCityAdapter.IViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IViewHolder holder, @SuppressLint("RecyclerView") int position) {


        holder.tv_title.setText(mList.get(position).cityDisplayName);

        holder.tv_title.setOnClickListener(view -> {

            HotCityBean dataBean = mList.get(position);
            if(commonListener != null) {
                commonListener.onReturn(2, dataBean);
            }

        });
    }


    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }


    class IViewHolder extends RecyclerView.ViewHolder {

        final TextView tv_title;

        public IViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tvHotCity);

        }
    }


    IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }
}
