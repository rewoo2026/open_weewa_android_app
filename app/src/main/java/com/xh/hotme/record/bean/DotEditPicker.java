package com.xh.hotme.record.bean;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.aigestudio.wheelpicker.WheelPicker;

import java.util.ArrayList;
import java.util.List;


/**
 * WheelAreaPicker
 * Created by Administrator on 2016/9/14 0014.
 */
public class DotEditPicker extends LinearLayout implements IDotEditPicker {
    private static final float ITEM_TEXT_SIZE = 18;
    private static final String SELECTED_ITEM_COLOR = "#353535";
    private static final int PROVINCE_INITIAL_INDEX = 0;

    private Context mContext;

    private List<Integer> mMinuteList;
    private List<Integer> mSecondList;
    private List<String> mMinuteName;
    private List<String> mSecondName;


    private AssetManager mAssetManager;

    private LayoutParams mLayoutParams;

    private WheelPicker mWPMinute, mWPSecond;

    int minute;
    int second;

    public DotEditPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        initLayoutParams();

        initView(context);
        initData();

        addListenerToWheelPicker();
    }

    private void initData() {


    }

    public void setData(long time) {
        long min = time / 60;
        mMinuteList = new ArrayList<>();
        mMinuteName = new ArrayList<>();
        if(min==0) {
            mMinuteList.add(0);
            mMinuteName.add(String.format("00"));
        } else {
            for (int i = 0; i <= min; i++) {
                mMinuteList.add(i);
                if (i < 10) {
                    mMinuteName.add(String.format("0%d", i));
                } else {
                    mMinuteName.add(String.format("%d", i));
                }
            }
        }


        mSecondList = new ArrayList<>();
        mSecondName = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            mSecondList.add(i);
            if (i < 10) {
                mSecondName.add(String.format("0%d", i));
            } else {
                mSecondName.add(String.format("%d", i));
            }
        }

        mWPMinute.setData(mMinuteName);
        mWPSecond.setData(mSecondName);

    }

    private void initLayoutParams() {
        mLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLayoutParams.setMargins(5, 5, 5, 5);
        mLayoutParams.width = 0;
    }

    private void initView(Context context) {
        setOrientation(HORIZONTAL);

        mContext = context;

        mAssetManager = mContext.getAssets();

        mMinuteList = new ArrayList<>();
        mSecondList = new ArrayList<>();

        mWPMinute = new WheelPicker(context);
        mWPSecond = new WheelPicker(context);

        initWheelPicker(mWPMinute, 1);
        initWheelPicker(mWPSecond, 1);
    }

    private void initWheelPicker(WheelPicker wheelPicker, float weight) {
        mLayoutParams.weight = weight;
        wheelPicker.setItemTextSize(dip2px(mContext, ITEM_TEXT_SIZE));
        wheelPicker.setSelectedItemTextColor(Color.parseColor(SELECTED_ITEM_COLOR));
        wheelPicker.setCurved(true);
        wheelPicker.setLayoutParams(mLayoutParams);
        wheelPicker.setVisibleItemCount(7);
        addView(wheelPicker);
    }

    private void addListenerToWheelPicker() {
        //监听省份的滑轮,根据省份的滑轮滑动的数据来设置市跟地区的滑轮数据
        mWPMinute.setOnItemSelectedListener((picker, data, position) -> {
            //获得该省所有城市的集合

        });

        mWPSecond.setOnItemSelectedListener((picker, data, position) -> {
            //获取城市对应的城区的名字
//                mWPPlace.setData(mPlaceList.get(position).getArea());
        });
    }


    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public int getMinute() {
        return mMinuteList.get(mWPMinute.getCurrentItemPosition());
    }

    @Override
    public int getSecond() {
        return mSecondList.get(mWPSecond.getCurrentItemPosition());
    }

    public void setSelectItemById(int min, int second) {
        mWPMinute.setSelectedItemPosition(min);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mWPSecond.setSelectedItemPosition(second);
            }
        }, 200);

    }

}
