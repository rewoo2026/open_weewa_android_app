package com.xh.hotme.record.score;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.GameStatusBean;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GameScoreAddBean;
import com.xh.hotme.bluetooth.bean.GameScoreRemoveBean;
import com.xh.hotme.bluetooth.bean.GameScoreStatAddBean;
import com.xh.hotme.bluetooth.bean.GameStatAddBean;
import com.xh.hotme.bluetooth.bean.GameStatListRequestBean;
import com.xh.hotme.bluetooth.bean.GameTagRequestBean;
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
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.databinding.ActivityScoreConterSuperBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CompetitionMarkBean;
import com.xh.hotme.live.bean.CompetitionMarkListBean;
import com.xh.hotme.live.bean.HmCompetitionPoint;
import com.xh.hotme.record.bean.DotStates;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@BLEAware
@IEventBus
@FullscreenLandscape(immersive = true)
public class ScoreCenterSuperActivity extends BaseActivity<ActivityScoreConterSuperBinding> implements IRecordControlListener, ISyncTimerListener {

    private final static String TAG = "SCSuperActivity";

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    private static final String RECORD_INFO = "record_info";
    private static final String RECORD_STATUS = "record_status";
    private static final String RECORD_MARK = "record_mark";

    RecordInfoBean _recordInfo;

    int _recordTime = 0;
    int _marchTime = 0;
    boolean isHomeRequest = false;

    RecordStatusResultBean _recordStatus;

    GoalItemAdapter _adapter;

    final ArrayList<GameStatusBean> _goalList = new ArrayList();
    final ArrayList<GameStatusListBean> _stateList = new ArrayList();

    HmCompetitionMark _competitionMark;
    int tagIndex = 0;

    public String hostTeamName;
    public String guestTeamName;

    private String _gameTag;

    List<String> _tagTypeList;

    List<Integer> _statTypeIntList;

    final Map<Integer, GameStatusBean> _gameStatMap = new HashMap<>();

    PopupWindow popupWindow;
    GameTagAdapter _tagAdapter;

    GameStatAddBean _pendingGameStateAddBean;
    GameScoreAddBean _pendingGameScoreAddBean;

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

    private final Runnable _timeOutRunnable = () -> {

        retryPendingRequestBean();

        DialogUtil.dismissDialog();
    };

    private final Runnable _dotTimeOutRunnable = () -> {

        retryPendingRequestBean();

        DialogUtil.dismissDialog();
    };

    public static void start(Context ctx, RecordInfoBean recordInfo, RecordStatusResultBean recordStatus, HmCompetitionMark competitionMark) {
        Intent intent = new Intent(ctx, ScoreCenterSuperActivity.class);
        intent.putExtra(RECORD_INFO, GsonUtils.GsonString(recordInfo));
        intent.putExtra(RECORD_STATUS, GsonUtils.GsonString(recordStatus));
        intent.putExtra(RECORD_MARK, GsonUtils.GsonString(competitionMark));
        ctx.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        BluetoothHandle.addPlayNotifyListener(this);
    }

