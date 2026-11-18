package com.xh.hotme.record;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityRecordInfoBinding;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.CityDialog;
import com.xh.hotme.record.bean.MotionBean;
import com.xh.hotme.record.bean.MotionTypeDialog;
import com.xh.hotme.record.bean.PlaceBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecordInfoActivity extends BaseActivity<ActivityRecordInfoBinding> implements IRecordControlListener {

    private final static String TAG = RecordInfoActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;


    BluetoothLoadingDialog _connectingDialog;
    RecordInfoBean _recordInfoBean;

    MotionTypeDialog _motionPlaceDialog;

    CityDialog _cityDialog;

    List<String> _motionTypeList;
    List<String> _placeTypeList;
    List<String> _playNumberList;

    List<CameraRecordInfoBean> _recordName;
    List<String> _historyNameList;


    private int _placeTypeIndex = 2;
    private int _motionTypeIndex = 1;

    private String _provinceCode = "440000000000";
    private String _cityCode = "440300000000";

    private final Runnable _timeOutRunnable = () -> {
        dismissConnectingDialog();
    };

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, RecordInfoActivity.class);
        ctx.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addPlayNotifyListener(this);
    }

    @Override
    protected void initView() {

        String[] motionTypeList = getResources().getStringArray(R.array.motion_type_list);
        String[] placeTypeList = getResources().getStringArray(R.array.place_type_list);
        String[] playNumberList = getResources().getStringArray(R.array.play_number_list);
        _motionTypeList = Arrays.asList(motionTypeList);
        _placeTypeList = Arrays.asList(placeTypeList);
        _playNumberList = Arrays.asList(playNumberList);

        _recordName = CompetitionManager.getCompetitionList(RecordInfoActivity.this);
        _historyNameList = new ArrayList<>();
        if(_recordName != null) {
            for(RecordInfoBean competitionName : _recordName) {
                _historyNameList.add(competitionName.getName());
            }
        }

        TitleLayoutHelper.initTitle(viewBinding.titleBar, getString(R.string.game_info_title));

        //比赛名称禁止EditText输入空格和换行符
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if(source.equals(" ") || source.toString().contentEquals("\n")) {
                return "";
            } else {
                return null;
            }
        };
        viewBinding.tvGameName.setFilters(new InputFilter[] { filter });
        // 比赛名称长度限制最多为20个字符
        viewBinding.tvGameName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });

        //场馆名称输入空格和换行符
        viewBinding.etAddress.setFilters(new InputFilter[] { filter });
        // 场馆名称长度限制最多为20个字符
        viewBinding.etAddress.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });


        _motionTypeIndex = 1;
        _placeTypeIndex = 2;
        viewBinding.etMotionType.setText("足球 8人场");
        viewBinding.llMotionType.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showMotionTypeDialog();
                return true;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.auto_complete_text_view_item, _historyNameList);
        viewBinding.tvGameName.setAdapter(adapter);

        viewBinding.etCity.setText("广东省深圳市");
        viewBinding.llCity.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_cityDialog != null && _cityDialog.isShowing()) {
                    return true;
                }
                _cityDialog = new CityDialog(RecordInfoActivity.this);
                _cityDialog.setTitle("选择城市").setSelectItem(_provinceCode, _cityCode).setCallback(new CityDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onConfirm(String provinceCode, String provinceCodeDisplay, String cityCode, String cityCodeDisplay) {
                        String cityInfo = provinceCodeDisplay + cityCodeDisplay;
                        if(provinceCodeDisplay.equalsIgnoreCase(cityCodeDisplay)) {
                            cityInfo = provinceCodeDisplay;
                        }
                        viewBinding.etCity.setText(cityInfo);

                        _provinceCode = provinceCode;
                        _cityCode = cityCode;
                    }
                }).show();

                return true;
            }
        });

        viewBinding.btnRecord.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String gameName = viewBinding.tvGameName.getText().toString();
                if(TextUtils.isEmpty(gameName)) {
                    ToastUtil.s(getString(R.string.hint_record_info_team_name));
                    return true;
                }

                _recordInfoBean = new RecordInfoBean("", Constants.FEC_VIDEO_MAX_RATE);
                _recordInfoBean.author = "";
                _recordInfoBean.width = Constants.FEC_VIDEO_WIDTH;
                _recordInfoBean.height = Constants.FEC_VIDEO_HEIGHT;
                _recordInfoBean.place = viewBinding.etAddress.getText().toString();
                _recordInfoBean.name = gameName;
                _recordInfoBean.campaignName = SharedPrefs.getImpl().getDefaultMatchName();
                _recordInfoBean.motionType = _motionTypeIndex;
                _recordInfoBean.placeType = _placeTypeIndex;
                _recordInfoBean.city = viewBinding.etCity.getText().toString();  //城市
                _recordInfoBean.provinceId = _provinceCode;  //城市
                _recordInfoBean.cityId = _cityCode;  //城市
                _recordInfoBean.hostTeam = viewBinding.homeTeamName.getText().toString();
                _recordInfoBean.guestTeam = viewBinding.guestTeamName.getText().toString();
                _recordInfoBean.time = System.currentTimeMillis();

                _recordInfoBean.maxRate = VideoUtil.getFecMBR(_motionTypeIndex, _placeTypeIndex);
                _recordInfoBean.avs_mbr = VideoUtil.getAvsMBR(_motionTypeIndex, _placeTypeIndex);
                _recordInfoBean.live_mbr = VideoUtil.getLiveMBR(_motionTypeIndex, _placeTypeIndex);

                _recordInfoBean.ctype = Constants.COMPETITION_TYPE_RECORD;
                _recordInfoBean.hide_osd = SharedPrefs.getImpl().getBoolean(SharedPrefs.PREF_HIDE_OSD) ? 1: 0;

                // 不是足球的话, 人数默认弄个5人, 不然0人会被当成训练模式
                if(_motionTypeIndex != Constants.SPORT_FOOTBALL) {
                    _recordInfoBean.player_num = 5;
                } else {
                    try {
                        _recordInfoBean.player_num = Integer.parseInt(_playNumberList.get(_placeTypeIndex));
                    } catch(NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                String bluetoothId = CameraManager.getDeviceBluetoothId();

                boolean isAppend = CompetitionManager.isAppend(gameName, _recordName, bluetoothId);

                _recordInfoBean.append = isAppend;

                EventBus.getDefault().post(new RecordStartEvent(_recordInfoBean));

                CameraRecordInfoBean cameraRecordInfoBean = CameraRecordInfoBean.newInstance(_recordInfoBean);
                cameraRecordInfoBean.deviceId = CameraManager.getDeviceBluetoothId();

                CompetitionManager.addCompetition(RecordInfoActivity.this, cameraRecordInfoBean);


                RecordStartRequestBean recordStartRequestBean = _recordInfoBean.convertRecordStartRequestBean();
                boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);

                if(ok) {
                    // 训练模式时player num为0, 训练模式需要重启rkipc, 超时时间设置的长一点
                    _recordInfoBean.time = System.currentTimeMillis();
                    _handler.postDelayed(_timeOutRunnable, 15000);
                    showConnectingDialog(RecordInfoActivity.this.getString(R.string.record_launching));
                } else {
                    showDisconnectToast();
                }

                return true;
            }
        });
        viewBinding.tvGameName.setOnItemClickListener((parent, view, position, id) -> updateCompetition(_recordName.get(position)));
    }

    @Override
    protected void initData() {
        viewBinding.tvGameName.requestFocus();

        _handler.postDelayed(() -> viewBinding.tvGameName.showDropDown(), 500);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();


        BluetoothHandle.removePlayNotifyListener(this);

        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }

        _connectingDialog = null;

    }

    @Override
    public void onRecordVideoStart() {
        finish();
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordInfo) {

    }

    @Override
    public void onRecordVideoStop() {

    }

    @Override
    public void onRecordException(String msg) {

    }


    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(RecordInfoActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }


    private void showMotionTypeDialog() {
//        if (_motionTypeDialog != null && _motionTypeDialog.isShowing()) {
//            return;
//        }
//        _motionTypeDialog = new WheelViewDialog(GameInfoActivity.this);
//        _motionTypeDialog.setTitle(getString(R.string.game_info_select_motion_type)).setDataList(_motionTypeList, _motionTypeIndex).setCallback(new WheelViewDialog.ICallback() {
//            @Override
//            public void onCancel() {
//            }
//
//            @Override
//            public void onConfirm(int index, String content) {
//                _motionTypeIndex = index;
//                viewBinding.etMotionType.setText(content);
//            }
//        }).setSelectedItemPosition(_motionTypeIndex).showOnBottom();


        if(_motionPlaceDialog != null && _motionPlaceDialog.isShowing()) {
            return;
        }
        _motionPlaceDialog = new MotionTypeDialog(RecordInfoActivity.this);
        _motionPlaceDialog.setCallback(new MotionTypeDialog.ICallback() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onConfirm(MotionBean motionBean, PlaceBean placeBean) {
                _motionTypeIndex = motionBean.type;
                _placeTypeIndex = placeBean.type;
                viewBinding.etMotionType.setText(motionBean.name + " " + placeBean.name);
            }
        }).show();

        _motionPlaceDialog.setSelectItem(_motionTypeIndex, _placeTypeIndex);
    }

    private void updateCompetition(RecordInfoBean recordInfoBean) {
        try {
            _motionTypeIndex = recordInfoBean.motionType;
            if(_motionTypeIndex >= _motionTypeList.size()) {
                _motionTypeIndex = 1;
            }

            _placeTypeIndex = recordInfoBean.placeType;

            viewBinding.etMotionType.setText(_motionTypeList.get(_motionTypeIndex) + " " + VideoUtil.getPlaceNameByType(_motionTypeIndex, _placeTypeIndex));
        } catch(Throwable e) {

        }

        if(!TextUtils.isEmpty(recordInfoBean.city)) {
            viewBinding.etCity.setText(recordInfoBean.city);
        }
        if(!TextUtils.isEmpty(recordInfoBean.place)) {
            viewBinding.etAddress.setText(recordInfoBean.place);
        }
        if(!TextUtils.isEmpty(recordInfoBean.hostTeam)) {
            viewBinding.homeTeamName.setText(recordInfoBean.hostTeam);
        }
        if(!TextUtils.isEmpty(recordInfoBean.guestTeam)) {
            viewBinding.guestTeamName.setText(recordInfoBean.guestTeam);
        }

        _provinceCode = recordInfoBean.provinceId;
        _cityCode = recordInfoBean.cityId;

    }
}
