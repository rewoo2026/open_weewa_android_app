package com.xh.hotme.live.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.xh.hotme.R;
import com.xh.hotme.databinding.PopResolutionBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.utils.ClickGuard;


public class ResolutionPopUtils {


    public static void showPop(Context context, TextView textView, IActionCallback commonListener) {
        try {
            //创建对象
            PopupWindow popupWindow = new PopupWindow(context);
            PopResolutionBinding binding = PopResolutionBinding.inflate(LayoutInflater.from(textView.getContext()));

            //设置view布局
            popupWindow.setContentView(binding.getRoot());
            popupWindow.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
            //设置动画的方法
//        popupWindow.setAnimationStyle(R.style.BottomDialog);
            //设置PopUpWindow的焦点，设置为true之后，PopupWindow内容区域，才可以响应点击事件
            popupWindow.setTouchable(true);
            //设置背景透明
            popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
            //点击空白处的时候让PopupWindow消失
            popupWindow.setOutsideTouchable(true);
            // true时，点击返回键先消失 PopupWindow
            // 但是设置为true时setOutsideTouchable，setTouchable方法就失效了（点击外部不消失，内容区域也不响应事件）
            // false时PopupWindow不处理返回键，默认是false
            popupWindow.setFocusable(true);
            //设置dismiss事件

            if(textView.getText().toString().startsWith("720")) {
                binding.tv720.setTextColor(ContextCompat.getColor(context, R.color.orange));
            }
            if(textView.getText().toString().startsWith("1080")) {
                binding.tv1080.setTextColor(ContextCompat.getColor(context, R.color.orange));
            }
            binding.tv720.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    commonListener.onReturn(720);
                    popupWindow.dismiss();

                    return false;
                }
            });

            binding.tv1080.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    commonListener.onReturn(1080);
                    popupWindow.dismiss();
                    return false;
                }
            });

            int[] location = new int[2];

            textView.getLocationOnScreen(location);

            binding.getRoot().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int measuredWidth = binding.getRoot().getMeasuredWidth();

            int measuredHeight = binding.getRoot().getMeasuredHeight();
            popupWindow.showAtLocation(textView, Gravity.NO_GRAVITY,
                (location[0] + textView.getWidth() / 2) - measuredWidth / 2, location[1] - measuredHeight);


            popupWindow.showAsDropDown(textView);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
