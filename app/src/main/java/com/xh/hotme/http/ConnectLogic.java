package com.xh.hotme.http;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.weewa.lib.IWeewaDelegate;
import com.weewa.lib.Prefs;
import com.weewa.lib.WeewaLib;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.CameraVideosBean;
import com.xh.hotme.bluetooth.event.CameraFilesDownloadedEvent;
import com.xh.hotme.bluetooth.event.NetWorkForQEvent;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MediaScannerHelper;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * date   : 2023/6/21
 * desc   :连接管理类
 */
public class ConnectLogic {
    final String TAG = "ConnectLogic";

    private String ssid = WifiManager.WEEWA_SOFTAP_NAME;
    private String ssidPwd = "";
    //    private String connectIp = MyApp.isTest ? "192.168.1.17" : "10.201.126.1";
    private String connectIp = "10.201.126.1";
    //        private String connectIp = "192.168.1.17";
    private String connectPort = "38888";
    private String connectBaseurl = "http://" + connectIp + ":" + connectPort;
    private boolean isConnectIng;//wifi连接状态
    private int connectSessionId = -1;
    private String basePath = "/data/weewa/video/";
    private final List<String> files = new ArrayList<>();
    private SessionStatus sessionState = SessionStatus.WAITE;//连接下载状态
    private String deviceId = "test1";//设备id
    private String deviceMac = ""; //设备id
    double sessionProgress;//连接下载进度
    double sessionProgressLast;//上次连接下载进度
    long rateSize;//速率
    long fileSize;//文件大小
    final int LOOPERPROGRESS = 101;//进度刷新
    long lastTime = 0;//计算速率的时间
    CameraVideoListBean.VideosBean selectData;

    long startDownTime;//开始下载的时间
    private String transferError = "";


    boolean isServerConnected = false;

    public interface ICallback {
        void onInfo(SessionStatus sessionStatus, double progress, long allSize, long rateSize);
    }

    private ICallback callback;

