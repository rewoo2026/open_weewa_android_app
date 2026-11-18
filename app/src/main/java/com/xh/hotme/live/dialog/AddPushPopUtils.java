package com.xh.hotme.live.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.databinding.PopDevicesBinding;
import com.xh.hotme.databinding.PopLivePlatformBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.me.setmeal.adapter.PopDevicesAdapter;


public class AddPushPopUtils {


    //书签popwindow
    public static void showPop(Context context, View view, IActionCallback commonListener) {

        //创建对象
        PopupWindow popupWindow = new PopupWindow(context);
        PopLivePlatformBinding binding = PopLivePlatformBinding.inflate(LayoutInflater.from(view.getContext()));

        //设置view布局
        popupWindow.setContentView(binding.getRoot());
        popupWindow.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
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


        try {
            binding.tvDy.setOnClickListener(view14 -> {
                commonListener.onReturn(2, binding.tvDy.getText().toString());
                popupWindow.dismiss();
            });
            binding.tvWx.setOnClickListener(view13 -> {
                commonListener.onReturn(1, binding.tvWx.getText().toString());
                popupWindow.dismiss();
            });
            binding.tvKs.setOnClickListener(view12 -> {
                commonListener.onReturn(3, binding.tvKs.getText().toString());
                popupWindow.dismiss();
            });
            binding.tvOther.setOnClickListener(view1 -> {
                commonListener.onReturn(5, binding.tvOther.getText().toString());
                popupWindow.dismiss();
            });

            popupWindow.showAsDropDown(view);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static void showDeviceSelectPop(Context context, View view, IActionCallback commonListener) {

        // 断开当前异常的链接
        BluetoothManager.getImpl().disconnectBle();

        //创建对象
        PopupWindow popupWindow = new PopupWindow(context);
        PopDevicesBinding binding = PopDevicesBinding.inflate(LayoutInflater.from(view.getContext()));

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


        try {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            binding.recyclerView.setLayoutManager(linearLayoutManager);
            PopDevicesAdapter popDevicesAdapter = new PopDevicesAdapter();
            popDevicesAdapter.setList(DeviceManager.getBindDeviceList());
            popDevicesAdapter.setCommonListener((code, objects) -> {
                commonListener.onReturn(code, objects);
                popupWindow.dismiss();
            });
            binding.recyclerView.setAdapter(popDevicesAdapter);


            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int vH = view.getHeight();

            popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0] - popupWindow.getWidth() - dp2px(context, 18),
                location[1] + view.getHeight() + dp2px(context, 5));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

}
