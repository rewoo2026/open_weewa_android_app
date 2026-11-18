package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStartRequestBean;
import com.xh.hotme.bluetooth.bean.RkipcStopRequestBean;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.event.RkIpcStopEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.databinding.ActivityTestAutoBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.PlayUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@IEventBus
public class TestAutoActivity extends BaseActivity<ActivityTestAutoBinding> implements IRecordControlListener, IBleDeviceInfoNotifyListener {

    private final static String TAG = TestAutoActivity.class.getSimpleName();


    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;


    String uploadId = "1671090275338526721";
    String videoName;

    DeviceInfo _deviceInfo;
    boolean getDeviceInfoAndPreview = false;

    final List<String> infoList = new ArrayList<>();

    final int maxLines = 50;

    boolean isRkipcStart = false;
    final boolean isRecordStart = false;

    RecordInfoBean _recordInfoBean;

    int failNum = 0;
    int runIndex = 0;

    String startTime = "";

    public static void start(Context context) {
        Intent intent = new Intent(context, TestAutoActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        viewBinding.titlebar.tvTitle.setText("定时录制测试");
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });


        viewBinding.btnRecord.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(isRkipcStart) {
                    if(!autoRun) {
                        showRkipcDialog();
                    } else {
                        autoRun = false;
                        viewBinding.btnRecord.setText("定时录制");
                        if(isRecordStart) {
                            DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                        }

                    }
                } else {
                    ToastUtil.s(getString(R.string.info_rkipc_not_lunched));
                }

