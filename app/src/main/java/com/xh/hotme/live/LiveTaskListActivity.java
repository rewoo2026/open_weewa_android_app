package com.xh.hotme.live;

import android.content.Intent;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
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
import com.xh.hotme.databinding.ActivityLiveTaskListBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.CompetitionLiveManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.adapter.LiveTaskListAdapter;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.HmLivePlatform;
import com.xh.hotme.live.bean.LiveTaskBean;
import com.xh.hotme.live.bean.LiveTaskListBean;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/3
 * @Description:
 */
@IEventBus
public class LiveTaskListActivity extends BaseActivity<ActivityLiveTaskListBinding> implements IRecordControlListener {
    private LiveTaskListAdapter _adapter;
    private List<CameraRecordInfoBean> _recordName;

    private static final class PlayResult {
        boolean needRefresh3rdLiveUrl = false;
        String errMsg = null;
    }

    private PlayResult _playResult = null;

    private HmCompetitionLive _bean;

    private LiveUrlEvent _urlEvent;
    // 0 idle, 1 service, 2 protocol query, 3 recording, 4 push URL, 5 server start.
    private int _liveStartStage;
    private boolean _workflowPending;
    private CoordinatedLiveStarter _coordinatedStarter;
    private ModalDialog _confirmRefreshWxLiveUrlDialog = null;
    private ActivityResultLauncher<Intent> _thirdPltLauncher = null;


    private String str_timeout_lunch_rkipc;
    private String str_lunch_rkipc_fail;
    private String str_request_timeout;
    private String str_add_fec_url_fail;

    private static boolean isActive = false;


