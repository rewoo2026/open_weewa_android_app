package com.xh.hotme.record.score;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GameScoreClearBean;
import com.xh.hotme.bluetooth.bean.GameStatEditBean;
import com.xh.hotme.bluetooth.bean.GameStatListRequestBean;
import com.xh.hotme.bluetooth.bean.GameStatRemoveBean;
import com.xh.hotme.bluetooth.event.CompetitionLabelEvent;
import com.xh.hotme.bluetooth.event.GameScoreClearEvent;
import com.xh.hotme.bluetooth.event.GameStatEditEvent;
import com.xh.hotme.bluetooth.event.GameStatListEvent;
import com.xh.hotme.bluetooth.event.GameStatRemoveEvent;
import com.xh.hotme.bluetooth.event.StateReloadEvent;
import com.xh.hotme.databinding.ScoreCenterLayoutDotListBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.HmCompetitionPoint;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.record.bean.DotTimeEditDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.RecycleViewDivider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/15 17:19
 */
public class GameStateListDialog extends BaseDialog<ScoreCenterLayoutDotListBinding> {

    private final static String TAG = "GameStateEditDialog";

    public interface ICallback {
        void onCancel();

        void onConfirm(String tag);
    }

    private ICallback iCallback;

    private String _gameTag;

    Handler _handler;

    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    GameStatSingleAdapter _adapter;


    private ArrayList<GameStatusListBean> _goalList;

    private ArrayList<GameStatusListBean> _filterList;

    List<Integer> _statTypeIntList;

    List<DotStates> _customerList = new ArrayList<>();

    public String hostTeamName;
    public String guestTeamName;

    int tabIndex = 0;

    DotTimeEditDialog timeEditDialog;

    ISyncTimerListener syncTimerListener;

    Context _context;

    public GameStateListDialog(@NonNull Context context) {
        super(context);
        _context = context;
    }


    public void setSyncTimerListener(ISyncTimerListener listener) {
        this.syncTimerListener = listener;
    }

    public GameStateListDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public void setName(String hostName, String guestName) {
        this.hostTeamName = hostName;
        this.guestTeamName = guestName;

        updateName();

    }

    private void updateName() {
        if (TextUtils.isEmpty(hostTeamName)) {
            viewBinding.tvStateHostName.setText("主队");
        } else {
            viewBinding.tvStateHostName.setText(hostTeamName);
        }

        if (TextUtils.isEmpty(guestTeamName)) {
            viewBinding.tvStateGuestName.setText("客队");
        } else {
            viewBinding.tvStateGuestName.setText(guestTeamName);
        }
    }

    public void setCustomerList(List<DotStates> dotStateList) {
        if (_customerList == null) {
            _customerList = new ArrayList<>();
        }
        if (dotStateList != null) {
            _customerList.addAll(dotStateList);

            if (_adapter != null) {
                _adapter.setCustomerList(dotStateList);
            }

        }
    }


    public void setStateType(List<Integer> _statTypeIntList) {
        this._statTypeIntList = _statTypeIntList;

        loadScoreList();
    }

    public void setDataList(List<GameStatusListBean> dataList) {
        if (_goalList != null) {
            _goalList.clear();
        }
        if (_goalList != null && dataList != null) {
            _goalList.addAll(dataList);
        }

        filterList();
    }


    String _competitionId;
    int _dotScene;

    public void setRecordInfo(String competitionId, int dotScene) {
        _competitionId = competitionId;
        _dotScene = dotScene;
    }

