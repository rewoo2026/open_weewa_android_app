package com.xh.hotme.record;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.FecRtmpAddUrlRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.event.FecRtmpAddUrlEvent;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityRecordInfoBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.LiveTaskBean;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.CityDialog;
import com.xh.hotme.record.bean.MotionBean;
import com.xh.hotme.record.bean.MotionTypeDialog;
import com.xh.hotme.record.bean.PlaceBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@IEventBus
public class AddRecordTaskActivity extends BaseActivity<ActivityRecordInfoBinding> implements IRecordControlListener {

    private final static String TAG = AddRecordTaskActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    public static final String EXTRA_EDIT_BEAN = "edit_bean";
    public static final String EXTRA_HISTORY_LIST = "history_list";


    BluetoothLoadingDialog _connectingDialog;
    RecordInfoBean _recordInfoBean;

    MotionTypeDialog _motionPlaceDialog;

    CityDialog _cityDialog;

    List<String> _motionTypeList;
    List<String> _placeTypeList;
    List<String> _playNumberList;

    List<CameraRecordInfoBean> _recordName;
    List<String> _historyNameList;


    HmCompetitionRecordBean _oriBean;

    private int _placeTypeIndex = 2;
    private int _motionTypeIndex = 1;

    private String _provinceCode = "440000000000";
    private String _cityCode = "440300000000";

    String competitionStatus = "" + Constants.COMPETITION_UNSET;

    // 当前的bean(编辑后的, 或者新加的, 都有可能)
    HmCompetitionLive _bean;

    private String str_timeout_lunch_rkipc;
    private String str_lunch_rkipc_fail;
    private String str_add_fec_url_fail;
    // 0=idle, 1=status query, 2=server task, 3=legacy recorder,
    // 4=legacy record_start, 5=server start, 6=cancelling, 7=local start, 8=usage check.
    private int _recordStartStage;
    private int _recordStartAttempt;
    private boolean _coordinatedRecording;
    private boolean _localRecording;
    private boolean _unattended;
    private Long _selectedStartAt, _selectedEndAt;
    private LocalRecordingWindow _recordingWindow;
    private long _observedDeviceVersion;
    private LocalRecordStarter _localStarter;
    private CoordinatedRecordStarter _coordinatedStarter;

    private final Runnable _timeOutRunnable = () -> {
        failRecordStart("录制启动尚未确认，请检查设备状态后重试");
    };

