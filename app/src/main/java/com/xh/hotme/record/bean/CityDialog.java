package com.xh.hotme.record.bean;


import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.aigestudio.wheelpicker.model.HmCity;
import com.aigestudio.wheelpicker.model.HmProvince;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogCityBinding;
import com.xh.hotme.live.adapter.HotCityAdapter;
import com.xh.hotme.live.bean.HotCityBean;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;


@Keep
@SheetStyle
public class CityDialog extends BaseDialog<DialogCityBinding> {
    private static final String TAG = "WheelViewDialog";

    String provinceId;
    String cityId;


    HotCityAdapter _adapter;

    private List<HotCityBean> _hotCityList;

    public interface ICallback {
        void onCancel();

        void onConfirm(String provinceCode, String provinceCodeDisplay, String cityCode, String cityCodeDisplay);
    }

    private CityDialog.ICallback iCallback;


    public CityDialog(@NonNull final Context context) {
        super(context);
    }

    public CityDialog setCallback(CityDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public CityDialog setTitle(String content) {
        viewBinding.title.setText(content);
        return this;
    }

    public CityDialog setConfirm(String content) {
        viewBinding.btnOk.setText(content);
        return this;
    }

    @Override
    protected void onInit() {

        HotCityBean bjBean = new HotCityBean("市辖区", "北京", "110100000000", "北京", "110100000000");
        HotCityBean shBean = new HotCityBean("市辖区", "上海", "310100000000", "上海", "310000000000");
        HotCityBean gzBean = new HotCityBean("广州市", "广州", "440100000000", "广东省", "440000000000");
        HotCityBean szBean = new HotCityBean("深圳市", "深圳", "440300000000", "广东省", "440000000000");
        HotCityBean whBean = new HotCityBean("武汉市", "武汉", "420100000000", "湖北省", "420000000000");
        HotCityBean hzBean = new HotCityBean("杭州市", "杭州", "330100000000", "浙江省", "330000000000");
        _hotCityList = new ArrayList<>();
        _hotCityList.add(bjBean);
        _hotCityList.add(shBean);
        _hotCityList.add(gzBean);
        _hotCityList.add(szBean);
        _hotCityList.add(hzBean);
        _hotCityList.add(whBean);

        _adapter = new HotCityAdapter();
        viewBinding.rvHotCity.setAdapter(_adapter);
        viewBinding.rvHotCity.setLayoutManager(new GridLayoutManager(getContext(), 3));
        viewBinding.rvHotCity.addItemDecoration(new GridSpacingItemDecoration(3, DensityUtil.dip2px(10), false));
        _adapter.setList(_hotCityList);
        _adapter.setCommonListener((code, objects) -> {
            if(code == 2) {
                HotCityBean hotCityBean = (HotCityBean) objects[0];

                if(iCallback != null) {
                    iCallback.onConfirm(hotCityBean.province_id, hotCityBean.provinceName, hotCityBean.city_id, hotCityBean.name);
                }
                dismiss();
            }
        });

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(iCallback != null) {
                    iCallback.onCancel();
                }

                dismiss();
                return true;
            }
        });

        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                HmProvince province = viewBinding.wheelPicker.getProvince();
                HmCity city = viewBinding.wheelPicker.getCity();

                if(iCallback != null) {
                    iCallback.onConfirm(province.province_id, province.name, city.city_id, city.name);
                }

                dismiss();

                return true;
            }
        });

        viewBinding.wheelPicker.setSelectItemById(provinceId, cityId);
    }

    public CityDialog setSelectItem(String pId, String cId) {
        this.provinceId = pId;
        this.cityId = cId;
        viewBinding.wheelPicker.setSelectItemById(provinceId, cityId);
        return this;
    }

}
