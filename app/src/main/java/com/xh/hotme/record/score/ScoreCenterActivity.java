package com.xh.hotme.record.score;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.base.BLEAware;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.GameStatusBean;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GameScoreRemoveBean;
import com.xh.hotme.bluetooth.bean.GameScoreStatAddBean;
import com.xh.hotme.bluetooth.bean.GameStatAddBean;
import com.xh.hotme.bluetooth.bean.GameStatEditBean;
import com.xh.hotme.bluetooth.bean.GameStatListRequestBean;
import com.xh.hotme.bluetooth.bean.GameTagRequestBean;
import com.xh.hotme.bluetooth.bean.GetOSDSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.MatchTimePause;
import com.xh.hotme.bluetooth.bean.MatchTimeResume;
import com.xh.hotme.bluetooth.bean.MatchTimeReset;
import com.xh.hotme.bluetooth.bean.OSDControlRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.event.AddMarkEvent;
import com.xh.hotme.bluetooth.event.CompetitionLabelEvent;
import com.xh.hotme.bluetooth.event.GameScoreAddEvent;
import com.xh.hotme.bluetooth.event.GameScoreClearEvent;
import com.xh.hotme.bluetooth.event.GameScoreRemoveEvent;
import com.xh.hotme.bluetooth.event.GameScoreStatAddEvent;
import com.xh.hotme.bluetooth.event.GameStatAddEvent;
import com.xh.hotme.bluetooth.event.GameStatEditEvent;
import com.xh.hotme.bluetooth.event.GameStatListEvent;
import com.xh.hotme.bluetooth.event.GameStatRemoveEvent;
import com.xh.hotme.bluetooth.event.GetOSDSettingsEvent;
import com.xh.hotme.bluetooth.event.MatchTimePauseEvent;
import com.xh.hotme.bluetooth.event.MatchTimeResetEvent;
import com.xh.hotme.bluetooth.event.MatchTimeResumeEvent;
import com.xh.hotme.bluetooth.event.OSDControlEvent;
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.databinding.ActivityScoreConterBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CompetitionMarkBean;
import com.xh.hotme.live.bean.CompetitionMarkListBean;
import com.xh.hotme.live.bean.HmCompetitionPoint;
import com.xh.hotme.record.bean.DotTimeEditDialog;
import com.xh.hotme.record.bean.HmCompetitionMark;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ColorUtil;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.RecycleViewDivider;
import com.xh.hotme.widget.ScoreCounterView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BLEAware
@IEventBus
public class ScoreCenterActivity extends BaseActivity<ActivityScoreConterBinding> implements IRecordControlListener {

    private final static String TAG = ScoreCenterActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    private static final String RECORD_INFO = "record_info";

    RecordInfoBean _recordInfo;

    int _recordTime = 0;
    int _marchTime = 0;

    boolean isHomeRequest = false;

    RecordStatusResultBean _recordStatus;

    GoalItemAdapter _adapter;

    final ArrayList<GameStatusListBean> _goalList = new ArrayList();
    final ArrayList<GameStatusListBean> _stateList = new ArrayList();

    DotTimeEditDialog timeEditDialog;

    public String hostTeamName;
    public String guestTeamName;

    private String _gameTag;

    List<Integer> _statTypeIntList;

    int retryAddScoreNum = 0;
    int retryAddStateNum = 0;

    ModalDialog _dialog;

    private final Runnable _recordTimeRunnable = new Runnable() {
        @Override
        public void run() {

            _recordTime += 1;
            _marchTime += 1;
            updateRecordTime();
            if(_handler != null) {
                _handler.postDelayed(_recordTimeRunnable, 1000);
            }
        }
    };

    private final Runnable _timeOutRunnable = () -> DialogUtil.dismissDialog();

    private final Runnable _dotTimeOutRunnable = () -> {
        AppTrace.d(TAG, "dotTimeOutRunnable active....");
        retryPendingRequestBean();

        DialogUtil.dismissDialog();
    };

    List<String> _tagTypeList;
    PopupWindow popupWindow;
    GameTagAdapter _tagAdapter;

    int tagIndex = 0;

    HmCompetitionMark _competitionMark;

    boolean _isTimerPause = false;