    @Override
    protected void onInit() {

        if (_goalList == null) {
            _goalList = new ArrayList();
        }

        if (_filterList == null) {
            _filterList = new ArrayList();
        }

        _handler = new Handler();

        viewBinding.tvClearState.setOnClickListener(v -> {

            if(_filterList.isEmpty()){
                ToastUtil.s(getContext().getString(R.string.state_empty));
                return;
            }

            showConfirmDialog(
                    getContext().getString(R.string.clear_data),
                    getContext().getString(R.string.clear_data_confirm_message),
                    getContext().getString(R.string.ok),
                    getContext().getString(R.string.cancel),
                    new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            deleteAllPoint();
                            return true;
                        }
                    }, null);


        });

        viewBinding.tvStateHostName.setSelected(true);
        viewBinding.tvStateGuestName.setSelected(false);

        updateName();

        viewBinding.tvStateHostName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                tabIndex = 0;
                viewBinding.tvStateHostName.setSelected(true);
                viewBinding.tvStateGuestName.setSelected(false);

                filterList();
                return true;
            }
        });
        viewBinding.tvStateGuestName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                tabIndex = 1;
                viewBinding.tvStateHostName.setSelected(false);
                viewBinding.tvStateGuestName.setSelected(true);

                filterList();
                return true;
            }
        });

        _adapter = new GameStatSingleAdapter(getContext(), _filterList);
        View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.score_center_edit_empty_view, null);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        _adapter.setEmptyView(emptyView);

        _adapter.setListener(new GameStatSingleAdapter.GoalEditCallback() {
            @Override
            public void onEdit(int index, GameStatusListBean goalInfoBean) {
                AppTrace.d(TAG, "click edit index: " + index);
                if (_dotScene == Constants.DOT_SCENE_RECORDING || _dotScene == Constants.DOT_SCENE_LIVING) {
                    boolean ok = DeviceApiManager.isConnected();
                    if (!ok) {
                        showDisconnectToast();
                        return;
                    }
                }

                int curCompetitionTime = 120 * 60;
                if (syncTimerListener != null) {
                    curCompetitionTime = syncTimerListener.getTime();
                }

                if (timeEditDialog != null && timeEditDialog.isShowing()) {
                    timeEditDialog.dismiss();
                }
                timeEditDialog = new DotTimeEditDialog(_context);
                timeEditDialog.setDate(curCompetitionTime);
                timeEditDialog.setCallback(new DotTimeEditDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onConfirm(int min, int second) {
                        int time = min * 60 + second;
                        if (!TextUtils.isEmpty(_competitionId) && (_dotScene == Constants.DOT_SCENE_LIVING || _dotScene == Constants.DOT_SCENE_VIDEO)) {
                            ApiUtil.updateCompetitionLivePointTime(
                                    _competitionId,
                                    goalInfoBean.getId(),
                                    time,
                                    new IGenericApiCallback<HmCompetitionPoint>() {
                                        @Override
                                        public void onSuccess(HmCompetitionPoint data) {
                                            if (_dotScene == Constants.DOT_SCENE_LIVING) {
                                                editStat(index, time);
                                            } else {
                                                EventBus.getDefault().post(new StateReloadEvent());
                                            }
                                        }

                                        @Override
                                        public void onFail(String code, String message) {
                                            ToastUtil.s(getContext().getString(R.string.update_fail));
                                        }

                                        @Override
                                        public void onFinish() {

                                        }
                                    }
                            );

                        } else {
                            //向相机更新时间点
                            editStat(index, time);
                        }

                    }
                }).show();

                timeEditDialog.setSelectItem(goalInfoBean.getTime());
            }

            @Override
            public void onDelete(int index, GameStatusListBean goalInfoBean) {
                AppTrace.d(TAG, "click delete index: " + index);
                if (_dotScene == Constants.DOT_SCENE_RECORDING || _dotScene == Constants.DOT_SCENE_LIVING) {
                    boolean ok = DeviceApiManager.isConnected();
                    if (!ok) {
                        showDisconnectToast();
                        return;
                    }
                }

                showConfirmDialog(
                        "",
                        "确认要删除这条数据吗",
                        getContext().getString(R.string.ok),
                        getContext().getString(R.string.cancel),
                        new ClickGuard.GuardedOnClickListener() {
                            @Override
                            public boolean onClicked() {
                                if (!TextUtils.isEmpty(_competitionId) && (_dotScene == Constants.DOT_SCENE_LIVING || _dotScene == Constants.DOT_SCENE_VIDEO)) {
                                    ApiUtil.deleteCompetitionLivePoint(_competitionId,
                                            goalInfoBean.getId(),
                                            new IGenericApiCallback<Object>() {
                                                @Override
                                                public void onSuccess(Object data) {
                                                    if (_dotScene == Constants.DOT_SCENE_LIVING) {
                                                        deleteStat(goalInfoBean, index);
                                                    } else {
                                                        EventBus.getDefault().post(new StateReloadEvent());
                                                    }
                                                }

                                                @Override
                                                public void onFail(String code, String message) {
                                                    AppTrace.d(TAG, "删除打点失败： code=" + code + ", msg=" + message);
                                                    if(code.equalsIgnoreCase("602")){
                                                        if (_dotScene == Constants.DOT_SCENE_LIVING) {
                                                            deleteStat(goalInfoBean, index);
                                                        } else {
                                                            EventBus.getDefault().post(new StateReloadEvent());
                                                        }
                                                    } else {
                                                        ToastUtil.s(getContext().getString(R.string.remove_dot_fail));
                                                    }
                                                }

                                                @Override
                                                public void onFinish() {

                                                }
                                            });

                                } else {
                                    deleteStat(goalInfoBean, index);
                                }

                                return true;
                            }
                        }, null
                );
            }
        });
        viewBinding.stateRecyclerView.addItemDecoration(new RecycleViewDivider(getContext(), LinearLayoutManager.VERTICAL, getContext().getResources().getColor(R.color.color_353535)));
        viewBinding.stateRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewBinding.stateRecyclerView.setAdapter(_adapter);

        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = BaseAppUtil.getDeviceWidth() / 2;
        windowparams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        windowparams.gravity = Gravity.LEFT;
        window.setWindowAnimations(R.style._leftDialogAnimation);
    }

    private void deleteAllPoint(){
        if (!TextUtils.isEmpty(_competitionId) && (_dotScene == Constants.DOT_SCENE_LIVING || _dotScene == Constants.DOT_SCENE_VIDEO)) {

            AppTrace.d(TAG, "delete stat pointId = ");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < _filterList.size(); i++) {
                ids.add(_filterList.get(i).getId());
            }

            ApiUtil.deleteAllCompetitionLivePoint(
                    _competitionId,
                    ids,
                    new IGenericApiCallback<Object>() {
                        @Override
                        public void onSuccess(Object data) {
                            if (_dotScene == Constants.DOT_SCENE_LIVING) {
                                clearScore();
                            } else {
                                EventBus.getDefault().post(new StateReloadEvent());
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {
                            AppTrace.d(TAG, "删除打点失败： code=" + code + ", msg=" + message);
                            ToastUtil.s(String.format(getContext().getString(R.string.remove_dot_fail) +", code=" + code + ", msg=" + message));
                        }

                        @Override
                        public void onFinish() {

                        }
                    }
            );

        } else {
            //先删掉比分，后删掉打点
            clearScore();
        }
    }

    private void editStat(int seqIndex, int time) {
        GameStatEditBean goalEditBean = new GameStatEditBean();
        goalEditBean.host_team = tabIndex == 0 ? 1 : 0;
        goalEditBean.type = -1;
        goalEditBean.time = time;
        goalEditBean.seq = seqIndex;
        goalEditBean.remove = 0;
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(goalEditBean);
        if (!isOk) {
            ToastUtil.s(getContext().getString(R.string.device_offline));
            return;
        }
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        DialogUtil.showDialog(getContext().getString(R.string.loading));
    }

    private void deleteStat(GameStatusListBean goalInfoBean, int index) {
        GameStatEditBean goalEditBean = new GameStatEditBean();
        goalEditBean.host_team = tabIndex == 0 ? 1 : 0;
        goalEditBean.seq = index;
        goalEditBean.type = -1;
        goalEditBean.remove = 1;
        goalEditBean.time = goalInfoBean.getTime();
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(goalEditBean);
        if (!isOk) {
            ToastUtil.s(getContext().getString(R.string.device_offline));
            return;
        }
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        DialogUtil.showDialog(getContext().getString(R.string.loading));
    }

    private void clearStat() {
        GameStatRemoveBean gameStatBean = new GameStatRemoveBean(tabIndex == 0 ? 1 : 0, -1);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(gameStatBean);
        if (!isOk) {
            DialogUtil.dismissDialog();
            ToastUtil.s(getContext().getString(R.string.device_offline));
            return;
        }
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
    }

    private void clearScore() {
        GameScoreClearBean scoreClearBean = new GameScoreClearBean(tabIndex == 0 ? 1 : 0);
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(scoreClearBean);
        if (!isOk) {
            ToastUtil.s(getContext().getString(R.string.device_offline));
            return;
        }
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
        DialogUtil.showDialog(getContext().getString(R.string.loading));
    }

    private void showConfirmDialog(String title, String message, String positiveBtnText, String negativeBtnText, ClickGuard.GuardedOnClickListener positiveListener, ClickGuard.GuardedOnClickListener negativeListener) {
        ModalDialog d = new ModalDialog(getContext());
        d.setTitle(title);
        d.setMessage(message);
        d.setNegativeButton(negativeBtnText, negativeListener);
        d.setPositiveButton(positiveBtnText, positiveListener);
        d.show();
    }

    private void loadScoreList() {
        AppTrace.d(TAG, "loadScoreList");
        if (_statTypeIntList != null) {
            GameStatListRequestBean requestBean = new GameStatListRequestBean(_statTypeIntList);
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
            if (!isOk) {
                AppTrace.d(TAG, "获取打点列表失败");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetGameEvent(CompetitionLabelEvent event) {
        DialogUtil.dismissDialog();
        _handler.removeCallbacksAndMessages(null);
        if (event != null && event.state == 1) {
            cancel();
            if (iCallback != null) {
                iCallback.onConfirm(_gameTag);
            }
        } else {
            ToastUtil.s(getContext().getString(R.string.game_tag_fail));
        }
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameScoreClearEvent(GameScoreClearEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        if (event != null && event.goalStatusBean!=null) {
            clearStat();
        } else {
            ToastUtil.s(getContext().getString(R.string.clear_data_fail));
            DialogUtil.dismissDialog();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameStatEditEvent(GameStatEditEvent event) {
        if (event != null) {
            loadScoreList();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGameStatRemoveEvent(GameStatRemoveEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        if (event != null) {
            loadScoreList();
        }
    }

    private void filterList() {
        if (_filterList != null) {
            _filterList.clear();
        }

        for (int i = 0; i < _goalList.size(); i++) {
            GameStatusListBean gameStatusListBean = _goalList.get(i);
            if (gameStatusListBean.getHost() == 1 && tabIndex == 0) {
                _filterList.add(gameStatusListBean);
            } else if (gameStatusListBean.getHost() == 0 && tabIndex == 1) {
                _filterList.add(gameStatusListBean);
            }
        }

        _adapter.notifyDataSetChanged();
    }

    public void reload() {
        if (_adapter != null) {
            _adapter.notifyDataSetChanged();
        }
    }
}