    //线程
    final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == LOOPERPROGRESS) {
                try {
                    long time = System.currentTimeMillis() - lastTime;
                    if(time > 0 && sessionProgress >= sessionProgressLast) {
                        rateSize = (long) (fileSize * (sessionProgress - sessionProgressLast) / time * 1000);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

                lastTime = System.currentTimeMillis();
                sessionProgressLast = sessionProgress;
                if(callback != null) {
                    callback.onInfo(sessionState, sessionProgress, fileSize, rateSize);
                }
//                EventBus.getDefault().post(new SessionEvent(sessionState, sessionProgress, fileSize, rateSize));
            }
        }
    };

    public enum SessionStatus {
        WAITE,
        CONNECT,
        START,
        DOWNLOADING,
        UNCONNECT,
        FAILED,
        Ended
    }

    final List<CameraVideoDetailListBean.VideosBean> datasSelect = new ArrayList();

    private final int minRefreshTime = 500;
    private long lastRefreshTime;

    public ConnectLogic() {

        try {
            WeewaLib.Companion.shared().setDelegate(new IWeewaDelegate() {
                @Override
                public void sessionWaiting(int id) {
                    AppTrace.d(TAG, "sessionWaiting=" + id + ", connectSessionId=" + connectSessionId);
                    if(connectSessionId == id) {
                        sessionState = SessionStatus.WAITE;
                        mHandler.removeMessages(LOOPERPROGRESS);
                        mHandler.sendEmptyMessage(LOOPERPROGRESS);
                    }

                }

                @Override
                public void sessionConnecting(int id) {
                    AppTrace.d(TAG, "sessionConnecting=" + id + ", connectSessionId=" + connectSessionId);
                    if(connectSessionId == id) {
                        sessionState = SessionStatus.CONNECT;
                        mHandler.sendEmptyMessage(LOOPERPROGRESS);
                    }
                }

                @Override
                public void sessionStarted(int id) {
                    AppTrace.d(TAG, "sessionStarted=" + id + ", connectSessionId=" + connectSessionId);
                    if(connectSessionId == id) {
                        sessionState = SessionStatus.START;
                        sessionProgress = 0;
                        sessionProgressLast = 0;
                        startDownTime = System.currentTimeMillis();
                        mHandler.removeMessages(LOOPERPROGRESS);
                        mHandler.sendEmptyMessage(LOOPERPROGRESS);
                    }
                }

                @Override
                public void sessionProgress(int id, double progress) {
                    if(connectSessionId == id) {
                        if(sessionState != SessionStatus.Ended) {
                            long curT = SystemClock.elapsedRealtime();
                            if(curT - lastRefreshTime < minRefreshTime) {
                                return;
                            }
                            lastRefreshTime = curT;
                            // Log at the UI refresh rate, not once per 64 KB packet.
                            AppTrace.d(TAG, "sessionProgress=" + id + ", progress=" + progress);
                            sessionState = SessionStatus.DOWNLOADING;
                            sessionProgress = Math.max(0, Math.min(1, progress));
                            mHandler.removeMessages(LOOPERPROGRESS);
                            mHandler.sendEmptyMessage(LOOPERPROGRESS);
                        }

                    }

                }

                @Override
                public void sessionEnded(int id) {
                    AppTrace.d(TAG, "sessionEnded=" + id + ", connectSessionId=" + connectSessionId);
                    if(connectSessionId == id) {
                        sessionState = SessionStatus.Ended;
                        sessionProgress = 1;
                        mHandler.removeMessages(LOOPERPROGRESS);
                        mHandler.sendEmptyMessage(LOOPERPROGRESS);

                        try {
                            if (selectData != null) {
                                List<CameraVideoDetailListBean.VideosBean> lists = AppFileUtil.loadDeviceVideoJson(selectData);
                                if (lists != null && !lists.isEmpty()) {
                                    for (int j = 0; j < datasSelect.size(); j++) {
                                        boolean isHas = false;
                                        for (int i = 0; i < lists.size(); i++) {
                                            try {
                                                if (lists.get(i).deviceId.equals(datasSelect.get(j).deviceId) && lists.get(i).link.equals(datasSelect.get(j).link)) {
                                                    isHas = true;
                                                    break;
                                                }

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        if (!isHas) {
                                            lists.add(datasSelect.get(j));
                                        }
                                    }
                                } else {
                                    lists.addAll(datasSelect);
                                }

                                AppFileUtil.saveDeviceVideoJson(selectData, lists);
                            }

                            EventBus.getDefault().post(new CameraFilesDownloadedEvent());

                            //同步文件到媒体库相册里
                            if (!files.isEmpty()) {
                                String downloadPath = AppFileUtil.getWeewaDownloadRoot() + "/" + deviceId.hashCode();
                                List<String> scanFileList = new ArrayList<>();
                                for (String file : files) {
                                    if (file.endsWith(".mp4")) {
                                        scanFileList.add(downloadPath + file);
                                    }
                                }
                                if(!scanFileList.isEmpty()) {
                                    MediaScannerHelper.scanFiles(HotmeApplication.getContext(), scanFileList);
                                }
                            }

                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void sessionError(int id, @NonNull String errMsg) {
                    AppTrace.d(TAG, "sessionError=" + id + ", errMsg=" + errMsg + ", connectSessionId=" + connectSessionId);
                    if(connectSessionId == id) {
                        transferError = errMsg;
                        if(!NetworkManager.getImpl().isConnectedSoftAp()) {
                            sessionState = SessionStatus.UNCONNECT;
                        } else {
                            sessionState = SessionStatus.FAILED;
                        }
                        mHandler.removeMessages(LOOPERPROGRESS);
                        mHandler.sendEmptyMessage(LOOPERPROGRESS);
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }


    }

    public long getStartDownTime() {
        return startDownTime;
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String path) {
        basePath = path;
    }

    private static final class HolderClass {
        private static final ConnectLogic INSTANCE = new ConnectLogic();
    }

    public static synchronized ConnectLogic getImpl() {
        return HolderClass.INSTANCE;
    }

    public String getConnectPort() {
        return connectPort;
    }

    public void setConnectPort(String connectPort) {
        this.connectPort = connectPort;
    }

    public String getConnectBaseurl() {
        return connectBaseurl;
    }

    public void setConnectBaseurl(String connectBaseurl) {
        this.connectBaseurl = connectBaseurl;
    }

    public List<CameraVideoDetailListBean.VideosBean> getDatasSelect() {
        return datasSelect;
    }


    public void setConnected(boolean isConnected) {
        this.isServerConnected = isConnected;
    }

    public boolean isConnected() {
        return this.isServerConnected;
    }

    /**
     * 终止传输文件
     */
    public void unReceive() {
        int previousId = connectSessionId;
        connectSessionId = -1;
        WeewaLib.Companion.shared().stop(previousId);
        sessionState = SessionStatus.WAITE;
    }

    public void addCustomFiles(List<String> filePath) {
        files.clear();
        fileSize = 0;
        files.addAll(filePath);
    }

    public void addFiles(List<CameraVideoDetailListBean.VideosBean> datasSelect1) {
        this.datasSelect.clear();
        this.datasSelect.addAll(datasSelect1);
        fileSize = 0;
        files.clear();
        for(CameraVideosBean cameraVideoBean : datasSelect1) {
            files.add(cameraVideoBean.link);
            fileSize += cameraVideoBean.size;
        }
    }

    public void addFiles(ArrayList<CameraVideoDetailListBean.VideosBean> datasSelect1, List<String> files1) {
//        fileSize = afileSize;
        files.clear();
        files.addAll(files1);
        this.datasSelect.clear();
        this.datasSelect.addAll(datasSelect1);


    }

    public void addFiles(CameraVideoListBean.VideosBean myData, long afileSize, ArrayList<CameraVideoDetailListBean.VideosBean> datasSelect1, List<String> files1) {
        selectData = myData;
        fileSize = afileSize;
        files.clear();
        files.addAll(files1);
        this.datasSelect.clear();
        this.datasSelect.addAll(datasSelect1);

    }

    /**
     * 传输文件
     */
    public boolean toReceive() {
        if(isDownLoading()) return true;
        if(!isServerConnected) {
            AppTrace.d(TAG, "file server is disconnected...");
            return false;
        }

        if (files.isEmpty()) {
            AppTrace.d(TAG, "download file list is empty");
            transferError = "请选择需要下载的视频";
            return false;
        }
        String downloadPath = AppFileUtil.getWeewaDownloadRoot();
        java.io.File destination = new java.io.File(downloadPath, String.valueOf(deviceId.hashCode()));
        if((!destination.exists() && !destination.mkdirs()) || !destination.canWrite()) {
            transferError = "手机下载目录不可写，请检查存储权限和剩余空间";
            sessionState = SessionStatus.FAILED;
            mHandler.sendEmptyMessage(LOOPERPROGRESS);
            return false;
        }
        transferError = "";
        sessionState = SessionStatus.CONNECT;
        sessionProgress = sessionProgressLast = 0;
        rateSize = 0;
        lastTime = System.currentTimeMillis();
        lastRefreshTime = 0;
        Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(WeewaLib.Companion.shared().getContext()), downloadPath + "/" + deviceId.hashCode());

        AppTrace.d(TAG, "download file size =" + files.size());
        AppTrace.d(TAG, "download file =" + GsonUtils.GsonString(files));
        connectSessionId = WeewaLib.Companion.shared().receive(connectIp, "", files, false);
        AppTrace.d(TAG, "toReceive=" + connectSessionId);
        try {
            if (selectData == null) {
                return true;
            }
            selectData.deviceId = deviceId;
            List<CameraVideoListBean.VideosBean> lists = AppFileUtil.loadAllDevicesJson();
            boolean hasData = false;
            for(int i = 0; i < lists.size(); i++) {
                try {
                    if(selectData.deviceId.equals(lists.get(i).deviceId) && selectData.path.equals(lists.get(i).path)) {
                        hasData = true;
                        break;
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            if(!hasData) {
                lists.add(selectData);
            }
            AppFileUtil.saveAllDevicesJson(lists);

        } catch(Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean isConnectIng() {
        return isConnectIng;
    }

    public void setConnectIng(boolean connectIng) {
        isConnectIng = connectIng;
    }

    public boolean isDownLoading() {
        return sessionState == SessionStatus.CONNECT || sessionState == SessionStatus.START
            || sessionState == SessionStatus.DOWNLOADING
            || (sessionState == SessionStatus.WAITE && connectSessionId >= 0);
    }

    public String getTransferError() { return transferError; }

    public double getSessionProgress() {
        return sessionProgress;
    }

    public long getRateSize() {
        return rateSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public SessionStatus getSessionState() {
        return sessionState;
    }

    public void setSessionState(SessionStatus sessionState) {
        this.sessionState = sessionState;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getSsidPwd() {
        return ssidPwd;
    }

    public void setSsidPwd(String ssidPwd) {
        this.ssidPwd = ssidPwd;
    }

    public String getConnectIp() {
        return connectIp;
    }

    public void setConnectIp(String connectIp) {
        this.connectIp = connectIp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }


    /**
     * 连接异常
     */
    ModalDialog errorDialog;

    public void showConnectError(Context context) {
        if(isActiveDisconnection) {
            isActiveDisconnection = false;
            return;
        }
        if(null == errorDialog) {
            if(unConnectDialog != null) {
                unConnectDialog.dismiss();
            }
            errorDialog = new ModalDialog(context);
            errorDialog.setPositiveButton(context.getString(R.string.reconnect), v -> {
                errorDialog = null;
                checkWiFiAndGetVideList(context);
            });
            errorDialog.setNegativeButton(context.getString(R.string.cancel), v -> errorDialog = null);
            errorDialog.show();
        } else {
            if(errorDialog != null && errorDialog.isShowing()) {
                return;
            }
            errorDialog.show();
        }

    }

    ModalDialog unConnectDialog;
    boolean isActiveDisconnection = false;//主动断开

    /**
     * 断开链接
     *
     * @param context
     */
    public void unConnectDialog(Context context) {
        if(null == unConnectDialog) {
            unConnectDialog = new ModalDialog(context);
            unConnectDialog.setPositiveButton(context.getString(R.string.disconnect), v -> {
                unConnectDialog = null;
//                            WifiUtils.getInstance(context).disconnectNetWork();
            });
            unConnectDialog.setNegativeButton(context.getString(R.string.cancel), v -> {
                unConnectDialog = null;
//                            WifiUtils.getInstance(context).disconnectNetWork();
            });
            unConnectDialog.setMessage(context.getString(R.string.disconnect_title));

            unConnectDialog.show();
        }

    }

    public static void checkWiFiAndGetVideList(Context context) {
        String curSsid = WifiManager.getImpl().getConnectWifiSsid();
        if(TextUtils.equals(curSsid, ConnectLogic.getImpl().getSsid())) {
//            getVideoList();
            ConnectLogic.getImpl().setConnectIng(true);
            EventBus.getDefault().post(new NetWorkForQEvent(NetWorkForQEvent.AVAILABLE));
        } else {
            ToastUtil.s(String.format(context.getString(R.string.connect_soft_ap_for_download_file), ConnectLogic.getImpl().getSsid()));
            new Handler().postDelayed(() -> {
                context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }, 1500);
        }
    }

    public void resetState() {
        connectSessionId = -1;
        transferError = "";
        sessionState = SessionStatus.WAITE;
        fileSize = 0;
        files.clear();
        datasSelect.clear();
    }
}
