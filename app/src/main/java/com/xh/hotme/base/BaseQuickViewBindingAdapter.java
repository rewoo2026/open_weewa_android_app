package com.xh.hotme.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;


import java.util.List;


public abstract class BaseQuickViewBindingAdapter<T, VH extends BaseViewHolder> extends BaseQuickAdapter<T, VH> {

    public BaseQuickViewBindingAdapter(int layoutResId, List<T> data) {
        super(layoutResId, data);
    }

    @Override
    protected VH onCreateDefViewHolder(ViewGroup parent, int viewType) {
        return getViewBinding(
                viewType,
                LayoutInflater.from(parent.getContext()),
                parent
        );
    }

    protected abstract VH getViewBinding(int viewType, LayoutInflater from, ViewGroup parent);

}