                return true;
            }
        });
        viewBinding.btnRecord.setEnabled(false);

        viewBinding.btnRkipc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!isRkipcStart) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStartRequestBean());
                    if(isOk) {
                        printInfo("启动RKIPC命令已发送");
                    }
                } else {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RkipcStopRequestBean());
                    if(isOk) {
                        printInfo("结束RKIPC命令已发送");
                    }
                }

                return true;
            }
        });
        viewBinding.btnRkipc.setEnabled(true);

        BluetoothHandle.addPlayNotifyListener(this);
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removePlayNotifyListener(this);

        autoRun = false;
        if(rkipcHandle != null) {
            rkipcHandle.removeCallbacksAndMessages(null);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private String getCurrentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    private String getTaskInfo() {

        if(TextUtils.isEmpty(startTime)) {
            startTime = getCurrentTime();
        }

        String showInfo = "任务开始时间: " + startTime + "\n";
        showInfo += "总次数: " + rkipcLoop + "次, 单次录制时长：" + rkipcDuration + "秒, 间隔：" + rkipcInterval + "秒\n";
        return showInfo;
    }

    @Override
    public void onRecordVideoStart() {
        printInfo("视频开始录制...");

        rkipcHandle.postDelayed(recordStopRunnable, rkipcDuration * 1000);

        String showInfo = getTaskInfo();
        showInfo += String.format("当前第%d次，录制中...", rkipcLoopCounter + 1);
        if(viewBinding.tvTask != null) {
            viewBinding.tvTask.setText(showInfo);
        }
    }

    @Override
    public void onRecordVideoStatusUpdate(RecordStatusResultBean recordStatus) {
        printInfo("录制状态： status=" + recordStatus.getStatus() + ", time= " + recordStatus.getDuration());

    }

    @Override
    public void onRecordVideoStop() {
        printInfo("视频已停止录制");
        String showInfo = getTaskInfo();
        showInfo += String.format("当前第%d次，停止录制.", rkipcLoopCounter + 1);
        if(viewBinding.tvTask != null) {
            viewBinding.tvTask.setText(showInfo);
        }

        if(autoRun) {

            rkipcLoopCounter++;
            if(rkipcLoopCounter < rkipcLoop) {
                printInfo(String.format("等待%d秒后执行下一次", rkipcInterval));
                rkipcHandle.postDelayed(recordRunnable, rkipcInterval * 1000);
            } else {
                printInfo("执行结束");
            }
        } else {
            printInfo("任务执行取消");
        }
    }

    @Override
    public void onRecordException(String msg) {
        printInfo("录制状态： error=" + msg);
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        _deviceInfo = data;

        printInfo("设备信息：" + new Gson().toJson(data));

        _handler.post(() -> {
            if(!BaseAppUtil.isDestroy(TestAutoActivity.this) && BaseAppUtil.isForegroundActivity(TestAutoActivity.this, TestAutoActivity.class.getName())) {
                if(getDeviceInfoAndPreview) {
                    PlayUtil.startPlay(TestAutoActivity.this, data, null);
                    getDeviceInfoAndPreview = false;
                }
            }
        });
    }

    @Override
    public void onDeviceInfoFail() {
        _handler.post(() -> viewBinding.tvStatus.setText("获取设备信息异常"));
    }


    boolean autoRun = false;

    long rkipcLoop = 1000;
    long rkipcDuration = 1800;
    long rkipcInterval = 60;
    long rkipcLoopCounter = 0;

    private final Runnable recordRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(autoRun) {
                String showInfo = getTaskInfo();
                showInfo += String.format("当前第%d次，开始录制", rkipcLoopCounter + 1);
                viewBinding.tvTask.setText(showInfo);
                printInfo(String.format("当前第%d次", rkipcLoopCounter + 1));
                sendRecord();
            } else {
                printInfo("自动化执行停止");
            }
        }
    };


    private final Runnable recordStopRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            AppTrace.d("app run record stop runnable");
            if(autoRun) {

                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
                if(isOk) {
                    printInfo("蓝牙发送停止录制命令成功");
                } else {
                    printInfo("蓝牙停止录制命令发送失败");
                }

            } else {
                printInfo("自动化执行停止");
            }
        }
    };

    Handler rkipcHandle;

    private void showRkipcDialog() {
        AutoRkipcDialog dialog = new AutoRkipcDialog(TestAutoActivity.this);
        dialog.setOnClickListener(new AutoRkipcDialog.IAutoRkipcInfoListener() {
            @Override
            public void onUpdate(long loop, long duration, long interval) {
                autoRun = true;
                String showInfo = getTaskInfo();
                viewBinding.tvTask.setText(showInfo);

                viewBinding.btnRecord.setText("停止定时录制");

                printInfo(showInfo);

                rkipcLoop = loop;
                rkipcDuration = duration;
                rkipcInterval = interval;

                rkipcLoopCounter = 0;
                if(rkipcHandle == null) {
                    rkipcHandle = new Handler();
                }
                rkipcHandle.post(recordRunnable);

                startTime = getCurrentTime();
                viewBinding.llTask.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel() {
                autoRun = false;
            }
        });

        dialog.show();
    }

    private void printInfo(String showInfo) {
        try {
            AppTrace.d(showInfo);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = simpleDateFormat.format(new Date());
            infoList.add(0, timeStr + " " + showInfo);
            if(infoList.size() > maxLines) {
                infoList.remove(infoList.size() - 1);
            }
            printToStatusView();
        } catch(Throwable e) {

        }

    }

    private void printToStatusView() {
        try {
            if(TestAutoActivity.this != null && !TestAutoActivity.this.isDestroyed()) {
                runOnUiThread(() -> {
                    StringBuilder logBuilder = new StringBuilder();
                    for(String log : infoList) {
                        logBuilder.append(log).append("\n");
                    }
                    AppTrace.d(TAG, "status output: " + logBuilder);
                    viewBinding.tvStatus.setText(logBuilder.toString());
                });
            }
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStart(RkIpcEvent event) {
        if(event != null && event.status == 1) {
            isRkipcStart = true;
            printInfo("启动RKIPC成功");
            viewBinding.btnRkipc.setText("停止RkIpc");
            viewBinding.btnRecord.setEnabled(true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRkipcStop(RkIpcStopEvent event) {
        if(event != null && event.status == 1) {
            isRkipcStart = false;
            printInfo("RKIPC已停止");
            viewBinding.btnRkipc.setText("开启RkIpc");
            viewBinding.btnRecord.setEnabled(false);
        }
    }

    private void sendRecord() {
        _recordInfoBean = new RecordInfoBean("", 8192);
        _recordInfoBean.author = "zzh";
        _recordInfoBean.width = 1920;
        _recordInfoBean.height = 1080;
        _recordInfoBean.place = "测试场";
        _recordInfoBean.name = "自动化测试";
        _recordInfoBean.motionType = 1;
        _recordInfoBean.placeType = 2;
        _recordInfoBean.city = "深圳市";  //城市
        _recordInfoBean.hostTeam = "";
        _recordInfoBean.guestTeam = "";
        _recordInfoBean.time = System.currentTimeMillis();


        RecordStartRequestBean recordStartRequestBean = _recordInfoBean.convertRecordStartRequestBean();
        boolean ok = DeviceApiManager.sendCmdSimpleObj(recordStartRequestBean);
        if(ok) {
            printInfo("录制命令已发送成功");
        } else {
            showDisconnectToast();
        }
    }
}
