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
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.liulishuo.filedownloader.FileDownloader;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.GameStatusListBean;

import com.xh.hotme.bluetooth.bean.GameStatListRequestBean;
import com.xh.hotme.bluetooth.event.GameStatListEvent;
import com.xh.hotme.databinding.VideoDotLayoutDotListBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.RecycleViewDivider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class VideoStateListDialog extends BaseDialog<VideoDotLayoutDotListBinding> {

    private final static String TAG = "VideoStateListDialog";

    public interface ICallback {
        void onCancel();

        void onConfirm(GameStatusListBean statusListBean);
    }

    private ICallback iCallback;


    Handler _handler;

    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    VideoStatListAdapter _adapter;


    private ArrayList<GameStatusListBean> _goalList;

    private ArrayList<GameStatusListBean> _filterList;

    List<Integer> _statTypeIntList;

    List<DotStates> _customerList = new ArrayList<>();

    public String hostTeamName;
    public String guestTeamName;

    ISyncTimerListener syncTimerListener;

    Context _context;

    int appendNumber = 0;

    int filterType;

    boolean isOnlyGoal = false;

    int _selectIndex = -1;
    GameStatusListBean _selectedStateBean = null;

    public VideoStateListDialog(@NonNull Context context) {
        super(context);
        _context = context;
    }


    public void setSyncTimerListener(ISyncTimerListener listener) {
        this.syncTimerListener = listener;
    }

    public VideoStateListDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public void setName(String hostName, String guestName) {
        this.hostTeamName = hostName;
        this.guestTeamName = guestName;

        if (_adapter != null) {
            _adapter.setHostTeamName(hostTeamName);
            _adapter.setGuestTeamName(guestTeamName);
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

    CameraVideoListBean.VideosBean _competitionBean;

    public void setCompetitionBean(CameraVideoListBean.VideosBean videoBean) {
        this._competitionBean = videoBean;
        hostTeamName = videoBean.host_team;
        guestTeamName = videoBean.guest_team;
        if (TextUtils.isEmpty(hostTeamName)) {
            hostTeamName = getContext().getString(R.string.home_team);
        }
        if (TextUtils.isEmpty(guestTeamName)) {
            guestTeamName = getContext().getString(R.string.guest_team);
        }

        if (_adapter != null) {
            _adapter.setHostTeamName(hostTeamName);
            _adapter.setGuestTeamName(guestTeamName);
        }
        initStateList();
    }

    public void setDataList(ArrayList<GameStatusListBean> stateList){
        if (_goalList == null) {
            _goalList = new ArrayList();
        }

        if (_filterList == null) {
            _filterList = new ArrayList();
        }

        _goalList.clear();
        _goalList.addAll(stateList);

        filterList();
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

        viewBinding.goalType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                isOnlyGoal = isChecked?true:false;
                filterList();
            }
        });


        viewBinding.category.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.categoryAll) {
                filterType = Constants.INT_STATE_FILTER_TYPE_ALL_TEAM;
            } else if (checkedId == R.id.categoryHostTeam) {
                filterType = Constants.INT_STATE_FILTER_TYPE_HOST_TEAM;
            } else if (checkedId == R.id.categoryGuestTeam) {
                filterType = Constants.INT_STATE_FILTER_TYPE_GUEST_TEAM;
            } else {
                filterType = Constants.INT_STATE_FILTER_TYPE_ALL_TEAM;
            }
            filterList();
        });


        _adapter = new VideoStatListAdapter(getContext(), _filterList);
        View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.video_dot_edit_empty_view, null);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        _adapter.setEmptyView(emptyView);

        _adapter.setHostTeamName(hostTeamName);
        _adapter.setGuestTeamName(guestTeamName);

        _adapter.setListener(new VideoStatListAdapter.VideoStateCallback() {
            @Override
            public void onClick(int index, GameStatusListBean goalInfoBean) {
                AppTrace.d(TAG, "click edit index: ");
                if (iCallback != null) {
                    iCallback.onConfirm(goalInfoBean);
                }
                _adapter.setSelectedGameStatusListBean(goalInfoBean);
                _adapter.notifyDataSetChanged();
            }

        });
        viewBinding.stateRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, DensityUtil.dip2px(10), false));
        viewBinding.stateRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewBinding.stateRecyclerView.setAdapter(_adapter);

        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = BaseAppUtil.getDeviceWidth() * 2 / 5;
        windowparams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        windowparams.gravity = Gravity.RIGHT;
        window.setWindowAnimations(R.style._rightDialogAnimation);
    }

    private void initStateList() {
        if (_competitionBean != null) {
            if (_goalList != null) {
                _goalList.clear();
            }
            String stateFilePath = VideoUtil.getCompetitionStateFilePath(_competitionBean);
            List<GameStatusListBean> gameStatusListBeanList =  ScoreCenterUtil.getStateListFromFile(stateFilePath, appendNumber);
            if (gameStatusListBeanList.size() > 0) {
                _goalList.addAll(gameStatusListBeanList);
            }

            filterType = -1;
            filterList();
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
    public void onGameStatListEvent(GameStatListEvent event) {
        AppTrace.d(TAG, "onGameStatListEvent");
        try {
            if (event.requestBean != null) {
                GameStatListRequestBean requestBean = (GameStatListRequestBean) event.requestBean;
                int typeSize = requestBean.getTypes().size();
                if (typeSize != _statTypeIntList.size()) {
                    return;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }


        if (_goalList != null) {
            _goalList.clear();
        }
        for (int i = 0; i < event.goalList.size(); i++) {
            GameStatusListBean gameStatusListBean = event.goalList.get(i);
            gameStatusListBean.setOri_seq(i);
            _goalList.add(gameStatusListBean);
        }

        filterList();
    }

    private void filterList() {
        if (_filterList != null) {
            _filterList.clear();
        }

        for (int i = 0; i < _goalList.size(); i++) {
            GameStatusListBean gameStatusListBean = _goalList.get(i);
            if(isOnlyGoal){
                if( ScoreCenterUtil.isGoalType(gameStatusListBean.getType())){
                    if (filterType == Constants.INT_STATE_FILTER_TYPE_ALL_TEAM) {
                        _filterList.add(gameStatusListBean);
                    } else if (filterType == Constants.INT_STATE_FILTER_TYPE_HOST_TEAM) {
                        if (gameStatusListBean.getHost() == 1) {
                            _filterList.add(gameStatusListBean);
                        }
                    } else if (filterType == Constants.INT_STATE_FILTER_TYPE_GUEST_TEAM) {
                        if (gameStatusListBean.getHost() != 1) {
                            _filterList.add(gameStatusListBean);
                        }
                    }
                }
            }else{
                if (filterType == Constants.INT_STATE_FILTER_TYPE_ALL_TEAM) {
                    _filterList.add(gameStatusListBean);
                } else if (filterType == Constants.INT_STATE_FILTER_TYPE_HOST_TEAM) {
                    if (gameStatusListBean.getHost() == 1) {
                        _filterList.add(gameStatusListBean);
                    }
                } else if (filterType == Constants.INT_STATE_FILTER_TYPE_GUEST_TEAM) {
                    if (gameStatusListBean.getHost() != 1) {
                        _filterList.add(gameStatusListBean);
                    }
                }
            }

        }

        _adapter.notifyDataSetChanged();
    }

    public void resetPlayStatus(){
        if(_adapter!=null){
            _adapter.setSelectedGameStatusListBean(null);
            _adapter.notifyDataSetChanged();
        }
    }
}