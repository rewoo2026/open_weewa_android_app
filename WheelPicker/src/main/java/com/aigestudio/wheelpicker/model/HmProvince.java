package com.aigestudio.wheelpicker.model;


import java.io.Serializable;
import java.util.List;

/**
 * Created by Administrator on 2016/9/14 0014.
 */
public class HmProvince implements Serializable {
    public String name;
    public String province_id;
    public List<HmCity> city;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvinceId() {
        return province_id;
    }

    public void setProvinceId(String province_id) {
        this.province_id = province_id;
    }

    public List<HmCity> getCity() {
        return city;
    }

    public void setCity(List<HmCity> city) {
        this.city = city;
    }
}
