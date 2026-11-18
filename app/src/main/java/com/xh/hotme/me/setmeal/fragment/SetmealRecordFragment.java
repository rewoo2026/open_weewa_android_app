package com.xh.hotme.me.setmeal.fragment;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.FragmentSetmealRecordBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setmeal.adapter.RecordContentAdapter;
import com.xh.hotme.me.setmeal.bean.RecordContentBean;
import com.xh.hotme.me.setmeal.bean.UsageRecordBean;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description:
 */
public class SetmealRecordFragment extends BaseFragment<FragmentSetmealRecordBinding> {

    int type;
    final RecordContentAdapter recordContentAdapter = new RecordContentAdapter();

    @Override
    protected void initView() {

        type = getArguments().getInt("type");

    }

    Calendar calendarLast;

    @Override
    protected void initData() {
        String usedType = "";
        if(type != 0) {
            usedType = String.valueOf(type);
        }
        OkHttpUtil.get(SdkApi.packageUsed_list() + "?deviceId=" +
                CameraManager.getDeviceIdLast() + "&usedType=" + usedType, null,
            new OkHttpCallbackDecode<UsageRecordBean>(UsageRecordBean.class) {
                @Override
                public void onDataSuccess(UsageRecordBean data) {
                    MainHandler.runOnUiThread(() -> {

                        List<RecordContentBean> list = new ArrayList<>();
                        RecordContentBean recordContentBean = new RecordContentBean();
                        for(int i = 0; i < data.data.rows.size(); i++) {
                            try {
                                UsageRecordBean.DataBean.RowsBean rowsBean = data.data.rows.get(i);
                                if(i >= 5) {
                                    rowsBean.createTime = "2023-12-25T15:58:55";
                                }
                                if(i >= 10) {
                                    rowsBean.createTime = "2023-11-25T15:58:55";
                                }
                                if(i >= 13) {
                                    rowsBean.createTime = "2023-9-25T15:58:55";
                                }
                                if(i >= 18) {
                                    rowsBean.createTime = "2023-4-25T15:58:55";
                                }

//                                        if (i % 2 == 0) {
//                                            rowsBean.usedType = 2;
//                                        }
//                                        if (i % 3 == 0) {
//                                            rowsBean.usedType = 3;
//                                        }
//                                        if (i % 4 == 0) {
//                                            rowsBean.usedType = 4;
//                                        }

                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date parse = format.parse(rowsBean.createTime.replaceAll("T", " "));
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(parse);

                                if(calendarLast != null) {
                                    if(calendar.get(Calendar.MONTH) != calendarLast.get(Calendar.MONTH)) {
                                        list.add(recordContentBean);
                                        recordContentBean = new RecordContentBean();
                                    }
                                }

                                recordContentBean.title = (calendar.get(Calendar.MONTH) + 1) + "";
                                recordContentBean.list.add(rowsBean);


                                calendarLast = calendar;


                                if(data.data.rows.size() - 1 == i) {
                                    list.add(recordContentBean);
                                    calendarLast = null;
                                }

                            } catch(Exception e) {
                                e.printStackTrace();
                            }


                        }

                        initList(list);
                    });

                }

                @Override
                public void onFailure(String code, String message) {
                    MainHandler.runOnUiThread(() -> {

                    });
                }

                @Override
                public void onFinish() {

                }
            });
    }


    private void initList(List<RecordContentBean> list) {


        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getContext());
        linearLayoutManager2.setOrientation(LinearLayoutManager.VERTICAL);
        _viewBinding.rv.setLayoutManager(linearLayoutManager2);

        _viewBinding.rv.setAdapter(recordContentAdapter);
        recordContentAdapter.setList(list);

        if(list.isEmpty()) {
            _viewBinding.ivNodata.setVisibility(View.VISIBLE);
            _viewBinding.tvNodata.setVisibility(View.VISIBLE);
        } else {
            _viewBinding.ivNodata.setVisibility(View.GONE);
            _viewBinding.tvNodata.setVisibility(View.GONE);
        }
    }
}