    public static void start(Context ctx, RecordInfoBean recordInfo) {
        Intent intent = new Intent(ctx, ScoreCenterActivity.class);
        intent.putExtra(RECORD_INFO, GsonUtils.GsonString(recordInfo));
        ctx.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        BluetoothHandle.addPlayNotifyListener(this);

        // refresh osd settings
        DeviceApiManager.sendCmdSimpleObj(new GetOSDSettingsRequestBean());
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitleBack(viewBinding.titleBar, getString(R.string.score_control_center));
        viewBinding.titleBar.ivRight.setVisibility(View.GONE);
        viewBinding.titleBar.tvRight.setVisibility(View.VISIBLE);
        viewBinding.titleBar.tvRight.setText(getString(R.string.advance_score));
        viewBinding.titleBar.tvRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        viewBinding.titleBar.tvRight.setTextColor(ColorUtil.parseColor("#F16C2F"));
        viewBinding.titleBar.tvRight.setCompoundDrawablePadding(4);

        //获取更换的图片
        Drawable drawable = getResources().getDrawable(R.mipmap.score_center_advance);
        //setBounds(x,y,width,height)
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        //mDownLoad是控件的名称,setCompoundDrawables(left,top,right,bottom)
        viewBinding.titleBar.tvRight.setCompoundDrawables(drawable, null, null, null);
        viewBinding.toggleShowTeamName.setSelected(true);
        viewBinding.toggleShowScore.setSelected(true);
        
        // 初始化计时控制按钮
        viewBinding.btnPauseResumeTimer.setOnClickListener(v -> {
            handlePauseResumeTimer();
        });
        
        viewBinding.btnResetTimer.setOnClickListener(v -> {
            handleResetTimer();
        });
    }

    @Override
    protected void initData() {
        if (getIntent() != null) {
            String recordInfoStr = (String) getIntent().getSerializableExtra(RECORD_INFO);
            _recordInfo = GsonUtils.GsonToBean(recordInfoStr, RecordInfoBean.class);
        }
        if (_recordInfo == null) {
            finish();
            return;
        }
        _statTypeIntList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            _statTypeIntList.add(i);
            GameStatusBean gameStatusBean = new GameStatusBean(i);
        }
        _statTypeIntList.add(Constants.STAT_CUSTOM_1);
        _statTypeIntList.add(Constants.STAT_CUSTOM_2);
        _statTypeIntList.add(Constants.STAT_CUSTOM_3);

        String[] tagTypeList = getResources().getStringArray(R.array.game_tag_list);
        _tagTypeList = Arrays.asList(tagTypeList);