    @Override
    protected void initView() {

        viewBinding.llExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        viewBinding.ivSelectTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupView();
            }
        });

        viewBinding.ivHostAddScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkDotEnable()) {
                    addScore(true, Constants.STAT_GOAL_0, "进球");
                }
            }
        });

        viewBinding.ivHostRemoveScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkDotEnable()) {
                    removeScore(true);
                }
            }
        });

        viewBinding.ivGuestAddScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkDotEnable()) {
                    addScore(false, Constants.STAT_GOAL_0, "进球");
                }
            }
        });

        viewBinding.ivGuestRemoveScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkDotEnable()) {
                    removeScore(false);
                }
            }
        });


        viewBinding.hmScoreView.setSyncTmeListener(new ISyncTimerListener() {
            @Override
            public int getTime() {

                return _recordTime;
            }

            @Override
            public boolean isDotEnable() {
                return checkDotEnable();
            }

            @Override
            public ArrayList<GameStatusListBean> getStateList() {
                return _stateList;
            }
        });

        viewBinding.hmScoreView.setCallback(new IHmStateListener() {
            @Override
            public void onAddState(int hotTeam, DotStates dotState) {
                //显示最近的打点
                if (hotTeam == 1) {
                    viewBinding.tvHostLatestDot.setVisibility(View.VISIBLE);
                    viewBinding.tvHostLatestDot.setText(dotState.getName());
                } else {
                    viewBinding.tvGuestLatestDot.setVisibility(View.VISIBLE);
                    viewBinding.tvGuestLatestDot.setText(dotState.getName());
                }

                //
                int id = dotState.getId();
                String content = dotState.getName();
                if (ScoreCenterUtil.isGoalType(id)) {
                    addScore(hotTeam == 1 ? true : false, id, content);
                } else {
                    addState(hotTeam==1 ? true : false, id, content);
                }
            }

            @Override
            public void onRemoveState(int hotTeam, DotStates dotStat) {

            }

            @Override
            public void onEditState(int hotTeam, DotStates dotStat) {

            }
        });
    }


    private void showPopupView() {
        final View popup_view = getLayoutInflater().inflate(R.layout.score_center_super_layout_tag_view, null, false);
        //popup_view 内的某个空间id，无关紧要
        RecyclerView recyclerView = popup_view.findViewById(R.id.recyclerView);

        if (_tagAdapter == null) {
            _tagAdapter = new GameTagAdapter(ScoreCenterSuperActivity.this, _tagTypeList);
            _tagAdapter.setListener(new GameTagAdapter.TagCallback() {
                @Override
                public void onClick(String content, int index) {
                    _gameTag = content;
                    updateTagUI(content);
                    tagIndex = index;
                    if(popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                    //同步到相机和服务器
                    updateTag(content);
                }
            });
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(ScoreCenterSuperActivity.this));
        recyclerView.addItemDecoration(new RecycleViewDivider(ScoreCenterSuperActivity.this, LinearLayoutManager.VERTICAL, getResources().getColor(R.color.tranparent)));

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
        popupWindow.showAsDropDown(viewBinding.ivSelectTag, -280, 0);
        // 点击其他地方消失
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

            }
        });
    }

    private void updateTag(String tag){
        if (_recordInfo != null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {

            if (_competitionMark == null || !_competitionMark.getMarkName().equalsIgnoreCase(tag)) {

                ApiUtil.addCompetitionMark(_recordInfo.live_cid, _recordInfo.fec_fn, tag, _recordTime, new IGenericApiCallback<CompetitionMarkBean>() {
                    @Override
                    public void onSuccess(CompetitionMarkBean data) {
                        _competitionMark = data.data;

                        viewBinding.hmScoreView.setMark(_competitionMark.getMarkName(), _competitionMark.getMarkId());
                        requestCameraTag(_competitionMark.getMarkName());

                        EventBus.getDefault().post(new AddMarkEvent(_competitionMark));
                    }

                    @Override
                    public void onFail(String code, String message) {
                        ToastUtil.s(getString(R.string.info_add_completement_mark_fail));
                    }

                    @Override
                    public void onFinish() {

                    }
                });
            }
        } else {
            viewBinding.hmScoreView.setMarkName(_gameTag);
            requestCameraTag(_gameTag);
        }
    }

    @Override
    protected void initData() {
        String[] tagTypeList = getResources().getStringArray(R.array.game_tag_list);
        _tagTypeList = Arrays.asList(tagTypeList);

        _statTypeIntList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            _statTypeIntList.add(i);
            GameStatusBean gameStatusBean = new GameStatusBean(i);
            _gameStatMap.put(i, gameStatusBean);
            _goalList.add(gameStatusBean);
        }
        _statTypeIntList.add(Constants.STAT_CUSTOM_1);
        _statTypeIntList.add(Constants.STAT_CUSTOM_2);
        _statTypeIntList.add(Constants.STAT_CUSTOM_3);

        if (getIntent() != null) {
            String recordInfoStr = (String) getIntent().getSerializableExtra(RECORD_INFO);
            _recordInfo = GsonUtils.GsonToBean(recordInfoStr, RecordInfoBean.class);

            String recordStatusInfoStr = (String) getIntent().getSerializableExtra(RECORD_STATUS);
            _recordStatus = GsonUtils.GsonToBean(recordStatusInfoStr, RecordStatusResultBean.class);

            String recordMarkStr = (String) getIntent().getSerializableExtra(RECORD_MARK);
            if(!TextUtils.isEmpty(recordMarkStr)) {
                _competitionMark = GsonUtils.GsonToBean(recordMarkStr, HmCompetitionMark.class);
                if(_competitionMark !=null ) {
                    viewBinding.hmScoreView.setMark(_competitionMark.getMarkName(), _competitionMark.getMarkId());
                }
            }

        }
        if (_recordInfo == null) {
            ToastUtil.s(getString(R.string.error_record_info_empty));
            finish();
            return;
        }

        if (_recordStatus != null) {
            if (!TextUtils.isEmpty(_recordStatus.getLabel())) {
                updateTagUI(_recordStatus.getLabel());
            }
            _recordTime = _recordStatus.getDuration();
            updateRecordTime();

            //同步比分
            viewBinding.tvHostScore.setText(String.valueOf(_recordStatus.getHost_goal()));
            viewBinding.tvGuestScore.setText(String.valueOf(_recordStatus.getGuest_goal()));
        }

        if (!TextUtils.isEmpty(_recordInfo.hostTeam)) {
            hostTeamName = _recordInfo.hostTeam;
        } else {
            hostTeamName = getResources().getString(R.string.home_team);
        }
        viewBinding.tvHostName.setText(hostTeamName);

        if (!TextUtils.isEmpty(_recordInfo.guestTeam)) {
            guestTeamName = _recordInfo.guestTeam;
        } else {
            guestTeamName = getResources().getString(R.string.guest_team);
        }
        viewBinding.tvGuestName.setText(guestTeamName);

        viewBinding.hmScoreView.setStateTypeList(_statTypeIntList);
        viewBinding.hmScoreView.setHostTeamName(_recordInfo.hostTeam);
        viewBinding.hmScoreView.setGuestTeamName(_recordInfo.guestTeam);
        viewBinding.hmScoreView.setCompetitionId(_recordInfo.live_cid);
        viewBinding.hmScoreView.setVideoName(_recordInfo.fec_fn);
        viewBinding.hmScoreView.setDotScene(_recordInfo.isLive() ? Constants.DOT_SCENE_LIVING : Constants.DOT_SCENE_RECORDING);

        loadScoreList();
    }

    @Override
    public void onResume() {
        super.onResume();

        DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean());
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


    private void addState(boolean isHost, int type, String name) {
        int dotScene = _recordInfo.isLive() ? Constants.DOT_SCENE_LIVING : Constants.DOT_SCENE_RECORDING;
        if (!TextUtils.isEmpty(_recordInfo.live_cid) && (dotScene == Constants.DOT_SCENE_LIVING || dotScene == Constants.DOT_SCENE_VIDEO)) {
            String teamName = isHost ? hostTeamName : guestTeamName;
            ApiUtil.addCompetitionLivePoint(_recordInfo.live_cid, _recordInfo.fec_fn, teamName, _recordTime, name, _competitionMark.getMarkId(), new IGenericApiCallback<HmCompetitionPoint>() {
                @Override
                public void onSuccess(HmCompetitionPoint data) {
                    if (data != null) {
                        requestCameraAddState(isHost, type, name, data.pointId, _competitionMark.getMarkName(), _competitionMark.getMarkId());
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
            requestCameraAddState(isHost, type, name, "", _gameTag, "");
        }
    }

    private void requestCameraAddScore(boolean isHost, int type, String name, String pointId, String markName, String markId) {
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return ;
        }

        GameScoreStatAddBean gameScoreStateAddBean  = new GameScoreStatAddBean(isHost ? 1 : 0, type, name, pointId, markName,  markId);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameScoreStateAddBean);
        if (!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        retryAddScoreNum = 0;
        retryAddStateNum = 0;
        if(_handler != null) {
            _handler.postDelayed(_dotTimeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        }
        DialogUtil.showDialog(getString(R.string.loading));
    }

    private void requestCameraAddState(boolean isHost, int type, String name, String pointId, String markName, String markId) {
        retryAddScoreNum = 0;
        retryAddStateNum = 0;
        GameStatAddBean gameStatBean = new GameStatAddBean(isHost ? 1 : 0, type, name, pointId, markName, markId);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameStatBean);
        if (!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        if(_handler != null) {
            _handler.postDelayed(_dotTimeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        }
        DialogUtil.showDialog(getString(R.string.loading));
    }

    private void addScore(boolean isHost, int type, String name) {
        int dotScene = _recordInfo.isLive() ? Constants.DOT_SCENE_LIVING : Constants.DOT_SCENE_RECORDING;
        if (!TextUtils.isEmpty(_recordInfo.live_cid) && (dotScene == Constants.DOT_SCENE_LIVING || dotScene == Constants.DOT_SCENE_VIDEO) && _competitionMark != null) {
            String teamName = isHost ? hostTeamName : guestTeamName;
            ApiUtil.addCompetitionLivePoint(_recordInfo.live_cid, _recordInfo.fec_fn, teamName, _recordTime, name, _competitionMark.getMarkId(), new IGenericApiCallback<HmCompetitionPoint>() {
                @Override
                public void onSuccess(HmCompetitionPoint data) {
                    if (data != null) {
                        requestCameraAddScore(isHost, type, name, data.pointId, _competitionMark.getMarkName(), _competitionMark.getMarkId());
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
            requestCameraAddScore(isHost, type, name, "", _gameTag, "");
        }
    }

    private void removeScore(boolean isHost) {
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }
        isHomeRequest = false;

        GameScoreRemoveBean gameScoreAddBean = new GameScoreRemoveBean(isHost ? 1 : 0);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameScoreAddBean);
        if (!isOk) {
            ToastUtil.s(getString(R.string.device_offline));
            return;
        }
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        DialogUtil.showDialog(getString(R.string.loading));
    }

    private void updateScore() {
        //更新比赛比分
        if (viewBinding != null) {
            viewBinding.tvHostScore.setText(String.valueOf(_recordStatus.getHost_goal()));
            viewBinding.tvGuestScore.setText(String.valueOf(_recordStatus.getGuest_goal()));
        }

    }

    private void updateRecordTime() {
        if (viewBinding != null) {
            viewBinding.tvTime.setText(TimeUtil.intToTimeMin(_recordTime));
        }
    }

    @Override
    public void onRecordVideoStart() {

    }

    private void resetTag(){
        String tag = _tagTypeList.get(0);
        viewBinding.tvTag.setText(tag);
        viewBinding.tvTag.setTextColor(ColorUtil.parseColor("#767676"));
    }

    private void updateTagUI(String tag){
        viewBinding.tvTag.setText(tag);
        viewBinding.tvTag.setTextColor(ColorUtil.parseColor("#F16C2F"));
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        _recordTime = recordStatus.getDuration();
        _marchTime = recordStatus.getMatchTime();
        _recordStatus = recordStatus;
        AppTrace.d(TAG, String.format("record status: %d, time: %d", recordStatus.getStatus(), recordStatus.getDuration()));

        _handler.removeCallbacks(_recordTimeRunnable);

        _handler.post(() -> {

            if (viewBinding != null) {

                if (!TextUtils.isEmpty(recordStatus.getLabel())) {
                    updateTagUI(recordStatus.getLabel());
                    int index = ScoreCenterUtil.getMarkIndex(ScoreCenterSuperActivity.this, recordStatus.getLabel());
                    tagIndex = index > 0 ? index : 0;
                    _gameTag = recordStatus.getLabel();
                    viewBinding.hmScoreView.setMarkName(_gameTag);
                } else {
                    resetTag();
                }
                if (!TextUtils.isEmpty(_recordStatus.getHostTeam())) {
                    hostTeamName = _recordStatus.getHostTeam();
                } else {
                    hostTeamName = getResources().getString(R.string.home_team);
                }
                viewBinding.tvHostName.setText(hostTeamName);

                if (!TextUtils.isEmpty(_recordStatus.getGuestTeam())) {
                    guestTeamName = _recordStatus.getGuestTeam();
                } else {
                    guestTeamName = getResources().getString(R.string.guest_team);
                }
                viewBinding.tvGuestName.setText(guestTeamName);

                updateScore();

                updateRecordTime();

                //查询当前markId
                if (_competitionMark == null && _recordInfo.isLive() && !TextUtils.isEmpty(_recordInfo.live_cid)
                        && !TextUtils.isEmpty(_recordInfo.videoId)
                        && !TextUtils.isEmpty(recordStatus.getLabel())) {
                    getMarkId(_recordInfo.live_cid, _recordInfo.videoId, recordStatus.getLabel());
                }

            }
            _handler.postDelayed(_recordTimeRunnable, 1000);
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
        _pendingGameScoreAddBean = null;
        retryAddScoreNum = 0;
        retryAddStateNum = 0;
        if (event != null && event.status == 1 && event.goalStatusBean != null) {

            //更新比赛比分
            viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
            viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));

            if(_pendingGameStateAddBean != null ){
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(_pendingGameStateAddBean);
                if (!isOk) {
                    ToastUtil.s(getString(R.string.device_offline));
                    return;
                } else {
                    _handler.postDelayed(_dotTimeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                }
            } else {
                DialogUtil.dismissDialog();
            }

//            // report score
//            if(!_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
//                ApiUtil.reportCompetitionLiveScore(
//                        _recordInfo.live_cid,
//                        viewBinding.homeTeamScore.getScoreNumber(),
//                        viewBinding.guestTeamScore.getScoreNumber()
//                );
//            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveScoreEvent(GameScoreRemoveEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        if (event != null && event.status == 1 && event.goalStatusBean != null) {
            DialogUtil.dismissDialog();

            //更新比赛比分
            viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
            viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));

            _handler.postDelayed(() -> loadScoreList(), 200);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEditStateEvent(GameStatEditEvent event) {
        if (event != null && event.status == 1 && event.goalStatusBean != null) {
            _handler.post(() -> {

                //更新比赛比分
                viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
                viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));
                if (_adapter != null) {
                    _adapter.notifyDataSetChanged();
                }

                loadScoreList();
            });
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddScoreStateEvent(GameScoreStatAddEvent event) {
        _handler.removeCallbacks(_dotTimeOutRunnable);
        _pendingGameStateAddBean = null;
        retryAddStateNum = 0;
        if (event != null && event.status == 1 && event.goalStatusBean != null) {
            _handler.post(() -> {

                //更新比赛比分
                viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
                viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));
                if (_adapter != null) {
                    _adapter.notifyDataSetChanged();
                }
                DialogUtil.dismissDialog();
                loadScoreList();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddStateEvent(GameStatAddEvent event) {
        _handler.removeCallbacks(_dotTimeOutRunnable);
        _pendingGameStateAddBean = null;
        retryAddStateNum = 0;
        if (event != null && event.status == 1 && event.goalStatusBean != null) {
            _handler.post(() -> {

                //更新比赛比分
                viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
                viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));
                if (_adapter != null) {
                    _adapter.notifyDataSetChanged();
                }
                DialogUtil.dismissDialog();
                loadScoreList();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameStatListEvent(GameStatListEvent event) {
        AppTrace.d(TAG, "onGameStatListEvent");

        if (event != null && event.goalList != null) {
            if (_goalList != null) {
                _goalList.clear();
            }
            if (_stateList != null) {
                _stateList.clear();
            }
            _stateList.addAll(event.goalList);

            resetStatMap();
            String hostStatName = "";
            String guestStatName = "";
            for (int i = 0; i < event.goalList.size(); i++) {
                GameStatusListBean gameStatusListBean = event.goalList.get(i);
                int type = gameStatusListBean.getType();
                GameStatusBean gameStatusBean;
                if (_gameStatMap.containsKey(type)) {
                    gameStatusBean = _gameStatMap.get(type);
                    if (gameStatusListBean.getHost() == 1) {
                        gameStatusBean.setHost_goal(gameStatusBean.getHost_goal() + 1);
                    } else {
                        gameStatusBean.setGuest_goal(gameStatusBean.getGuest_goal() + 1);
                    }
                } else {
                    gameStatusBean = new GameStatusBean(type);
                    if (gameStatusListBean.getHost() == 1) {
                        gameStatusBean.setHost_goal(gameStatusBean.getHost_goal() + 1);
                    } else {
                        gameStatusBean.setGuest_goal(gameStatusBean.getGuest_goal() + 1);
                    }
                    _gameStatMap.put(type, gameStatusBean);
                }
                if (gameStatusListBean.getHost() == 1) {
                    hostStatName = gameStatusListBean.getName();
                } else {
                    guestStatName = gameStatusListBean.getName();
                }
            }
            for (int type : _statTypeIntList) {
                GameStatusBean gameStatusBean = _gameStatMap.get(type);
                _goalList.add(gameStatusBean);
            }

            //同步一下最新的打点显示
            if (!TextUtils.isEmpty(hostStatName)) {
                viewBinding.tvHostLatestDot.setVisibility(View.VISIBLE);
                viewBinding.tvHostLatestDot.setText(hostStatName);
            }
            if (!TextUtils.isEmpty(guestStatName)) {
                viewBinding.tvGuestLatestDot.setVisibility(View.VISIBLE);
                viewBinding.tvGuestLatestDot.setText(guestStatName);
            }
            AppTrace.d(TAG, "hmScoreView reloadState");
            viewBinding.hmScoreView.reloadState();
        }
    }

    private void resetStatMap() {
        Iterator<Map.Entry<Integer, GameStatusBean>> iterator = _gameStatMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, GameStatusBean> entry = iterator.next();
            Integer key = entry.getKey();
            GameStatusBean gameStatusBean = entry.getValue();
            gameStatusBean.reset();
        }
    }

    private void loadScoreList() {
        AppTrace.d(TAG, "loadScoreList");
        GameStatListRequestBean requestBean = new GameStatListRequestBean(_statTypeIntList);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
        if (!isOk) {
            AppTrace.d(TAG, "获取进球列表失败");
        }
    }

    @Override
    public int getTime() {
        return _recordTime;
    }

    @Override
    public boolean isDotEnable() {
        return checkDotEnable();
    }

    @Override
    public ArrayList<GameStatusListBean> getStateList() {
        return _stateList;
    }

    public boolean checkDotEnable() {
        if (_recordInfo.isLive() && !TextUtils.isEmpty(_recordInfo.live_cid)) {
            if (_competitionMark == null) {
                ToastUtil.s(getString(R.string.please_setup_game_stage));
                return false;
            }
        }
        if (tagIndex == 0 ) {
            ToastUtil.s(getString(R.string.please_setup_game_stage));
            return false;
        }
        return true;

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
            ;
        } else {
            ToastUtil.s(getString(R.string.game_tag_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameScoreClearEvent(GameScoreClearEvent event) {
        if (event != null && event.goalStatusBean!=null) {
            //同步比分
            viewBinding.tvHostScore.setText(String.valueOf(event.goalStatusBean.getHost_goal()));
            viewBinding.tvGuestScore.setText(String.valueOf(event.goalStatusBean.getGuest_goal()));
        }
    }

    private void getMarkId(String competitionId, String videoId, String markName) {

        ApiUtil.queryCompetitionMark(competitionId, videoId, markName, new IGenericApiCallback<CompetitionMarkListBean>() {
            @Override
            public void onSuccess(CompetitionMarkListBean data) {
                try{
                    _competitionMark = data.data.get(0);
                    viewBinding.hmScoreView.setMark(_competitionMark.getMarkName(), _competitionMark.getMarkId());
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(String code, String message) {

            }

            @Override
            public void onFinish() {

            }
        });
    }
    private void retryPendingRequestBean(){
        if(_pendingGameScoreAddBean != null){
            if(retryAddScoreNum >=1 ){
                ToastUtil.s(getString(R.string.add_dot_fail));
                return;
            }
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(_pendingGameScoreAddBean);
            if (!isOk) {
                ToastUtil.s(getString(R.string.device_offline));
                return;
            }
            retryAddScoreNum++;
            return;
        }
        if(_pendingGameStateAddBean != null){
            if(retryAddStateNum >=1 ){
               ToastUtil.s(getString(R.string.add_dot_fail));
               return;
            }
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(_pendingGameStateAddBean);
            if (!isOk) {
                ToastUtil.s(getString(R.string.device_offline));
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordStoppedOnLowStorage(RecordStoppedOnLowStorageEvent event) {
        onRecordVideoStop();
        if(BaseAppUtil.isForegroundActivity(ScoreCenterSuperActivity.this, getClass().getName())){
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
}
