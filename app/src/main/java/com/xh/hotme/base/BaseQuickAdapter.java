package com.xh.hotme.base;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/** Adapts existing convert/ViewBinding callbacks to the BRVAH 4 lifecycle. */
public abstract class BaseQuickAdapter<T, VH extends BaseViewHolder>
        extends com.chad.library.adapter4.BaseQuickAdapter<T, VH> {
    private final int layoutResId;

    protected BaseQuickAdapter(int layoutResId, List<T> data) {
        super(data == null ? new ArrayList<>() : data);
        this.layoutResId = layoutResId;
        setStateViewEnable(true);
    }

    @Override
    protected VH onCreateViewHolder(Context context, ViewGroup parent, int viewType) {
        return onCreateDefViewHolder(parent, viewType);
    }

    @SuppressWarnings("unchecked")
    protected VH onCreateDefViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return (VH) new BaseViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(VH holder, int position, T item) {
        convert(holder, item);
    }

    protected abstract void convert(VH holder, T item);

    public List<T> getData() {
        return getItems();
    }

    public void setList(List<T> data) {
        submitList(data);
    }

}
