package com.xh.hotme.live.dialog;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.aigestudio.wheelpicker.WheelPicker;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogSelectCityBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.live.adapter.HotCityAdapter;
import com.xh.hotme.live.bean.CityBean;
import com.xh.hotme.live.bean.HotCityBean;
import com.xh.hotme.live.bean.ProvinceBean;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;


@Keep
@SheetStyle
public class SelectCityDialog extends BaseDialog<DialogSelectCityBinding> {
    private CityResult listener;

    HotCityAdapter _adapter;

    private List<HotCityBean> _hotCityList;


    public SelectCityDialog(@NonNull final Context context) {
        super(context);
    }

    ProvinceBean provinceBean;
    CityBean cityBean;

    public void init(String initProvinceCode, String initCityCode) {
        HotCityBean bjBean = new HotCityBean("市辖区", "北京", "110100000000", "北京", "110000000000");
        HotCityBean shBean = new HotCityBean("市辖区", "上海", "310100000000", "上海", "310000000000");
        HotCityBean gzBean = new HotCityBean("广州市", "广州", "440100000000", "广东省", "440000000000");
        HotCityBean szBean = new HotCityBean("深圳市", "深圳", "440300000000", "广东省", "440000000000");
        HotCityBean whBean = new HotCityBean("武汉市", "武汉", "420100000000", "湖北省", "420000000000");
        HotCityBean hzBean = new HotCityBean("杭州市", "杭州", "330100000000", "浙江省", "330000000000");
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
                listener.result(hotCityBean.province_id, hotCityBean.provinceName, hotCityBean.city_id, hotCityBean.name);


                dismiss();
            }
        });

        viewBinding.wheelPicker.setData(new ArrayList<String>());
        viewBinding.wheelPicker2.setData(new ArrayList<String>());
        OkHttpUtil.get(SdkApi.area_province(), new OkHttpCallbackDecode<ProvinceBean>(ProvinceBean.class) {
            @Override
            public void onDataSuccess(ProvinceBean bean) {
                MainHandler.runOnUiThread(() -> {
                    provinceBean = bean;
                    List<String> data = new ArrayList<>();

                    int idx = 0;
                    for(int i = 0; i < bean.data.size(); i++) {
                        data.add(bean.data.get(i).name);
                        if(bean.data.get(i).provinceCode.equals(initProvinceCode)) {
                            idx = i;
                        }
                    }
                    viewBinding.wheelPicker.setData(data);
                    viewBinding.wheelPicker.setCyclic(false);
                    viewBinding.wheelPicker.setSelectedItemPosition(idx);

                    getCity(bean.data.get(idx).provinceCode, initCityCode);

                    viewBinding.wheelPicker.setOnWheelChangeListener(new WheelPicker.OnWheelChangeListener() {
                        @Override
                        public void onWheelScrolled(int offset) {

                        }

                        @Override
                        public void onWheelSelected(int position) {
                            getCity(bean.data.get(position).provinceCode, null);
                        }

                        @Override
                        public void onWheelScrollStateChanged(int state) {

                        }
                    });
                });
            }
        });
    }


    private void getCity(String provinceCode, String initCityCode) {
        OkHttpUtil.get(SdkApi.area_city() + provinceCode,
            new OkHttpCallbackDecode<CityBean>(CityBean.class) {
                @Override
                public void onDataSuccess(CityBean bean) {
                    MainHandler.runOnUiThread(() -> {
                        cityBean = bean;
                        List<String> data = new ArrayList<>();

                        int idx = 0;
                        for(int i = 0; i < bean.data.size(); i++) {
                            data.add(bean.data.get(i).name);
                            if(bean.data.get(i).cityCode.equals(initCityCode)) {
                                idx = i;
                            }
                        }
                        viewBinding.wheelPicker2.setData(data);
                        viewBinding.wheelPicker2.setCyclic(false);
                        viewBinding.wheelPicker2.setSelectedItemPosition(idx);
                        viewBinding.wheelPicker2.setOnWheelChangeListener(new WheelPicker.OnWheelChangeListener() {
                            @Override
                            public void onWheelScrolled(int offset) {

                            }

                            @Override
                            public void onWheelSelected(int position) {

                            }

                            @Override
                            public void onWheelScrollStateChanged(int state) {

                            }
                        });
                    });
                }
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onInit() {
        _hotCityList = new ArrayList<>();
        viewBinding.wheelPicker.setVisibleItemCount(7);
        viewBinding.wheelPicker2.setVisibleItemCount(7);
        viewBinding.ivClose.setOnClickListener(view -> dismiss());
        viewBinding.btnOk.setOnClickListener(view -> {
            dismiss();
            try {
                String provinceCodeDisplay = provinceBean.data.get(viewBinding.wheelPicker.getCurrentItemPosition()).name;
                String provinceCode = provinceBean.data.get(viewBinding.wheelPicker.getCurrentItemPosition()).provinceCode;

                String cityCode = cityBean.data.get(viewBinding.wheelPicker2.getCurrentItemPosition()).cityCode;
                String cityCodeDisplay = cityBean.data.get(viewBinding.wheelPicker2.getCurrentItemPosition()).name;
                listener.result(provinceCode, provinceCodeDisplay, cityCode, cityCodeDisplay);
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setListener(CityResult listener) {
        this.listener = listener;
    }


    public interface CityResult {

        void result(String provinceCode, String provinceCodeDisplay, String cityCode, String cityCodeDisplay);
    }
}