    @Override
    protected void initView() {

        str_timeout_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_live);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);
        str_request_timeout = getString(R.string.request_timeout);
        str_add_fec_url_fail = getString(R.string.info_add_fec_url_fail);


        viewBinding.ivBack.setOnClickListener(view -> finish());

        BluetoothHandle.addPlayNotifyListener(this);
        _recordName = CompetitionLiveManager.getCompetitionList();

        _adapter = new LiveTaskListAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        viewBinding.recyclerView.setLayoutManager(linearLayoutManager);
        viewBinding.recyclerView.setAdapter(_adapter);
        _adapter.setCommonListener((code, objects) -> {

            if(code == IActionCallback.CALLBACK_ACTION_DELETE) {
                ModalDialog dialog = new ModalDialog(LiveTaskListActivity.this);
                dialog.setTitle(getString(R.string.live_task_delete));
                dialog.setMessage(objects[1].toString());
                dialog.setNegativeButton(getString(R.string.cancel), v -> dialog.dismiss());
                dialog.setPositiveButton(getString(R.string.ok), v -> {
                    dialog.dismiss();

                    DialogUtil.showDialog("");
                    ApiUtil.deleteCompetitionLive(objects[0].toString(), new ApiCallback() {
                        @Override
                        public void onSuccess() {
                            getLiveList();
                        }

                        @Override
                        public void onFail(String code, String message) {
                            ToastUtil.s(message);
                            DialogUtil.dismissDialog();
                        }
                    });
                });
                dialog.show();
            } else if(code == IActionCallback.CALLBACK_ACTION_EDIT) {
                //编辑
                HmCompetitionLive bean = (HmCompetitionLive) objects[0];
                Intent intent = new Intent(LiveTaskListActivity.this, AddLiveTaskActivity.class);
                intent.putExtra(AddLiveTaskActivity.EXTRA_EDIT_BEAN, bean);
                startActivity(intent);

            } else if(code == IActionCallback.CALLBACK_ACTION_START) {
                //开启直播
                playWorkflowStep1_getCommentaryPushStream((HmCompetitionLive) objects[0]);
            }
        });

        viewBinding.tvAdd.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                isActive = false;
                Intent intent = new Intent(LiveTaskListActivity.this,
                    AddLiveTaskActivity.class);
                if(liveTaskListBean != null) {
                    intent.putExtra(AddLiveTaskActivity.EXTRA_HISTORY_LIST, liveTaskListBean);
                }
                startActivity(intent);
                return false;
            }
        });
    }

    @Override
    protected void initData() {
        _thirdPltLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode() == RESULT_OK) {
                Intent data = o.getData();
                int livePlatformType = data.getIntExtra(LiveThirdPlatformActivity.EXTRA_PLATFORM_TYPE, Constants.LIVE_PLATFORM_XQM);
                String livePlatformId = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_LIVE_PLATFORM_ID);
                updateCompetitionLivePlatform(livePlatformType, livePlatformId);
            }
        });
    }

    private void updateCompetitionLivePlatform(int livePlatformType, String livePlatformId) {
        DialogUtil.showDialog("");
        HashMap<String, String> paramsdata = new HashMap<>();
        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("competitionId", _bean.competitionId);
        paramsdata.put("livePlatformType", String.valueOf(livePlatformType));
        paramsdata.put("livePlatformId", livePlatformId);
        OkHttpUtil.postData(SdkApi.competitionLive_update(), paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                runOnUiThread(() -> {
                    ToastUtil.s(getString(R.string.info_push_url_update_success));
                    _bean.livePlatformType = String.valueOf(livePlatformType);
                    _bean.livePlatformId = livePlatformId;
                });
            }

            @Override
            public void onFailure(String code, String message) {
                runOnUiThread(() -> {
                    ToastUtil.s(message);
                });
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        isActive = true;
        DialogUtil.showDialog("");
        getLiveList();
    }

    @Override
    public void onPause() {
        super.onPause();
        isActive = false;
    }

    LiveTaskListBean liveTaskListBean;

    public static boolean isActiveFunc() {
        return isActive;
    }

    private void getLiveList() {
        HashMap<String, String> params = new HashMap<>();
        params.put("deviceId", CameraManager.getDeviceId());
        params.put("competitionBeginTime", TimeUtil.getTodayString());
        params.put("competitionEndTime", TimeUtil.getTodayEndString());

        OkHttpUtil.postData(SdkApi.competitionLive_query_all(), params, null,
            new OkHttpCallbackDecode<LiveTaskListBean>(LiveTaskListBean.class) {
                @Override
                public void onDataSuccess(LiveTaskListBean data) {
                    runOnUiThread(() -> {
                        liveTaskListBean = data;
                        _adapter.setList(data.data);
                    });
                }

                @Override
                public void onFinish() {
                    DialogUtil.dismissDialog();
                }
            });
    }

    private void playWorkflowStep1_getCommentaryPushStream(HmCompetitionLive bean) {
        if(_workflowPending) return;
        _workflowPending = true;
        DialogUtil.showDialog("");

        _bean = bean;
        _urlEvent = new LiveUrlEvent();
        _urlEvent.third_party = !TextUtils.isEmpty(bean.livePlatformId);
        try{
            _urlEvent.third_plt = Integer.parseInt(bean.livePlatformType);
        }catch (Throwable e){
            e.printStackTrace();
        }
        _playResult = new PlayResult();

        if(_urlEvent.third_party) {
            playWorkflowStep2_get3rdLiveUrl();
        } else if(_bean.commentate == 1) {
            _urlEvent.commentary = true;
            ApiUtil.getPushStreamWithCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _urlEvent.url_push_commentary = data.data;
                    playWorkflowStep4_getCommentaryPullStream();
                }

                @Override
                public void onFail(String code, String message) {
                    _playResult.errMsg = message;
                    playWorkflowStep8_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        } else {
            playWorkflowStep5_getPullStream();
        }
    }

    private void playWorkflowStep2_get3rdLiveUrl() {
        ApiUtil.getLivePlatform(_bean.livePlatformId, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _urlEvent.url_push = data.pushStreamAddress;
                _urlEvent.url_pull = data.pullStreamAddress;
                _urlEvent.url_pull_commentary = data.pullStreamAddress;
                playWorkflowStep3_validate3rdUrl();
            }

            @Override
            public void onFail(String code, String message) {
                int livePlatformType = Constants.LIVE_PLATFORM_XQM;
                try {
                    livePlatformType = Integer.parseInt(_bean.livePlatformType);
                }catch (Throwable e){
                    e.printStackTrace();
                }
                switch(livePlatformType) {
                    case Constants.LIVE_PLATFORM_XQM:
                        _playResult.errMsg = message;
                        break;
                    default:
                        _playResult.needRefresh3rdLiveUrl = true;
                        break;
                }
                playWorkflowStep8_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void playWorkflowStep3_validate3rdUrl() {
        int livePlatformType = Constants.LIVE_PLATFORM_XQM;
        try {
            livePlatformType = Integer.parseInt(_bean.livePlatformType);
        }catch (Throwable e){
            e.printStackTrace();
        }
        switch(livePlatformType) {
            case Constants.LIVE_PLATFORM_WX_CHANNEL:
                if(TextUtils.isEmpty(_urlEvent.url_push)) {
                    _playResult.needRefresh3rdLiveUrl = true;
                    playWorkflowStep8_wrapUp();
                } else {
                    ApiUtil.checkWxPushUrl(_urlEvent.url_push, new IGenericApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            if (data) {
                                playWorkflowStep7_startRkipc();
                            } else {
                                _playResult.needRefresh3rdLiveUrl = true;
                                playWorkflowStep8_wrapUp();
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {
                            _playResult.errMsg = message;
                            _playResult.needRefresh3rdLiveUrl = true;
                            playWorkflowStep8_wrapUp();
                        }

                        @Override
                        public void onFinish() {
                        }
                    });
                }
                break;
            case Constants.LIVE_PLATFORM_DY_CHANNEL:
                if(TextUtils.isEmpty(_urlEvent.url_push)) {
                    _playResult.needRefresh3rdLiveUrl = true;
                    playWorkflowStep8_wrapUp();
                } else {
                    ApiUtil.checkDyPushUrl(_urlEvent.url_push, new IGenericApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            if (data) {
                                playWorkflowStep7_startRkipc();
                            } else {
                                _playResult.needRefresh3rdLiveUrl = true;
                                playWorkflowStep8_wrapUp();
                            }
                        }

                        @Override
                        public void onFail(String code, String message) {
                            _playResult.errMsg = message;
                            _playResult.needRefresh3rdLiveUrl = true;
                            playWorkflowStep8_wrapUp();
                        }

                        @Override
                        public void onFinish() {
                        }
                    });
                }
                break;
            default:
                playWorkflowStep7_startRkipc();
                break;
        }
    }

    private void playWorkflowStep4_getCommentaryPullStream() {
        if(_bean.commentate == 1) {
            _urlEvent.commentary = true;
//            if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
//                ApiUtil.getPullStreamUrlWithCommentaryPassword(_bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
//                    @Override
//                    public void onSuccess(PullStreamAddrBean data) {
//                        _urlEvent.url_pull_commentary = data.data;
//                        playWorkflowStep5_getPullStream();
//                    }
//
//                    @Override
//                    public void onFail(String code, String message) {
//                        _playResult.errMsg = message;
//                        playWorkflowStep8_wrapUp();
//                    }
//
//                    @Override
//                    public void onFinish() {
//                    }
//                });
//            } else {
                ApiUtil.getPullStreamUrlWithCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                    @Override
                    public void onSuccess(PullStreamAddrBean data) {
                        _urlEvent.url_pull_commentary = data.data;
                        playWorkflowStep5_getPullStream();
                    }

                    @Override
                    public void onFail(String code, String message) {
                        _playResult.errMsg = message;
                        playWorkflowStep8_wrapUp();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
//            }
        } else {
            playWorkflowStep5_getPullStream();
        }
    }

    private void playWorkflowStep5_getPullStream() {
        if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
            ApiUtil.getPullStreamUrlWithCommentaryPassword(_bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _urlEvent.url_pull = data.data;
                    playWorkflowStep6_getPushStream();
                }

                @Override
                public void onFail(String code, String message) {
                    _playResult.errMsg = message;
                    playWorkflowStep8_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        } else {
            ApiUtil.getPullStreamUrlWithoutCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    // 这两个其实是一个
                    _urlEvent.url_pull = data.data;
                    playWorkflowStep6_getPushStream();
                }

                @Override
                public void onFail(String code, String message) {
                    _playResult.errMsg = message;
                    playWorkflowStep8_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        }
    }

    private void playWorkflowStep6_getPushStream() {
        ApiUtil.getPushStreamWithoutCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {
                _urlEvent.url_push = data.data;
                playWorkflowStep7_startRkipc();
            }

            @Override
            public void onFail(String code, String message) {
                _playResult.errMsg = message;
            }

            @Override
            public void onFinish() {
                playWorkflowStep8_wrapUp();
            }
        });
    }

    private void playWorkflowStep7_startRkipc() {
        // start rkipc
        _liveStartStage = 1;
        boolean ok = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
        if(ok) {
            _handler.postDelayed(_startRkipcTimeOutRunnable,
                BluetoothManager.RESP_TIMEOUT_SHORT * 5);
            showConnectingDialog(getString(R.string.loading_live_ready));
        } else {
            String error = getString(R.string.info_bluetooth_offline);
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                error = getString(R.string.info_network_disconnect);
            }
            _liveStartStage = 0;
            _playResult.errMsg = error;
        }

        // wrap up
        playWorkflowStep8_wrapUp();
    }

    private void playWorkflowStep8_wrapUp() {
        if(_liveStartStage == 0) _workflowPending = false;
        DialogUtil.dismissDialog();
        if(_playResult.needRefresh3rdLiveUrl) {
            showConfirmRefresh3rdLiveUrlDialog();
        } else if(!TextUtils.isEmpty(_playResult.errMsg)) {
            ToastUtil.s(_playResult.errMsg);
        }
    }

    private final Runnable _startRkipcTimeOutRunnable = () ->
        failLiveStart(getString(R.string.info_running_timeout_on_lunch_live));

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        if(!isActive()) return;
        runOnUiThread(() -> {
            if(event != null && _urlEvent != null && _liveStartStage == 1) {
                _handler.removeCallbacks(_startRkipcTimeOutRunnable);
                AppTrace.d(TAG, "onRkipcStart");
                if(event.status == 1) {
                    _liveStartStage = 2;
                    if(DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean())) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    } else {
                        failLiveStart(getString(R.string.info_network_disconnect));
                    }

                } else {
                    failLiveStart(str_lunch_rkipc_fail);
                }
            }
        });

    }

    // 这里是不是应该一同退出
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFecRtmpAddUrl(FecRtmpAddUrlEvent event) {
//        if(!isActive()) return;
        AppTrace.d(TAG, "FecRtmpAddUrl");
        if(_liveStartStage != 4) return;
        if(event != null) {
            if(event.status == 1) {
                _liveStartStage = 0;
                //上报比赛开始

                    if (isActive()) {
                        if(_bean != null) {
                            ApiUtil.comprehensiveReportCompetitionLiveStatus(_bean.competitionId, _urlEvent.url_push, _urlEvent.third_plt, Constants.COMPETITION_START);
                        }
                    }
//                    startRecord();
                    _handler.removeCallbacks(_startRkipcTimeOutRunnable);
                    dismissConnectingDialog();

                    _handler.removeCallbacks(_timeOutRunnable);

                    finish();

            } else {
                failLiveStart(str_add_fec_url_fail);
            }
        }

    }



    @Override
    public void onRecordVideoStart() {
        runOnUiThread(() -> {
            if(!isActive()) return;
            if(_liveStartStage != 3 || _urlEvent == null) return;
            AppTrace.d(TAG, "onRecordVideoStart");
            _liveStartStage = 4;
            _handler.removeCallbacks(_timeOutRunnable);
            if(DeviceApiManager.sendCmdSimpleObj(new FecRtmpAddUrlRequestBean(_urlEvent.url_push))) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            } else {
                failLiveStart(getString(R.string.info_network_disconnect));
            }
        });
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        runOnUiThread(() -> {
            if(recordStatus == null || _liveStartStage != 2) return;
            _handler.removeCallbacks(_timeOutRunnable);
            if(recordStatus.recordingProtocolVersion >= 3) {
                startCoordinatedLive();
            } else {
                _liveStartStage = 3;
                EventBus.getDefault().post(_urlEvent);
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
            if(_liveStartStage != 3 && _liveStartStage != 4) return;
            if("device_version required".equals(msg)) startCoordinatedLive();
            else failLiveStart(msg);
        });
    }

    private void startCoordinatedLive() {
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        _handler.removeCallbacks(_timeOutRunnable);
        _liveStartStage = 5;
        _coordinatedStarter = CoordinatedLiveStarter.create(_handler, new CoordinatedLiveStarter.Listener() {
            public void onStarted() {
                _liveStartStage = 0;
                EventBus.getDefault().post(_urlEvent);
                dismissConnectingDialog();
                finish();
            }
            public void onFailure(String message) { failLiveStart(message); }
        });
        _coordinatedStarter.start(_bean.competitionId);
    }

    private void failLiveStart(String message) {
        _liveStartStage = 0;
        _workflowPending = false;
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        _handler.removeCallbacks(_timeOutRunnable);
        dismissConnectingDialog();
        ToastUtil.s(TextUtils.isEmpty(message) ? getString(R.string.info_running_timeout_on_lunch_live) : message);
    }


    private void startRecord() {
        String[] placeTypeList = getResources().getStringArray(R.array.place_type_list);
        String[] playNumberList = getResources().getStringArray(R.array.play_number_list);

        RecordInfoBean recordInfoBean = new RecordInfoBean("", Constants.FEC_VIDEO_MAX_RATE);
        recordInfoBean.author = "";
        recordInfoBean.width = Constants.FEC_VIDEO_WIDTH;
        recordInfoBean.height = Constants.FEC_VIDEO_HEIGHT;
        recordInfoBean.place = _bean.place;
        recordInfoBean.name = _bean.competitionName;
        recordInfoBean.motionType = Integer.parseInt(_bean.motionType);//运动类型
        recordInfoBean.placeType = Integer.parseInt(_bean.placeType);//场地类型
        recordInfoBean.city = _bean.cityCodeDisplay;
        recordInfoBean.provinceId = _bean.provinceCode;
        recordInfoBean.cityId = _bean.cityCode;
        recordInfoBean.hostTeam = _bean.homeTeamName;
        recordInfoBean.guestTeam = _bean.awayTeamName;
        recordInfoBean.time = System.currentTimeMillis();
        recordInfoBean.live_cid = _bean.competitionId;
        recordInfoBean.ctype = Constants.COMPETITION_TYPE_LIVE;
        recordInfoBean.maxRate = VideoUtil.getFecMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));
        recordInfoBean.avs_mbr = VideoUtil.getAvsMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));
        recordInfoBean.live_mbr = VideoUtil.getLiveMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));

        // 不是足球的话, 人数默认弄个5人, 不然0人会被当成训练模式
        if(recordInfoBean.motionType != Constants.SPORT_FOOTBALL) {
            recordInfoBean.player_num = 5;
        } else {
            try {
                recordInfoBean.player_num = Integer.parseInt(playNumberList[Integer.parseInt(_bean.placeType)]);  //参赛人数
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }

        String bluetoothId = CameraManager.getDeviceBluetoothId();

        boolean isAppend = CompetitionLiveManager.isAppend(_bean.competitionName, _recordName, bluetoothId);
        recordInfoBean.append = isAppend;

        EventBus.getDefault().post(new RecordStartEvent(recordInfoBean));
        CameraRecordInfoBean cameraRecordInfoBean = CameraRecordInfoBean.newInstance(recordInfoBean);
        cameraRecordInfoBean.deviceId = CameraManager.getDeviceBluetoothId();


        recordInfoBean.hide_osd = SharedPrefs.getImpl().getBoolean(SharedPrefs.PREF_HIDE_OSD) ? 1: 0;

        RecordStartRequestBean recordStartRequestBean = recordInfoBean.convertRecordStartRequestBean();
        boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);
        if(ok) {
            _handler.postDelayed(_timeOutRunnable, 5000);
//            showConnectingDialog("相机正在开启直播");
        } else {
            failLiveStart(getString(R.string.info_network_disconnect));
        }


    }

    private final Runnable _timeOutRunnable = () -> failLiveStart(getString(R.string.info_running_timeout_on_lunch_live));

    BluetoothLoadingDialog _connectingDialog;

    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(LiveTaskListActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    private void showConfirmRefresh3rdLiveUrlDialog() {
        if(_confirmRefreshWxLiveUrlDialog != null && _confirmRefreshWxLiveUrlDialog.isShowing()) {
            return;
        }
        _confirmRefreshWxLiveUrlDialog = new ModalDialog(this);
        _confirmRefreshWxLiveUrlDialog.setPositiveButton(getString(R.string.update), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                Intent intent = new Intent(LiveTaskListActivity.this, LiveThirdPlatformActivity.class);
                intent.putExtra(LiveThirdPlatformActivity.EXTRA_PRESELECT_PLATFORM_TYPE, Constants.LIVE_PLATFORM_WX_CHANNEL);
                intent.putExtra(LiveThirdPlatformActivity.EXTRA_COMPETITION_NAME, _bean.competitionName);
                _thirdPltLauncher.launch(intent);
                return true;
            }
        });
        int livePlatformType = Constants.LIVE_PLATFORM_XQM;
        try {
            livePlatformType = Integer.parseInt(_bean.livePlatformType);
        } catch (Throwable e){
            e.printStackTrace();
        }
        switch(livePlatformType) {
            case Constants.LIVE_PLATFORM_WX_CHANNEL:
                _confirmRefreshWxLiveUrlDialog.setMessage(getString(R.string.wx_live_url_expired_need_update));
                break;
            case Constants.LIVE_PLATFORM_DY_CHANNEL:
                _confirmRefreshWxLiveUrlDialog.setMessage(getString(R.string.dy_live_url_expired_need_update));
                break;
        }
        _confirmRefreshWxLiveUrlDialog.setCancelable(true);
        _confirmRefreshWxLiveUrlDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _confirmRefreshWxLiveUrlDialog.dismiss();
                return true;
            }
        });

        _confirmRefreshWxLiveUrlDialog.show();
    }

    @Override
    protected void onDestroy() {
        if(_coordinatedStarter != null) _coordinatedStarter.cancel();
        dismissConnectingDialog();
        super.onDestroy();

        if(_confirmRefreshWxLiveUrlDialog != null && _confirmRefreshWxLiveUrlDialog.isShowing()) {
            _confirmRefreshWxLiveUrlDialog.dismiss();
        }
        _confirmRefreshWxLiveUrlDialog = null;

        BluetoothHandle.removePlayNotifyListener(this);
    }
}
