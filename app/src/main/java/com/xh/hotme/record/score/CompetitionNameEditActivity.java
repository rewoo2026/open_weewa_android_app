package com.xh.hotme.record.score;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;

import com.xh.hotme.R;
import com.xh.hotme.base.BLEAware;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetOSDSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.SetVideoNameRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateTeamNameRequestBean;
import com.xh.hotme.bluetooth.event.SetTeamNameEvent;
import com.xh.hotme.bluetooth.event.SetVideoNameEvent;
import com.xh.hotme.databinding.ActivityCompetitionNameEditBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@BLEAware
@IEventBus
public class CompetitionNameEditActivity extends BaseActivity<ActivityCompetitionNameEditBinding> {

    private final static String TAG = CompetitionNameEditActivity.class.getSimpleName();

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    private static final String RECORD_INFO = "record_info";

    RecordInfoBean _recordInfo;

    public String hostTeamName;
    public String guestTeamName;

    private String _gameTag;

    boolean needUpdateGameName = false;
    boolean needUpdateTeamName = false;


    private final Runnable _timeOutRunnable = () -> DialogUtil.dismissDialog();

    public static void start(Context ctx, RecordInfoBean recordInfo) {
        Intent intent = new Intent(ctx, CompetitionNameEditActivity.class);
        intent.putExtra(RECORD_INFO, GsonUtils.GsonString(recordInfo));
        ctx.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // refresh osd settings
        DeviceApiManager.sendCmdSimpleObj(new GetOSDSettingsRequestBean());
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titleBar, getString(R.string.edit));
    }

    @Override
    protected void initData() {
        if(getIntent() != null) {
            String recordInfoStr = (String) getIntent().getSerializableExtra(RECORD_INFO);
            _recordInfo = GsonUtils.GsonToBean(recordInfoStr, RecordInfoBean.class);
        }
        if(_recordInfo == null) {
            finish();
            return;
        }
        //比赛名称禁止EditText输入空格和换行符
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (source.equals(" ") || source.toString().contentEquals("\n")) {
                return "";
            } else {
                return null;
            }
        };
        viewBinding.etGameName.setFilters(new InputFilter[]{filter});
        // 比赛名称长度限制最多为20个字符
        viewBinding.etGameName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        viewBinding.etGameName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: s = " + s + ", start = " + start +
                        ", before = " + before + ", count = " + count);

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                String content = s.toString();
                try {
                    viewBinding.gameNameCount.setText(String.format("%d/20",content.length()));
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        viewBinding.etGameName.setText(_recordInfo.name);

        viewBinding.etHostName.setFilters(new InputFilter[]{filter});
        // 比赛名称长度限制最多为20个字符
        viewBinding.etHostName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        viewBinding.etHostName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: s = " + s + ", start = " + start +
                        ", before = " + before + ", count = " + count);

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                String content = s.toString();
                try {
                    viewBinding.hostNameCount.setText(String.format("%d/20",content.length()));
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        viewBinding.etGuestName.setFilters(new InputFilter[]{filter});
        // 比赛名称长度限制最多为20个字符
        viewBinding.etGuestName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        viewBinding.etGuestName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: s = " + s + ", start = " + start +
                        ", before = " + before + ", count = " + count);

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                String content = s.toString();
                try {
                    viewBinding.guestNameCount.setText(String.format("%d/20",content.length()));
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        if(!TextUtils.isEmpty(_recordInfo.hostTeam)) {
            hostTeamName = _recordInfo.hostTeam;
        } else {
            hostTeamName = getResources().getString(R.string.home_team);
        }

        if(!TextUtils.isEmpty(_recordInfo.guestTeam)) {
            guestTeamName = _recordInfo.guestTeam;
        } else {
            guestTeamName = getResources().getString(R.string.guest_team);
        }
        viewBinding.etHostName.setText(hostTeamName);
        viewBinding.etGuestName.setText(guestTeamName);

        viewBinding.tvOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }

                String gameName = viewBinding.etGameName.getText().toString();
                if(!gameName.equalsIgnoreCase(_recordInfo.name)) {
                    needUpdateGameName = true;
                }

                if(needUpdateGameName) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SetVideoNameRequestBean(gameName));
                    if(!isOk) {
                        ToastUtil.s(getString(R.string.device_offline));
                    } else {
                        DialogUtil.showDialog(CompetitionNameEditActivity.this.getString(R.string.loading));
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                    }
                } else {
                    String hostName = viewBinding.etHostName.getText().toString();
                    String guestName = viewBinding.etGuestName.getText().toString();
                    if(!hostName.equalsIgnoreCase(hostTeamName) || !guestName.equalsIgnoreCase(guestTeamName)) {
                        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new UpdateTeamNameRequestBean(hostName, guestName));
                        if(!isOk) {
                            DialogUtil.dismissDialog();
                            ToastUtil.s(getString(R.string.device_offline));
                        } else {
                            DialogUtil.showDialog(CompetitionNameEditActivity.this.getString(R.string.loading));
                            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                        }
                    } else {
                        ToastUtil.s(getString(R.string.info_notneed_update));
                    }

                }

                return true;
            }
        });
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateTeamNameEvent(SetTeamNameEvent event) {
        AppTrace.d(TAG, "onUpdateTeamNameEvent");
        _handler.removeCallbacksAndMessages(null);
        DialogUtil.dismissDialog();
        if(event != null && event.state == 1) {
            //同步到服务器
            updateCompetitionNameToServer();
            ToastUtil.s(getString(R.string.info_update_success));
            finish();
        } else {
            ToastUtil.s(getString(R.string.info_update_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetVideoNameEvent(SetVideoNameEvent event) {
        _handler.removeCallbacksAndMessages(null);
        if(event != null && event.state == 1) {
            String hostName = viewBinding.etHostName.getText().toString();
            String guestName = viewBinding.etGuestName.getText().toString();
            if(!hostName.equalsIgnoreCase(hostTeamName) || !guestName.equalsIgnoreCase(guestTeamName)) {
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new UpdateTeamNameRequestBean(hostName, guestName));
                if(!isOk) {
                    DialogUtil.dismissDialog();
                    ToastUtil.s(getString(R.string.device_offline));
                } else {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_LIGHT);
                }
            } else {
                //同步到服务器
                updateCompetitionNameToServer();
                DialogUtil.dismissDialog();
                ToastUtil.s(getString(R.string.info_update_success));
                finish();
            }
        } else {
            DialogUtil.dismissDialog();
            ToastUtil.s(getString(R.string.info_update_fail));
        }
    }

    private void updateCompetitionNameToServer() {
        if(_recordInfo != null && !_recordInfo.live_cid.isEmpty() && _recordInfo.isLive()) {
            String gameName = viewBinding.etGameName.getText().toString();
            String hostName = viewBinding.etHostName.getText().toString();
            String guestName = viewBinding.etGuestName.getText().toString();
            ApiUtil.updateCompetitionLiveName(_recordInfo.live_cid, gameName, hostName, guestName);
        }
    }
}
