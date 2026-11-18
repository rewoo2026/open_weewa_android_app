package com.aigestudio.wheelpicker.widgets;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.aigestudio.wheelpicker.WheelPicker;
import com.aigestudio.wheelpicker.model.City;
import com.aigestudio.wheelpicker.model.HmCity;
import com.aigestudio.wheelpicker.model.HmProvince;
import com.aigestudio.wheelpicker.model.Province;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * WheelAreaPicker
 * Created by Administrator on 2016/9/14 0014.
 */
public class WheelCityPicker extends LinearLayout implements IWheelCityPicker {
    private static final float ITEM_TEXT_SIZE = 18;
    private static final String SELECTED_ITEM_COLOR = "#353535";
    private static final int PROVINCE_INITIAL_INDEX = 0;

    private Context mContext;

    private List<HmProvince> mProvinceList;
    private List<HmCity> mCityList;
    private List<String> mProvinceName, mCityName;

    private AssetManager mAssetManager;

    private LayoutParams mLayoutParams;

    private WheelPicker mWPProvince, mWPCity;

    public WheelCityPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        initLayoutParams();

        initView(context);

        mProvinceList = getJsonDataFromAssets(mAssetManager);

        obtainProvinceData();

        addListenerToWheelPicker();
    }

    @SuppressWarnings("unchecked")
    private List<HmProvince> getJsonDataFromAssets(AssetManager assetManager) {
        List<HmProvince> provinceList = null;
        try {
            InputStream inputStream = assetManager.open("hm_province_city.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            provinceList = new Gson().fromJson(json, new TypeToken<List<HmProvince>>() {
            }.getType());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return provinceList;
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

        mProvinceName = new ArrayList<>();
        mCityName = new ArrayList<>();

        mWPProvince = new WheelPicker(context);
        mWPCity = new WheelPicker(context);

        initWheelPicker(mWPProvince, 1);
        initWheelPicker(mWPCity, 1.5f);
    }

    private void initWheelPicker(WheelPicker wheelPicker, float weight) {
        mLayoutParams.weight = weight;
        wheelPicker.setItemTextSize(dip2px(mContext, ITEM_TEXT_SIZE));
        wheelPicker.setSelectedItemTextColor(Color.parseColor(SELECTED_ITEM_COLOR));
        wheelPicker.setCurved(true);
        wheelPicker.setLayoutParams(mLayoutParams);
        addView(wheelPicker);
    }

    private void obtainProvinceData() {
        if (mProvinceList == null) {
            mProvinceList = new ArrayList<>();
        }
        for (HmProvince province : mProvinceList) {
            mProvinceName.add(province.getName());
        }
        mWPProvince.setData(mProvinceName);
        setCityAndAreaData(PROVINCE_INITIAL_INDEX);
    }

    private void addListenerToWheelPicker() {
        //监听省份的滑轮,根据省份的滑轮滑动的数据来设置市跟地区的滑轮数据
        mWPProvince.setOnItemSelectedListener(new WheelPicker.OnItemSelectedListener() {
            @Override
            public void onItemSelected(WheelPicker picker, Object data, int position) {
                //获得该省所有城市的集合
                mCityList = mProvinceList.get(position).getCity();
                setCityAndAreaData(position);
            }
        });
    }

    private void setCityAndAreaData(int position) {
        //获得该省所有城市的集合
        mCityList = mProvinceList.get(position).getCity();
        //获取所有city的名字
        //重置先前的城市集合数据
        mCityName.clear();
        for (HmCity city : mCityList)
            mCityName.add(city.getName());
        mWPCity.setData(mCityName);
        mWPCity.setSelectedItemPosition(0);
        //获取第一个城市对应的城区的名字
        //重置先前的城区集合的数据
//        mWPArea.setData(mCityList.get(0).getArea());
//        mWPArea.setSelectedItemPosition(0);
    }

    public void setSelectItemById(String provinceId, String cityId) {
        int provincePos = 0;
        int cityPos = 0;
        for (int i = 0; i < mProvinceList.size(); i++) {
            HmProvince province = mProvinceList.get(i);
            if(province.province_id.equalsIgnoreCase(provinceId)){
                provincePos = i;
                List<HmCity> cityList = province.getCity();

                for (int j = 0; j < cityList.size(); j++) {
                    HmCity city = cityList.get(j);

                    if(city.city_id.equalsIgnoreCase(cityId)){
                        cityPos = j;
                        break;
                    }
                }
                break;
            }
        }
        mWPProvince.setSelectedItemPosition(provincePos);
        mCityList = mProvinceList.get(provincePos).getCity();
        setCityAndAreaData(provincePos);
        mWPCity.setSelectedItemPosition(cityPos);
    }

    public void setSelectItemPosition(int provincePos, int cityPos) {
        mWPProvince.setSelectedItemPosition(provincePos);
        mCityList = mProvinceList.get(provincePos).getCity();
        setCityAndAreaData(provincePos);
        mWPCity.setSelectedItemPosition(cityPos);
    }

    @Override
    public HmProvince getProvince() {
        return mProvinceList.get(mWPProvince.getCurrentItemPosition());
    }

    @Override
    public HmCity getCity() {
        return mCityList.get(mWPCity.getCurrentItemPosition());
    }

    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
