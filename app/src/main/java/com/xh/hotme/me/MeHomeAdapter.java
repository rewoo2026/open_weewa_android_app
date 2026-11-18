package com.xh.hotme.me;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.me.holder.CommonViewHolder;
import com.xh.hotme.me.holder.MeLoginOutHolder;
import com.xh.hotme.me.holder.MeProfileHolder;
import com.xh.hotme.me.holder.MeSettingHolder;
import com.xh.hotme.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;

public class MeHomeAdapter extends RecyclerView.Adapter<CommonViewHolder<MeModuleBean>> {
    private final List<MeModuleBean> _models;
    private final Activity mContext;

    public MeHomeAdapter(Activity ctx) {
        mContext = ctx;
        _models = new ArrayList<>();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return _models.size();
    }

    @Override
    public int getItemViewType(int position) {
        return _models.get(position).getType();
    }

    @NonNull
    @Override
    public CommonViewHolder<MeModuleBean> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

//        SetMealCheckManager.checkSetMeal(mContext);

        switch(viewType) {
            case Constants.ME_MODULE_PROFILE:
                return MeProfileHolder.create(mContext, parent);
            case Constants.ME_MODULE_SETTING:
                return MeSettingHolder.create(mContext, parent);
            default:
                return MeLoginOutHolder.create(mContext, parent);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder<MeModuleBean> holder, int pos) {
        holder.onBind(_models.get(pos), pos);
    }

    public void setModels(List<MeModuleBean> moduleList) {
        if(moduleList == null || moduleList.isEmpty()) {
            return;
        }

        _models.clear();

        _models.addAll(moduleList);

    }
}