        viewBinding.titleBar.tvRight.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_recordInfo == null) {
                    ToastUtil.s(getString(R.string.error_record_info_empty));
                    return true;
                }

                ScoreCenterSuperActivity.start(ScoreCenterActivity.this, _recordInfo, _recordStatus, _competitionMark);
                return true;
            }
        });

        viewBinding.toggleShowScore.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.toggleShowScore.setSelected(!viewBinding.toggleShowScore.isSelected());
                OSDControlRequestBean bean = new OSDControlRequestBean();
                bean.setHide_score(viewBinding.toggleShowScore.isSelected() ? 0 : 1);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(!isOk) {
                    ToastUtil.s(getString(R.string.device_offline));
                } else {
                    DialogUtil.showDialog(getString(R.string.loading));
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                }
                return true;
            }
        });
        viewBinding.toggleShowTeamName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.toggleShowTeamName.setSelected(!viewBinding.toggleShowTeamName.isSelected());
                OSDControlRequestBean bean = new OSDControlRequestBean();
                bean.setHide_team(viewBinding.toggleShowTeamName.isSelected() ? 0 : 1);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(!isOk) {
                    ToastUtil.s(getString(R.string.device_offline));
                } else {
                    DialogUtil.showDialog(getString(R.string.loading));
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                }
                return true;
            }
        });

        long recordTimeStamp = _recordInfo.time;

        _recordTime = (int) (System.currentTimeMillis() - recordTimeStamp) / 1000;
        updateRecordTime();

        resetTag();

        viewBinding.tvGameName.setText(_recordInfo.name);
        viewBinding.tvGameName.setSelected(true);


        if(!TextUtils.isEmpty(_recordInfo.hostTeam)) {
            hostTeamName = _recordInfo.hostTeam;
        } else {
            hostTeamName = getResources().getString(R.string.home_team);
        }
        viewBinding.homeTeamScore.setTeamName(hostTeamName);

        if(!TextUtils.isEmpty(_recordInfo.guestTeam)) {
            guestTeamName = _recordInfo.guestTeam;
        } else {
            guestTeamName = getResources().getString(R.string.guest_team);
        }
        viewBinding.guestTeamScore.setTeamName(guestTeamName);

        viewBinding.homeTeamScore.setScoreNumber(_recordInfo.getHostGoal());
        viewBinding.guestTeamScore.setScoreNumber(_recordInfo.getGuestGoal());

        viewBinding.homeTeamScore.setCallBack(new ScoreCounterView.ICallBack() {
            @Override
            public void onAddScore(int score) {
                if(!isDotEnable()) {
                    return;
                }
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }
                isHomeRequest = true;
                if(_recordInfo != null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive() && _competitionMark != null) {
                    ApiUtil.addCompetitionLivePoint(_recordInfo.live_cid, _recordInfo.fec_fn, hostTeamName, _recordTime, "进球", _competitionMark.getMarkId(), new IGenericApiCallback<HmCompetitionPoint>() {
                        @Override
                        public void onSuccess(HmCompetitionPoint data) {
                            if(data!=null){
                                addGoal(1, data.pointId, _competitionMark.getMarkName(), _competitionMark.getMarkId());
                            } else {
                                ToastUtil.s(getString(R.string.add_dot_fail));
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {

                        }

                        @Override
                        public void onFinish() {

                        }
                    });

                } else {
                    addGoal(1, "", _gameTag, "");
                }
            }

            @Override
            public void onRemoveScore(int score) {
                if(!isDotEnable()) {
                    return;
                }
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }

                isHomeRequest = true;
                //在相机删除后，在删除回调中，同步比分到服务器
                GameScoreRemoveBean gameScoreRemoveBean = new GameScoreRemoveBean(1);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameScoreRemoveBean);
                if(!isOk) {
                    ToastUtil.s(getString(R.string.device_offline));
                    return;
                }
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                DialogUtil.showDialog(getString(R.string.loading));
            }
        });

        viewBinding.guestTeamScore.setCallBack(new ScoreCounterView.ICallBack() {
            @Override
            public void onAddScore(int score) {
                if(!isDotEnable()) {
                    return;
                }
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }

                isHomeRequest = false;

                if(_recordInfo != null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive() && _competitionMark != null) {
                    ApiUtil.addCompetitionLivePoint(_recordInfo.live_cid, _recordInfo.fec_fn, guestTeamName, _recordTime, "进球", _competitionMark.getMarkId(), new IGenericApiCallback<HmCompetitionPoint>() {
                        @Override
                        public void onSuccess(HmCompetitionPoint data) {
                            if(data!=null){
                                addGoal(0, data.pointId, _competitionMark.getMarkName(), _competitionMark.getMarkId());
                            } else {
                                ToastUtil.s(getString(R.string.add_dot_fail));
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {

                        }

                        @Override
                        public void onFinish() {

                        }
                    });

                } else {
                    addGoal(0, "", _gameTag, "");
                }
            }

            @Override
            public void onRemoveScore(int score) {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }

                isHomeRequest = false;

                GameScoreRemoveBean gameScoreRemoveBean = new GameScoreRemoveBean(0);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameScoreRemoveBean);
                if(!isOk) {
                    ToastUtil.s(getString(R.string.device_offline));
                    return;
                }
                if(_handler != null) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                }
                DialogUtil.showDialog(getString(R.string.loading));
            }
        });

        viewBinding.tvTagBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showPopupView();

                return true;
            }
        });
        viewBinding.tvEditName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                //跳到编辑页面
                CompetitionNameEditActivity.start(ScoreCenterActivity.this, _recordInfo);
                return true;
            }
        });

        _adapter = new GoalItemAdapter(ScoreCenterActivity.this, _goalList);
        View emptyView = LayoutInflater.from(ScoreCenterActivity.this).inflate(R.layout.score_center_item_empty_view, null);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        _adapter.setEmptyView(emptyView);
        _adapter.setHostTeamName(hostTeamName);
        _adapter.setGuestTeamName(guestTeamName);
        _adapter.setListener(new GoalItemAdapter.GoalEditCallback() {
            @Override
            public void onEdit(int index, GameStatusListBean goalInfoBean) {
                AppTrace.d(TAG, "click edit index: " + index);
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }

                showEditDialog(goalInfoBean);
            }

            @Override
            public void onDelete(int index, GameStatusListBean goalInfoBean) {
                AppTrace.d(TAG, "click delete index: " + index);
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return ;
                }

                showConfirmDialog(
                    "",
                    "删除进球时间",
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {

                            if(_recordInfo !=null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive() && !TextUtils.isEmpty(goalInfoBean.getId())) {
                                ApiUtil.deleteCompetitionLivePoint(_recordInfo.live_cid,
                                        goalInfoBean.getId(),
                                        new IGenericApiCallback<Object>() {
                                            @Override
                                            public void onSuccess(Object data) {
                                                deleteStat(goalInfoBean);
                                            }

                                            @Override
                                            public void onFail(String code, String message) {
                                                ToastUtil.s(getString(R.string.remove_dot_fail));
                                            }

                                            @Override
                                            public void onFinish() {

                                            }
                                        });
                            } else {
                                deleteStat(goalInfoBean);
                            }
                            return true;
                        }
                    }
                );
            }
        });
        viewBinding.recyclerView.addItemDecoration(new RecycleViewDivider(ScoreCenterActivity.this, LinearLayoutManager.VERTICAL, getResources().getColor(R.color.color_353535)));
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(ScoreCenterActivity.this));
        viewBinding.recyclerView.setAdapter(_adapter);

        loadScoreList();
    }

    private boolean isDotEnable() {
        if (_recordInfo.isLive() && !TextUtils.isEmpty(_recordInfo.live_cid)) {
            if (_competitionMark == null) {
                ToastUtil.s(getString(R.string.please_setup_game_stage));
                return false;
            }
            return true;
        }
        if ( tagIndex == 0 ){
            ToastUtil.s(getString(R.string.please_setup_game_stage));
            return false;
        }
        return true;
    }

    private void showEditDialog(GameStatusListBean goalInfoBean) {

        if (timeEditDialog != null && timeEditDialog.isShowing()) {
            timeEditDialog.dismiss();
        }
        timeEditDialog = new DotTimeEditDialog(ScoreCenterActivity.this);
        timeEditDialog.setDate(_recordTime);
        timeEditDialog.setCallback(new DotTimeEditDialog.ICallback() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onConfirm(int min, int second) {
                int time = min * 60 + second;
                if(_recordInfo !=null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive() && !TextUtils.isEmpty(goalInfoBean.getId())) {
                    ApiUtil.updateCompetitionLivePointTime(
                            _recordInfo.live_cid,
                            goalInfoBean.getId(),

                            time,
                            new IGenericApiCallback<HmCompetitionPoint>() {
                                @Override
                                public void onSuccess(HmCompetitionPoint data) {
                                    editStat(goalInfoBean, getDotSeqByType(goalInfoBean), time);
                                }

                                @Override
                                public void onFail(String code, String message) {
                                    ToastUtil.s(getString(R.string.update_fail));
                                }

                                @Override
                                public void onFinish() {

                                }
                            }
                    );

                } else {
                    //向相机更新时间点
                    editStat(goalInfoBean, getDotSeqByType(goalInfoBean), time);
                }
            }
        }).show();

        timeEditDialog.setSelectItem(goalInfoBean.getTime());
    }

    private void addGoal(int hostTeam, String pointId, String markName, String markId){
        isHomeRequest = true;
        GameScoreStatAddBean gameScoreStateAddBean  = new GameScoreStatAddBean(hostTeam, Constants.STAT_GOAL_0, "进球", pointId, markName,  markId);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameScoreStateAddBean);
        if(!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        retryAddScoreNum = 0;
        retryAddStateNum = 0;
        if(_handler != null) {
            _handler.postDelayed(_dotTimeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
        DialogUtil.showDialog(getString(R.string.loading));

    }
    private void addStat(int hostTeam, String pointId, String markName, String markId){
        GameStatAddBean gameStatAddBean = new GameStatAddBean(hostTeam, Constants.STAT_GOAL_0, "进球", pointId, markName,  markId);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameStatAddBean);
        if(!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        if(_handler != null) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
        DialogUtil.showDialog(getString(R.string.loading));
    }

    private void deleteStat(GameStatusListBean goalInfoBean){

        GameStatEditBean goalEditBean = new GameStatEditBean();
        goalEditBean.host_team = goalInfoBean.getHost();
        goalEditBean.type = goalInfoBean.getType();
        goalEditBean.time = goalInfoBean.getTime();
        goalEditBean.seq = getDotSeqByType(goalInfoBean);
        goalEditBean.remove = 1;

        boolean isOk = DeviceApiManager.sendCmdSimpleObj(goalEditBean);
        if(!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return ;
        }
        if(_handler != null) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
        DialogUtil.showDialog(getString(R.string.loading));
    }

    private void editStat(GameStatusListBean goalInfoBean, int seqIndex, int time){
        GameStatEditBean goalEditBean = new GameStatEditBean();
        goalEditBean.host_team = goalInfoBean.getHost();
        goalEditBean.type = goalInfoBean.getType();
        goalEditBean.time = time;
        goalEditBean.seq = seqIndex;
        goalEditBean.remove = 0;
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(goalEditBean);
        if (!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        if(_handler != null) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        }
        DialogUtil.showDialog(getString(R.string.loading));
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOSControlEvent(OSDControlEvent event) {
        DialogUtil.dismissDialog();
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetOSDSettingsEvent(GetOSDSettingsEvent event) {
        viewBinding.toggleShowScore.setSelected(event.hide_score == 0);
        viewBinding.toggleShowTeamName.setSelected(event.hide_team == 0);
    }

    @Override
    public void onResume() {
        super.onResume();

       boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
       if(isOk) {
           if(_handler != null) {
               _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
           }
           DialogUtil.showDialog(getString(R.string.loading));
       }
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
    }

    private void updateScore() {
        //更新客队比分
        if(TextUtils.isEmpty(_recordStatus.getGuestTeam())) {
            viewBinding.guestTeamScore.setTeamName(getString(R.string.guest_team));
        } else {
            viewBinding.guestTeamScore.setTeamName(_recordStatus.getGuestTeam());
        }
        viewBinding.guestTeamScore.setScoreNumber(_recordStatus.getGuest_goal());
        //更新主队比分
        if(TextUtils.isEmpty(_recordStatus.getHostTeam())) {
            viewBinding.homeTeamScore.setTeamName(getString(R.string.home_team));
        } else {
            viewBinding.homeTeamScore.setTeamName(_recordStatus.getHostTeam());
        }
        viewBinding.homeTeamScore.setScoreNumber(_recordStatus.getHost_goal());
    }

    private void updateRecordTime() {

        viewBinding.tvGameTime.setText(TimeUtil.intToTimeMin(_recordTime));
    }

    @Override
    public void onRecordVideoStart() {

    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        _recordTime = recordStatus.getDuration();
        _marchTime = recordStatus.getMatchTime();
        _recordStatus = recordStatus;
        AppTrace.d(TAG, String.format("record status: %d, time: %d", recordStatus.getStatus(), recordStatus.getDuration()));
        _handler.removeCallbacks(_recordTimeRunnable);
        


        _handler.post(() -> {
            DialogUtil.dismissDialog();
            if(!TextUtils.isEmpty(recordStatus.getLabel())) {
                int index = ScoreCenterUtil.getMarkIndex(ScoreCenterActivity.this, recordStatus.getLabel());
                tagIndex = index > 0 ? index : 0;
                _gameTag = recordStatus.getLabel();
                updateTagUI(recordStatus.getLabel());

            } else {
                resetTag();
            }

            viewBinding.tvGameName.setText(recordStatus.getVideoName());
            //同步比赛名称和队名
            _recordInfo.name = recordStatus.getVideoName();
            _recordInfo.hostTeam = recordStatus.getHostTeam();
            _recordInfo.guestTeam = recordStatus.getGuestTeam();

            updateScore();

            // 处理match_time_paused字段，更新计时暂停状态
            if(recordStatus != null && viewBinding != null) {
                _isTimerPause = (recordStatus.getMatchTimePaused() == 1);
                // 更新按钮文本
                if(_isTimerPause) {
                    viewBinding.btnPauseResumeTimer.setText(R.string.resume_timer);
                    viewBinding.btnPauseResumeTimer.setBackgroundResource(R.drawable.sh_r10_2a387c);
                } else {
                    viewBinding.btnPauseResumeTimer.setText(R.string.pause_timer);
                    viewBinding.btnPauseResumeTimer.setBackgroundResource(R.drawable.sh_r10_f16c2f);
                }
            }

            updateRecordTime();
            if(_handler != null) {
                _handler.postDelayed(_recordTimeRunnable, 1000);
            }

            //查询当前markId
            if (_competitionMark == null && _recordInfo.isLive() && !TextUtils.isEmpty(_recordInfo.live_cid)
                    && !TextUtils.isEmpty(recordStatus.getLabel())) {
                getMarkId(_recordInfo.live_cid, _recordInfo.fec_fn, recordStatus.getLabel()) ;
            }
        });

    }

    @Override
    public void onRecordVideoStop() {

    }

    @Override
    public void onRecordException(String msg) {

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddScoreEvent(GameScoreAddEvent event) {
        _handler.removeCallbacks(_dotTimeOutRunnable);

        retryAddScoreNum = 0;
        retryAddStateNum = 0;
        if(event != null && event.status == 1 && event.goalStatusBean != null && event.goalStatusBean.getType() == 0) {

            viewBinding.homeTeamScore.setScoreNumber(event.goalStatusBean.getHost_goal());
            viewBinding.guestTeamScore.setScoreNumber(event.goalStatusBean.getGuest_goal());

            DialogUtil.dismissDialog();
            // report score
            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
                ApiUtil.reportCompetitionLiveScore(
                        _recordInfo.live_cid,
                        viewBinding.homeTeamScore.getScoreNumber(),
                        viewBinding.guestTeamScore.getScoreNumber()
                );
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveScoreEvent(GameScoreRemoveEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        if(event != null && event.status == 1 && event.goalStatusBean != null && ScoreCenterUtil.isGoalType(event.goalStatusBean.getType())) {
            DialogUtil.dismissDialog();
            viewBinding.homeTeamScore.setScoreNumber(event.goalStatusBean.getHost_goal());
            viewBinding.guestTeamScore.setScoreNumber(event.goalStatusBean.getGuest_goal());
            _handler.postDelayed(() -> loadScoreList(), 200);

            // report score
            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
                ApiUtil.reportCompetitionLiveScore(
                        _recordInfo.live_cid,
                        viewBinding.homeTeamScore.getScoreNumber(),
                        viewBinding.guestTeamScore.getScoreNumber()
                );
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddScoreStatEvent(GameScoreStatAddEvent event) {
        _handler.removeCallbacks(_dotTimeOutRunnable);
        retryAddStateNum = 0;
        if(event != null && event.status == 1 && event.goalStatusBean != null ) {

            viewBinding.homeTeamScore.setScoreNumber(event.goalStatusBean.getHost_goal());
            viewBinding.guestTeamScore.setScoreNumber(event.goalStatusBean.getGuest_goal());

            // update 打点时间 score
            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
                ApiUtil.updateCompetitionLivePointTime(
                        _recordInfo.live_cid,
                        event.goalStatusBean.getPointId(),

                        event.goalStatusBean.getTime(),
                        null
                );
                // report score
                ApiUtil.reportCompetitionLiveScore(
                        _recordInfo.live_cid,
                        event.goalStatusBean.getHost_goal(),
                        event.goalStatusBean.getGuest_goal()
                );
            }

            if(ScoreCenterUtil.isGoalType(event.goalStatusBean.getType())){
                DialogUtil.dismissDialog();

                _handler.postDelayed(() -> loadScoreList(), 200);
            }

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddStatEvent(GameStatAddEvent event) {
        _handler.removeCallbacks(_dotTimeOutRunnable);
        retryAddStateNum = 0;
        if(event != null && event.status == 1 && event.goalStatusBean != null ) {

            // update 打点时间 score
            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
                ApiUtil.updateCompetitionLivePointTime(
                        _recordInfo.live_cid,
                        event.goalStatusBean.getPointId(),

                        event.goalStatusBean.getTime(),
                        null
                );
            }

            if(ScoreCenterUtil.isGoalType(event.goalStatusBean.getType())){
                DialogUtil.dismissDialog();

                _handler.postDelayed(() -> loadScoreList(), 200);
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEditStatEvent(GameStatEditEvent event) {
        AppTrace.d(TAG, "onEditStatEvent");
        _handler.removeCallbacks(_timeOutRunnable);
        if(event != null && event.status == 1 && event.goalStatusBean != null ) {

            // update 打点时间 score
            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
                if(event.goalStatusBean.getRemove() == 1 ){
                    AppTrace.d(TAG, "delete success");
                } else {
                    AppTrace.d(TAG, "edit stat pointId = " + event.goalStatusBean.getPointId());
                    ApiUtil.updateCompetitionLivePointTime(
                            _recordInfo.live_cid,
                            event.goalStatusBean.getPointId(),

                            event.goalStatusBean.getTime(),
                            null
                    );
                }
            }

            if(ScoreCenterUtil.isGoalType(event.goalStatusBean.getType())) {
                DialogUtil.dismissDialog();

                _handler.postDelayed(() -> loadScoreList(), 200);
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveStatEvent(GameStatRemoveEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        if(event != null && event.status == 1 && event.goalStatusBean != null ) {

            if(ScoreCenterUtil.isGoalType(event.goalStatusBean.getType())) {
                DialogUtil.dismissDialog();
                _handler.postDelayed(() -> loadScoreList(), 200);
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameScoreClearEvent(GameScoreClearEvent event) {
        if (event != null && event.goalStatusBean!=null) {
            //同步比分
            viewBinding.homeTeamScore.setScoreNumber(event.goalStatusBean.getHost_goal());
            viewBinding.guestTeamScore.setScoreNumber(event.goalStatusBean.getGuest_goal());
            _handler.postDelayed(() -> loadScoreList(), 200);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameStatListEvent(GameStatListEvent event) {
        AppTrace.d(TAG, "onGameStatListEvent");
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        if(event != null && event.goalList != null) {

            if (_stateList != null) {
                _stateList.clear();
            }
            _stateList.addAll(event.goalList);

            if(_goalList != null) {
                _goalList.clear();
            }
            for(int i = 0; i < event.goalList.size(); i++) {
                GameStatusListBean gameStatusListBean = event.goalList.get(i);
                if(ScoreCenterUtil.isGoalType(gameStatusListBean.getType())) {
                    _goalList.add(gameStatusListBean);
                }
            }

            _adapter.notifyDataSetChanged();
        }
    }

    private void loadScoreList() {
        AppTrace.d(TAG, "loadScoreList");

        GameStatListRequestBean requestBean = new GameStatListRequestBean(_statTypeIntList);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
        if(!isOk) {
            showDisconnectToast();
            AppTrace.d(TAG, "获取进球列表失败");
        }
    }

    public int getDotSeqByType(GameStatusListBean goalInfoBean){
        int type = goalInfoBean.getType();
        int typeIndex = 0;
        for(int i = 0; i < _goalList.size(); i++) {
            if(goalInfoBean.getHost() == _goalList.get(i).getHost() && type == _goalList.get(i).getType()){
                if(goalInfoBean.getTime() == _goalList.get(i).getTime()
                        && goalInfoBean.getName().equalsIgnoreCase(_goalList.get(i).getName()) ){
                    break;
                }
                typeIndex++;
            }
        }

        return typeIndex;
    }

    private void showPopupView() {
        final View popup_view = getLayoutInflater().inflate(R.layout.score_center_layout_tag_view, null, false);
        //popup_view 内的某个空间id，无关紧要
        RecyclerView recyclerView = popup_view.findViewById(R.id.recyclerView);

        if (_tagAdapter == null) {
            _tagAdapter = new GameTagAdapter(ScoreCenterActivity.this, _tagTypeList);
            _tagAdapter.setListener(new GameTagAdapter.TagCallback() {
                @Override
                public void onClick(String content, int index) {
                    tagIndex = index;
                    _gameTag = content;
                    updateTagUI(_gameTag);
                    if(popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                    //同步到相机和服务器
                    updateTag(_gameTag);
                }
            });
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(ScoreCenterActivity.this));
        recyclerView.addItemDecoration(new RecycleViewDivider(ScoreCenterActivity.this, LinearLayoutManager.VERTICAL, getResources().getColor(R.color.tranparent)));

        _tagAdapter.setSelectedItem(tagIndex);
        recyclerView.setAdapter(_tagAdapter);

        TextView cancelView = popup_view.findViewById(R.id.tv_cancel);
        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }
            }
        });

        // 创建PopupWindow实例,200,LayoutParams.MATCH_PARENT分别是宽度和高度
        popupWindow = new PopupWindow(popup_view, 600, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        //设置可以获取焦点
        popupWindow.setFocusable(true);
        //设置可以触摸弹出框以外的区域
        popupWindow.setOutsideTouchable(true);
        //注意点：获取屏幕宽度
        WindowManager systemService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //注意点：相当于中间
        int xpos = systemService.getDefaultDisplay().getWidth() / 2 - popupWindow.getWidth() / 2;
        //注意点：放在具体控件下方  这里的x，y坐标是小关键
        popupWindow.showAsDropDown(viewBinding.layoutTag, xpos, 0);
        // 点击其他地方消失
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

            }
        });
    }
    //同步到相机和服务器
    private void updateTag(String tag){
        //同步到相机和服务器
        if (_recordInfo != null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
            //防止重复提交
            if (_competitionMark == null || !_competitionMark.getMarkName().equalsIgnoreCase(tag)) {

                ApiUtil.addCompetitionMark(_recordInfo.live_cid, _recordInfo.fec_fn, tag, _recordTime, new IGenericApiCallback<CompetitionMarkBean>() {
                    @Override
                    public void onSuccess(CompetitionMarkBean data) {
                        _competitionMark = data.data;
                        requestCameraTag(tag);
                    }

                    @Override
                    public void onFail(String code, String message) {

                    }

                    @Override
                    public void onFinish() {

                    }
                });
            }
        } else {
            requestCameraTag(_gameTag);
        }
    }

    private void resetTag(){
        String tag = _tagTypeList.get(0);
        //鉴于竖版显示不全，截取前三个字
        if(tag.length() > 3 ){
            viewBinding.tvGameTag.setText(tag.substring(0,3));
        }
        viewBinding.tvGameTag.setTextColor(ColorUtil.parseColor("#767676"));
    }

    private void updateTagUI(String tag){
        viewBinding.tvGameTag.setText(tag);
        //鉴于竖版显示不全，截取前三个字
        if(tag.length() > 3 ){
            viewBinding.tvGameTag.setText(tag.substring(0,3));
        }

        viewBinding.tvGameTag.setTextColor(ColorUtil.parseColor("#F16C2F"));
    }

    private void requestCameraTag(String tagStr) {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new GameTagRequestBean(tagStr));
        if (!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
        } else {
            DialogUtil.showDialog(getString(R.string.loading));
            if(_handler != null) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetGameEvent(CompetitionLabelEvent event) {
        AppTrace.d(TAG, "onSetGameEvent");
        DialogUtil.dismissDialog();
        _handler.removeCallbacks(_timeOutRunnable);
        if (event != null && event.state == 1) {
            //只有直播才进行暂停时间和回复时间
//            if (!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
//                if (ScoreCenterUtil.isMatchPause(tagIndex)) {
//                    DeviceApiManager.sendCmdSimpleObj(new MatchTimePause());
//                } else {
//                    DeviceApiManager.sendCmdSimpleObj(new MatchTimeResume());
//                }
//            }
        } else {
            ToastUtil.s(getString(R.string.game_tag_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateMark(AddMarkEvent event) {
        AppTrace.d(TAG, "onUpdateMark=" + event.markBean.getMarkName());
        if(event != null && event.markBean != null) {
            _competitionMark = event.markBean;
            updateTagUI(_competitionMark.getMarkName());

            int index = ScoreCenterUtil.getMarkIndex(ScoreCenterActivity.this, _competitionMark.getMarkName());
            tagIndex = index > 0 ? index : 0;
        }
    }

    private void getMarkId(String competitionId, String videoName, String markName) {
        AppTrace.d(TAG, "getMarkId...");
        ApiUtil.queryCompetitionMark(competitionId, videoName, markName, new IGenericApiCallback<CompetitionMarkListBean>() {
            @Override
            public void onSuccess(CompetitionMarkListBean data) {
                if(data != null && data.data != null && data.data.size() > 0) {
                    _competitionMark = data.data.get(0);
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getMarkId fail: code= "+ code+", message=" +message);
            }

            @Override
            public void onFinish() {

            }
        });
    }
    private void retryPendingRequestBean(){
        ToastUtil.s(getString(R.string.add_dot_fail));
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordStoppedOnLowStorage(RecordStoppedOnLowStorageEvent event) {
        onRecordVideoStop();
        if(BaseAppUtil.isForegroundActivity(ScoreCenterActivity.this, getClass().getName())){
            showStateStoppedOnLowStorageDialog();
        } else {
            finish();
        }
    }

    private void showStateStoppedOnLowStorageDialog() {
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _dialog = new ModalDialog(this);
        _dialog.setMessage(getString(R.string.info_dot_stopped_on_low_storage));
        _dialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        _dialog.show();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMatchTimePauseEvent(MatchTimePauseEvent event) {
        AppTrace.d(TAG, "onMatchTimePauseEvent");
        DialogUtil.dismissDialog();
        _isTimerPause = true;
        viewBinding.btnPauseResumeTimer.setText(R.string.resume_timer);
        // 暂停时设置为灰色背景
        viewBinding.btnPauseResumeTimer.setBackgroundResource(R.drawable.sh_r10_f16c2f);
        DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMatchTimeResumeEvent(MatchTimeResumeEvent event) {
        AppTrace.d(TAG, "onMatchTimeResumeEvent");
        DialogUtil.dismissDialog();
        _isTimerPause = false;
        // 恢复时恢复默认背景
        viewBinding.btnPauseResumeTimer.setBackgroundResource(R.drawable.sh_r10_2a387c);
        viewBinding.btnPauseResumeTimer.setText(R.string.pause_timer);
        DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMatchTimeResetEvent(MatchTimeResetEvent event) {
        AppTrace.d(TAG, "onMatchTimeResetEvent status: " + event.status);
        DialogUtil.dismissDialog();

        if (event.status == 1) {
            ToastUtil.s(getString(R.string.timer_reset_success));
        } else if (event.status == 0) {
            ToastUtil.s(getString(R.string.timer_reset_fail));
        }
        
        // 发送请求获取最新状态
        DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
    }
    
    /**
     * 处理暂停/恢复计时按钮点击
     */
    private void handlePauseResumeTimer() {
        if (_isTimerPause) {
            // 恢复计时
            DeviceApiManager.sendCmdSimpleObj(new MatchTimeResume());
        } else {
            // 暂停计时
            DeviceApiManager.sendCmdSimpleObj(new MatchTimePause());
        }

        DialogUtil.showDialog(getString(R.string.loading));
    }
    
    /**
     * 处理重置计时按钮点击
     */
    private void handleResetTimer() {
        // 显示确认对话框
        _dialog = new ModalDialog(this);
        _dialog.setTitle(getString(R.string.confirm_reset));
        _dialog.setMessage(getString(R.string.confirm_reset_timer_msg));
        _dialog.setNegativeButton(getString(R.string.cancel), null);
        _dialog.setPositiveButton(getString(R.string.confirm), v -> {
            // 确认重置，发送命令
            DeviceApiManager.sendCmdSimpleObj(new MatchTimeReset());
            DialogUtil.showDialog(getString(R.string.loading));
        });
        _dialog.show();
    }
}
