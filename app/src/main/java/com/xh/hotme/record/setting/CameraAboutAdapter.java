package com.xh.hotme.record.setting;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.me.holder.CommonViewHolder;

import java.util.ArrayList;
import java.util.List;

public class CameraAboutAdapter extends RecyclerView.Adapter<CommonViewHolder> {
    private List<CameraAboutModuleBean> _models;
    private final Activity mContext;


    public CameraAboutAdapter(Activity ctx) {
        mContext = ctx;
        _models = new ArrayList<>();

    }

    public void setModuleList(List<CameraAboutModuleBean> l) {
        if(_models == null) {
            _models = new ArrayList<>();
        }
        _models.clear();

        _models.addAll(l);
    }

    public List<CameraAboutModuleBean> getModuleList() {
        return _models;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return _models == null ? 0 : _models.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public CommonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = LayoutInflater.from(mContext).inflate(R.layout.camera_about_item_content, parent, false);
        return new CameraAboutModule(mContext, convertView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int pos) {
        CommonViewHolder vh = holder;

        vh.onBind(_models.get(pos), pos);
    }


    public class CameraAboutModule extends CommonViewHolder<CameraAboutModuleBean> {
        public CameraAboutModule(Context context, View view) {
            super(view);

        }

        @Override
        public void onBind(CameraAboutModuleBean model, int position) {
            TextView titleView = itemView.findViewById(R.id.tvName);
            TextView contentView = itemView.findViewById(R.id.tv_content);
            ImageView iconView = itemView.findViewById(R.id.ic_arraw);
            titleView.setText(model.title);
            if(!TextUtils.isEmpty(model.content)) {
                contentView.setText(model.content);
            } else {
                contentView.setText("");
            }
            if(model.listener != null) {
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
            itemView.getRootView().setOnClickListener(model.listener);
        }
    }
}