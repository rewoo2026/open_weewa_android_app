package com.xh.hotme.live;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.view.TimePickerView;
import com.google.gson.Gson;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
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
import com.xh.hotme.databinding.ActivityLiveAddTaskBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.CompetitionLiveManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.HmLivePlatform;
import com.xh.hotme.live.bean.LiveTaskBean;
import com.xh.hotme.live.bean.LiveTaskListBean;
import com.xh.hotme.live.bean.LiveUrlEvent;
import com.xh.hotme.live.bean.PullStreamAddrBean;
import com.xh.hotme.live.dialog.SelectCityDialog;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
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
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@IEventBus
public class AddLiveTaskActivity extends BaseActivity<ActivityLiveAddTaskBinding> implements IRecordControlListener {
    public static final String EXTRA_EDIT_BEAN = "edit_bean";
    public static final String EXTRA_HISTORY_LIST = "history_list";

    //运行类型
    List<String> _motionTypeList;
    //场地类型
    List<String> _placeTypeList;
    //选中运动类型
    private int _motionTypeIndex = 1;
    //当前选中的场地类型
    private int _placeTypeIndex = 2;

    private SelectCityDialog selectCityDialog;

    //城市
    private String provinceCode;
    private String provinceCodeDisplay;
    private String cityCode;
    private String cityCodeDisplay;

    private String competitionTime;
    private String competitionShowTime;

    // 要编辑的任务bean
    HmCompetitionLive _oriBean;

    //比赛是否开始
    String competitionStatus = "" + Constants.COMPETITION_UNSET;

    private String[] _playNumberList = null;

    private String _url_pull_without_commentary = null;
    private String _url_pull_commentary = null;
    private String _url_push_commentary = null;
    private ActivityResultLauncher<Intent> _thirdPltLauncher = null;

    private ModalDialog _confirmDisselect3rdPltDialog = null;
    private ModalDialog _confirmRefreshWxLiveUrlDialog = null;

    List<CameraRecordInfoBean> _recordName;
    List<String> _historyNameList;

    // 当前的bean(编辑后的, 或者新加的, 都有可能)
    HmCompetitionLive _bean;

    private LiveUrlEvent _urlEvent;
    // 0 idle, 1 service, 2 protocol query, 3 recording, 4 push URL, 5 server start.
    private int _liveStartStage;
    private boolean _workflowPending;
    private boolean _unattended;
    private CoordinatedLiveStarter _coordinatedStarter;
    private boolean _pushTo3rd = false;
    private String _3rdPushUrl = null;
    private String _3rdPullUrl = null;
    private String _3rdPushKey = "";
    private int _3rdPlt = Constants.LIVE_PLATFORM_XQM;
    private String _3rdPltId = "";
    private String _coverUrl = "";

    private static final class AddResult {
        boolean needFinish = false;
        boolean duplicateCompetitionName = false;
        boolean needRefresh3rdLiveUrl = false;
        String errMsg = null;
    }

    private AddResult _addResult = null;

