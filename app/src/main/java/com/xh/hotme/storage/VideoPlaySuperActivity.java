package com.xh.hotme.storage;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bluetooth.event.StateReloadEvent;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.ActivityVideoPlayerSuperBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CompetitionMarkListBean;
import com.xh.hotme.live.bean.CompetitionStatListBean;
import com.xh.hotme.live.bean.HmCompetitionPoint;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.record.bean.HmCompetitionMark;
import com.xh.hotme.record.score.IHmStateListener;
import com.xh.hotme.record.score.ISyncTimerListener;
import com.xh.hotme.record.score.VideoStateListDialog;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

@IEventBus
@FullscreenLandscape(immersive = true)
public class VideoPlaySuperActivity extends BaseActivity<ActivityVideoPlayerSuperBinding> {
    private final static String TAG = VideoPlaySuperActivity.class.getSimpleName();

    CmoIjkVideoView _playerView;

    String _videoUrl = "";

    CameraVideoDetailListBean.VideosBean _videoBean;
    CameraVideoListBean.VideosBean _competitionBean;


    BluetoothLoadingDialog _connectingDialog;

    int retryCount = 0;

    boolean isPlaying;


    CompetitionStatListBean _competitionStateListBean;

    CompetitionMarkListBean _competitionMarkListBean;


    boolean isShowDotView = false;

    ModalDialog _networkDialog;
    ModalDialog _syncDialog;

    boolean isSynced;


    VideoStateListDialog _stateListDialog;

    private int appendNumber = 0;
    private ArrayList<GameStatusListBean> _goalList;

    List<CameraVideoDetailListBean.VideosBean> _videoList = new ArrayList<>();

    private final Runnable _deviceUsageInfoRunnable = new Runnable() {
        @Override
        public void run() {
            _handler.postDelayed(_deviceUsageInfoRunnable, 1000);
        }
    };

    private final Runnable updateSeekBarRunnable = new Runnable() {
        public void run() {
            updateSeekBar();
        }
    };

