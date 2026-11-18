package com.aigestudio.wheelpicker.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2016/9/14 0014.
 */
public class HmCity implements Serializable {
    public String name;
    public String city_id;
    public String province_id;
    public List<String> area;
    public String getProvinceId() {
        return province_id;
    }

    public void setProvinceId(String province_id) {
        this.province_id = province_id;
    }

    public String getCityId() {
        return city_id;
    }

    public void setCityId(String city_id) {
        this.city_id = city_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArea() {
        return area;
    }

    public void setArea(List<String> area) {
        this.area = area;
    }
}