    private String str_timeout_lunch_rkipc;
    private String str_lunch_rkipc_fail;
    private String str_add_fec_url_fail;
    @Override
    protected void initView() {

        str_timeout_lunch_rkipc = getString(R.string.info_running_timeout_on_lunch_live);
        str_lunch_rkipc_fail = getString(R.string.info_lunch_rkipc_fail);
        str_add_fec_url_fail = getString(R.string.info_add_fec_url_fail);

        BluetoothHandle.addPlayNotifyListener(this);
        _recordName = CompetitionLiveManager.getCompetitionList();

        viewBinding.titlebar.ivBack.setOnClickListener(view -> finish());
        viewBinding.titlebar.tvTitle.setText(getString(R.string.add_live_task_title));
        viewBinding.titlebar.right.setVisibility(View.GONE);
        viewBinding.groupCopyUrl.setVisibility(View.GONE);
        viewBinding.unattendedMode.unattendedHint.setText(R.string.unattended_live_hint);


        CameraInfo _cameraInfo = CameraManager.getCameraInfo(null);
        if(_cameraInfo != null) {
            viewBinding.pltRewoo.setSelected(
                SharedPrefs.getImpl().getBoolean(
                    "push_rewoo_" + _cameraInfo.deviceInfo.device_id, true
                )
            );
            viewBinding.pltWxDy.setSelected(
                SharedPrefs.getImpl().getBoolean(
                    "push_other_" + _cameraInfo.deviceInfo.device_id, false
                )
            );
            viewBinding.pltMgc.setSelected(
                SharedPrefs.getImpl().getBoolean(
                    "push_mgc_" + _cameraInfo.deviceInfo.device_id, false
                )
            );
        }

        //推流平台选择
        viewBinding.pltRewoo.setOnClickListener(view -> {
            if(viewBinding.plt3rd.isSelected() && !viewBinding.pltRewoo.isSelected()) {
                showConfirmDisselect3rdPltDialog(viewBinding.pltRewoo);
            } else {
                viewBinding.pltRewoo.setSelected(!viewBinding.pltRewoo.isSelected());
            }
        });

        viewBinding.pltWxDy.setOnClickListener(view -> {
            if(viewBinding.plt3rd.isSelected() && !viewBinding.pltWxDy.isSelected()) {
                showConfirmDisselect3rdPltDialog(viewBinding.pltWxDy);
            } else {
                viewBinding.pltWxDy.setSelected(!viewBinding.pltWxDy.isSelected());
            }
        });

        viewBinding.pltMgc.setOnClickListener(view -> {
            if(viewBinding.plt3rd.isSelected() && !viewBinding.pltMgc.isSelected()) {
                showConfirmDisselect3rdPltDialog(viewBinding.pltMgc);
            } else {
                viewBinding.pltMgc.setSelected(!viewBinding.pltMgc.isSelected());
            }
        });

        _thirdPltLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode() == RESULT_OK) {
                Intent data = o.getData();
                _3rdPlt = data.getIntExtra(LiveThirdPlatformActivity.EXTRA_PLATFORM_TYPE, Constants.LIVE_PLATFORM_XQM);
                _3rdPushUrl = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_PUSH_URL);
                _3rdPushKey = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_PUSH_KEY);
                _3rdPullUrl = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_PULL_URL);
                _3rdPltId = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_LIVE_PLATFORM_ID);
                _coverUrl = data.getStringExtra(LiveThirdPlatformActivity.EXTRA_COVER_URL);
                if(_3rdPushKey == null) {
                    _3rdPushKey = "";
                }
                _pushTo3rd = true;
                viewBinding.pltMgc.setSelected(false);
                viewBinding.pltWxDy.setSelected(false);
                viewBinding.pltRewoo.setSelected(false);
                viewBinding.plt3rd.setSelected(true);
                toggleCommentaryGroupVisibility();
            }
        });
        viewBinding.plt3rd.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                // 因为微信视频号获取推流地址的时候需要传一个描述(这里用比赛名称), 所以需要用户先填比赛名称
                if(TextUtils.isEmpty(viewBinding.etCompetitionName.getText().toString())) {
                    ToastUtil.s(getString(R.string.hint_record_info_team_name));
                    return true;
                }

                // start LiveThirdPlatformActivity
                Intent intent = new Intent(AddLiveTaskActivity.this, LiveThirdPlatformActivity.class);
                intent.putExtra(LiveThirdPlatformActivity.EXTRA_COMPETITION_NAME, viewBinding.etCompetitionName.getText().toString());
                _thirdPltLauncher.launch(intent);
                return true;
            }
        });
        viewBinding.tvOk.setOnClickListener(view -> {
            addWorkflowStep1_validate();
        });
        viewBinding.tvCopyPullStreamWithoutCommentary.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(TextUtils.isEmpty(_url_pull_commentary)) {
                    DialogUtil.showDialog("");
                    ApiUtil.getPullStreamUrlWithCommentary(_oriBean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                        @Override
                        public void onSuccess(PullStreamAddrBean data) {
                            _url_pull_commentary = data.data;
                            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_commentary));
                            ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                        }

                        @Override
                        public void onFail(String code, String message) {
                            ToastUtil.s(message);
                        }

                        @Override
                        public void onFinish() {
                            DialogUtil.dismissDialog();
                        }
                    });
                } else {
                    ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_commentary));
                    ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                }
                return true;
            }
        });
        viewBinding.tvCopyPushStreamWithCommentary.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(TextUtils.isEmpty(_url_push_commentary)) {
                    DialogUtil.showDialog("");
                    ApiUtil.getPushStreamWithCommentary(_oriBean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                        @Override
                        public void onSuccess(PullStreamAddrBean data) {
                            _url_push_commentary = data.data;
                            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clip.setPrimaryClip(ClipData.newPlainText("url", _url_push_commentary));
                            ToastUtil.s(getString(R.string.live_copied_push_stream_url));
                        }

                        @Override
                        public void onFail(String code, String message) {
                            ToastUtil.s(message);
                        }

                        @Override
                        public void onFinish() {
                            DialogUtil.dismissDialog();
                        }
                    });
                } else {
                    ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clip.setPrimaryClip(ClipData.newPlainText("url", _url_push_commentary));
                    ToastUtil.s(getString(R.string.live_copied_push_stream_url));
                }
                return true;
            }
        });
        viewBinding.btnCopyPullStreamWithCommentary.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_pushTo3rd) {
                    if(TextUtils.isEmpty(_url_pull_without_commentary)) {
                        DialogUtil.showDialog("");
                        ApiUtil.getLivePlatform(_3rdPltId, new IGenericApiCallback<HmLivePlatform>() {
                            @Override
                            public void onSuccess(HmLivePlatform data) {
                                _url_pull_without_commentary = data.pullStreamAddress;
                                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                                ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                            }

                            @Override
                            public void onFail(String code, String message) {
                                ToastUtil.s(message);
                            }

                            @Override
                            public void onFinish() {
                                DialogUtil.dismissDialog();
                            }
                        });
                    } else {
                        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                        ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                    }
                } else {
                    if(viewBinding.toggleCommentary.isSelected()) {
                        if(TextUtils.isEmpty(_url_pull_without_commentary)) {
                            DialogUtil.showDialog("");
                            if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
                                ApiUtil.getPullStreamUrlWithCommentaryPassword(_oriBean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
                                    @Override
                                    public void onSuccess(PullStreamAddrBean data) {
                                        _url_pull_without_commentary = data.data;
                                        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                                        ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                                    }

                                    @Override
                                    public void onFail(String code, String message) {
                                        ToastUtil.s(message);
                                    }

                                    @Override
                                    public void onFinish() {
                                        DialogUtil.dismissDialog();
                                    }
                                });
                            } else {
                                ApiUtil.getPullStreamUrlWithoutCommentary(_oriBean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                                    @Override
                                    public void onSuccess(PullStreamAddrBean data) {
                                        _url_pull_without_commentary = data.data;
                                        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                                        ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                                    }

                                    @Override
                                    public void onFail(String code, String message) {
                                        ToastUtil.s(message);
                                    }

                                    @Override
                                    public void onFinish() {
                                        DialogUtil.dismissDialog();
                                    }
                                });
                            }
                        } else {
                            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_commentary));
                            ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                        }
                    } else {
                        if(TextUtils.isEmpty(_url_pull_without_commentary)) {
                            DialogUtil.showDialog("");
                            ApiUtil.getPullStreamUrlWithoutCommentary(_oriBean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                                @Override
                                public void onSuccess(PullStreamAddrBean data) {
                                    _url_pull_without_commentary = data.data;
                                    ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                                    ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                                }

                                @Override
                                public void onFail(String code, String message) {
                                    ToastUtil.s(message);
                                }

                                @Override
                                public void onFinish() {
                                    DialogUtil.dismissDialog();
                                }
                            });
                        } else {
                            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clip.setPrimaryClip(ClipData.newPlainText("url", _url_pull_without_commentary));
                            ToastUtil.s(getString(R.string.live_copied_pull_stream_url));
                        }
                    }
                }
                return true;
            }
        });

        initTimePicker();
    }

    private void popupHistoryList() {
        if(getIntent().hasExtra(EXTRA_HISTORY_LIST)) {
            LiveTaskListBean data = (LiveTaskListBean) getIntent().getSerializableExtra(EXTRA_HISTORY_LIST);
            assert data != null;

            _historyNameList = new ArrayList<>();
            for(int i = 0; i < data.data.size(); i++) {
                if(i == 5) {
                    break;
                }
                _historyNameList.add(data.data.get(i).competitionName);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(AddLiveTaskActivity.this, R.layout.auto_complete_text_view_item, _historyNameList);
            viewBinding.etCompetitionName.setAdapter(adapter);
            viewBinding.etCompetitionName.setOnItemClickListener((parent, view, position, id) -> updateCompetition(data.data.get(position)));
            viewBinding.etCompetitionName.setText("");
            viewBinding.etCompetitionName.requestFocus();

            _handler.postDelayed(() -> viewBinding.etCompetitionName.showDropDown(), 500);
        }
    }

    @Override
    protected void initData() {
        _playNumberList = getResources().getStringArray(R.array.play_number_list);
        String[] motionTypeList = getResources().getStringArray(R.array.motion_type_list);
        String[] placeTypeList = getResources().getStringArray(R.array.place_type_list);
        _placeTypeList = Arrays.asList(placeTypeList);
        _motionTypeList = Arrays.asList(motionTypeList);

        String motionName = _motionTypeList.get(_motionTypeIndex) + " " + VideoUtil.getPlaceNameByType(_motionTypeIndex, _placeTypeIndex);

        //运动类型
        viewBinding.tvTypeContent.setText(motionName);
        viewBinding.tvTypeContent.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showMotionTypeDialog();
                return true;
            }
        });


        //城市
        provinceCode = "440000000000";
        cityCode = "440300000000";
        provinceCodeDisplay = "广东省";
        cityCodeDisplay = "深圳市";
        viewBinding.tvCitySelect.setText("广东省深圳市");
        viewBinding.tvCitySelect.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(selectCityDialog != null && selectCityDialog.isShowing()) {
                    return true;
                }

                selectCityDialog = new SelectCityDialog(AddLiveTaskActivity.this);
                selectCityDialog.setListener((provinceCode, provinceCodeDisplay, cityCode, cityCodeDisplay) -> {

                    AddLiveTaskActivity.this.provinceCode = provinceCode;
                    AddLiveTaskActivity.this.provinceCodeDisplay = provinceCodeDisplay;
                    AddLiveTaskActivity.this.cityCode = cityCode;
                    AddLiveTaskActivity.this.cityCodeDisplay = cityCodeDisplay;
                    viewBinding.tvCitySelect.setText(provinceCodeDisplay + cityCodeDisplay);
                });
                selectCityDialog.init(provinceCode, cityCode);
                selectCityDialog.show();


                return true;
            }
        });

        //比赛日期时间
        viewBinding.tvTimeSelect.setOnClickListener(view -> {
            pvTime.show();
        });

        //解说
        viewBinding.groupToggleCommentary.setOnClickListener(view -> {
            viewBinding.toggleCommentary.setSelected(!viewBinding.toggleCommentary.isSelected());
            toggleCommentaryGroupVisibility();
        });

        if(getIntent().hasExtra(EXTRA_EDIT_BEAN)) {
            _oriBean = (HmCompetitionLive) getIntent().getSerializableExtra(EXTRA_EDIT_BEAN);
            assert _oriBean != null;
            _pushTo3rd = !TextUtils.isEmpty(_oriBean.livePlatformId);
            _3rdPltId = _oriBean.livePlatformId;
            try  {
                _3rdPlt = Integer.parseInt(_oriBean.livePlatformType);
            } catch (Throwable e){
                e.printStackTrace();
            }
            viewBinding.titlebar.tvTitle.setText(getString(R.string.edit_live_task_title));
            //不能在历史信息里同步状态，所以移到编辑的时候同步
            competitionStatus = _oriBean.competitionStatus;
            updateCompetition(_oriBean);
        } else {
            _motionTypeIndex = 1;
            _placeTypeIndex = 2;
            popupHistoryList();
        }

        // 拷贝拉流地址按钮仅当编辑状态可见
        viewBinding.btnCopyPullStreamWithCommentary.setVisibility(_oriBean != null ? View.VISIBLE : View.GONE);
    }

    private void showConfirmRefresh3rdLiveUrlDialog() {
        if(_confirmRefreshWxLiveUrlDialog != null && _confirmRefreshWxLiveUrlDialog.isShowing()) {
            return;
        }
        _confirmRefreshWxLiveUrlDialog = new ModalDialog(this);
        _confirmRefreshWxLiveUrlDialog.setPositiveButton(getString(R.string.update), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                Intent intent = new Intent(AddLiveTaskActivity.this, LiveThirdPlatformActivity.class);
                intent.putExtra(LiveThirdPlatformActivity.EXTRA_PRESELECT_PLATFORM_TYPE, Constants.LIVE_PLATFORM_WX_CHANNEL);
                intent.putExtra(LiveThirdPlatformActivity.EXTRA_COMPETITION_NAME, viewBinding.etCompetitionName.getText().toString());
                _thirdPltLauncher.launch(intent);
                return true;
            }
        });

        switch(_3rdPlt) {
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

    private void showConfirmDisselect3rdPltDialog(View pltView) {
        if(_confirmDisselect3rdPltDialog != null && _confirmDisselect3rdPltDialog.isShowing()) {
            return;
        }
        _confirmDisselect3rdPltDialog = new ModalDialog(this);
        _confirmDisselect3rdPltDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _pushTo3rd = false;
                viewBinding.plt3rd.setSelected(false);
                pltView.setSelected(true);
                toggleCommentaryGroupVisibility();
                return true;
            }
        });

        _confirmDisselect3rdPltDialog.setMessage(getString(R.string.will_disselect_3rd_platform));
        _confirmDisselect3rdPltDialog.setCancelable(true);
        _confirmDisselect3rdPltDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _confirmDisselect3rdPltDialog.dismiss();
                return true;
            }
        });

        _confirmDisselect3rdPltDialog.show();
    }

    private String getTime(Date date) { //可根据需要自行截取数据显示
        Log.d("getTime()", "choice date millis: " + date.getTime());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return format.format(date);
    }


    private String getShowTime(Date date) { //可根据需要自行截取数据显示
        Log.d("getTime()", "choice date millis: " + date.getTime());
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        return format.format(date);
    }

    TimePickerView pvTime;

    private void initTimePicker() {//Dialog 模式下，在底部弹出
        pvTime = new TimePickerBuilder(this, (date, v) -> {
            AppTrace.i("pvTime", "onTimeSelect");

            competitionTime = getTime(date);
            competitionShowTime = getShowTime(date);

            viewBinding.tvTimeSelect.setText(competitionShowTime);

        })
            .setCancelColor(R.color.text_gray_76)
            .setSubmitColor(R.color.orange)
            .setTimeSelectChangeListener(date -> Log.i("pvTime", "onTimeSelectChanged"))
            .setType(new boolean[] { true, true, true, true, true, false })
            .isDialog(true) //默认设置false ，内部实现将DecorView 作为它的父控件。
            .addOnCancelClickListener(view -> Log.i("pvTime", "onCancelClickListener"))
            .setItemVisibleCount(7) //若设置偶数，实际值会加1（比如设置6，则最大可见条目为7）
            .setLineSpacingMultiplier(2.0f)
            .isAlphaGradient(true)
            .build();

        Dialog mDialog = pvTime.getDialog();
        if(mDialog != null) {

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);

            params.leftMargin = 0;
            params.rightMargin = 0;
            pvTime.getDialogContainerLayout().setLayoutParams(params);

            Window dialogWindow = mDialog.getWindow();
            if(dialogWindow != null) {
                dialogWindow.setWindowAnimations(com.bigkoo.pickerview.R.style.picker_view_slide_anim);//修改动画样式
                dialogWindow.setGravity(Gravity.BOTTOM);//改成Bottom,底部显示
                dialogWindow.setDimAmount(0.3f);
            }
        }
    }


    //运动类型
    private MotionTypeDialog _motionPlaceDialog;

    private void showMotionTypeDialog() {
        if(_motionPlaceDialog != null && _motionPlaceDialog.isShowing()) {
            return;
        }
        _motionPlaceDialog = new MotionTypeDialog(AddLiveTaskActivity.this);
        _motionPlaceDialog.setCallback(new MotionTypeDialog.ICallback() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onConfirm(MotionBean motionBean, PlaceBean placeBean) {
                _motionTypeIndex = motionBean.type;
                _placeTypeIndex = placeBean.type;
                viewBinding.tvTypeContent.setText(motionBean.name + " " + placeBean.name);
            }
        }).show();

        _motionPlaceDialog.setSelectItem(_motionTypeIndex, _placeTypeIndex);
    }


    long hour = 0L;

    private void addWorkflowStep1_validate() {
        if(_workflowPending) return;
        _workflowPending = true;
        _unattended = viewBinding.unattendedMode.switchUnattended.isChecked();
        viewBinding.unattendedMode.switchUnattended.setEnabled(false);
        DialogUtil.showDialog("");
        _addResult = new AddResult();
        do {
            if(TextUtils.isEmpty(viewBinding.etCompetitionName.getText().toString())) {
                _addResult.errMsg = "输入比赛名称";
                break;
            }

            if(TextUtils.isEmpty(viewBinding.tvTimeSelect.getText().toString())) {
                _addResult.errMsg = "请选择比赛日期";
                break;
            }

            if(TextUtils.isEmpty(viewBinding.tvCitySelect.getText().toString())) {
                _addResult.errMsg = "请选择城市";
                break;
            }

            if(!viewBinding.pltRewoo.isSelected() &&
                !viewBinding.pltMgc.isSelected() &&
                !viewBinding.pltWxDy.isSelected() &&
                !viewBinding.plt3rd.isSelected()) {
                _addResult.errMsg = "请设置推流平台";
                break;
            }

            //时间
            String time = competitionTime + ":00";
            try {
                time = time.replaceAll("年", "-");
                time = time.replaceAll("月", "-");
                time = time.replaceAll("日", "");
                long liveTime = TimeUtil.str2Millis(time);
                long now = System.currentTimeMillis();

                long diff = now - liveTime;
                long day = diff / (1000 * 60 * 60 * 24); //以天数为单位取整
                hour = (diff / (60 * 60 * 1000) - day * 24) + day * 24; //以小时为单位取整

                if(now > liveTime && !_unattended) {
                    _addResult.errMsg = "选择时间的必须大于当前时间";
                    break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } while(false);

        if(TextUtils.isEmpty(_addResult.errMsg)) {
            addWorkflowStep2_addOrUpdateCompetition();
        } else {
            addWorkflowStep10_wrapUp();
        }
    }

    private void addWorkflowStep2_addOrUpdateCompetition() {
        HashMap<String, String> paramsdata = new HashMap<>();
//        paramsdata.put("createUserId", LoginManager.getUserId(this));

        paramsdata.put("deviceId", CameraManager.getDeviceId());
        paramsdata.put("autoPushStream", _unattended ? "1" : "0");

        paramsdata.put("competitionName", viewBinding.etCompetitionName.getText().toString());

        //比赛状态：0：未开始，1：进行中，2：已结束
        //按老黄的说法，编辑直播任务时不需要更新competitionStatus
        if(_oriBean == null) {
            paramsdata.put("competitionStatus", competitionStatus);
        }

        //比赛(运动)类型: 0特殊，1足球,2 篮球，3 训练
        paramsdata.put("motionType", _motionTypeIndex + "");

        String time = competitionTime + ":00";
        paramsdata.put("competitionTime", time);
        paramsdata.put("liveTime", time);

        paramsdata.put("pushStream3rd", viewBinding.pltWxDy.isSelected() ? "1" : "0");
        paramsdata.put("pushStreamMgc", viewBinding.pltMgc.isSelected() ? "1" : "0");
        paramsdata.put("pushStreamWxMiniApp", viewBinding.pltRewoo.isSelected() ? "1" : "0");
        if(_pushTo3rd) {
            paramsdata.put("livePlatformType", String.valueOf(_3rdPlt));
            paramsdata.put("livePlatformId", _3rdPltId);
        } else {
            paramsdata.put("livePlatformType", "0");
            paramsdata.put("livePlatformId", "");
        }

        //主队
        paramsdata.put("homeTeamName", viewBinding.etHomeName.getText().toString());
        paramsdata.put("awayTeamName", viewBinding.etVisitName.getText().toString());

        //城市
        paramsdata.put("cityCode", cityCode);
        paramsdata.put("provinceCode", provinceCode);

        //场馆
        paramsdata.put("placeType", _placeTypeIndex + "");
        paramsdata.put("place", viewBinding.etVenueName.getText().toString());

        //手机号
        paramsdata.put("competitionOrderPhones", viewBinding.etOrderPhone.getText().toString());

        //解说
        paramsdata.put("commentate", (viewBinding.toggleCommentary.isSelected() && !_pushTo3rd) ? "1" : "0");

        paramsdata.put("pushStreamKey", "");

        paramsdata.put("competitionImage", _coverUrl);

        String url;
        if(_oriBean != null) {
            url = SdkApi.competitionLive_update();
            paramsdata.put("competitionId", _oriBean.competitionId);
        } else if(_bean != null) {
            url = SdkApi.competitionLive_update();
            paramsdata.put("competitionId", _bean.competitionId);
        } else {
            url = SdkApi.competitionLive_add();

            // 需要比赛id, 版本是v2
            paramsdata.put("liveUrlVersion", "v2");

            // set live password
            if(SharedPrefs.getImpl().isLivePasswordEnabled()) {
                String pwd = SharedPrefs.getImpl().getLivePassword();
                if(!pwd.isEmpty()) {
                    paramsdata.put("encrypt", "1");
                    paramsdata.put("encryptText", pwd);
                }
            }
        }

        OkHttpUtil.postData(url, paramsdata, null, new OkHttpCallbackDecode<LiveTaskBean>(LiveTaskBean.class) {
            @Override
            public void onDataSuccess(LiveTaskBean data) {
                runOnUiThread(() -> {
                    if (data == null || data.data == null || TextUtils.isEmpty(data.data.competitionId)) {
                        _addResult.errMsg = "创建直播任务失败，请重试";
                        addWorkflowStep10_wrapUp();
                        return;
                    }
                    _bean = data.data;
                    if (_unattended) {
                        ToastUtil.s(getString(R.string.unattended_saved));
                        _addResult.needFinish = true;
                        addWorkflowStep10_wrapUp();
                    } else if(hour >= -2) {
                        addWorkflowStep3_getCommentaryPushStream(data.data);
                    } else {
                        _addResult.errMsg = "直播任务保存成功";
                        _addResult.needFinish = true;
                        addWorkflowStep10_wrapUp();
                    }
                });
            }

            @Override
            public void onFailure(String code, String message) {
                runOnUiThread(() -> {
                    _addResult.errMsg = message;
                    if(!TextUtils.isEmpty(code) && code.equals("201")) {
                        _addResult.duplicateCompetitionName = true;
                    }
                    addWorkflowStep10_wrapUp();
                });
            }
        });
    }

    private void addWorkflowStep3_getCommentaryPushStream(HmCompetitionLive bean) {
        _bean = bean;
        _urlEvent = new LiveUrlEvent();
        _urlEvent.third_party = _pushTo3rd;
        try {
            _urlEvent.third_plt = Integer.parseInt(bean.livePlatformType);
        } catch (Throwable e){
            e.printStackTrace();
        }
        // 如果是用第三方平台, 不需要从服务器取地址,所以直接就开始启动rkipc
        if(_pushTo3rd) {
            if(TextUtils.isEmpty(_3rdPushUrl) ||
                TextUtils.isEmpty(_3rdPullUrl)) {
                addWorkflowStep7_get3rdLiveUrl();
            } else {
                _urlEvent.url_push = _3rdPushUrl + _3rdPushKey;
                _urlEvent.url_pull = _3rdPullUrl;
                _urlEvent.url_pull_commentary = _3rdPullUrl;
                addWorkflowStep8_validate3rdUrl();
            }
        } else {
            if(_bean.commentate == 1) {
                _urlEvent.commentary = true;
                ApiUtil.getPushStreamWithCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
                    @Override
                    public void onSuccess(PullStreamAddrBean data) {
                        _urlEvent.url_push_commentary = data.data;
                        addWorkflowStep4_getCommentaryPullStream();
                    }

                    @Override
                    public void onFail(String code, String message) {
                        _addResult.errMsg = message;
                        addWorkflowStep10_wrapUp();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
            } else {
                addWorkflowStep5_getPullStream();
            }
        }
    }

    private void addWorkflowStep4_getCommentaryPullStream() {
        if(_bean.commentate == 1) {
            _urlEvent.commentary = true;
//            if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
//                ApiUtil.getPullStreamUrlWithCommentaryPassword(_bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
//                    @Override
//                    public void onSuccess(PullStreamAddrBean data) {
//                        _urlEvent.url_pull_commentary = data.data;
//                        addWorkflowStep5_getPullStream();
//                    }
//
//                    @Override
//                    public void onFail(String code, String message) {
//                        _addResult.errMsg = message;
//                        addWorkflowStep10_wrapUp();
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
                        addWorkflowStep5_getPullStream();
                    }

                    @Override
                    public void onFail(String code, String message) {
                        _addResult.errMsg = message;
                        addWorkflowStep10_wrapUp();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
//            }
        } else {
            addWorkflowStep5_getPullStream();
        }
    }

    private void addWorkflowStep5_getPullStream() {
        if(SharedPrefs.getImpl().isLivePasswordEnabled() && !TextUtils.isEmpty(SharedPrefs.getImpl().getLivePassword())) {
            ApiUtil.getPullStreamUrlWithCommentaryPassword(_bean.competitionId, SharedPrefs.getImpl().getLivePassword(), new IGenericApiCallback<PullStreamAddrBean>() {
                @Override
                public void onSuccess(PullStreamAddrBean data) {
                    _urlEvent.url_pull = data.data;
                    addWorkflowStep6_getPushStream();
                }

                @Override
                public void onFail(String code, String message) {
                    _addResult.errMsg = message;
                    addWorkflowStep10_wrapUp();
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
                    addWorkflowStep6_getPushStream();
                }

                @Override
                public void onFail(String code, String message) {
                    _addResult.errMsg = message;
                    addWorkflowStep10_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        }

    }

    private void addWorkflowStep6_getPushStream() {
        ApiUtil.getPushStreamWithoutCommentary(_bean.competitionId, new IGenericApiCallback<PullStreamAddrBean>() {
            @Override
            public void onSuccess(PullStreamAddrBean data) {
                _urlEvent.url_push = data.data;
                addWorkflowStep9_startRkipc();
            }

            @Override
            public void onFail(String code, String message) {
                _addResult.errMsg = message;
                addWorkflowStep10_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void addWorkflowStep8_validate3rdUrl() {
        switch(_3rdPlt) {
            case Constants.LIVE_PLATFORM_WX_CHANNEL:
                ApiUtil.checkWxPushUrl(_urlEvent.url_push, new IGenericApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        if(data) {
                            addWorkflowStep9_startRkipc();
                        } else {
                            _addResult.needRefresh3rdLiveUrl = true;
                            addWorkflowStep10_wrapUp();
                        }
                    }

                    @Override
                    public void onFail(String code, String message) {
                        _addResult.errMsg = message;
                        _addResult.needRefresh3rdLiveUrl = true;
                        addWorkflowStep10_wrapUp();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
                break;
            case Constants.LIVE_PLATFORM_DY_CHANNEL:
                ApiUtil.checkDyPushUrl(_urlEvent.url_push, new IGenericApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        if(data) {
                            addWorkflowStep9_startRkipc();
                        } else {
                            _addResult.needRefresh3rdLiveUrl = true;
                            addWorkflowStep10_wrapUp();
                        }
                    }

                    @Override
                    public void onFail(String code, String message) {
                        _addResult.errMsg = message;
                        _addResult.needRefresh3rdLiveUrl = true;
                        addWorkflowStep10_wrapUp();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
                break;
            default:
                addWorkflowStep9_startRkipc();
                break;
        }
    }

    private void addWorkflowStep7_get3rdLiveUrl() {
        ApiUtil.getLivePlatform(_3rdPltId, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _3rdPushUrl = data.pushStreamAddress;
                _3rdPushKey = "";
                _3rdPullUrl = data.pullStreamAddress;
                _urlEvent.url_push = _3rdPushUrl + _3rdPushKey;
                _urlEvent.url_pull = _3rdPullUrl;
                _urlEvent.url_pull_commentary = _3rdPullUrl;
                addWorkflowStep8_validate3rdUrl();
            }

            @Override
            public void onFail(String code, String message) {
                switch(_3rdPlt) {
                    case Constants.LIVE_PLATFORM_XQM:
                        _addResult.errMsg = message;
                        break;
                    default:
                        _addResult.needRefresh3rdLiveUrl = true;
                        break;
                }
                addWorkflowStep10_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void addWorkflowStep9_startRkipc() {
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
            _addResult.errMsg = error;
        }

        // wrap up
        addWorkflowStep10_wrapUp();
    }

    private void addWorkflowStep10_wrapUp() {
        if(_liveStartStage == 0) {
            _workflowPending = false;
            viewBinding.unattendedMode.switchUnattended.setEnabled(true);
        }
        DialogUtil.dismissDialog();
        if(_addResult.duplicateCompetitionName) {
            showHintDialog("比赛名称重复", _addResult.errMsg, "好的", null);
        } else if(_addResult.needRefresh3rdLiveUrl) {
            showConfirmRefresh3rdLiveUrlDialog();
        } else if(!TextUtils.isEmpty(_addResult.errMsg)) {
            ToastUtil.s(_addResult.errMsg);
        }

        // finish activity if needed
        if(_addResult.needFinish) {
            finish();
        }
    }

    private final Runnable _startRkipcTimeOutRunnable = () ->
        failLiveStart(getString(R.string.info_running_timeout_on_lunch_live));


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        runOnUiThread(() -> {
            AppTrace.d(TAG, "onRkipcStart 创建页面");

            if(event != null && _urlEvent != null && _liveStartStage == 1) {
                _handler.removeCallbacks(_startRkipcTimeOutRunnable);
                if(event.status == 1) {
                    _liveStartStage = 2;
                    if(DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean())) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    } else {
                        failLiveStart(getString(R.string.info_network_disconnect));
                    }
                }else{
                    failLiveStart(str_lunch_rkipc_fail);
                }

            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFecRtmpAddUrl(FecRtmpAddUrlEvent event) {
        AppTrace.d(TAG, "FecRtmpAddUrl");
        if(_liveStartStage != 4) return;

        if(event != null) {
            if(event.status == 1) {
                _liveStartStage = 0;
                try {
                    ApiUtil.comprehensiveReportCompetitionLiveStatus(_bean.competitionId, _urlEvent.url_push, _urlEvent.third_plt, Constants.COMPETITION_START);
//                    startRecord();
                    _handler.removeCallbacks(_startRkipcTimeOutRunnable);
                    dismissConnectingDialog();
                    _handler.removeCallbacks(_timeOutRunnable);
                    finish();
                } catch(Exception e) {
                    e.printStackTrace();
                }

            } else {
                failLiveStart(str_add_fec_url_fail);
            }
        }

    }


    @Override
    public void onRecordVideoStart() {
        runOnUiThread(() -> {
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
        viewBinding.unattendedMode.switchUnattended.setEnabled(true);
        _handler.removeCallbacks(_startRkipcTimeOutRunnable);
        _handler.removeCallbacks(_timeOutRunnable);
        dismissConnectingDialog();
        ToastUtil.s(TextUtils.isEmpty(message) ? getString(R.string.info_running_timeout_on_lunch_live) : message);
    }


    private void startRecord() {
        //开始前，清除之前的自定义打点记录
        AppTrace.d(TAG, "clear custom dot history addlive");
        ScoreCenterUtil.removeCustomDotRecord();

        RecordInfoBean _recordInfoBean = new RecordInfoBean("", 4096);
        _recordInfoBean.author = "";
        _recordInfoBean.width = Constants.FEC_VIDEO_WIDTH;
        _recordInfoBean.height = Constants.FEC_VIDEO_HEIGHT;
        _recordInfoBean.place = _bean.place;
        _recordInfoBean.name = _bean.competitionName;
        _recordInfoBean.campaignName = SharedPrefs.getImpl().getDefaultMatchName();
        _recordInfoBean.motionType = Integer.parseInt(_bean.motionType);//运动类型
        _recordInfoBean.placeType = Integer.parseInt(_bean.placeType);//场地类型
        _recordInfoBean.city = _bean.cityCodeDisplay;
        _recordInfoBean.provinceId = _bean.provinceCode;
        _recordInfoBean.cityId = _bean.cityCode;
        _recordInfoBean.hostTeam = _bean.homeTeamName;
        _recordInfoBean.guestTeam = _bean.awayTeamName;
        _recordInfoBean.time = System.currentTimeMillis();

        _recordInfoBean.maxRate = VideoUtil.getFecMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));
        _recordInfoBean.avs_mbr = VideoUtil.getAvsMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));
        _recordInfoBean.live_mbr = VideoUtil.getLiveMBR(Integer.parseInt(_bean.motionType), Integer.parseInt(_bean.placeType));

        // 不是足球的话, 人数默认弄个5人, 不然0人会被当成训练模式
        if(_recordInfoBean.motionType != Constants.SPORT_FOOTBALL) {
            _recordInfoBean.player_num = 5;
        } else {
            try {
                _recordInfoBean.player_num = Integer.parseInt(_playNumberList[Integer.parseInt(_bean.placeType)]);  //参赛人数
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        String bluetoothId = CameraManager.getDeviceBluetoothId();

        boolean isAppend = CompetitionLiveManager.isAppend(_bean.competitionName, _recordName, bluetoothId);
        _recordInfoBean.append = isAppend;

        _recordInfoBean.live_cid = _bean.competitionId;
        _recordInfoBean.ctype = Constants.COMPETITION_TYPE_LIVE;

        _recordInfoBean.hide_osd = SharedPrefs.getImpl().getBoolean(SharedPrefs.PREF_HIDE_OSD) ? 1: 0;

        _recordInfoBean.pause_time = 1;

        EventBus.getDefault().post(new RecordStartEvent(_recordInfoBean));
        CameraRecordInfoBean cameraRecordInfoBean = CameraRecordInfoBean.newInstance(_recordInfoBean);
        cameraRecordInfoBean.deviceId = CameraManager.getDeviceBluetoothId();

        CompetitionLiveManager.addCompetition(cameraRecordInfoBean);

        RecordStartRequestBean recordStartRequestBean = _recordInfoBean.convertRecordStartRequestBean();
        boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);
        if(ok) {
            // 训练模式时player num为0, 训练模式需要重启rkipc, 超时时间设置的长一点
            _handler.postDelayed(_timeOutRunnable, 15000);
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
        _connectingDialog = new BluetoothLoadingDialog(AddLiveTaskActivity.this);
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
        if(_coordinatedStarter != null) _coordinatedStarter.cancel();
        dismissConnectingDialog();
        super.onDestroy();

        if(_confirmDisselect3rdPltDialog != null && _confirmDisselect3rdPltDialog.isShowing()) {
            _confirmDisselect3rdPltDialog.dismiss();
        }
        _confirmDisselect3rdPltDialog = null;
        if(_confirmRefreshWxLiveUrlDialog != null && _confirmRefreshWxLiveUrlDialog.isShowing()) {
            _confirmRefreshWxLiveUrlDialog.dismiss();
        }
        _confirmRefreshWxLiveUrlDialog = null;

        BluetoothHandle.removePlayNotifyListener(this);
    }

    private void updateCompetition(HmCompetitionLive bean) {
        viewBinding.etCompetitionName.setText(bean.competitionName);
        viewBinding.unattendedMode.switchUnattended.setChecked(
                Integer.valueOf(1).equals(bean.autoPushStream));

        _motionTypeIndex = Math.max(0, Math.min(_motionTypeList.size() - 1, Integer.parseInt(bean.motionType)));
        _placeTypeIndex = Integer.parseInt(bean.placeType);

        String motionName = _motionTypeList.get(_motionTypeIndex) + " " + VideoUtil.getPlaceNameByType(_motionTypeIndex, _placeTypeIndex);
        viewBinding.tvTypeContent.setText(motionName);

        try {
            competitionTime = getTime(TimeUtil.stampToDateObj(bean.competitionTime));
            competitionShowTime = getTime(TimeUtil.stampToDateObj(bean.competitionTime));
            viewBinding.tvTimeSelect.setText(competitionShowTime);
        } catch(Exception e) {
            e.printStackTrace();
        }

        viewBinding.etHomeName.setText(bean.homeTeamName);
        viewBinding.etVisitName.setText(bean.awayTeamName);

        provinceCode = bean.provinceCode;
        provinceCodeDisplay = bean.provinceCodeDisplay;
        cityCode = bean.cityCode;
        cityCodeDisplay = bean.cityCodeDisplay;

        viewBinding.tvCitySelect.setText(provinceCodeDisplay + cityCodeDisplay);
        viewBinding.etVenueName.setText(bean.place);

        viewBinding.pltWxDy.setSelected(bean.pushStream3rd != 0 && !_pushTo3rd);
        viewBinding.pltRewoo.setSelected(bean.pushStreamWxMiniApp != 0 && !_pushTo3rd);
        viewBinding.pltMgc.setSelected(bean.pushStreamMgc != 0 && !_pushTo3rd);
        viewBinding.plt3rd.setSelected(_pushTo3rd);

        try {
            //解说
            viewBinding.toggleCommentary.setSelected(bean.commentate != 0);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }

        toggleCommentaryGroupVisibility();
    }

    private void toggleCommentaryGroupVisibility() {
        viewBinding.groupCommentary.setVisibility(_pushTo3rd ? View.GONE : View.VISIBLE);
        viewBinding.groupCopyUrl.setVisibility((_oriBean != null && viewBinding.toggleCommentary.isSelected() && !_pushTo3rd) ? View.VISIBLE : View.GONE);
    }
}
