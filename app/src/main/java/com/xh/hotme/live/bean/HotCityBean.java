package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import com.aigestudio.wheelpicker.model.HmCity;

@Keep
public class HotCityBean extends HmCity {

    public final String provinceName;
    public final String cityDisplayName;


    public HotCityBean(String name, String cityDisplayName, String cityId, String provinceName, String provinceId) {
        this.name = name;
        this.cityDisplayName = cityDisplayName;
        this.city_id = cityId;
        this.province_id = provinceId;
        this.provinceName = provinceName;
    }

}
