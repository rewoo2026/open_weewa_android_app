package com.xh.hotme.record;

import android.content.Intent;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;

import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityLiveTaskListBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.CompetitionLiveManager;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/3
 * @Description:
 */
@IEventBus
public class RecordTaskListActivity extends BaseActivity<ActivityLiveTaskListBinding> implements IRecordControlListener {
    private RecordTaskListAdapter _adapter;
    private List<CameraRecordInfoBean> _recordName;


    private HmCompetitionRecordBean _pendingBean;

    List<String> _playNumberList;


    private static final class PlayResult {
        boolean needRefresh3rdLiveUrl = false;
        String errMsg = null;
    }

    private PlayResult _playResult = null;

    private ModalDialog _confirmRefreshWxLiveUrlDialog = null;
    private ActivityResultLauncher<Intent> _thirdPltLauncher = null;

    private String str_timeout_lunch_rkipc;
    private String str_lunch_rkipc_fail;

    @Override
    protected void initView() {

        str_timeout_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_live);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);

        String[] playNumberList = getResources().getStringArray(R.array.play_number_list);
        _playNumberList = Arrays.asList(playNumberList);


        viewBinding.ivBack.setOnClickListener(view -> finish());
        viewBinding.tvTitle.setText(getString(R.string.record_task));
        viewBinding.tvAdd.setText(getString(R.string.add_record_task));

        BluetoothHandle.addPlayNotifyListener(this);
        _recordName = CompetitionLiveManager.getCompetitionList();

        _adapter = new RecordTaskListAdapter(RecordTaskListActivity.this, _taskListBean);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        viewBinding.recyclerView.setLayoutManager(linearLayoutManager);
        viewBinding.recyclerView.setAdapter(_adapter);
        _adapter.setCommonListener((code, objects) -> {

            if(code == IActionCallback.CALLBACK_ACTION_DELETE) {
                if(!NetUtil.isNetWorkConneted(RecordTaskListActivity.this)){
                    showNetworkDialog();
                    return;
                }
                ModalDialog dialog = new ModalDialog(RecordTaskListActivity.this);
                dialog.setTitle("删除录制任务");
                dialog.setMessage(objects[1].toString());
                dialog.setNegativeButton("取消", v -> dialog.dismiss());
                dialog.setPositiveButton("确定", v -> {
                    dialog.dismiss();

                    DialogUtil.showDialog("");
                    ApiUtil.deleteCompetition(objects[0].toString(), new ApiCallback() {
                        @Override
                        public void onSuccess() {
                            getRecordList();
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
                HmCompetitionRecordBean bean = (HmCompetitionRecordBean) objects[0];
                Intent intent = new Intent(RecordTaskListActivity.this, AddRecordTaskActivity.class);
                intent.putExtra(AddRecordTaskActivity.EXTRA_EDIT_BEAN, bean);
                startActivity(intent);

            } else if(code == IActionCallback.CALLBACK_ACTION_START) {
                //开启录制
                HmCompetitionRecordBean bean = (HmCompetitionRecordBean) objects[0];
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    doRecord(bean);
                }else{
                    _pendingBean = bean;
                    addWorkflowStep1_validate();
                }
            }
        });

        viewBinding.tvAdd.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                Intent intent = new Intent(RecordTaskListActivity.this,
                    AddRecordTaskActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onResume() {
        super.onResume();

//        DialogUtil.showDialog("");
        loadRecordList();

        _adapter.notifyDataSetChanged();
    }

    List<HmCompetitionRecordBean> _taskListBean = new ArrayList<>();

    private void loadRecordList() {
        String content = AppFileUtil.loadStringFromFile(CameraManager.getDeviceId() + "_record_task_" + TimeUtil.getTodayString());
        List<HmCompetitionRecordBean> dataList = GsonUtils.fromJsonList(content, HmCompetitionRecordBean.class);
        if(!dataList.isEmpty()){
            _taskListBean.clear();

            //只显示当天的录制任务
            for (HmCompetitionRecordBean bean : dataList) {
                if(TimeUtil.isToday(bean.competitionTime)) {
                    _taskListBean.add(bean);
                }
            }
        }

    }

    private void getRecordList() {
         ApiUtil.queryAllRecordTask(CameraManager.getDeviceId(), new IGenericApiCallback<List<HmCompetitionRecordBean>>() {
            @Override
            public void onSuccess(List<HmCompetitionRecordBean> data) {
//                MainHandler.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        loadRecordList();
//                    }
//                });
            }

            @Override
            public void onFail(String code, String message) {

            }

            @Override
            public void onFinish() {

            }
        });
    }


    private final Runnable _startRkipcTimeOutRunnable = () -> {
        dismissConnectingDialog();
        ToastUtil.s(str_timeout_lunch_rkipc);
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        if(!isActive()) return;
        runOnUiThread(() -> {
            if(event != null ) {
                AppTrace.d(TAG, "onRkipcStart  " );
            }
        });
    }

    @Override
    public void onRecordVideoStart() {

        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        dismissConnectingDialog();

        _handler.removeCallbacks(_timeOutRunnable);

        finish();
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        if(recordStatus != null) {

        }
    }

    @Override
    public void onRecordVideoStop() {

    }

    @Override
    public void onRecordException(String msg) {

    }

    private final Runnable _timeOutRunnable = () -> dismissConnectingDialog();

    BluetoothLoadingDialog _connectingDialog;

    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(RecordTaskListActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(_confirmRefreshWxLiveUrlDialog != null && _confirmRefreshWxLiveUrlDialog.isShowing()) {
            _confirmRefreshWxLiveUrlDialog.dismiss();
        }
        _confirmRefreshWxLiveUrlDialog = null;

        BluetoothHandle.removePlayNotifyListener(this);
    }

    private void doRecord(HmCompetitionRecordBean competitionViewBean){

        int _motionTypeIndex = Integer.parseInt(competitionViewBean.motionType);
        int _placeTypeIndex = Integer.parseInt(competitionViewBean.placeType);

        RecordInfoBean _recordInfoBean = new RecordInfoBean("", Constants.FEC_VIDEO_MAX_RATE);
        _recordInfoBean.author = "";
        _recordInfoBean.width = Constants.FEC_VIDEO_WIDTH;
        _recordInfoBean.height = Constants.FEC_VIDEO_HEIGHT;
        _recordInfoBean.place = competitionViewBean.place;
        _recordInfoBean.name = competitionViewBean.competitionName;
        _recordInfoBean.campaignName = SharedPrefs.getImpl().getDefaultMatchName();
        _recordInfoBean.motionType = Integer.parseInt(competitionViewBean.motionType);
        _recordInfoBean.placeType =  Integer.parseInt( competitionViewBean.placeType);
        _recordInfoBean.city = competitionViewBean.provinceCodeDisplay + competitionViewBean.cityCodeDisplay;  //城市
        _recordInfoBean.provinceId = competitionViewBean.provinceCode;  //城市
        _recordInfoBean.cityId = competitionViewBean.cityCode;  //城市
        _recordInfoBean.hostTeam = competitionViewBean.homeTeamName;
        _recordInfoBean.guestTeam =competitionViewBean.awayTeamName;
        _recordInfoBean.time = System.currentTimeMillis();

        _recordInfoBean.maxRate = VideoUtil.getFecMBR(_motionTypeIndex, _placeTypeIndex);
        _recordInfoBean.avs_mbr = VideoUtil.getAvsMBR(_motionTypeIndex, _placeTypeIndex);
        _recordInfoBean.live_mbr = VideoUtil.getLiveMBR(_motionTypeIndex, _placeTypeIndex);

        _recordInfoBean.live_cid = competitionViewBean.competitionId;
        _recordInfoBean.ctype = Constants.COMPETITION_TYPE_RECORD;

        _recordInfoBean.hide_osd = SharedPrefs.getImpl().getBoolean(SharedPrefs.PREF_HIDE_OSD) ? 1: 0;

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

        boolean isAppend = CompetitionManager.isAppend(competitionViewBean.competitionName, _recordName, bluetoothId);

        _recordInfoBean.append = isAppend;

        EventBus.getDefault().post(new RecordStartEvent(_recordInfoBean));

        CameraRecordInfoBean cameraRecordInfoBean = CameraRecordInfoBean.newInstance(_recordInfoBean);
        cameraRecordInfoBean.deviceId = CameraManager.getDeviceBluetoothId();

        CompetitionManager.addCompetition(RecordTaskListActivity.this, cameraRecordInfoBean);

        RecordStartRequestBean recordStartRequestBean = _recordInfoBean.convertRecordStartRequestBean();
        boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);
        if (ok) {
            // 训练模式时player num为0, 训练模式需要重启rkipc, 超时时间设置的长一点
            _recordInfoBean.time = System.currentTimeMillis();
            _handler.postDelayed(_timeOutRunnable, 15000);
            showConnectingDialog(RecordTaskListActivity.this.getString(R.string.record_launching));
        } else {
            showDisconnectToast();
        }
    }

    ModalDialog _networkDialog;

    private void showNetworkDialog(){
        _networkDialog = new ModalDialog(RecordTaskListActivity.this);
        _networkDialog.setTitle(getString(R.string.info_operation_fail));
        _networkDialog.setMessage(getString(R.string.info_network_outside_message));
        _networkDialog.setNegativeButton(getString(R.string.cancel), v -> _networkDialog.dismiss());
        _networkDialog.setPositiveButton(getString(R.string.ok), v ->  _networkDialog.dismiss());

        _networkDialog.show();
    }

    private static final class AddResult {
        boolean needFinish = false;
        boolean duplicateCompetitionName = false;
        boolean needRefresh3rdLiveUrl = false;
        String errMsg = null;
    }

    private LiveUrlEvent _urlEvent;
    private AddResult _addResult = null;


    private void addWorkflowStep1_validate() {
        DialogUtil.showDialog("");
        _addResult = new AddResult();
        do {
            String gameName = _pendingBean.competitionName;
            if (TextUtils.isEmpty(gameName)) {
                _addResult.errMsg = getString(R.string.hint_record_info_team_name);
                ToastUtil.s(getString(R.string.hint_record_info_team_name));
                return;
            }

        } while(false);

        addWorkflowStep1_getPullStreamUrl();
    }

    private void addWorkflowStep1_getPullStreamUrl(){
        ApiUtil.getPullStreamUrlWithDeviceId(CameraManager.getDeviceId(), new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {

                runOnUiThread(() -> {
                    _urlEvent = new LiveUrlEvent();
                    _urlEvent.url_pull = data.data;

                    addWorkflowStep2_startRkipc();
                });
            }

            @Override
            public void onFail(String code, String message) {
                runOnUiThread(() -> {
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

        // finish activity if needed
        if(_addResult.needFinish) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRKIpcStart(RkIpcEvent event) {
        AppTrace.d(TAG, "onRKIpcStart");
        runOnUiThread(() -> {
            if(event != null && _urlEvent != null) {
                AppTrace.d(TAG, "onRkipcStart  " + _urlEvent.url_pull);
                if(event.status == 1) {
                    EventBus.getDefault().post(_urlEvent);
                    doRecord(_pendingBean);
                } else {
                    dismissConnectingDialog();
                    ToastUtil.s(str_lunch_rkipc_fail);
                }
            }
        });
    }
}
