package com.xh.hotme.base;

import android.app.Activity;
import android.view.View;

import com.xh.hotme.databinding.CommonIncludeHeaderBinding;
import com.xh.hotme.databinding.CommonIncludeHeaderBlackBinding;
import com.xh.hotme.databinding.CommonIncludeHeaderTransBinding;

public class TitleLayoutHelper {

    public static void initTitle(CommonIncludeHeaderBinding titleBinding, String title) {
        titleBinding.tvTitle.setText(title);
        titleBinding.ivBack.setOnClickListener(v -> {
            if(v.getContext() instanceof Activity)
                ((Activity) v.getContext()).finish();
        });
    }

    public static void initTitle(CommonIncludeHeaderBinding titleBinding, String title, View.OnClickListener listener) {
        titleBinding.tvTitle.setText(title);
        titleBinding.ivBack.setOnClickListener(listener);
    }


    public static void initTitleBack(CommonIncludeHeaderBlackBinding titleBinding, String title) {
        titleBinding.tvTitle.setText(title);
        titleBinding.ivBack.setOnClickListener(v -> {
            if(v.getContext() instanceof Activity)
                ((Activity) v.getContext()).finish();
        });
    }

    public static void initTitleTrans(CommonIncludeHeaderTransBinding titleBinding, String title) {
        titleBinding.tvTitle.setText(title);
        titleBinding.ivBack.setOnClickListener(v -> {
            if(v.getContext() instanceof Activity)
                ((Activity) v.getContext()).finish();
        });
    }
}
