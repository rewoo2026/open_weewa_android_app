package com.xh.hotme.record.bean;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
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
public class MotionPlacePicker extends LinearLayout implements IMotionPlacePicker {
    private static final float ITEM_TEXT_SIZE = 18;
    private static final String SELECTED_ITEM_COLOR = "#353535";
    private static final int PROVINCE_INITIAL_INDEX = 0;

    private Context mContext;

    private List<MotionBean> mMotionList;
    private List<PlaceBean> mPlaceList;
    private List<String> mMotionName, mPlaceName;

    private AssetManager mAssetManager;

    private LayoutParams mLayoutParams;

    private WheelPicker mWPMotion, mWPPlace;

    public MotionPlacePicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        initLayoutParams();

        initView(context);
        initData();

        obtainProvinceData();

        addListenerToWheelPicker();
    }

    private void initData() {
        MotionBean footballMotion = new MotionBean(1, "足球");
        PlaceBean footballPlace1 = new PlaceBean(1, "5人场");
        PlaceBean footballPlace2 = new PlaceBean(2, "8人场");
        PlaceBean footballPlace3 = new PlaceBean(3, "11人场");
        PlaceBean footballPlace4 = new PlaceBean(0, "训练");
        List<PlaceBean> footballPlaceList = new ArrayList<>();
        footballPlaceList.add(footballPlace1);
        footballPlaceList.add(footballPlace2);
        footballPlaceList.add(footballPlace3);
        footballPlaceList.add(footballPlace4);
        footballMotion.placeList = footballPlaceList;

        MotionBean basketballMotion = new MotionBean(2, "篮球");
        PlaceBean basketballPlace1 = new PlaceBean(1, "全场");
        PlaceBean basketballPlace2 = new PlaceBean(0, "半场");
        List<PlaceBean> basketballPlaceList = new ArrayList<>();
        basketballPlaceList.add(basketballPlace1);
        basketballPlaceList.add(basketballPlace2);
        basketballMotion.placeList = basketballPlaceList;

        MotionBean otherMotion = new MotionBean(0, "其它");
        PlaceBean otherPlace1 = new PlaceBean(1, "其它");
        List<PlaceBean> otherPlaceList = new ArrayList<>();
        otherPlaceList.add(otherPlace1);
        otherMotion.placeList = otherPlaceList;

        mMotionList = new ArrayList<>();
        mMotionList.add(footballMotion);
        mMotionList.add(basketballMotion);
        mMotionList.add(otherMotion);
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

        mMotionName = new ArrayList<>();
        mPlaceName = new ArrayList<>();

        mWPMotion = new WheelPicker(context);
        mWPPlace = new WheelPicker(context);

        initWheelPicker(mWPMotion, 1);
        initWheelPicker(mWPPlace, 1.5f);
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

    private void obtainProvinceData() {
        for(MotionBean province : mMotionList) {
            mMotionName.add(province.getName());
        }
        mWPMotion.setData(mMotionName);
        setCityAndAreaData(PROVINCE_INITIAL_INDEX, 0);
    }

    private void addListenerToWheelPicker() {
        //监听省份的滑轮,根据省份的滑轮滑动的数据来设置市跟地区的滑轮数据
        mWPMotion.setOnItemSelectedListener((picker, data, position) -> {
            //获得该省所有城市的集合
            mPlaceList = mMotionList.get(position).getPlaceList();
            setCityAndAreaData(position, 0);
        });

        mWPPlace.setOnItemSelectedListener((picker, data, position) -> {
            //获取城市对应的城区的名字
//                mWPPlace.setData(mPlaceList.get(position).getArea());
        });
    }

    private void setCityAndAreaData(int position, int selectPosition) {
        //获得该省所有城市的集合
        mPlaceList = mMotionList.get(position).getPlaceList();
        //获取所有city的名字
        //重置先前的城市集合数据
        mPlaceName.clear();
        for(PlaceBean placeBean : mPlaceList)
            mPlaceName.add(placeBean.getName());
        mWPPlace.setData(mPlaceName);
        mWPPlace.setSelectedItemPosition(selectPosition);

    }

    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public MotionBean getMotionType() {
        return mMotionList.get(mWPMotion.getCurrentItemPosition());
    }

    @Override
    public PlaceBean getPlaceType() {
        return mPlaceList.get(mWPPlace.getCurrentItemPosition());
    }

    public void setSelectItemById(int motionType, int placeType) {
        int motionPos = 0;
        int placePos = 0;
        for(int i = 0; i < mMotionList.size(); i++) {
            MotionBean motionBean = mMotionList.get(i);
            if(motionBean.type == motionType) {
                motionPos = i;
                List<PlaceBean> placeList = motionBean.getPlaceList();

                for(int j = 0; j < placeList.size(); j++) {
                    PlaceBean place = placeList.get(j);

                    if(place.type == placeType) {
                        placePos = j;
                        break;
                    }
                }
                break;
            }
        }
        mWPMotion.setSelectedItemPosition(motionPos);
//        mPlaceList = mMotionList.get(motionPos).getPlaceList();
        setCityAndAreaData(motionPos, placePos);
    }


    public void setMotionSelectItemById(int motionType) {
        int motionPos = 0;
        int placePos = 0;
        for(int i = 0; i < mMotionList.size(); i++) {
            MotionBean motionBean = mMotionList.get(i);
            if(motionBean.type == motionType) {
                motionPos = i;
                break;
            }
        }
        mWPMotion.setSelectedItemPosition(motionPos, false);
        //获得该省所有城市的集合
        mPlaceList = mMotionList.get(motionPos).getPlaceList();
        //获取所有city的名字
        //重置先前的城市集合数据
        mPlaceName.clear();
        for(PlaceBean placeBean : mPlaceList)
            mPlaceName.add(placeBean.getName());
        mWPPlace.setData(mPlaceName);
    }

    public void setPlaceSelectItemById(int placeType) {
        int placePos = 0;
        List<PlaceBean> placeList = mMotionList.get(mWPMotion.getCurrentItemPosition()).getPlaceList();
        for(int j = 0; j < placeList.size(); j++) {
            PlaceBean place = placeList.get(j);
            if(place.type == placeType) {
                placePos = j;
                break;
            }
        }
        mWPPlace.setSelectedItemPosition(placePos, false);
    }
}
