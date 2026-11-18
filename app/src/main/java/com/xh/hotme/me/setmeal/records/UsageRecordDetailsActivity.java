package com.xh.hotme.me.setmeal.records;

import com.google.gson.Gson;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivitySetmealRecordDetailsBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setmeal.bean.UsageRecordBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;

import java.text.SimpleDateFormat;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description: 使用详情
 */
public class UsageRecordDetailsActivity extends BaseActivity<ActivitySetmealRecordDetailsBinding> {


    int type = 2;

    UsageRecordBean.DataBean.RowsBean bean;

    @Override
    protected void initView() {

        try {
            showDetails();
        } catch(Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void initData() {

    }


    private void showDetails() {
        bean = (UsageRecordBean.DataBean.RowsBean) getIntent().getSerializableExtra("bean");

        RecordInfoBean recordInfoBean = new Gson().fromJson(bean.competitionContent, RecordInfoBean.class);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        viewBinding.tvTime.setText(format.format(recordInfoBean.time));
        viewBinding.tvName.setText(recordInfoBean.name);

        String ss = getResources()
            .getStringArray(R.array.motion_type_list)[recordInfoBean.motionType];
        viewBinding.tvType.setText(ss);
        viewBinding.tvAddress.setText(recordInfoBean.place);
        viewBinding.tvAddressType.setText(recordInfoBean.placeType);

        viewBinding.tvHome.setText(recordInfoBean.hostTeam);
        viewBinding.tvVisiting.setText(recordInfoBean.guestTeam);

        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return false;
            }
        });

        if(bean.usedType == 1) {
            viewBinding.ivViewbg.setImageResource(R.mipmap.icon_record_detail_bg1);
            viewBinding.ivViewbg2.setImageResource(R.mipmap.icon_record_icon_bg1);
            viewBinding.tvTopName.setText(getString(R.string.competition_record));
        } else if(bean.usedType == 2) {
            viewBinding.ivViewbg.setImageResource(R.mipmap.icon_record_detail_bg2);
            viewBinding.ivViewbg2.setImageResource(R.mipmap.icon_record_icon_bg2);
            viewBinding.tvTopName.setText(getString(R.string.cloud_storage));
        } else if(bean.usedType == 3) {
            viewBinding.ivViewbg.setImageResource(R.mipmap.icon_record_detail_bg3);
            viewBinding.ivViewbg2.setImageResource(R.mipmap.icon_record_icon_bg3);
            viewBinding.tvTopName.setText(getString(R.string.competition_live));
        } else if(bean.usedType == 4) {
            viewBinding.ivViewbg.setImageResource(R.mipmap.icon_record_detail_bg4);
            viewBinding.ivViewbg2.setImageResource(R.mipmap.icon_record_icon_bg4);
            viewBinding.tvTopName.setText(getString(R.string.big_data));
        }


        OkHttpUtil.get(SdkApi.packageUsed_packageUsedInfo() + "?deviceId=" +
            CameraManager.getDeviceIdLast() + "&usedType=", null, new OkHttpCallbackDecode<UsageRecordBean>() {
            @Override
            public void onDataSuccess(UsageRecordBean data) {

            }

            @Override
            public void onFailure(String code, String msg) {
                super.onFailure(code, msg);

                runOnUiThread(() -> ToastUtil.s(msg));
            }
        });

    }

}