    public static void start(Context context, CameraVideoListBean.VideosBean competitionBean, CameraVideoDetailListBean.VideosBean model) {
        Intent intent = new Intent(context, VideoPlaySuperActivity.class);
        intent.putExtra(Constants.COMPETITION_BEAN, GsonUtils.GsonString(competitionBean));
        intent.putExtra(Constants.VIDEO_BEAN, GsonUtils.GsonString(model));
        context.startActivity(intent);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.COMPETITION_BEAN, GsonUtils.GsonString(_videoBean));
        outState.putString(Constants.VIDEO_BEAN, GsonUtils.GsonString(_videoBean));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            _videoUrl = savedInstanceState.getString(Constants.VIDEO_URL);
        }

        if (getIntent() != null) {
            String videoInfo = getIntent().getStringExtra(Constants.VIDEO_BEAN);
            String competitionInfo = getIntent().getStringExtra(Constants.COMPETITION_BEAN);
            _videoBean = GsonUtils.GsonToBean(videoInfo, CameraVideoDetailListBean.VideosBean.class);
            _competitionBean = GsonUtils.GsonToBean(competitionInfo, CameraVideoListBean.VideosBean.class);
            _videoUrl = VideoUtil.getVideoFilePath(_videoBean);

            _videoList = AppFileUtil.loadDeviceVideoJson(_competitionBean);
        }


        super.onCreate(savedInstanceState);
    }

    @Override
    public void initView() {
        _playerView = viewBinding.playerView;

        viewBinding.videoPlayBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                //暂停 和播放：
                boolean tag = viewBinding.videoPlayBtn.getTag() == null ? false : (boolean) viewBinding.videoPlayBtn.getTag();
                viewBinding.videoPlayBtn.setTag(!tag);
                if (viewBinding.playerView.isPlaying()) {
                    setPlayStatus(true);
                } else {
                    setPlayStatus(false);
                }

                return true;
            }
        });

        viewBinding.videoVoiceBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                //静音和外放：
                boolean tag = viewBinding.videoVoiceBtn.getTag() == null ? false : (boolean) viewBinding.videoVoiceBtn.getTag();

                viewBinding.videoVoiceBtn.setImageResource(tag ? R.mipmap.ic_video_voice_on_white : R.mipmap.ic_video_voice_off_white);

                viewBinding.videoVoiceBtn.setTag(!tag);

                return true;
            }
        });

        viewBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            boolean isTouch = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    //计算  拖拉后的  时间长度：
                    viewBinding.playerView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTouch = true;
                setPlayStatus(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTouch = false;
                setPlayStatus(false);
            }
        });

        viewBinding.playerView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                AppTrace.d(TAG, "onInfo state: " + i);
                return false;
            }
        });
        viewBinding.playerView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                AppTrace.d(TAG, "onPrepared ...");
                AppTrace.d(TAG, "video duration =" + viewBinding.playerView.getDuration());
                viewBinding.seekBar.setMax(viewBinding.playerView.getDuration());
                updateSeekBar();
            }
        });


        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (viewBinding.playerView != null) {
                    if (viewBinding.playerView.isPlaying()) {
                        viewBinding.playerView.pause();
                    }
                    viewBinding.playerView.release(true);
                }

                finish();

                return true;
            }
        });
        viewBinding.llExit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                isShowDotView = false;
                setDotState(false);
                return true;
            }
        });

        viewBinding.icScDot.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                //更新打点状态
                setDotState(!isShowDotView);

                isShowDotView = !isShowDotView;

                viewBinding.icScDot.setTag(isShowDotView);
                return true;
            }
        });

        viewBinding.icScDotView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (_stateListDialog != null && _stateListDialog.isShowing()) {
                    _stateListDialog.dismiss();
                }
                _stateListDialog = new VideoStateListDialog(VideoPlaySuperActivity.this);
                _stateListDialog.setCompetitionBean(_competitionBean);
                _stateListDialog.setDataList(_goalList);
                _stateListDialog.setCallback(new VideoStateListDialog.ICallback() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onConfirm(GameStatusListBean statusListBean) {
                        AppTrace.d(TAG, "seek to dot " + GsonUtils.GsonString(statusListBean));
                        seekToPosition(statusListBean.getTime() * 1000);
                        setPlayStatus(true);
                    }
                });
                _stateListDialog.show();
                return false;
            }
        });

        viewBinding.tvPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (viewBinding.playerView.isPlaying()) {
                    setPlayStatus(true);
                } else {
                    setPlayStatus(false);
                }
            }
        });

        viewBinding.tvBackSecondFive.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                int position = viewBinding.playerView.getCurrentPosition();
                position = position - 5 * 1000 >= 0 ? position - 5 * 1000 : 0;
                seekToPosition(position);
                return true;
            }
        });
        viewBinding.tvBackSecondTen.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                int position = viewBinding.playerView.getCurrentPosition();
                position = position - 10 * 1000 >= 0 ? position - 10 * 1000 : 0;
                seekToPosition(position);
                return true;
            }
        });

        viewBinding.tvForwardSecondFive.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                int position = viewBinding.playerView.getCurrentPosition();
                position = position + 5 * 1000;
                seekToPosition(position);
                return true;
            }
        });
        viewBinding.tvForwardSecondTen.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                int position = viewBinding.playerView.getCurrentPosition();
                position = position + 10 * 1000;
                seekToPosition(position);
                return true;
            }
        });
        viewBinding.tvVideoTime.setText(String.format("回放视频 %s", TimeUtil.intToTimeMin(_videoBean.duration)));

        viewBinding.hmScoreView.setVisibility(View.GONE);
        viewBinding.hmScoreView.setHostTeamName(_competitionBean.host_team);
        viewBinding.hmScoreView.setGuestTeamName(_competitionBean.guest_team);
        viewBinding.hmScoreView.setCompetitionId(_competitionBean.competition_id);
        viewBinding.hmScoreView.setVideoName(_videoBean.name);
        viewBinding.hmScoreView.setVideoId(_videoBean.videoId);
        viewBinding.hmScoreView.setDotScene(Constants.DOT_SCENE_VIDEO);
        viewBinding.hmScoreView.setSyncTmeListener(new ISyncTimerListener() {
            @Override
            public int getTime() {
                int position = viewBinding.playerView.getCurrentPosition()/1000;

                return position;
            }

            @Override
            public boolean isDotEnable() {
                return true;
            }

            @Override
            public ArrayList<GameStatusListBean> getStateList() {
                return _goalList;
            }
        });

        viewBinding.hmScoreView.setCallback(new IHmStateListener() {
            @Override
            public void onAddState(int hotTeam, DotStates dotState) {
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

        setDotState(isShowDotView);

    }

    public void setPlayStatus(boolean pause){
        if(pause){
            viewBinding.playerView.pause();
            Drawable drawable = getResources().getDrawable(R.mipmap.iv_video_play_btn_play);
            viewBinding.tvPlayBtn.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            viewBinding.tvPlayBtn.setText("点击继续");

            viewBinding.videoPlayBtn.setImageResource(R.mipmap.iv_video_play_white);
        } else {
            viewBinding.playerView.start();
            Drawable drawable = getResources().getDrawable(R.mipmap.iv_video_play_btn_pause);
            viewBinding.tvPlayBtn.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            viewBinding.tvPlayBtn.setText("点击暂停");

            viewBinding.videoPlayBtn.setImageResource(R.mipmap.iv_video_pause_white);

            if(_stateListDialog != null && _stateListDialog.isShowing()){
                _stateListDialog.resetPlayStatus();
            }
        }
    }

    private void updateSeekBar() {
        int videoTime = viewBinding.playerView.getCurrentPosition();
        viewBinding.seekBar.setProgress(videoTime);
        viewBinding.tvVideoTime.setText(String.format("%s %s", _videoBean.name, TimeUtil.intToTimeMin(videoTime / 1000)));
        viewBinding.videoTime.setText(String.format("%s/%s", TimeUtil.intToTimeMin(videoTime / 1000), TimeUtil.intToTimeMin(viewBinding.playerView.getDuration() / 1000)));
        _handler.postDelayed(updateSeekBarRunnable, 1000);
    }

    /**
     * @param position 单位毫秒
     */
    private void seekToPosition(int position) {
        viewBinding.playerView.seekTo(position);
        viewBinding.seekBar.setProgress(position);
        viewBinding.tvVideoTime.setText(String.format("%s %s", _videoBean.name, TimeUtil.intToTimeMin(position / 1000)));
        viewBinding.videoTime.setText(String.format("%s/%s", TimeUtil.intToTimeMin(position / 1000), TimeUtil.intToTimeMin(viewBinding.playerView.getDuration() / 1000)));
    }

    @Override
    protected void initData() {
        startPlay();

        initStateList();

        initDotInfo();

        if(!NetUtil.isConnectedAvailableNetwork()){
            showNetworkDialog();
        }

        //延时加载加载打带你
        _handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initStateUI();
            }
        }, 1000);

    }
    private void showNetworkDialog(){
        if(_networkDialog != null && _networkDialog.isShowing()){
            return;
        }
        _networkDialog = new ModalDialog(VideoPlaySuperActivity.this);
        _networkDialog.setTitle(getString(R.string.tip));
        _networkDialog.setMessage(getString(R.string.dialog_connect_network_to_sync_dot_info));
        _networkDialog.setNegativeButton(getString(R.string.cancel), v -> _networkDialog.dismiss());
        _networkDialog.setPositiveButton(getString(R.string.ok), v ->  {
            _networkDialog.dismiss();
            HotmeWiFiManager.startWifiSettingPage(VideoPlaySuperActivity.this);
        });
        _networkDialog.show();

    }

    private void showSyncDotDialog(){
        if(_syncDialog!=null && _syncDialog.isShowing()){
            return;
        }
        _syncDialog = new ModalDialog(VideoPlaySuperActivity.this);
        _syncDialog.setMessage(getString(R.string.dialog_sync_point_info_to_server));
        _syncDialog.setNegativeButton(getString(R.string.cancel), v -> _syncDialog.dismiss());
        _syncDialog.setPositiveButton(getString(R.string.ok), v ->  {
            _syncDialog.dismiss();
            initDotInfo();
        });
        _syncDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppTrace.d(TAG, "onPause...");

        if (_playerView != null && _playerView.isPlaying()) {
            _playerView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume...");

        if (_playerView == null) {
            initView();
        }
        if (_goalList != null && !_goalList.isEmpty()) {
            if (!isSynced && NetUtil.isConnectedAvailableNetwork()) {
                showSyncDotDialog();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AppTrace.d(TAG, "onStop...");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy...");

        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
        }

        if (_playerView != null) {
            _playerView.pause();
            _playerView.release(true);
        }

    }

    @Override
    protected int getStatusBarColor() {
        return 0xff000000;
    }

    @Override
    protected int getNavigationBarColor() {
        return 0xff000000;
    }

    @Override
    protected boolean isDarkStatusBar() {
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void startPlay() {
        retryCount = 0;

        if (viewBinding.playerView.isPlaying()) {
            viewBinding.playerView.release(false);
            viewBinding.playerView.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }

        viewBinding.playerView.setVideoPath(_videoUrl);
        viewBinding.playerView.start();
    }

    private void showConnectingDialog(String message) {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(VideoPlaySuperActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }


    public void setDotState(boolean isDot) {
        if (isDot) {
            viewBinding.llDotView.setVisibility(View.VISIBLE);
            viewBinding.layoutDotState.setVisibility(View.GONE);
            viewBinding.hmScoreView.setVisibility(View.VISIBLE);
        } else {
            viewBinding.llDotView.setVisibility(View.GONE);
            viewBinding.layoutDotState.setVisibility(View.VISIBLE);
            viewBinding.hmScoreView.setVisibility(View.GONE);
        }
    }

    private void initStateUI(){
        int width = viewBinding.layoutSeekDot.getWidth();
        int duration = _videoBean.duration;  //单位秒
        for (GameStatusListBean stateBean : _goalList) {
            int time = stateBean.getTime();
            double percent = (double) time / duration;
            BigDecimal bg = new BigDecimal(percent);
            double f1 = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            int step = (int) (f1 * width);

            View view = LayoutInflater.from(VideoPlaySuperActivity.this).inflate(R.layout.video_dot_seek_dot_view, null);

            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.width = DensityUtil.dip2px(6);
            params.height = DensityUtil.dip2px(6);
            params.leftMargin = step - DensityUtil.dip2px(3) < 0 ? 0 : step - DensityUtil.dip2px(3);
            view.setLayoutParams(params);

            viewBinding.layoutSeekDot.addView(view);
            viewBinding.layoutSeekDot.setVisibility(View.VISIBLE);
        }
    }

    private void initStateList() {
        if (_competitionBean != null) {
            if (_goalList != null) {
                _goalList.clear();
            } else {
                _goalList = new ArrayList<>();
            }

            String stateFilePath = VideoUtil.getCompetitionStateFilePath(_competitionBean);
            List<GameStatusListBean> gameStatusListBeanList =  ScoreCenterUtil.getStateListFromFile(stateFilePath, appendNumber);
            if (gameStatusListBeanList.size() > 0) {
                _goalList.addAll(gameStatusListBeanList);
            }

        }
    }

    private void initStateListFromServer(CompetitionStatListBean dotListBean) {
        if (_competitionBean != null) {
            if (_goalList != null) {
                _goalList.clear();
            } else {
                _goalList = new ArrayList<>();
            }
            List<DotStates> customDotStateList = new ArrayList<>();
            for (HmCompetitionPoint dotBean : dotListBean.data) {
                String name = dotBean.pointId;
                int type = ScoreCenterUtil.getIdByName(dotBean.pointType);
                if (type == -1) {
                    type = 100 + customDotStateList.size();
                    DotStates customState = new DotStates(type, name);
                    customDotStateList.add(customState);
                }

                GameStatusListBean gameStatusListBean = new GameStatusListBean(type);
                String teamName = dotBean.pointTeam;
                if (teamName.equalsIgnoreCase(_competitionBean.host_team)) {
                    gameStatusListBean.setHost(1);
                } else {
                    gameStatusListBean.setHost(0);
                }
                gameStatusListBean.setTime(dotBean.pointTimeOffset);
                gameStatusListBean.setName(dotBean.pointType);
                gameStatusListBean.setMark_id(dotBean.markId);
                gameStatusListBean.setId(dotBean.pointId);


                _goalList.add(gameStatusListBean);
            }

            if (viewBinding.layoutSeekDot != null) {
                viewBinding.layoutSeekDot.removeAllViews();
            }

            initStateUI();
        }
    }

    private void syncCompetitionMarkToServer() {
        if (_goalList == null || _goalList.isEmpty()) {
            AppTrace.d(TAG, "syncCompetitionMarkToServer： no dot info....");
            DialogUtil.dismissDialog();
            return;
        }
        List<String > markNameList = new ArrayList<>();
        List<HmCompetitionMark> data = new ArrayList<>();
        for (GameStatusListBean stateBean : _goalList) {
            if(!markNameList.contains(stateBean.getMark_name())){
                HmCompetitionMark mark = new HmCompetitionMark();
                mark.setCompetitionId(_competitionBean.competition_id);
                mark.setDeviceId(_competitionBean.deviceId);
                mark.setMarkName(stateBean.getMark_name());
                mark.setVideoName(_videoBean.name);
                mark.setMarkTimeOffset(stateBean.getTime());
                data.add(mark);
                markNameList.add(stateBean.getMark_name());
            }
        }

        ApiUtil.addCompetitionMarkBatch(data, new IGenericApiCallback<CompetitionMarkListBean>() {
            @Override
            public void onSuccess(CompetitionMarkListBean data) {
                AppTrace.d(TAG, "syncCompetitionMarkToServer success");
                ToastUtil.s(getString(R.string.info_sync_mark_info_success));

                _competitionMarkListBean = data;
                resetMarkIdForStateList();

                syncDotListToServer();
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, String.format("syncCompetitionMarkToServer fail: code =%s, message=%s", code, message));

            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });


    }

    private String getMarkIdByName(String markName){
        if(_competitionMarkListBean != null && _competitionMarkListBean.data != null) {
            for (HmCompetitionMark markBean : _competitionMarkListBean.data) {
                if (markName.equalsIgnoreCase(markBean.getMarkName())) {
                    return markBean.getMarkId();
                }
            }
        }
        return "";
    }

    private void resetMarkIdForStateList(){
        if(_competitionMarkListBean != null && _competitionMarkListBean.data != null) {
            for (GameStatusListBean stateBean : _goalList) {
                String markId = getMarkIdByName(stateBean.getMark_name());
                stateBean.setMark_id(markId);
            }
        }
    }

    private void syncDotListToServer() {
        if (_goalList == null || _goalList.isEmpty()) {
            AppTrace.d(TAG, "syncDotListToServer：no dot info....");
            DialogUtil.dismissDialog();
            isSynced = true;
            return;
        }

        List<HmCompetitionPoint> data = new ArrayList<>();
        for (GameStatusListBean stateBean : _goalList) {
            HmCompetitionPoint competitionPoint = new HmCompetitionPoint();
            competitionPoint.competitionId = _competitionBean.competition_id;
            competitionPoint.deviceId = _competitionBean.deviceId;
            competitionPoint.pointType = stateBean.getName();
            competitionPoint.videoId = _videoBean.getVideoId();
            competitionPoint.pointTimeOffset = stateBean.getTime();
            competitionPoint.pointSource = Constants.DOT_SOURCE_MANUAL;
            competitionPoint.markId = stateBean.getMark_id();
            competitionPoint.videoName = _videoBean.name;
            if(stateBean.getHost() == 1) {
                competitionPoint.pointTeam = _competitionBean.host_team;
            } else {
                competitionPoint.pointTeam = _competitionBean.guest_team;
            }

            data.add(competitionPoint);
        }

        ApiUtil.addCompetitionPointBatch(data, new IGenericApiCallback<CompetitionStatListBean>() {
            @Override
            public void onSuccess(CompetitionStatListBean data) {
                AppTrace.d(TAG, "syncDotListToServer success");
                ToastUtil.s(getString(R.string.info_sync_dot_info_success));
                DialogUtil.dismissDialog();

                initStateListFromServer(data);
                isSynced = true;
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, String.format("syncDotListToServer fail: code =%s, message=%s", code, message));
                DialogUtil.dismissDialog();
            }

            @Override
            public void onFinish() {

            }
        });


    }

    private void initDotInfo() {
        if(!NetUtil.isConnectedAvailableNetwork()){
            showNetworkDialog();
            return;
        }
        DialogUtil.showDialog("正在同步视频信息");
        if (TextUtils.isEmpty(_competitionBean.competition_id)) {
            addCompetition();
        } else {
            checkCompetitionPoint();
        }

    }

    private void addCompetition() {
        HmCompetition requestBean = new HmCompetition();
        requestBean.setDeviceId(_competitionBean.deviceId);
        requestBean.setCompetitionName(_competitionBean.competition_name);
        long time = TimeUtil.str2Millis(_competitionBean.time);
        String formatTime = TimeUtil.millis2YMDHMS(time);
        requestBean.setCompetitionTime(formatTime);
        requestBean.setMotionType(_competitionBean.motion_type);
        requestBean.setCompetitionStatus(Constants.COMPETITION_STOP);
        requestBean.setPlace(_competitionBean.address);
        if (_competitionBean.player_num == 5) {
            requestBean.setPlaceType(1);
        } else if (_competitionBean.player_num == 11) {
            requestBean.setPlaceType(3);
        } else {
            requestBean.setPlaceType(2);
        }
        requestBean.setHomeTeamName(_competitionBean.host_team);
        requestBean.setAwayTeamName(_competitionBean.guest_team);
        requestBean.setHomeTeamScore(_competitionBean.home_goals);
        requestBean.setAwayTeamScore(_competitionBean.away_goals);

        AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition, upload competition info: " + new Gson().toJson(requestBean));
        ApiUtil.addCompetition(requestBean, new IGenericApiCallback<HmCompetition>() {
            @Override
            public void onSuccess(HmCompetition data) {
                DialogUtil.dismissDialog();
                _competitionBean.competition_id = data.getCompetitionId();
                syncCompetitionInfoToLocal(data.getCompetitionId());
                AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition success, competition id: " + data.getCompetitionId());
                _videoBean.competitionId = data.getCompetitionId();
                viewBinding.hmScoreView.setCompetitionId(_competitionBean.competition_id);
                //检查比赛打点数据是否同步
                checkCompetitionPoint();
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "competition info fail, code=" + code + ", message=" + message);
                DialogUtil.dismissDialog();
            }

            @Override
            public void onFinish() {
            }
        });


    }

    private void checkMark(String competitionId, String videoName){
        AppTrace.d(TAG, "checkMark");
        ApiUtil.queryCompetitionMark(competitionId, videoName, new IGenericApiCallback<CompetitionMarkListBean>() {
            @Override
            public void onSuccess(CompetitionMarkListBean data) {
                if(data != null && data.data.isEmpty()){
                    syncCompetitionMarkToServer();
                } else {
                    _competitionMarkListBean = data;
                    resetMarkIdForStateList();

                    syncDotListToServer();
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

    private void checkCompetitionPoint(){
        ApiUtil.queryCompetitionLivePoint(_competitionBean.competition_id, _videoBean.name, new IGenericApiCallback<CompetitionStatListBean>() {
            @Override
            public void onSuccess(CompetitionStatListBean data) {
                DialogUtil.dismissDialog();
                _competitionStateListBean = data;
                if (data.data.isEmpty()) {
                    checkMark(_competitionBean.competition_id, _videoBean.name);
                } else {
                    DialogUtil.dismissDialog();
                    initStateListFromServer(data); //初始化服务器数据
                    isSynced = true;
                }
            }

            @Override
            public void onFail(String code, String message) {
                DialogUtil.dismissDialog();
            }

            @Override
            public void onFinish() {

            }
        });
    }

    /*
     *  保存比赛Id到本地记录
     */
    private void syncCompetitionInfoToLocal(String competitionId) {
        // 得到一级列表
        List<CameraVideoListBean.VideosBean> listData = AppFileUtil.loadAllDevicesJson();
        VideoUtil.listSorting(listData);

        // 更新当前的bean
        for (int i = 0; i < listData.size(); i++) {
            CameraVideoListBean.VideosBean videosBean = listData.get(i);
            if (_competitionBean.deviceId.equalsIgnoreCase(videosBean.deviceId) &&
                    _competitionBean.competition_name.equalsIgnoreCase(videosBean.competition_name) &&
                    _competitionBean.path.equalsIgnoreCase(videosBean.path)
            ) {
                videosBean.competition_id = competitionId;
                break;
            }
        }
        AppFileUtil.saveAllDevicesJson(listData);

        // 更新二级列表
        List<CameraVideoDetailListBean.VideosBean> _videoList = AppFileUtil.loadDeviceVideoJson(_competitionBean);
        for (CameraVideoDetailListBean.VideosBean bean : _videoList) {
            bean.competitionId = competitionId;
        }
        AppFileUtil.saveDeviceVideoJson(_competitionBean, _videoList);
    }

    private void syncVideoInfoToLocal(CameraVideoDetailListBean.VideosBean localVideoBean, String VideoId, String domainId) {
        for (int i = 0; i < _videoList.size(); i++) {
            CameraVideoDetailListBean.VideosBean videosBean = _videoList.get(i);
            if (localVideoBean.deviceId.equalsIgnoreCase(videosBean.deviceId) && localVideoBean.link.equalsIgnoreCase(videosBean.link)) {
                videosBean.videoId = VideoId;
                videosBean.domainId = domainId;
                break;
            }
        }
        AppFileUtil.saveDeviceVideoJson(_competitionBean, _videoList);
    }

    private void syncVideoDotInfoToLocal(CameraVideoDetailListBean.VideosBean localVideoBean, String VideoId, String domainId) {
        for (int i = 0; i < _videoList.size(); i++) {
            CameraVideoDetailListBean.VideosBean videosBean = _videoList.get(i);
            if (localVideoBean.deviceId.equalsIgnoreCase(videosBean.deviceId) && localVideoBean.link.equalsIgnoreCase(videosBean.link)) {
                videosBean.videoId = VideoId;
                videosBean.domainId = domainId;
                break;
            }
        }
        AppFileUtil.saveDeviceVideoJson(_competitionBean, _videoList);
    }

    private void addVideo(CameraVideoDetailListBean.VideosBean videosBean) {
        HmVideo video = new HmVideo();
        video.setDeviceId(_competitionBean.deviceId);
        video.setCompetitionId(_competitionBean.competition_id);
        if (_competitionBean.competition_type == 0) {
            _competitionBean.competition_type = Constants.COMPETITION_TYPE_RECORD;
        }
        video.setCompetitionType(_competitionBean.competition_type);
        video.setVideoDuration(videosBean.duration);
        video.setVideoSize(videosBean.size);
        video.setVideoWidth(videosBean.video_width);
        video.setVideoHeight(videosBean.video_height);
        video.setVideoClassify(videosBean.type);
        video.setVideoStatus(Constants.STATUS_STARTING);
        video.setUploadType(Constants.UPLOAD_MANUAL);   //手动上传是2； 1是自动上传

        // 添加视频文件录制开始时间, 通过获取文件的创建时间来得到
        videosBean.path = VideoUtil.getVideoFilePath(videosBean);
        File f = new File(videosBean.path);
        if (f.exists() && f.isFile()) {
            long ctime = FileUtil.getCreationTime(f);
            if (ctime != -1) {
                video.setVideoTime(TimeUtil.millis2YMDHMS(ctime));
            }
        }

        // 使用文件名作为视频名称, 避免重复上传, 但是要看看这个有没有副作用
        video.setVideoName(StringUtils.lastPathComponent(videosBean.path));

        AppTrace.d(TAG, "upload video info: " + new Gson().toJson(video));

        ApiUtil.addVideo(video, new IGenericApiCallback<HmVideo>() {
            @Override
            public void onSuccess(HmVideo data) {
                if (data != null) {
                    String domainId = "";
                    videosBean.setVideoId(data.getVideoId());
                    if (data.getHmVideoUploadProgress() != null) {
                        domainId = data.getHmVideoUploadProgress().getDomainId();
                    }
                    videosBean.setDomainId(domainId);
                    syncVideoInfoToLocal(videosBean, data.getVideoId(), domainId);

                    _videoBean.videoId = data.getVideoId();

                    syncDotListToServer();
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "添加视频信息失败：" + message);
                DialogUtil.dismissDialog();
            }

            @Override
            public void onFinish() {

            }
        });

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshDataEvent(StateReloadEvent event) {
        ApiUtil.queryCompetitionLivePoint(_competitionBean.competition_id, _videoBean.name, new IGenericApiCallback<CompetitionStatListBean>() {
            @Override
            public void onSuccess(CompetitionStatListBean data) {
                _competitionStateListBean = data;
                if(data.data != null){
                    initStateListFromServer(data); //初始化服务器数据

                    //刷新控件数据
                    viewBinding.hmScoreView.reloadState();
                }
            }

            @Override
            public void onFail(String code, String message) {
                DialogUtil.dismissDialog();
            }

            @Override
            public void onFinish() {

            }
        });
    }


    private void addState(boolean isHost, int type, String name) {

        int recordTime = viewBinding.playerView.getCurrentPosition()/1000;
        if (!TextUtils.isEmpty(_competitionBean.competition_id) ) {
            String teamName = isHost ? _competitionBean.host_team : _competitionBean.guest_team;
            ApiUtil.addCompetitionLivePoint(_competitionBean.competition_id, _videoBean.name, teamName, recordTime, name, "", new IGenericApiCallback<HmCompetitionPoint>() {
                @Override
                public void onSuccess(HmCompetitionPoint data) {
                    //如果是视频打点，通知去服务器重新拉取打点列表，刷新
                    EventBus.getDefault().post(new StateReloadEvent());
                }

                @Override
                public void onFail(String code, String message) {

                }

                @Override
                public void onFinish() {

                }
            });
        } else {
            ToastUtil.s(getString(R.string.info_no_complete_info));
        }
    }



    private void addScore(boolean isHost, int type, String name) {
        int recordTime = viewBinding.playerView.getCurrentPosition()/1000;
        if (!TextUtils.isEmpty(_competitionBean.competition_id) ) {
            String teamName = isHost ? _competitionBean.host_team : _competitionBean.guest_team;
            ApiUtil.addCompetitionLivePoint(_competitionBean.competition_id, _videoBean.name, teamName, recordTime, name, "", new IGenericApiCallback<HmCompetitionPoint>() {
                @Override
                public void onSuccess(HmCompetitionPoint data) {
                    //如果是视频打点，通知去服务器重新拉取打点列表，刷新
                    EventBus.getDefault().post(new StateReloadEvent());
                }

                @Override
                public void onFail(String code, String message) {

                }

                @Override
                public void onFinish() {

                }
            });
        } else {
            ToastUtil.s(getString(R.string.info_no_complete_info));
        }
    }
}
