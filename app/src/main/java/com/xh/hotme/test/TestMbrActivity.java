package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.DeviceSettingMbrBean;
import com.xh.hotme.bean.TextModelBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityTestMbrSettingBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;

import java.util.ArrayList;
import java.util.List;


public class TestMbrActivity extends BaseActivity<ActivityTestMbrSettingBinding> {

    private final static String TAG = TestMbrActivity.class.getSimpleName();

    TextCheckBoxAdapter _adapter;

    TextEditorAdapter _mbrAdapter;

    final String[] _placeType = { "5人场", "8人场", "11人场", "篮球" };
    String[] _mbrType = { "avs码率", "fec码率" };

    int _placeTypeIndex;
    int _mbrTypeIndex;

    DeviceSettingMbrBean _deviceSettingMbrBean = null;
    final List<TextModelBean> _placeTypeList = new ArrayList<>();
    final List<TextValueModelBean> _mbrList = new ArrayList<>();

    public static void start(Context context) {
        Intent intent = new Intent(context, TestMbrActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        for(String type : _placeType) {
            TextModelBean textModelBean = new TextModelBean(0, type);
            _placeTypeList.add(textModelBean);
        }
        _placeTypeList.get(0).isSelect = true;

        TitleLayoutHelper.initTitle(viewBinding.titleBar, getString(R.string.mbr_setting));

        _adapter = new TextCheckBoxAdapter(TestMbrActivity.this, _placeTypeList);
        _adapter.setListener(position -> {
            _placeTypeIndex = position;
            for(int i = 0; i < _placeTypeList.size(); i++) {
                _placeTypeList.get(i).isSelect = i == position;
            }
            _adapter.notifyDataSetChanged();
            updatePlaceType();
        });
        viewBinding.rvPlaceType.setAdapter(_adapter);
        viewBinding.rvPlaceType.setLayoutManager(new LinearLayoutManager(TestMbrActivity.this));

        viewBinding.btnSetup.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                List<TextValueModelBean> modelBeanList = _mbrAdapter.getMbrValue();
                AppTrace.d(TAG, "update=" + GsonUtils.GsonString(modelBeanList));
                if(_deviceSettingMbrBean != null) {
                    _deviceSettingMbrBean.mbr.avs_video_mbr = modelBeanList.get(0).value;
                    _deviceSettingMbrBean.mbr.fec_video_mbr = modelBeanList.get(1).value;
                    _deviceSettingMbrBean.mbr.live_mbr = modelBeanList.get(2).value;


                    if(CameraManager.updateMbrSetting(_deviceSettingMbrBean)) {
                        ToastUtil.s(getString(R.string.info_local_update_success));
                    } else {
                        ToastUtil.s(getString(R.string.info_server_config_null));
                    }
                }
                return true;
            }
        });


    }

    @Override
    protected void initData() {

        _placeTypeIndex = 0;

        updatePlaceType();
    }

    private void initListener() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void updatePlaceType() {
        int motionType = 1;
        int placeType = 1;
        if(_placeTypeIndex >= 3) {
            motionType = 2;
            placeType = 0;
        } else {
            placeType = _placeTypeIndex + 1;
            motionType = 1;
        }
        List<DeviceSettingMbrBean> deviceSettingMbrBeanList = CameraManager.getMBRListByMotionTypeOrPlaceType(motionType, placeType, _placeTypeIndex >= 3);
        if(deviceSettingMbrBeanList != null && !deviceSettingMbrBeanList.isEmpty()) {
            _deviceSettingMbrBean = deviceSettingMbrBeanList.get(0);
            if(_deviceSettingMbrBean != null) {
                _mbrList.clear();
                _mbrList.add(new TextValueModelBean(1, "avs码率", _deviceSettingMbrBean.mbr.avs_video_mbr));
                _mbrList.add(new TextValueModelBean(1, "fec码率", _deviceSettingMbrBean.mbr.fec_video_mbr));
                _mbrList.add(new TextValueModelBean(1, "推流码率", _deviceSettingMbrBean.mbr.live_mbr));

                if(viewBinding.rvMbrType.getAdapter() == null) {
                    _mbrAdapter = new TextEditorAdapter(TestMbrActivity.this, _mbrList);
                    viewBinding.rvMbrType.setAdapter(_mbrAdapter);
                    viewBinding.rvMbrType.setLayoutManager(new LinearLayoutManager(TestMbrActivity.this));
                }

                _mbrAdapter.notifyDataSetChanged();
            }
        } else {
            ToastUtil.s(getString(R.string.info_use_default_value_because_of_server_config_null));
            _deviceSettingMbrBean = new DeviceSettingMbrBean();
            _deviceSettingMbrBean.mbr = _deviceSettingMbrBean.new MbrBean();
            _deviceSettingMbrBean.motion_type = motionType;
            _deviceSettingMbrBean.place_type = placeType;
            _deviceSettingMbrBean.mbr.avs_video_mbr = VideoUtil.getDefaultAvsMBR(motionType, placeType);
            _deviceSettingMbrBean.mbr.fec_video_mbr = VideoUtil.getFecMBR(motionType, placeType);
            _deviceSettingMbrBean.mbr.live_mbr = VideoUtil.getLiveMBR(motionType, placeType);

            _mbrList.clear();
            _mbrList.add(new TextValueModelBean(1, getString(R.string.avs_mbr), _deviceSettingMbrBean.mbr.avs_video_mbr));
            _mbrList.add(new TextValueModelBean(1, getString(R.string.fec_mbr), _deviceSettingMbrBean.mbr.fec_video_mbr));
            _mbrList.add(new TextValueModelBean(1, getString(R.string.live_mbr), _deviceSettingMbrBean.mbr.live_mbr));

            if(viewBinding.rvMbrType.getAdapter() == null) {
                _mbrAdapter = new TextEditorAdapter(TestMbrActivity.this, _mbrList);
                viewBinding.rvMbrType.setAdapter(_mbrAdapter);
                viewBinding.rvMbrType.setLayoutManager(new LinearLayoutManager(TestMbrActivity.this));
            }

            _mbrAdapter.notifyDataSetChanged();
        }
    }
}