    private final Runnable _startRkipcTimeOutRunnable = () -> {
        failRecordStart(str_timeout_lunch_rkipc);
    };


    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, AddRecordTaskActivity.class);
        ctx.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("record_start_at")) _selectedStartAt = savedInstanceState.getLong("record_start_at");
            if (savedInstanceState.containsKey("record_end_at")) _selectedEndAt = savedInstanceState.getLong("record_end_at");
            updateTimeInputs();
        }
        BluetoothHandle.addPlayNotifyListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (_selectedStartAt != null) state.putLong("record_start_at", _selectedStartAt);
        if (_selectedEndAt != null) state.putLong("record_end_at", _selectedEndAt);
    }

    @Override
    protected void initView() {
        str_timeout_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_rkipc);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);
        str_add_fec_url_fail = getString(R.string.info_add_fec_url_fail);

        String[] motionTypeList = getResources().getStringArray(R.array.motion_type_list);
        String[] placeTypeList = getResources().getStringArray(R.array.place_type_list);
        String[] playNumberList = getResources().getStringArray(R.array.play_number_list);
        _motionTypeList = Arrays.asList(motionTypeList);
        _placeTypeList = Arrays.asList(placeTypeList);
        _playNumberList = Arrays.asList(playNumberList);

        _recordName = CompetitionManager.getCompetitionList(AddRecordTaskActivity.this);
        _historyNameList = new ArrayList<>();
        if (_recordName != null) {
            for (RecordInfoBean competitionName : _recordName) {
                _historyNameList.add(competitionName.getName());
            }
        }

        TitleLayoutHelper.initTitle(viewBinding.titleBar, getString(R.string.game_info_title));
        viewBinding.unattendedMode.getRoot().setVisibility(android.view.View.VISIBLE);
        viewBinding.unattendedMode.unattendedHint.setText(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL
                ? R.string.unattended_record_hint : R.string.unattended_remote_record_hint);
        viewBinding.localRecordWindow.getRoot().setVisibility(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL
                ? View.VISIBLE : View.GONE);
        viewBinding.localRecordWindow.recordStartTime.setOnClickListener(v -> selectRecordingTime(true));
        viewBinding.localRecordWindow.recordEndTime.setOnClickListener(v -> selectRecordingTime(false));
        viewBinding.localRecordWindow.clearRecordStartTime.setOnClickListener(v -> {
            if (_recordStartStage == 0) { _selectedStartAt = null; updateTimeInputs(); }
        });
        viewBinding.localRecordWindow.clearRecordEndTime.setOnClickListener(v -> {
            if (_recordStartStage == 0) { _selectedEndAt = null; updateTimeInputs(); }
        });

        //比赛名称禁止EditText输入空格和换行符
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (source.equals(" ") || source.toString().contentEquals("\n")) {
                return "";
            } else {
                return null;
            }
        };
        viewBinding.tvGameName.setFilters(new InputFilter[]{filter});
        // 比赛名称长度限制最多为20个字符
        viewBinding.tvGameName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        //场馆名称输入空格和换行符
        viewBinding.etAddress.setFilters(new InputFilter[]{filter});
        // 场馆名称长度限制最多为20个字符
        viewBinding.etAddress.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        viewBinding.llMotionType.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showMotionTypeDialog();
                return true;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.auto_complete_text_view_item, _historyNameList);
        viewBinding.tvGameName.setAdapter(adapter);

        viewBinding.llCity.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (_cityDialog != null && _cityDialog.isShowing()) {
                    return true;
                }
                _cityDialog = new CityDialog(AddRecordTaskActivity.this);
                _cityDialog.setTitle("选择城市").setSelectItem(_provinceCode, _cityCode).setCallback(new CityDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onConfirm(String provinceCode, String provinceCodeDisplay, String cityCode, String cityCodeDisplay) {
                        String cityInfo = provinceCodeDisplay + cityCodeDisplay;
                        if (provinceCodeDisplay.equalsIgnoreCase(cityCodeDisplay)) {
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

                if (_recordStartStage != 0) return true;
                _unattended = viewBinding.unattendedMode.switchUnattended.isChecked();
                if (TextUtils.isEmpty(viewBinding.tvGameName.getText().toString().trim())) {
                    ToastUtil.s(getString(R.string.hint_record_info_team_name));
                    return true;
                }

                if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    long now = System.currentTimeMillis();
                    try {
                        _recordingWindow = LocalRecordingWindow.resolve(_selectedStartAt, _selectedEndAt, now);
                    } catch (IllegalArgumentException | ArithmeticException e) {
                        ToastUtil.s(getString(R.string.local_record_invalid_window));
                        return true;
                    }
                    if (_unattended && now >= _recordingWindow.endAt) {
                        ToastUtil.s(getString(R.string.local_record_window_expired));
                        return true;
                    }
                }

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                _recordStartStage = 1;
                _recordStartAttempt++;
                _localStarter = null;
                _coordinatedStarter = null;
                _coordinatedRecording = false;
                _localRecording = false;
                _recordInfoBean = null;
                viewBinding.btnRecord.setEnabled(false);
                viewBinding.unattendedMode.switchUnattended.setEnabled(false);
                showConnectingDialog(getString(R.string.record_status));
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                if (!DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean())) {
                    failRecordStart(getString(R.string.info_bluetooth_offline));
                }

                return true;
            }
        });
        viewBinding.tvGameName.setOnItemClickListener((parent, view, position, id) -> updateCompetition(_recordName.get(position)));
    }

    private void selectRecordingTime(boolean start) {
        if (_recordStartStage != 0) return;
        Long selected = start ? _selectedStartAt : _selectedEndAt;
        long initial = selected != null ? selected : start ? System.currentTimeMillis()
                : (_selectedStartAt != null ? _selectedStartAt : System.currentTimeMillis()) + LocalRecordingWindow.DEFAULT_DURATION_MS;
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(initial);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        new TimePickerBuilder(this, (value, view) -> {
            if (start) _selectedStartAt = value.getTime(); else _selectedEndAt = value.getTime();
            updateTimeInputs();
        }).setDate(date)
                .setTitleText(getString(start ? R.string.local_record_start_time : R.string.local_record_end_time))
                .setType(new boolean[]{true, true, true, true, true, false})
                .setMinuteStep(1)
                .build().show();
    }

    private void updateTimeInputs() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        viewBinding.localRecordWindow.recordStartTime.setText(_selectedStartAt == null ? "" : format.format(new Date(_selectedStartAt)));
        viewBinding.localRecordWindow.recordEndTime.setText(_selectedEndAt == null ? "" : format.format(new Date(_selectedEndAt)));
        viewBinding.localRecordWindow.clearRecordStartTime.setVisibility(_selectedStartAt == null ? View.GONE : View.VISIBLE);
        viewBinding.localRecordWindow.clearRecordEndTime.setVisibility(_selectedEndAt == null ? View.GONE : View.VISIBLE);
    }

    private void initDefaultData() {
        _motionTypeIndex = 1;
        _placeTypeIndex = 2;
        viewBinding.etMotionType.setText("足球 8人场");
        viewBinding.etCity.setText("广东省深圳市");
        _provinceCode = "440000000000";
        _cityCode = "440300000000";
    }

    @Override
    protected void initData() {


        if (getIntent().hasExtra(EXTRA_EDIT_BEAN)) {
            _oriBean = (HmCompetitionRecordBean) getIntent().getSerializableExtra(EXTRA_EDIT_BEAN);
            if (_oriBean != null) {
                updateCompetition(_oriBean);
            } else {
                initDefaultData();
                viewBinding.tvGameName.requestFocus();
                _handler.postDelayed(() -> viewBinding.tvGameName.showDropDown(), 500);
            }

        } else {
            initDefaultData();
            viewBinding.tvGameName.requestFocus();
            _handler.postDelayed(() -> viewBinding.tvGameName.showDropDown(), 500);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onDestroy() {
        _recordStartStage = 0;
        if (_coordinatedStarter != null) _coordinatedStarter.cancel();
        if (_localStarter != null) _localStarter.cancel();
        super.onDestroy();

        BluetoothHandle.removePlayNotifyListener(this);

        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }

        _connectingDialog = null;

    }

    @Override
    public void onRecordVideoStart() {
        runOnUiThread(() -> {
            if (_recordStartStage == 4) completeRecordStart(false);
        });
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordInfo) {
        runOnUiThread(() -> {
            if (_recordStartStage != 1) return;
            _handler.removeCallbacks(_timeOutRunnable);
            if (recordInfo == null || !"ok".equalsIgnoreCase(recordInfo.getR())) {
                failRecordStart("无法获取相机录制状态，请重试");
                return;
            }
            if (recordInfo.getStatus() > 0 || (recordInfo.recordingProtocolVersion >= 4 && !"idle".equals(recordInfo.deviceState))) {
                failRecordStart("相机正在录制，请先结束当前录制");
                return;
            }
            _localRecording = Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL && recordInfo.localControlSupported == 1;
            _observedDeviceVersion = recordInfo.deviceVersion;
            if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL && recordInfo.recordingProtocolVersion >= 3 && !_localRecording) {
                failRecordStart("相机固件暂不支持本地录制授权，请先更新相机固件");
                return;
            }
            _coordinatedRecording = recordInfo.recordingProtocolVersion >= 3;
            if (_localRecording) {
                if(recordInfo.recordingProtocolVersion<5){failRecordStart("请更新相机固件后使用每日离线录制授权");return;}
                final com.xh.hotme.account.DailyAccessManager.DeviceAccess access=com.xh.hotme.account.DailyAccessManager.device(CameraManager.getDeviceId());
                if(access==null){failRecordStart("请先断开相机热点，联网同步今日设备权限");return;}
                if(!access.recordingAllowed||access.permit==null){failRecordStart(access.recordingError==null?"录制套餐不可用":access.recordingError);return;}
                addWorkflowStep1_validate();
            } else if (_coordinatedRecording || Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
                addWorkflowStep1_validate();
            } else {
                startRecord();
            }
        });
    }

    @Override
    public void onRecordVideoStop() {

    }

    @Override
    public void onRecordException(String msg) {
        runOnUiThread(() -> {
            if (_recordStartStage == 4 && "device_version required".equals(msg)) {
                if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    failRecordStart("相机固件暂不支持本地录制授权，请先更新相机固件");
                    return;
                }
                _handler.removeCallbacks(_timeOutRunnable);
                _coordinatedRecording = true;
                addWorkflowStep1_validate();
            } else if (_recordStartStage > 0 && _recordStartStage != 5 && _recordStartStage != 7) {
                failRecordStart(msg);
            }
        });
    }

    private void failRecordStart(String message) {
        if (_recordStartStage == 0 || _recordStartStage == 6) return;
        _recordStartStage = 6;
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        dismissConnectingDialog();
        DialogUtil.dismissDialog();
        ToastUtil.s(TextUtils.isEmpty(message) ? "启动录制失败，请检查设备状态" : message);
        Runnable recovered = () -> {
            if (isFinishing() || isDestroyed()) return;
            _recordStartStage = 0;
            viewBinding.btnRecord.setEnabled(true);
            viewBinding.unattendedMode.switchUnattended.setEnabled(true);
        };
        if (_localStarter != null) _localStarter.cancel(recovered);
        else if (_coordinatedStarter != null) _coordinatedStarter.cancel(recovered);
        else recovered.run();
    }

    private void completeRecordStart(boolean notifyRecorder) {
        if (_recordStartStage == 0 || _recordInfoBean == null) return;
        _recordStartStage = 0;
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        dismissConnectingDialog();
        ScoreCenterUtil.removeCustomDotRecord();
        EventBus.getDefault().post(new RecordStartEvent(_recordInfoBean));
        CameraRecordInfoBean history = CameraRecordInfoBean.newInstance(_recordInfoBean);
        history.deviceId = CameraManager.getDeviceBluetoothId();
        CompetitionManager.addCompetition(this, history);
        if (notifyRecorder) BluetoothHandle.onRecordStartNotify();
        finish();
    }


    private void showConnectingDialog(String message) {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(AddRecordTaskActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.setOnClickListener((dialog, which) -> failRecordStart("已取消启动录制"));
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
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


        if (_motionPlaceDialog != null && _motionPlaceDialog.isShowing()) {
            return;
        }
        _motionPlaceDialog = new MotionTypeDialog(AddRecordTaskActivity.this);
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
            if (_motionTypeIndex >= _motionTypeList.size()) {
                _motionTypeIndex = 1;
            }
            _placeTypeIndex = recordInfoBean.placeType;

            viewBinding.etMotionType.setText(_motionTypeList.get(_motionTypeIndex) + " " + VideoUtil.getPlaceNameByType(_motionTypeIndex, _placeTypeIndex));
        } catch (Throwable e) {

        }

        if (!TextUtils.isEmpty(recordInfoBean.city)) {
            viewBinding.etCity.setText(recordInfoBean.city);
        }
        if (!TextUtils.isEmpty(recordInfoBean.place)) {
            viewBinding.etAddress.setText(recordInfoBean.place);
        }
        if (!TextUtils.isEmpty(recordInfoBean.hostTeam)) {
            viewBinding.homeTeamName.setText(recordInfoBean.hostTeam);
        }
        if (!TextUtils.isEmpty(recordInfoBean.guestTeam)) {
            viewBinding.guestTeamName.setText(recordInfoBean.guestTeam);
        }

        _provinceCode = recordInfoBean.provinceId;
        _cityCode = recordInfoBean.cityId;

    }

    private void updateCompetition(HmCompetitionRecordBean recordInfoBean) {

        viewBinding.tvGameName.setText(recordInfoBean.competitionName);

        try {
            _motionTypeIndex = Integer.parseInt(recordInfoBean.motionType);
            if (_motionTypeIndex >= _motionTypeList.size()) {
                _motionTypeIndex = 1;
            }

            _placeTypeIndex = Integer.parseInt(recordInfoBean.placeType);

            viewBinding.etMotionType.setText(_motionTypeList.get(_motionTypeIndex) + " " + VideoUtil.getPlaceNameByType(_motionTypeIndex, _placeTypeIndex));
        } catch (Throwable e) {

        }

        if (!TextUtils.isEmpty(recordInfoBean.cityCodeDisplay)) {
            viewBinding.etCity.setText(recordInfoBean.provinceCodeDisplay + recordInfoBean.cityCodeDisplay);
        }

        if (!TextUtils.isEmpty(recordInfoBean.place)) {
            viewBinding.etAddress.setText(recordInfoBean.place);
        }
        if (!TextUtils.isEmpty(recordInfoBean.homeTeamName)) {
            viewBinding.homeTeamName.setText(recordInfoBean.homeTeamName);
        }
        if (!TextUtils.isEmpty(recordInfoBean.awayTeamName)) {
            viewBinding.guestTeamName.setText(recordInfoBean.awayTeamName);
        }

        _provinceCode = recordInfoBean.provinceCode;
        _cityCode = recordInfoBean.cityCode;

    }

    private void startRecord(){
        _handler.removeCallbacks(_timeOutRunnable);
        String gameName = viewBinding.tvGameName.getText().toString().trim();
        if (TextUtils.isEmpty(gameName)) {
            failRecordStart(getString(R.string.hint_record_info_team_name));
            return;
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

        if (_oriBean != null) {
            _recordInfoBean.live_cid = _oriBean.competitionId;
        } else if(_bean!=null){
            _recordInfoBean.live_cid = _bean.competitionId;
        }else {
            _recordInfoBean.live_cid = competitionId;
        }

        Log.d(TAG, "startRecord: 传递比赛id" + _recordInfoBean.live_cid);
        _recordInfoBean.ctype = Constants.COMPETITION_TYPE_RECORD;

        // 不是足球的话, 人数默认弄个5人, 不然0人会被当成训练模式
        if (_motionTypeIndex != Constants.SPORT_FOOTBALL) {
            _recordInfoBean.player_num = 5;
        } else {
            try {
                _recordInfoBean.player_num = Integer.parseInt(_playNumberList.get(_placeTypeIndex));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        String bluetoothId = CameraManager.getDeviceBluetoothId();

        boolean isAppend = CompetitionManager.isAppend(gameName, _recordName, bluetoothId);

        _recordInfoBean.append = isAppend;

        if (_unattended && Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (System.currentTimeMillis() >= _recordingWindow.endAt) {
                failRecordStart(getString(R.string.local_record_window_expired));
                return;
            }
            UnattendedRecordTask task = UnattendedRecordTask.fromRecording(_recordInfoBean, _localRecording, _recordingWindow);
            String error = UnattendedRecordService.schedule(this, task);
            if (error != null) { failRecordStart(error); return; }
            _recordStartStage = 0;
            DialogUtil.dismissDialog();
            dismissConnectingDialog();
            ToastUtil.s(getString(R.string.local_record_scheduled));
            finish();
            return;
        }

        if (_localRecording) {
            _recordStartStage = 7;
            DialogUtil.dismissDialog();
            showConnectingDialog(getString(R.string.record_launching));
            final int attempt = _recordStartAttempt;
            _localStarter = LocalRecordStarter.createOffline(_recordInfoBean.convertRecordStartRequestBean(), new LocalRecordStarter.Listener() {
                public void onStarted() {
                    if (attempt == _recordStartAttempt && _recordStartStage == 7) {competitionId=_localStarter.competitionId();_recordInfoBean.live_cid=competitionId;completeRecordStart(true);}
                }
                public void onFailure(String message) {
                    if (attempt == _recordStartAttempt && _recordStartStage == 7) failRecordStart(message);
                }
                public void onWarning(String message) { ToastUtil.s(message); }
            }, com.xh.hotme.account.DailyAccessManager.device(CameraManager.getDeviceId()));
            _localStarter.start(_recordInfoBean.live_cid, _observedDeviceVersion);
            return;
        }

        if (_coordinatedRecording) {
            _recordStartStage = 5;
            _handler.removeCallbacks(_timeOutRunnable);
            DialogUtil.dismissDialog();
            showConnectingDialog(getString(R.string.record_launching));
            final int attempt = _recordStartAttempt;
            _coordinatedStarter = CoordinatedRecordStarter.create(_handler, new CoordinatedRecordStarter.Listener() {
                public void onStarted() {
                    if (attempt == _recordStartAttempt && _recordStartStage == 5) completeRecordStart(true);
                }
                public void onFailure(String message) {
                    if (attempt == _recordStartAttempt && _recordStartStage == 5) failRecordStart(message);
                }
            });
            _coordinatedStarter.start(_recordInfoBean.live_cid);
            return;
        }

        _recordStartStage = 4;
        RecordStartRequestBean recordStartRequestBean = _recordInfoBean.convertRecordStartRequestBean();


        boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);
        if (ok) {
            // 训练模式时player num为0, 训练模式需要重启rkipc, 超时时间设置的长一点
            _handler.postDelayed(_timeOutRunnable, 60000);
            showConnectingDialog(AddRecordTaskActivity.this.getString(R.string.record_launching));
        } else {
            failRecordStart(getString(R.string.info_bluetooth_offline));
        }

    }

    private static final class AddResult {
        boolean needFinish = false;
        boolean duplicateCompetitionName = false;
        boolean needRefresh3rdLiveUrl = false;
        String errMsg = null;
    }

    private LiveUrlEvent _urlEvent;
    private AddResult _addResult = null;

    long hour = 0L;

    private void addWorkflowStep1_validate() {
        _recordStartStage = 2;
        _handler.removeCallbacks(_timeOutRunnable);
        _handler.postDelayed(_timeOutRunnable, 120000);
        DialogUtil.showDialog("");
        _addResult = new AddResult();
        do {
            String gameName = viewBinding.tvGameName.getText().toString();
            if (TextUtils.isEmpty(gameName)) {
                _addResult.errMsg = getString(R.string.hint_record_info_team_name);
                failRecordStart(_addResult.errMsg);
                return;
            }

        } while(false);

        // 多加一个创建比赛
//        addWorkflowStep1_getPullStreamUrl();
        if(_localRecording){competitionId="";startRecord();}else addWorkflowStep1_getComId();
    }

    String competitionId = "";

    private void addWorkflowStep1_getComId() {
        final int attempt = _recordStartAttempt;
        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("enableAnalyse", CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS) ? "1" : "0");
        paramsdata.put("autoPushStream", _unattended && Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE ? "1" : "0");
        if (_localRecording) {
            paramsdata.put("autoPushStream", "0");
            paramsdata.put("cloudUploadRequested", String.valueOf(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD)));
        }
        paramsdata.put("competitionName", viewBinding.tvGameName.getText().toString());
        paramsdata.put("motionType", _motionTypeIndex + "");
        paramsdata.put("placeType", _placeTypeIndex + "");
        paramsdata.put("place", viewBinding.etAddress.getText().toString());
        paramsdata.put("homeTeamName", viewBinding.homeTeamName.getText().toString());
        paramsdata.put("awayTeamName", viewBinding.guestTeamName.getText().toString());
        paramsdata.put("cityCode", _cityCode);
        paramsdata.put("provinceCode", _provinceCode);
        String url;
        if(_oriBean != null) {
            url = SdkApi.competitionRecord_update();
            paramsdata.put("competitionId", _oriBean.competitionId);
        } else if(_bean != null) {
            url = SdkApi.competitionRecord_update();
            paramsdata.put("competitionId", _bean.competitionId);
        } else if (!TextUtils.isEmpty(competitionId)) {
            // Retry the task already created for this form rather than adding
            // another task with the same name after a rejected/late start.
            url = SdkApi.competitionRecord_update();
            paramsdata.put("competitionId", competitionId);
        } else {
            url = _localRecording ? SdkApi.addLocalCompetitionRecord() : SdkApi.competitionRecord_add();
        }        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        paramsdata.put("competitionTime", time);

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                runOnUiThread(() -> {
                    if (_recordStartStage != 2 || attempt != _recordStartAttempt) return;
                    if (data == null || data.data == null || TextUtils.isEmpty(data.data.competitionId)) {
                        failRecordStart("创建录制任务失败，请重试");
                        return;
                    }
                    // 获取到id  addWorkflowStep1_getPullStreamUrl();
                    Log.d(TAG, "onDataSuccess: 获取录制比赛id" + data.data.competitionId);
                    competitionId = data.data.competitionId;
                    if (_coordinatedRecording) startRecord();
                    else addWorkflowStep1_getPullStreamUrl();
                });
            }

            @Override
            public void onFailure(String code, String message) {
                runOnUiThread(() -> {
                    if (_recordStartStage != 2 || attempt != _recordStartAttempt) return;
                    _addResult.errMsg = message;
                    if(!TextUtils.isEmpty(code) && code.equals("201")) {
                        _addResult.duplicateCompetitionName = true;
                    }
                    addWorkflowStep10_wrapUp();
                });
            }
        });


    }


    private void addWorkflowStep1_getPullStreamUrl(){
        final int attempt = _recordStartAttempt;
        // 从设备获取改为比赛id获取
        ApiUtil.getPullStreamUrlWithDeviceIdV2(CameraManager.getDeviceId(), competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
//        ApiUtil.getPullStreamUrlWithoutCommentary(competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {

                runOnUiThread(() -> {
                    if (_recordStartStage != 2 || attempt != _recordStartAttempt) return;
                    if (data == null || TextUtils.isEmpty(data.data)) {
                        failRecordStart("无法获取相机预览地址，请重试");
                        return;
                    }
                    _urlEvent = new LiveUrlEvent();
                    _urlEvent.url_pull = data.data;

                    addWorkflowStep2_startRkipc();
                });
            }

            @Override
            public void onFail(String code, String message) {
                runOnUiThread(() -> {
                    if (_recordStartStage != 2 || attempt != _recordStartAttempt) return;
                    _addResult.errMsg = message;
                    if(!TextUtils.isEmpty(code) && code.equals("201")) {
                        _addResult.duplicateCompetitionName = true;
                    }
                    addWorkflowStep10_wrapUp();
                });
            }

            @Override
            public void onFinish() {

            }
        });
    }

    private void addWorkflowStep2_startRkipc() {
        _recordStartStage = 3;
        _handler.removeCallbacks(_timeOutRunnable);
        // start rkipc
        boolean ok = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
        if(ok) {
            _handler.postDelayed(_startRkipcTimeOutRunnable,
                    BluetoothManager.RESP_TIMEOUT_SHORT * 5);
            showConnectingDialog(getString(R.string.info_ready_to_record));
        } else {
            String error = getString(R.string.info_bluetooth_offline);
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                error = getString(R.string.info_network_disconnect);
            }
            _addResult.errMsg = error;
        }

        // wrap up
        addWorkflowStep10_wrapUp();
    }

    private void addWorkflowStep10_wrapUp() {
        DialogUtil.dismissDialog();
        if(_addResult.duplicateCompetitionName) {
            showHintDialog("比赛名称重复", _addResult.errMsg, "好的", null);
        } else if(!TextUtils.isEmpty(_addResult.errMsg)) {
            ToastUtil.s(_addResult.errMsg);
        }
        if (_addResult.duplicateCompetitionName || !TextUtils.isEmpty(_addResult.errMsg)) {
            failRecordStart(_addResult.errMsg);
        }

        // finish activity if needed
        if(_addResult.needFinish) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRKIpcStart(RkIpcEvent event) {
        AppTrace.d(TAG, "onRKIpcStart");
        runOnUiThread(() -> {
            if(_recordStartStage == 3 && event != null && _urlEvent != null) {
                _handler.removeCallbacks(_startRkipcTimeOutRunnable);
                AppTrace.d(TAG, "onRkipcStart  " + _urlEvent.url_pull);
                if(event.status == 1) {
                    EventBus.getDefault().post(_urlEvent);
                    startRecord();
                } else {
                    failRecordStart(str_lunch_rkipc_fail);
                }
            }
        });
    }
}
