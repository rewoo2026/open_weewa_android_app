package com.xh.hotme.bluetooth;


import android.util.Log;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraSettings;
import com.xh.hotme.bean.CameraVideoDateListBean;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoFilterListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bean.GoalBean;
import com.xh.hotme.bean.GoalStatusBean;
import com.xh.hotme.bean.LiveStatusResultBean;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.bean.NetChangeInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.PostProc;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bean.StorageBean;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.bean.GetEthernetSettingsResponseBean;
import com.xh.hotme.bluetooth.event.AddGoalEvent;
import com.xh.hotme.bluetooth.event.AutoUploadStartEvent;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import com.xh.hotme.bluetooth.event.AvsRtmpSetUrlEvent;
import com.xh.hotme.bluetooth.event.BeepEvent;
import com.xh.hotme.bluetooth.event.CompetitionLabelEvent;
import com.xh.hotme.bluetooth.event.DeleteFileEvent;
import com.xh.hotme.bluetooth.event.DeleteVideoDirEvent;
import com.xh.hotme.bluetooth.event.DeleteWatermarkEvent;
import com.xh.hotme.bluetooth.event.DeviceEnergyInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceLowerPoweroffEvent;
import com.xh.hotme.bluetooth.event.DeviceStorageInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceUnbindEvent;
import com.xh.hotme.bluetooth.event.DeviceUsageInfoEvent;
import com.xh.hotme.bluetooth.event.DeviceWeewaStartEvent;
import com.xh.hotme.bluetooth.event.FTArecordStartEvent;
import com.xh.hotme.bluetooth.event.FTArecordStopEvent;
import com.xh.hotme.bluetooth.event.FTAudioStartEvent;
import com.xh.hotme.bluetooth.event.FTAudioStopEvent;
import com.xh.hotme.bluetooth.event.FTFastChargeEvent;
import com.xh.hotme.bluetooth.event.FanSpeedEvent;
import com.xh.hotme.bluetooth.event.FecRtmpAddUrlEvent;
import com.xh.hotme.bluetooth.event.FecRtmpPreviewStartEvent;
import com.xh.hotme.bluetooth.event.FecRtmpPreviewStopEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveAllUrlEvent;
import com.xh.hotme.bluetooth.event.FecRtmpRemoveUrlEvent;
import com.xh.hotme.bluetooth.event.GameScoreAddEvent;
import com.xh.hotme.bluetooth.event.GameScoreClearEvent;
import com.xh.hotme.bluetooth.event.GameScoreRemoveEvent;
import com.xh.hotme.bluetooth.event.GameScoreStatAddEvent;
import com.xh.hotme.bluetooth.event.GameStatAddEvent;
import com.xh.hotme.bluetooth.event.GameStatEditEvent;
import com.xh.hotme.bluetooth.event.GameStatListEvent;
import com.xh.hotme.bluetooth.event.GameStatRemoveEvent;
import com.xh.hotme.bluetooth.event.GetOSDSettingsEvent;
import com.xh.hotme.bluetooth.event.GetPlanUsageEvent;
import com.xh.hotme.bluetooth.event.GoalListEvent;
import com.xh.hotme.bluetooth.event.HighTempEvent;
import com.xh.hotme.bluetooth.event.LiveStatusResultEvent;
import com.xh.hotme.bluetooth.event.MatchTimePauseEvent;
import com.xh.hotme.bluetooth.event.MatchTimeResumeEvent;
import com.xh.hotme.bluetooth.event.MatchTimeResetEvent;
import com.xh.hotme.bluetooth.event.NetChangeEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.OSDControlEvent;
import com.xh.hotme.bluetooth.event.OTAInstallSucEvent;
import com.xh.hotme.bluetooth.event.OTAStartDownloadEvent;
import com.xh.hotme.bluetooth.event.OTAStartInstallEvent;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import com.xh.hotme.bluetooth.event.PingEvent;
import com.xh.hotme.bluetooth.event.PostProcProgressEvent;
import com.xh.hotme.bluetooth.event.PostProcStartEvent;
import com.xh.hotme.bluetooth.event.PostProcStateEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RebootEvent;
import com.xh.hotme.bluetooth.event.RecordStatusResultEvent;
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.event.RemoveGoalEvent;
import com.xh.hotme.bluetooth.event.RkIpcEvent;
import com.xh.hotme.bluetooth.event.RkIpcStopEvent;
import com.xh.hotme.bluetooth.event.SceneSetupEvent;
import com.xh.hotme.bluetooth.event.SetApnEvent;
import com.xh.hotme.bluetooth.event.SetEthernetDhcpEvent;
import com.xh.hotme.bluetooth.event.SetEthernetStaticEvent;
import com.xh.hotme.bluetooth.event.SetTeamNameEvent;
import com.xh.hotme.bluetooth.event.SetTrainingModeEvent;
import com.xh.hotme.bluetooth.event.SetVideoNameEvent;
import com.xh.hotme.bluetooth.event.SetVolumeEvent;
import com.xh.hotme.bluetooth.event.SetWatermarkEvent;
import com.xh.hotme.bluetooth.event.SoftApCloseEvent;
import com.xh.hotme.bluetooth.event.SoftApOpenEvent;
import com.xh.hotme.bluetooth.event.SoftwareInfoEvent;
import com.xh.hotme.bluetooth.event.SpeedTestEvent;
import com.xh.hotme.bluetooth.event.StartUsbEvent;
import com.xh.hotme.bluetooth.event.StopUsbEvent;
import com.xh.hotme.bluetooth.event.UpdateSNEvent;
import com.xh.hotme.bluetooth.event.UploadCalibEvent;
import com.xh.hotme.bluetooth.event.UploadLogEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsUpdateEvent;
import com.xh.hotme.bluetooth.listener.IBleActiveLoginListener;
import com.xh.hotme.bluetooth.listener.IBleActiveSmsListener;
import com.xh.hotme.bluetooth.listener.IBleConnectListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceNameNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleDeviceUsageInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleLoginTokenListener;
import com.xh.hotme.bluetooth.listener.IBleNetworkInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleRkipcListener;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSimEnableNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleSoftApNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleUnbindListener;
import com.xh.hotme.bluetooth.listener.IBleVideoDetailListNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleVideoFilterNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleVideoListNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleVideoUploadNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.camera.wifi.IWifiNotifyListener;
import com.xh.hotme.camera.wifi.WifiInfo;
import com.xh.hotme.storage.VideoManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class BluetoothHandle {
    private static final String TAG = BluetoothHandle.class.getSimpleName();


    private static List<IBleScanNotifyListener> _scanNotifyListeners;
    private static List<IBleConnectListener> _scanConnectListeners;

    private static List<IWifiNotifyListener> _wifiNotifyListeners;

    private static List<IRecordControlListener> _playNotifyListeners;
    private static List<IBleDeviceUsageInfoNotifyListener> _deviceUsageInfoNotifyListeners;
    private static List<IBleDeviceInfoNotifyListener> _deviceInfoNotifyListeners;
    private static List<IBleWeewaInfoNotifyListener> _weewaInfoNotifyListeners;
    private static List<IBleSoftApNotifyListener> _softApNotifyListeners;

    private static List<IBleNetworkInfoNotifyListener> _networkNotifyListeners;

    public static List<IBleDeviceNameNotifyListener> _deviceNameListeners = new ArrayList<>();
    public static List<IBleActiveSmsListener> _activeSmsListeners = new ArrayList<>();
    public static List<IBleActiveLoginListener> _activeLoginListeners = new ArrayList<>();

    private static List<IBleVideoListNotifyListener> _videoListNotifyListeners;
    private static List<IBleVideoDetailListNotifyListener> _videoDetailListNotifyListeners;

    public static List<IBleVideoUploadNotifyListener> _videoUploadListeners = new ArrayList<>();
    public static List<IBleSimEnableNotifyListener> _simEnableListeners = new ArrayList<>();
    public static List<IBleRkipcListener> _rkipcListeners = new ArrayList<>();


    public static void handle(JSONObject jsonRoot, BaseBluetoothBean requestBean) {
        if(jsonRoot == null || jsonRoot.getString(BleConstants.BLE_CMD) == null)
            return;

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
            CameraMonitor.getImpl().setOnline(true);
        }

        String cmd = jsonRoot.getString(BleConstants.BLE_CMD);
        switch(cmd) {
            case BleConstants.BLE_CMD_WIFILIST:
                postWifiList(jsonRoot);
                break;
            case BleConstants.BLE_CMD_WIFI_SETUP:
                postWifiSetup(jsonRoot);
                break;
            case BleConstants.BLE_CMD_WIFI_TURN_OFF:
                postWifiTurnOff(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RECORDER_START:
                postRecordStart(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RECORDER_STOP:
                postRecordStop(jsonRoot);
                break;
            case BleConstants.BLE_CMD_STOP_RECORD_COZ_LOW_STORAGE:
                postStopRecordCozLowStorage(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RECORDER_STATUS:
                postRecordStatus(jsonRoot);
                break;
            case BleConstants.BLE_CMD_STORAGE:
                postStorageInfo(jsonRoot);
                break;
            case BleConstants.BLE_CMD_ENERGY:
                postEnergyInfo(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SOFTAP_OPEN:
                postSoftApOpen(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SOFTAP_CLOSE:
                postSoftApClose(jsonRoot);
                break;
            case BleConstants.BLE_CMD_NETWORK_STATUS:
                postNetworkStatus(jsonRoot);
                break;
            case BleConstants.BLE_CMD_POWER_ON:
            case BleConstants.BLE_CMD_WEEWA_START:
                postWeewaStart(jsonRoot);
                break;
            case BleConstants.BLE_CMD_POWER_OFF:
                postPowerOff(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RESTART:
                postReboot(jsonRoot);
                break;
            case BleConstants.BLE_CMD_USAGE_INFO:
                postUsageInfo(jsonRoot);
                break;
            case BleConstants.BLE_CMD_DELETE_WATERMARK:
                postDeleteWatermark(jsonRoot);
                break;
            case BleConstants.BLE_CMD_GET_PLAN_USAGE:
                postGetPlanUsage(jsonRoot);
                break;
            case BleConstants.BLE_CMD_WEEWA_INFO:
                postWeewaInfo(jsonRoot);
                break;

            case BleConstants.BLE_CMD_DEVICE_INFO:
                postDeviceInfo(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_VOLUME:
                postSetVolume(jsonRoot);
                break;
            case BleConstants.BLE_CMD_UPDATE_NAME:
                postDeviceUpdateName(jsonRoot);
                break;
            case BleConstants.BLE_CMD_DEVICE_ACTIVE_SMS:
                postActiveSms(jsonRoot);
                break;
            case BleConstants.BLE_CMD_DEVICE_ACTIVE_BIND:
                postActiveBind(jsonRoot);
                break;
            case BleConstants.BLE_CMD_DEVICE_UNBIND_SMS:
                postDeviceUnBindSms(jsonRoot);
                break;
            case BleConstants.BLE_CMD_DEVICE_UNBIND:
                postDeviceUnBind(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_TOKEN:
                postSetToken(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_LIST:
                postVideoList(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_DETAIL_LIST:
                postVideoDetailList(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_FILTER:
                postVideoFilter(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_DIR_UPLOAD:
                postVideoUpload(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SIM_OPEN:
                postSimOpen(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SIM_CLOSE:
                postSimClose(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_TRAINING_MODE:
                postSetTrainingMode(jsonRoot);
                break;
            case BleConstants.BLE_CMD_HIGH_TEMP:
                postHighTemp(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RKIPC_STOP:
                postRkipcStop(jsonRoot);
                break;
            case BleConstants.BLE_CMD_RKIPC_START:
                postRkipc(jsonRoot);
                break;

            case BleConstants.BLE_CMD_START_AUTO_UPLOAD:
                postStartAutoUpload(jsonRoot);
                break;

            case BleConstants.BLE_CMD_AUTO_UPLOAD_STATUS:
                postAutoUploadStatus(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_APN:
                postSetApn(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SOFTWARE_INFO:
                postSoftwareInfo(jsonRoot);
                break;
            case BleConstants.BLE_CMD_LOW_POWER_OFF:
                postLowerPoweroff(jsonRoot);
                break;
            case BleConstants.BLE_CMD_UPDATE_SN:
                postDeviceUpdateSN(jsonRoot);
                break;
            case BleConstants.BLE_CMD_POST_PROC_STATE:
                postPostProc(jsonRoot);
                break;
            case BleConstants.BLE_CMD_START_POST_PROC:
                postStartPostProc(jsonRoot);
                break;

            case BleConstants.BLE_CMD_POST_PROC_PROGRESS:
                postPostProcProgress(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_DELETE_SUB:
                postDeleteFileSub(jsonRoot);
                break;
            case BleConstants.BLE_CMD_VIDEO_DELETE:
                postDeleteVideoDir(jsonRoot);
                break;
            case BleConstants.BLE_CMD_ADD_GOAL:
                postAddGoal(jsonRoot);
                break;
            case BleConstants.BLE_CMD_REMOVE_GOAL:
                postRemoveGoal(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_FAN_SPEED:
                postFanSpeed(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_LABEL:
                postCompetitionLabel(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_VIDEO_NAME:
                postSetVideoName(jsonRoot);
                break;
            case BleConstants.BLE_CMD_OSD_CONTROL:
                postOSDControl(jsonRoot);
                break;
            case BleConstants.BLE_CMD_GET_OSD_SETTINGS:
                postGetOSDSettings(jsonRoot);
                break;
            case BleConstants.BLE_CMD_START_USB:
                postStartUsb(jsonRoot);
                break;
            case BleConstants.BLE_CMD_STOP_USB:
                postStopUsb(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_ADD_SCORE_STAT:
                postCompetitionScoreStatAdd(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_ADD_SCORE:
                postCompetitionScoreAdd(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_REMOVE_SCORE:
                postCompetitionScoreRemove(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_CLEAR_SCORE:
                postCompetitionScoreClear(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_ADD_STAT:
                postCompetitionStatAdd(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_REMOVE_STAT:
                postCompetitionStatRemove(jsonRoot);
                break;
            case BleConstants.BLE_CMD_COMPETITION_EDIT_STAT:
                postCompetitionStatEdit(jsonRoot);
                break;
            case BleConstants.BLE_CMD_STAT_LIST:
                postGameStatList(jsonRoot, requestBean);
                break;

            case BleConstants.BLE_CMD_AVS_RTMP_URL_SET:
                postAvsRtmpSetUrl(jsonRoot);
                break;

            case BleConstants.BLE_CMD_FEC_LIVE_URL_ADD:
                postFecLiveAddUrl(jsonRoot);
                break;

            case BleConstants.BLE_CMD_FEC_LIVE_URL_REMOVE:
                postFecLiveRemoveUrl(jsonRoot);
                break;

            case BleConstants.BLE_CMD_FEC_LIVE_URL_REMOVE_ALL:
                postFecLiveRemoveAllUrl(jsonRoot);
                break;

            case BleConstants.BLE_CMD_FEC_PREVIEW_START:
                postFecPreviewStart(jsonRoot);
                break;

            case BleConstants.BLE_CMD_FEC_PREVIEW_STOP:
                postFecPreviewStop(jsonRoot);
                break;
            case BleConstants.BLE_CMD_LIVE_STATUS:
                postLiveStatus(jsonRoot);
                break;
            case BleConstants.BLE_CMD_GET_OTA_STATUS:
                postOTAStatus(jsonRoot);
                break;
            case BleConstants.BLE_CMD_START_OTA_DOWNLOAD:
                postOTAStartDownload(jsonRoot);
                break;
            case BleConstants.BLE_CMD_START_OTA:
                postOTAStartInstall(jsonRoot);
                break;
            case BleConstants.BLE_CMD_OTA_UPGRADED:
                postOTAInstallSuccess(jsonRoot);
                break;

            case BleConstants.BLE_CMD_UPDATE_SETTINGS:
                postUpdateSettings(jsonRoot);
                break;

            case BleConstants.BLE_CMD_GET_SETTINGS:
                postGetSettings(jsonRoot);
                break;

            case BleConstants.BLE_CMD_NETWORK_CHANGE:
                postNetChange(jsonRoot);
                break;

            case BleConstants.BLE_CMD_GET_SETTINGS_EX:
                postGetSettingsEx(jsonRoot);
                break;

            case BleConstants.BLE_CMD_UPDATE_SETTINGS_EX:
                postUpdateSettingsEx(jsonRoot);
                break;

            case BleConstants.BLE_CMD_SET_TEAM_NAME:
                postSetTeamName(jsonRoot);
                break;

            case BleConstants.BLE_CMD_BEEP:
                postBeep(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_PING:
                postPing(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_AUDIO_START:
                postFactoryTestAudioStart(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_AUDIO_STOP:
                postFactoryTestAudioStop(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_FAST_CHARGE:
                postFactoryTestFastCharge(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_ARECORD_START:
                postFactoryTestArecordStart(jsonRoot);
                break;
            case BleConstants.BLE_CMD_FACTORY_TEST_ARECORD_STOP:
                postFactoryTestArecordStop(jsonRoot);
                break;
            case BleConstants.BLE_CMD_UPLOAD_LOG:
                postUploadLog(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_WATERMARK:
                postSetWatermark(jsonRoot);
                break;
            case BleConstants.BLE_CMD_MATCH_TIME_PAUSE:
                postMatchTimePause(jsonRoot);
                break;
            case BleConstants.BLE_CMD_MATCH_TIME_RESUME:
                postMatchTimeResume(jsonRoot);
                break;
            case BleConstants.BLE_CMD_MATCH_TIME_RESET:
                postMatchTimeReset(jsonRoot);
            case BleConstants.BLE_CMD_UPLOAD_CALIB:
                postUploadCalib(jsonRoot);
                break;
            case BleConstants.BLE_CMD_SET_ETHERNET_DHCP:
                postSetEthernetDhcp(jsonRoot);
                break;
            case BleConstants.BLE_CMD_4G_TEST_SPEED:
                postS4GTestSpeed(jsonRoot);
                break;

            default:
                AppTrace.t(TAG, "unknow cmd: " + cmd);
                break;
        }
    }

    public static void postWifiList(@NonNull JSONObject jsonRoot) {
        final List<WifiInfo> wifis = new ArrayList<>();

        //Utils.saveFile("ble_wifilist.txt", data);
        JSONArray content = jsonRoot.getJSONArray(BleConstants.BLE_KEY_WIFILIST);
        if(content != null) {
            for(int i = 0, size = content.size(); i < size; i++) {
                JSONObject wifiJson = (JSONObject) content.get(i);
                String ssid = wifiJson.getString("s");
                String bs = wifiJson.getString("bs");
                int frequency = wifiJson.getIntValue("f");
                int rs = wifiJson.getIntValue("rs");
                if(ssid == null || ssid.isEmpty())
                    continue;
                WifiInfo wifi = new WifiInfo();
                wifi.setBssid(bs);
                wifi.setSsid(ssid);
                wifi.setFrequency("" + frequency);
                wifi.setSignalLevel("" + rs);
                wifi.setFlags("");
                wifis.add(wifi);
            }
        }

        onWifiListNotify(wifis);
    }

    public static void postWifiSetup(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
            onWifiSetupNotify(1, "");
        } else {
            onWifiSetupNotify(0, status);
        }
    }

    public static void postWifiTurnOff(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
            onWifiTurnOffNotify(1, "");
        } else {
            onWifiTurnOffNotify(0, status);
        }
    }


    public static void postRecordStart(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
            onRecordStartNotify();
        } else {
            onRecordExceptionNotify(status);
        }
    }


    public static void postRecordStop(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
            onRecordStopNotify();
        } else {
            onRecordExceptionNotify(status);
        }
    }

    public static void postStopRecordCozLowStorage(@NonNull JSONObject jsonRoot) {
        EventBus.getDefault().post(new RecordStoppedOnLowStorageEvent());
    }

    public static void postRecordStatus(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
            RecordStatusResultBean recordStatusResultBean = GsonUtils.GsonToBean(jsonRoot.toString(), RecordStatusResultBean.class);

            onRecordStatusNotify(recordStatusResultBean);

            EventBus.getDefault().post(new RecordStatusResultEvent(1, recordStatusResultBean));
        } else {
            onRecordStatusNotify(new RecordStatusResultBean());

            EventBus.getDefault().post(new RecordStatusResultEvent(0, null));
        }
    }

    public static void postStorageInfo(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_FREE);
        if(suc) {
            long total = jsonRoot.getLong(BleConstants.BLE_KEY_TOTAL);
            long used = jsonRoot.getLong(BleConstants.BLE_KEY_USED);
            long free = jsonRoot.getLong(BleConstants.BLE_KEY_FREE);
            StorageBean storageBean = new StorageBean(total, used, free);
            onStorageInfoNotify(storageBean);
            EventBus.getDefault().post(new DeviceStorageInfoEvent(storageBean));
        } else {

        }
    }

    public static void postEnergyInfo(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_FREE);
        if(suc) {
            int free = jsonRoot.getIntValue(BleConstants.BLE_KEY_FREE);
            onEnergyNotify(free);
            EventBus.getDefault().post(new DeviceEnergyInfoEvent(free));
        } else {
            onEnergyNotify(-1);
            EventBus.getDefault().post(new DeviceEnergyInfoEvent(-1));
        }
    }

    public static void postWeewaInfo(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_DEVICE_NAME);
        if(suc) {
            WeewaInfo weewaInfo = new WeewaInfo();
            long timeStamp = System.currentTimeMillis();
            if(jsonRoot.containsKey(BleConstants.BLE_KEY_TIMESTAMP)){
                timeStamp = jsonRoot.getLong(BleConstants.BLE_KEY_TIMESTAMP);
            }
            weewaInfo._t = timeStamp;

            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.device_name = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_NAME);
            deviceInfo.device_id = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_ID);
            deviceInfo.device_model = jsonRoot.getString("device_model");
            if (TextUtils.isEmpty(deviceInfo.device_model)) {
                deviceInfo.device_model = jsonRoot.getString("deviceModel");
            }
            deviceInfo.mac = jsonRoot.getString(BleConstants.BLE_KEY_MAC);
            deviceInfo.active_status = jsonRoot.getIntValue(BleConstants.BLE_KEY_DEVICE_ACTIVE_STATUS);
            deviceInfo.active_time = "" + jsonRoot.getLong(BleConstants.BLE_KEY_DEVICE_ACTIVE_TIME);
            deviceInfo.account = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_ACCOUNT);
            deviceInfo.volume = jsonRoot.getIntValue(BleConstants.BLE_KEY_VOLUME);
            deviceInfo.version = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_VERSION);
            deviceInfo.ota_version = jsonRoot.getString(BleConstants.BLE_KEY_OTA_VERSION);
            deviceInfo.ota_version_code = jsonRoot.getInteger(BleConstants.BLE_KEY_OTA_VERSION_CODE);
            deviceInfo.nonce = jsonRoot.getString(BleConstants.BLE_KEY_NONCE);
            deviceInfo.adb = jsonRoot.getIntValue(BleConstants.BLE_KEY_ADB_ENABLE);
            if(jsonRoot.containsKey(BleConstants.BLE_KEY_AUTO_UPLOAD)) {
                deviceInfo.auto_upload = jsonRoot.getInteger(BleConstants.BLE_KEY_AUTO_UPLOAD);
            }
            weewaInfo.d = deviceInfo;

            NetworkBean networkBean = new NetworkBean();
            networkBean._t = timeStamp;
            networkBean.setReachable(jsonRoot.getIntValue(BleConstants.BLE_KEY_REACHABLE) != 0);
            boolean wifi = jsonRoot.containsKey(BleConstants.BLE_KEY_WIFI);
            if(wifi) {
                JSONObject wifiJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_WIFI);
                NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
                networkStatus.ip = wifiJson.getString(BleConstants.BLE_KEY_IP);
                networkStatus.st = wifiJson.getIntValue(BleConstants.BLE_KEY_ST);
                networkStatus.s = wifiJson.getString(BleConstants.BLE_KEY_SOFTAP_SSID);
                networkBean.setWifi(networkStatus);
            }

            boolean eth = jsonRoot.containsKey(BleConstants.BLE_KEY_ETH);
            if(eth) {
                JSONObject ethJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_ETH);
                NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
                networkStatus.ip = ethJson.getString(BleConstants.BLE_KEY_IP);
                networkStatus.st = ethJson.getIntValue(BleConstants.BLE_KEY_ST);
                networkBean.setEth(networkStatus);

                GetEthernetSettingsResponseBean ethbean = new GetEthernetSettingsResponseBean();
                ethbean.ip = ethJson.getString(BleConstants.BLE_KEY_IP);
                ethbean.dns = ethJson.getString(BleConstants.BLE_KEY_DNS);
                ethbean.netmask = ethJson.getString(BleConstants.BLE_KEY_NETMASK);
                ethbean.gateway = ethJson.getString(BleConstants.BLE_KEY_GATEWAY);
                ethbean.dhcp = ethJson.getIntValue(BleConstants.BLE_KEY_DHCP);
                weewaInfo.ethbean = ethbean;

            }

            boolean sim = jsonRoot.containsKey(BleConstants.BLE_KEY_SIM);
            if(sim) {
                JSONObject simJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_SIM);
                NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
                networkStatus.ip = simJson.getString(BleConstants.BLE_KEY_IP);
                networkStatus.st = simJson.getIntValue(BleConstants.BLE_KEY_ST);
                networkStatus.has_card = simJson.getIntValue(BleConstants.BLE_KEY_HAS_CARD);
                networkStatus.connected = simJson.getIntValue(BleConstants.BLE_KEY_CONNECTED);
                networkStatus.rssi_lv = simJson.getIntValue(BleConstants.BLE_KEY_RSSI_LEVEL);
                networkStatus.imei = simJson.getString(BleConstants.BLE_KEY_IMEI);
                boolean hasICCID = simJson.containsKey(BleConstants.BLE_KEY_ICCID);
                if(hasICCID){
                    networkStatus.iccid = simJson.getString(BleConstants.BLE_KEY_ICCID);
                }
                networkStatus.apn = simJson.getString(BleConstants.BLE_KEY_APN);
                networkBean.setSim(networkStatus);
            }

            boolean ap = jsonRoot.containsKey(BleConstants.BLE_KEY_SOFTAP);
            if(ap) {
                JSONObject simJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_SOFTAP);
                NetworkBean.ApInfo apInfo = new NetworkBean.ApInfo();
                apInfo.ssid = simJson.getString(BleConstants.BLE_KEY_SOFTAP_SSID);
                apInfo.password = simJson.getString(BleConstants.BLE_KEY_SOFTAP_PASSWORD);
                apInfo.st = simJson.getIntValue(BleConstants.BLE_KEY_ST);
                networkBean.setAp(apInfo);
            }

            int port = jsonRoot.getIntValue(BleConstants.BLE_KEY_HTTP);
            networkBean.setHttp(port);

            weewaInfo.n = networkBean;

            CameraManager.setNetworkInfo(networkBean);

            CameraManager.setDeviceInfo(deviceInfo);

            AppTrace.d(TAG, "postWeewaInfo end");
            onWeewaInfoNotify(weewaInfo);

            onNetworkStatus(networkBean);

            if(networkBean.getAp() != null) {
                onSoftApStatusNotify(networkBean.getAp().st);
            }

            // 如果相机那边还没有同步过默认配置, 则发送默认配置给相机
            boolean defSynced = jsonRoot.getIntValue(BleConstants.BLE_KEY_DEFAULT_SETTINGS_SYNCED) != 0;
            if(!defSynced) {
                CameraManager.syncDefaultConfig();
            }

//            EventBus.getDefault().post(new WeewaInfoEvent(weewaInfo));

        } else {
            onWeewaInfoFail();
//            EventBus.getDefault().post(new WeewaInfoEvent(null));
        }

    }


    public static void postDeviceInfo(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_DEVICE_NAME);
        if(suc) {
            long timeStamp = System.currentTimeMillis();
            if(jsonRoot.containsKey(BleConstants.BLE_KEY_TIMESTAMP)){
                timeStamp = jsonRoot.getLong(BleConstants.BLE_KEY_TIMESTAMP);
            }
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo._t = timeStamp;
            deviceInfo.device_name = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_NAME);
            deviceInfo.device_id = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_ID);
            deviceInfo.device_model = jsonRoot.getString("device_model");
            if (TextUtils.isEmpty(deviceInfo.device_model)) {
                deviceInfo.device_model = jsonRoot.getString("deviceModel");
            }
            deviceInfo.mac = jsonRoot.getString(BleConstants.BLE_KEY_MAC);
            deviceInfo.active_status = jsonRoot.getIntValue(BleConstants.BLE_KEY_DEVICE_ACTIVE_STATUS);
            deviceInfo.active_time = "" + jsonRoot.getLong(BleConstants.BLE_KEY_DEVICE_ACTIVE_TIME);
            deviceInfo.account = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_ACCOUNT);
            deviceInfo.volume = jsonRoot.getIntValue(BleConstants.BLE_KEY_VOLUME);
            deviceInfo.version = jsonRoot.getString(BleConstants.BLE_KEY_DEVICE_VERSION);
            deviceInfo.ota_version = jsonRoot.getString(BleConstants.BLE_KEY_OTA_VERSION);
            deviceInfo.ota_version_code = jsonRoot.getInteger(BleConstants.BLE_KEY_OTA_VERSION_CODE);
            deviceInfo.nonce = jsonRoot.getString(BleConstants.BLE_KEY_NONCE);
            deviceInfo.adb = jsonRoot.getIntValue(BleConstants.BLE_KEY_ADB_ENABLE);
            if(jsonRoot.containsKey(BleConstants.BLE_KEY_AUTO_UPLOAD)) {
                deviceInfo.auto_upload = jsonRoot.getInteger(BleConstants.BLE_KEY_AUTO_UPLOAD);
            }
            onDeviceInfoNotify(deviceInfo);

            CameraManager.setDeviceInfo(deviceInfo);

            // 如果相机那边还没有同步过默认配置, 则发送默认配置给相机
            boolean defSynced = jsonRoot.getIntValue(BleConstants.BLE_KEY_DEFAULT_SETTINGS_SYNCED) != 0;
            if(!defSynced) {
                CameraManager.syncDefaultConfig();
            }
        } else {
            onDeviceInfoFail();
        }
    }

    public static void postSoftApOpen(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                String ssid = jsonRoot.getString(BleConstants.BLE_KEY_SOFTAP_SSID);
                String password = jsonRoot.getString(BleConstants.BLE_KEY_SOFTAP_PASSWORD);
                onSoftApOpenNotify(ssid, password);
            } else {
                onSoftApOpenFailNotify(status);
            }
        } else {
            onSoftApOpenFailNotify("");
        }
    }

    public static void postSoftApClose(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            onSoftApCloseNotify();
        } else {
            onSoftApCloseFailNotify();
        }
    }

    public static void postNetworkStatus(@NonNull JSONObject jsonRoot) {
        long timeStamp = System.currentTimeMillis();
        if(jsonRoot.containsKey(BleConstants.BLE_KEY_TIMESTAMP)){
            timeStamp = jsonRoot.getLong(BleConstants.BLE_KEY_TIMESTAMP);
        }
        NetworkBean networkBean = new NetworkBean();
        networkBean._t = timeStamp;
        networkBean.setReachable(jsonRoot.getIntValue(BleConstants.BLE_KEY_REACHABLE) != 0);
        boolean wifi = jsonRoot.containsKey(BleConstants.BLE_KEY_WIFI);
        if(wifi) {
            JSONObject wifiJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_WIFI);
            NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
            networkStatus.ip = wifiJson.getString(BleConstants.BLE_KEY_IP);
            networkStatus.st = wifiJson.getIntValue(BleConstants.BLE_KEY_ST);
            networkStatus.s = wifiJson.getString(BleConstants.BLE_KEY_SSID);
            networkBean.setWifi(networkStatus);
        }

        boolean eth = jsonRoot.containsKey(BleConstants.BLE_KEY_ETH);
        if(eth) {
            JSONObject ethJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_ETH);
            NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
            networkStatus.ip = ethJson.getString(BleConstants.BLE_KEY_IP);
            networkStatus.st = ethJson.getIntValue(BleConstants.BLE_KEY_ST);
            networkBean.setEth(networkStatus);
        }

        boolean sim = jsonRoot.containsKey(BleConstants.BLE_KEY_SIM);
        if(sim) {
            JSONObject simJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_SIM);
            NetworkBean.NetworkStatus networkStatus = new NetworkBean.NetworkStatus();
            networkStatus.ip = simJson.getString(BleConstants.BLE_KEY_IP);
            networkStatus.st = simJson.getIntValue(BleConstants.BLE_KEY_ST);
            networkStatus.has_card = simJson.getIntValue(BleConstants.BLE_KEY_HAS_CARD);
            networkStatus.connected = simJson.getIntValue(BleConstants.BLE_KEY_CONNECTED);
            networkStatus.rssi_lv = simJson.getIntValue(BleConstants.BLE_KEY_RSSI_LEVEL);
            networkStatus.imei = simJson.getString(BleConstants.BLE_KEY_IMEI);
            networkStatus.apn = simJson.getString(BleConstants.BLE_KEY_APN);
            boolean hasICCID = simJson.containsKey(BleConstants.BLE_KEY_ICCID);
            if(hasICCID){
                networkStatus.iccid = simJson.getString(BleConstants.BLE_KEY_ICCID);
            }
            networkBean.setSim(networkStatus);
        }

        boolean ap = jsonRoot.containsKey(BleConstants.BLE_KEY_SOFTAP);
        if(ap) {
            JSONObject simJson = jsonRoot.getJSONObject(BleConstants.BLE_KEY_SOFTAP);
            NetworkBean.ApInfo apInfo = new NetworkBean.ApInfo();
            apInfo.ssid = simJson.getString(BleConstants.BLE_KEY_SOFTAP_SSID);
            apInfo.password = simJson.getString(BleConstants.BLE_KEY_SOFTAP_PASSWORD);
            apInfo.st = simJson.getIntValue(BleConstants.BLE_KEY_ST);
            networkBean.setAp(apInfo);
        }

        int port = jsonRoot.getIntValue(BleConstants.BLE_KEY_HTTP);
        networkBean.setHttp(port);

        CameraManager.setNetworkInfo(networkBean);

        onNetworkStatus(networkBean);

        if(networkBean.getAp() != null) {
            onSoftApStatusNotify(networkBean.getAp().st);
        }
    }


    public static void onScanning(boolean isBackground) {
        if(_scanNotifyListeners != null) {
            for(IBleScanNotifyListener listener : _scanNotifyListeners) {
                listener.onScanning(isBackground);
            }
        }
    }


    public static void onScanTimeout() {
        if(_scanNotifyListeners != null) {
            for(IBleScanNotifyListener listener : _scanNotifyListeners) {
                listener.onTimeout();
            }
        }
    }

    public static void onScanEnd() {

        if(_scanNotifyListeners != null) {
            for(IBleScanNotifyListener listener : _scanNotifyListeners) {
                listener.onScanEnd();
            }
        }
    }

    public static void onLeScan(Device blueDevice) {
        if(_scanNotifyListeners != null) {
            for(IBleScanNotifyListener listener : _scanNotifyListeners) {
                listener.onLeScan(blueDevice);
            }
        }
    }

    public static void addScanNotifyListener(IBleScanNotifyListener listener) {
        if(_scanNotifyListeners == null) {
            _scanNotifyListeners = new ArrayList<>();
        }
        _scanNotifyListeners.add(listener);
    }

    public static void removeScanNotifyListener(IBleScanNotifyListener listener) {
        if(_scanNotifyListeners == null) {
            _scanNotifyListeners = new ArrayList<>();
        }
        _scanNotifyListeners.remove(listener);
    }

    public static void addConnectNotifyListener(IBleConnectListener listener) {
        if(_scanConnectListeners == null) {
            _scanConnectListeners = new ArrayList<>();
        }
        _scanConnectListeners.add(listener);
    }

    public static void removeConnectNotifyListener(IBleConnectListener listener) {
        if(_scanConnectListeners == null) {
            _scanConnectListeners = new ArrayList<>();
        }
        _scanConnectListeners.remove(listener);
    }

    public static void onConnecting(Device device) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onConnecting(device);
            }
        }
    }

    public static void onConnectSuccess(int status) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onConnectSuccess(null, status);
            }
        }
    }

    public static void onConnectFailure(String address, String msg) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onConnectFailure(address, msg);
            }
        }
    }

    public static void onConnectTimeoutNotify(String address) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onConnectTimeOut(address);
            }
        }
    }

    public static void onDisConnecting(String address) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onDisConnecting(address);
            }
        }
    }

    public static void onDisConnectSuccess(String address, int status) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onDisConnectSuccess(address, status);
            }
        }
    }

    public static void onServiceDiscoverySucceed(String address, int status) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                listener.onServiceDiscoverySucceed(address, status);
            }
        }
    }

    public static void onServiceDiscoveryFailed(String address, String message) {
        if(_scanConnectListeners != null) {
            for(IBleConnectListener listener : _scanConnectListeners) {
                if(listener != null) {
                    listener.onServiceDiscoveryFailed(address, message);
                }
            }
        }
    }

    public static void addWifiNotifyListener(IWifiNotifyListener listener) {
        if(_wifiNotifyListeners == null) {
            _wifiNotifyListeners = new ArrayList<>();
        }
        _wifiNotifyListeners.add(listener);
    }

    public static void removeWifiNotifyListener(IWifiNotifyListener listener) {
        if(_wifiNotifyListeners == null) {
            _wifiNotifyListeners = new ArrayList<>();
        }
        _wifiNotifyListeners.remove(listener);
    }

    public static void onWifiListNotify(List<WifiInfo> data) {
        if(_wifiNotifyListeners != null && !_wifiNotifyListeners.isEmpty()) {
            for(IWifiNotifyListener listener : _wifiNotifyListeners) {
                if(listener != null) {
                    listener.onWifiList(data);
                }
            }
        }
    }

    public static void onWifiSetupNotify(int status, String message) {
        if(_wifiNotifyListeners != null && !_wifiNotifyListeners.isEmpty()) {
            for(IWifiNotifyListener listener : _wifiNotifyListeners) {
                if(listener != null) {
                    listener.onSetupStatus(status, message);
                }
            }
        }
    }


    public static void onWifiTurnOffNotify(int status, String message) {
        if(_wifiNotifyListeners != null && !_wifiNotifyListeners.isEmpty()) {
            for(IWifiNotifyListener listener : _wifiNotifyListeners) {
                if(listener != null) {
                    listener.onTurnOff(status, message);
                }
            }
        }
    }

    public static void addSoftApNotifyListener(IBleSoftApNotifyListener listener) {
        if(_softApNotifyListeners == null) {
            _softApNotifyListeners = new ArrayList<>();
        }
        _softApNotifyListeners.add(listener);
    }

    public static void removeSoftApNotifyListener(IBleSoftApNotifyListener listener) {
        if(_softApNotifyListeners == null) {
            _softApNotifyListeners = new ArrayList<>();
        }
        _softApNotifyListeners.remove(listener);
    }

    public static void onSoftApOpenNotify(String ssid, String password) {
        //sync cache
        NetworkBean networkBean = CameraManager.getNetworkInfo(null);
        if(networkBean != null && networkBean.getAp() != null) {
            networkBean.getAp().ssid = ssid;
            networkBean.getAp().password = password;
            networkBean.getAp().st = 1;
            EventBus.getDefault().post(new SoftApOpenEvent(ssid, password));
        }

        if(_softApNotifyListeners != null) {
            for(IBleSoftApNotifyListener listener : _softApNotifyListeners) {
                listener.onSoftApStart(ssid, password);
            }
        }
    }

    public static void onSoftApOpenFailNotify(String msg) {
        if(_softApNotifyListeners != null) {
            for(IBleSoftApNotifyListener listener : _softApNotifyListeners) {
                listener.onSoftApStartFail(msg);
            }
        }
    }

    public static void onSoftApCloseNotify() {
        NetworkBean networkBean = CameraManager.getNetworkInfo(null);
        if(networkBean != null && networkBean.getAp() != null) {
            networkBean.getAp().st = 0;
            EventBus.getDefault().post(new SoftApCloseEvent());
        }

        if(_softApNotifyListeners != null) {
            for(IBleSoftApNotifyListener listener : _softApNotifyListeners) {
                listener.onSoftApStop();
            }
        }
    }

    public static void onSoftApCloseFailNotify() {
        if(_softApNotifyListeners != null) {
            for(IBleSoftApNotifyListener listener : _softApNotifyListeners) {
                listener.onSoftApStopFail("");
            }
        }
    }

    public static void onSoftApStatusNotify(int status) {
        if(_softApNotifyListeners != null) {
            for(IBleSoftApNotifyListener listener : _softApNotifyListeners) {
                listener.onSoftApStatus(status);
            }
        }
    }

    public static void addNetworkNotifyListener(IBleNetworkInfoNotifyListener listener) {
        if(_networkNotifyListeners == null) {
            _networkNotifyListeners = new ArrayList<>();
        }
        _networkNotifyListeners.add(listener);
    }

    public static void removeNetworkNotifyListener(IBleNetworkInfoNotifyListener listener) {
        if(_networkNotifyListeners == null) {
            _networkNotifyListeners = new ArrayList<>();
        }
        _networkNotifyListeners.remove(listener);
    }

    public static void onNetworkStatus(NetworkBean networkBean) {
        if(_networkNotifyListeners != null) {
            for(IBleNetworkInfoNotifyListener listener : _networkNotifyListeners) {
                listener.onNetworkStatus(networkBean);
            }
        }
        EventBus.getDefault().post(new NetworkStatusEvent(networkBean));
    }

    public static void postPowerOff(@NonNull JSONObject jsonRoot) {

        EventBus.getDefault().post(new PowerOffEvent());
    }

    public static void postReboot(@NonNull JSONObject jsonRoot) {

        EventBus.getDefault().post(new RebootEvent());
    }


    public static void postUsageInfo(@NonNull JSONObject jsonRoot) {
        DeviceUsageInfo usageInfo = new DeviceUsageInfo();

        JSONObject storage = jsonRoot.getJSONObject("s");
        long total = storage.getLong(BleConstants.BLE_KEY_TOTAL);
        long used = storage.getLong(BleConstants.BLE_KEY_USED);
        long free = storage.getLong(BleConstants.BLE_KEY_FREE);
        StorageBean storageBean = new StorageBean(total, used, free);
        usageInfo.storage = storageBean;
        usageInfo.energy = jsonRoot.getIntValue("e");
        usageInfo.temperature = jsonRoot.getIntValue("t");
        usageInfo.charging = jsonRoot.getIntValue("charging");
        onUsageNotify(usageInfo);
        CameraManager.setUsageInfo(usageInfo);
        EventBus.getDefault().post(new DeviceUsageInfoEvent(usageInfo));
    }

    public static void postGetPlanUsage(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            int record_used = jsonRoot.getIntValue("record_used");
            int rtmp_used = jsonRoot.getIntValue("rtmp_used");
            EventBus.getDefault().post(new GetPlanUsageEvent(rtmp_used, record_used));
        }
    }

    public static void postSetWatermark(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        EventBus.getDefault().post(new SetWatermarkEvent(status));
    }

    public static void postDeleteWatermark(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        EventBus.getDefault().post(new DeleteWatermarkEvent(status));
    }

    public static void postActiveSms(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_BIND) || status.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_LOGIN) || status.equalsIgnoreCase(BleConstants.BLE_SMS_SUCCESS_DEVICE_BOUND)) {
                onActiveSms(status);
            } else {
                onActiveSmsFail(status);
            }
        } else {
            onActiveSmsFail("unknow");
        }
    }

    public static void postActiveBind(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                String token = jsonRoot.getString(BleConstants.BLE_KEY_TOKEN);
                LoginResultBean loginResultBean = new LoginResultBean(token);
                onActive(loginResultBean);
            } else {
                onActiveFail(status);
            }
        } else {
            onActiveFail("unknow");
        }
    }

    public static void postDeviceUnBindSms(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onUnbindSms();
            } else {
                onUnbindSmsFail(status);
            }
        } else {
            onUnbindSmsFail("unknow");
        }
    }

    public static void postDeviceUnBind(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(DeviceUnbindResult.isSuccess(status)) {
                onUnbind();

                // also post an event
                EventBus.getDefault().post(new DeviceUnbindEvent());
            } else {
                onUnbindFail(status);
            }
        } else {
            onUnbindFail("unknow");
        }
    }

    public static void postSetVolume(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new SetVolumeEvent(1));
            } else {
                EventBus.getDefault().post(new SetVolumeEvent(0));
            }
        } else {
            EventBus.getDefault().post(new SetVolumeEvent(0));
        }
    }

    public static void postDeviceUpdateName(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onUpdateDeviceName();
            } else {
                onUpdateDeviceNameFail(status);
            }
        } else {
            onUpdateDeviceNameFail("unknow");
        }
    }

    public static void postDeviceUpdateSN(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new UpdateSNEvent(1, ""));
            } else {
                EventBus.getDefault().post(new UpdateSNEvent(0, ""));
            }
        } else {
            EventBus.getDefault().post(new UpdateSNEvent(0, ""));
        }
    }

    public static void postSetToken(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onSyncToken();
            } else {
                onSyncTokenFail(status);
            }
        } else {
            onSyncTokenFail("unknow");
        }
    }

    public static void postVideoList(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_VIDEOS);
        if(suc) {
            CameraVideoListBean bean = new Gson().fromJson(jsonRoot.toJSONString(), CameraVideoListBean.class);
            if(bean != null && bean.videos != null) {
                for (CameraVideoListBean.VideosBean video : bean.videos) {
                    video.deviceId=bean.device_id;
                    video.delete_protocol=bean.video_delete_protocol;
                }
                EventBus.getDefault().post(new com.xh.hotme.bluetooth.event.CameraVideoSnapshotEvent(bean));
                onVideoList(bean.videos);
            } else {
                onVideoList(null);
            }
        } else {
            onVideoListFail("unknow");
        }
    }

    public static void postVideoDetailList(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_VIDEOS);
        if(suc) {
            CameraVideoDetailListBean bean = new Gson().fromJson(jsonRoot.toJSONString(), CameraVideoDetailListBean.class);
            if(bean != null && bean.videos != null) {
                onVideoDetailList(bean.videos);
            } else {
                onVideoDetailList(null);
            }
        } else {
            onVideoDetailListFail("unknow");
        }
    }

    public static void postVideoFilter(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_GROUP);
        if(suc) {
            CameraVideoFilterListBean bean = new Gson().fromJson(jsonRoot.toJSONString(), CameraVideoFilterListBean.class);
            if(bean != null && bean.groups != null && !bean.groups.isEmpty()) {

                List<CameraVideoDateListBean> data = VideoManager.convertVideoInfo(bean.groups);
                if(!data.isEmpty()) {
                    int type = data.get(0).groups.get(0).type;
                    VideoManager.saveVideoFilterInfo(type, bean.groups);
                    onVideoFilterList(type, data);
                } else {
                    onVideoFilterList(0, data);
                }

            } else {
                onVideoFilterListFail("no data");
            }
        } else {
            onVideoFilterListFail("unknow");
        }
    }

    public static void postVideoUpload(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onVideoUpload();
            } else {
                onVideoUploadFail(status);
            }
        } else {
            onVideoUploadFail("unknow");
        }
    }


    public static void onRecordStartNotify() {
        if(_playNotifyListeners != null && !_playNotifyListeners.isEmpty()) {
            for(IRecordControlListener listener : _playNotifyListeners) {
                listener.onRecordVideoStart();
            }
        }
    }

    public static void onRecordStopNotify() {
        if(_playNotifyListeners != null) {
            for(IRecordControlListener listener : _playNotifyListeners) {
                if(listener != null) {
                    listener.onRecordVideoStop();
                }
            }
        }
    }

    public static void onRecordStatusNotify(RecordStatusResultBean recordStatusResultBean) {
        if(_playNotifyListeners != null) {
            for(IRecordControlListener listener : _playNotifyListeners) {
                listener.onRecordVideoStatusUpdate(recordStatusResultBean);
            }
        }
    }

    public static void onRecordExceptionNotify(String message) {
        if(_playNotifyListeners != null) {
            for(IRecordControlListener listener : _playNotifyListeners) {
                listener.onRecordException(message);
            }
        }
    }


    public static void onStorageInfoNotify(StorageBean storageBean) {
        if(_deviceUsageInfoNotifyListeners != null) {
            for(IBleDeviceUsageInfoNotifyListener listener : _deviceUsageInfoNotifyListeners) {
                listener.onStorageInfo(storageBean);
            }
        }
    }

    public static void onEnergyNotify(int energy) {
        if(_deviceUsageInfoNotifyListeners != null) {
            for(IBleDeviceUsageInfoNotifyListener listener : _deviceUsageInfoNotifyListeners) {
                listener.onEnergy(energy);
            }
        }
    }

    public static void onWeewaInfoNotify(WeewaInfo deviceInfo) {
        if(_weewaInfoNotifyListeners != null) {
            for(IBleWeewaInfoNotifyListener listener : _weewaInfoNotifyListeners) {
                listener.onWeewaInfo(deviceInfo);
            }
        }
    }

    public static void onWeewaInfoFail() {
        if(_weewaInfoNotifyListeners != null) {
            for(IBleWeewaInfoNotifyListener listener : _weewaInfoNotifyListeners) {
                listener.onWeewaInfo(null);
            }
        }
    }


    public static void onDeviceInfoNotify(DeviceInfo deviceInfo) {
        if(_deviceInfoNotifyListeners != null) {
            for(IBleDeviceInfoNotifyListener listener : _deviceInfoNotifyListeners) {
                listener.onDeviceInfo(deviceInfo);
            }
        }
    }

    public static void onDeviceInfoFail() {
        if(_deviceInfoNotifyListeners != null) {
            for(IBleDeviceInfoNotifyListener listener : _deviceInfoNotifyListeners) {
                listener.onDeviceInfoFail();
            }
        }
    }


    public static void onUsageNotify(DeviceUsageInfo info) {
        if(_deviceUsageInfoNotifyListeners != null) {
            for(IBleDeviceUsageInfoNotifyListener listener : _deviceUsageInfoNotifyListeners) {
                listener.onDeviceInfo(info);
            }
        }
    }

    public static void addPlayNotifyListener(IRecordControlListener listener) {
        if(_playNotifyListeners == null) {
            _playNotifyListeners = new ArrayList<>();
        }
        if(!_playNotifyListeners.contains(listener)) {
            _playNotifyListeners.add(listener);
        }
    }

    public static void removePlayNotifyListener(IRecordControlListener listener) {
        if(_playNotifyListeners == null) {
            _playNotifyListeners = new ArrayList<>();
        }
        _playNotifyListeners.remove(listener);
    }


    public static void addDeviceUsageInfoNotifyListener(IBleDeviceUsageInfoNotifyListener listener) {
        if(_deviceUsageInfoNotifyListeners == null) {
            _deviceUsageInfoNotifyListeners = new ArrayList<>();
        }
        _deviceUsageInfoNotifyListeners.add(listener);
    }

    public static void removeDeviceUsageInfoNotifyListener(IBleDeviceUsageInfoNotifyListener listener) {
        if(_deviceUsageInfoNotifyListeners == null) {
            _deviceUsageInfoNotifyListeners = new ArrayList<>();
        }
        _deviceUsageInfoNotifyListeners.remove(listener);
    }

    public static void addDeviceInfoNotifyListener(IBleDeviceInfoNotifyListener listener) {
        if(_deviceInfoNotifyListeners == null) {
            _deviceInfoNotifyListeners = new ArrayList<>();
        }
        _deviceInfoNotifyListeners.add(listener);
    }

    public static void removeDeviceInfoNotifyListener(IBleDeviceInfoNotifyListener listener) {
        if(_deviceInfoNotifyListeners == null) {
            _deviceInfoNotifyListeners = new ArrayList<>();
        }
        _deviceInfoNotifyListeners.remove(listener);
    }


    public static void addWeewaInfoNotifyListener(IBleWeewaInfoNotifyListener listener) {
        if(_weewaInfoNotifyListeners == null) {
            _weewaInfoNotifyListeners = new ArrayList<>();
        }
        _weewaInfoNotifyListeners.add(listener);
    }

    public static void removeWeewaInfoNotifyListener(IBleWeewaInfoNotifyListener listener) {
        if(_weewaInfoNotifyListeners == null) {
            _weewaInfoNotifyListeners = new ArrayList<>();
        }
        _weewaInfoNotifyListeners.remove(listener);
    }

    public static void addVideoUploadNotifyListener(IBleVideoUploadNotifyListener listener) {
        if(_videoUploadListeners == null) {
            _videoUploadListeners = new ArrayList<>();
        }
        _videoUploadListeners.add(listener);
    }

    public static void removeVideoUploadNotifyListener(IBleVideoUploadNotifyListener listener) {
        if(_videoUploadListeners == null) {
            _videoUploadListeners = new ArrayList<>();
        }
        _videoUploadListeners.remove(listener);
    }


    public static void onVideoUpload() {
        if(_videoUploadListeners != null) {
            for(IBleVideoUploadNotifyListener listener : _videoUploadListeners) {
                listener.onVideoUpload();
            }
        }
    }

    public static void onVideoUploadFail(String msg) {
        if(_videoUploadListeners != null) {
            for(IBleVideoUploadNotifyListener listener : _videoUploadListeners) {
                listener.onVideoUploadFail(msg);
            }
        }
    }

    public static List<IBleVideoFilterNotifyListener> _videoFilterListeners = new ArrayList<>();

    public static void addVideoFilterNotifyListener(IBleVideoFilterNotifyListener listener) {
        if(_videoFilterListeners == null) {
            _videoFilterListeners = new ArrayList<>();
        }
        _videoFilterListeners.add(listener);
    }

    public static void removeVideoFilterNotifyListener(IBleVideoFilterNotifyListener listener) {
        if(_videoFilterListeners == null) {
            _videoFilterListeners = new ArrayList<>();
        }
        _videoFilterListeners.remove(listener);
    }

    public static void onVideoFilterList(int type, List<CameraVideoDateListBean> data) {
        if(_videoFilterListeners != null) {
            for(IBleVideoFilterNotifyListener listener : _videoFilterListeners) {
                listener.onVideoFilterList(type, data);
            }
        }
    }

    public static void onVideoFilterListFail(String msg) {
        if(_videoFilterListeners != null) {
            for(IBleVideoFilterNotifyListener listener : _videoFilterListeners) {
                listener.onVideoFilterListFail(msg);
            }
        }
    }


    public static void onActiveSms(String code) {
        if(_activeSmsListeners != null) {
            for(IBleActiveSmsListener listener : _activeSmsListeners) {
                listener.onSendActiveSms(code);
            }
        }
    }


    public static void onActiveSmsFail(String message) {
        if(message.contains("Could not resolve host")){
            message = HotmeApplication.getContext().getString(R.string.info_camera_send_sms_fail_without_network);
        }
        if(_activeSmsListeners != null) {
            for(IBleActiveSmsListener listener : _activeSmsListeners) {
                listener.onSendActiveSmsFail(message);
            }
        }
    }

    public static void onActive(LoginResultBean loginBean) {
        if(_activeLoginListeners != null) {
            for(IBleActiveLoginListener listener : _activeLoginListeners) {
                listener.onActive(loginBean);
            }
        }
    }

    public static void onActiveFail(String msg) {
        if(_activeLoginListeners != null) {
            for(IBleActiveLoginListener listener : _activeLoginListeners) {
                listener.onActiveFail(msg);
            }
        }
    }


    public static void onSimOpen() {
        if(_simEnableListeners != null) {
            for(IBleSimEnableNotifyListener listener : _simEnableListeners) {
                listener.onSimOpen();
            }
        }
    }

    public static void onSimOpenFail(String msg) {
        if(_simEnableListeners != null) {
            for(IBleSimEnableNotifyListener listener : _simEnableListeners) {
                listener.onSimOpenFail(msg);
            }
        }
    }


    public static void onSimClose() {
        if(_simEnableListeners != null) {
            for(IBleSimEnableNotifyListener listener : _simEnableListeners) {
                listener.onSimClose();
            }
        }
    }

    public static void onSimCloseFail(String msg) {
        if(_simEnableListeners != null) {
            for(IBleSimEnableNotifyListener listener : _simEnableListeners) {
                listener.onSimCloseFail(msg);
            }
        }
    }


    public static List<IBleUnbindListener> _unbindListeners = new ArrayList<>();

    public static void addDeviceUnbindNotifyListener(IBleUnbindListener listener) {
        if(_unbindListeners == null) {
            _unbindListeners = new ArrayList<>();
        }
        _unbindListeners.add(listener);
    }

    public static void removeDeviceUnbindNotifyListener(IBleUnbindListener listener) {
        if(_unbindListeners == null) {
            _unbindListeners = new ArrayList<>();
        }
        _unbindListeners.remove(listener);
    }

    public static void onUnbindSms() {
        if(_unbindListeners != null) {
            for(IBleUnbindListener listener : _unbindListeners) {
                listener.onSendUnbindSms();
            }
        }
    }


    public static void onUnbindSmsFail(String message) {
        if(_unbindListeners != null) {
            for(IBleUnbindListener listener : _unbindListeners) {
                listener.onSendUnbindSmsFail(message);
            }
        }
    }

    public static void onUnbind() {
        if(_unbindListeners != null) {
            for(IBleUnbindListener listener : _unbindListeners) {
                listener.onUnbind();
            }
        }
    }

    public static void onUnbindFail(String msg) {
        if(_unbindListeners != null) {
            for(IBleUnbindListener listener : _unbindListeners) {
                listener.onUnbindFail(msg);
            }
        }
    }


    public static void addDeviceActiveSmsNotifyListener(IBleActiveSmsListener listener) {
        if(_activeSmsListeners == null) {
            _activeSmsListeners = new ArrayList<>();
        }
        _activeSmsListeners.add(listener);
    }

    public static void removeDeviceActiveSmsNotifyListener(IBleActiveSmsListener listener) {
        if(_activeSmsListeners == null) {
            _activeSmsListeners = new ArrayList<>();
        }
        _activeSmsListeners.remove(listener);
    }


    public static void addDeviceActiveLoginNotifyListener(IBleActiveLoginListener listener) {
        if(_activeLoginListeners == null) {
            _activeLoginListeners = new ArrayList<>();
        }
        _activeLoginListeners.add(listener);
    }

    public static void removeDeviceActiveLoginNotifyListener(IBleActiveLoginListener listener) {
        if(_activeLoginListeners == null) {
            _activeLoginListeners = new ArrayList<>();
        }
        _activeLoginListeners.remove(listener);
    }


    public static List<IBleLoginTokenListener> _loginTokenListeners = new ArrayList<>();


    public static void addLoginTokenNotifyListener(IBleLoginTokenListener listener) {
        if(_loginTokenListeners == null) {
            _loginTokenListeners = new ArrayList<>();
        }
        _loginTokenListeners.add(listener);
    }

    public static void removeLoginTokenNotifyListener(IBleLoginTokenListener listener) {
        if(_loginTokenListeners == null) {
            _loginTokenListeners = new ArrayList<>();
        }
        _loginTokenListeners.remove(listener);
    }


    public static void onSyncToken() {
        if(_loginTokenListeners != null) {
            for(IBleLoginTokenListener listener : _loginTokenListeners) {
                listener.onSyncToken();
            }
        }
    }


    public static void onSyncTokenFail(String msg) {
        if(_loginTokenListeners != null) {
            for(IBleLoginTokenListener listener : _loginTokenListeners) {
                listener.onSyncTokenFail(msg);
            }
        }
    }


    public static void addDeviceNameNotifyListener(IBleDeviceNameNotifyListener listener) {
        if(_deviceNameListeners == null) {
            _deviceNameListeners = new ArrayList<>();
        }
        _deviceNameListeners.add(listener);
    }

    public static void removeDeviceNameNotifyListener(IBleDeviceNameNotifyListener listener) {
        if(_deviceNameListeners == null) {
            _deviceNameListeners = new ArrayList<>();
        }
        _deviceNameListeners.remove(listener);
    }

    public static void onUpdateDeviceName() {
        if(_deviceNameListeners != null) {
            for(IBleDeviceNameNotifyListener listener : _deviceNameListeners) {
                listener.onDeviceName();
            }
        }
    }

    public static void onUpdateDeviceNameFail(String msg) {
        if(_deviceNameListeners != null) {
            for(IBleDeviceNameNotifyListener listener : _deviceNameListeners) {
                listener.onDeviceNameFail(msg);
            }
        }
    }


    public static void addVideoListNotifyListener(IBleVideoListNotifyListener listener) {
        if(_videoListNotifyListeners == null) {
            _videoListNotifyListeners = new ArrayList<>();
        }
        if(!_videoListNotifyListeners.contains(listener)) {
            _videoListNotifyListeners.add(listener);
        }
    }

    public static void removeVideoListNotifyListener(IBleVideoListNotifyListener listener) {
        if(_videoListNotifyListeners == null) {
            _videoListNotifyListeners = new ArrayList<>();
        }
        _videoListNotifyListeners.remove(listener);
    }

    public static void addVideoDetailListNotifyListener(IBleVideoDetailListNotifyListener listener) {
        if(_videoDetailListNotifyListeners == null) {
            _videoDetailListNotifyListeners = new ArrayList<>();
        }
        if(!_videoDetailListNotifyListeners.contains(listener)) {
            _videoDetailListNotifyListeners.add(listener);
        }
    }

    public static void removeVideoDetailListNotifyListener(IBleVideoDetailListNotifyListener listener) {
        if(_videoDetailListNotifyListeners == null) {
            _videoDetailListNotifyListeners = new ArrayList<>();
        }
        _videoDetailListNotifyListeners.remove(listener);
    }


    public static void onVideoList(List<CameraVideoListBean.VideosBean> list) {
        if(_videoListNotifyListeners != null) {
            for(IBleVideoListNotifyListener listener : _videoListNotifyListeners) {
                listener.onVideoList(list);
            }
        }
    }

    public static void onVideoListFail(String msg) {
        if(_videoListNotifyListeners != null) {
            for(IBleVideoListNotifyListener listener : _videoListNotifyListeners) {
                listener.onVideoListFail(msg);
            }
        }
    }

    public static void onVideoDetailList(List<CameraVideoDetailListBean.VideosBean> list) {
        if(_videoDetailListNotifyListeners != null) {
            for(IBleVideoDetailListNotifyListener listener : _videoDetailListNotifyListeners) {
                listener.onVideoDetailList(list);
            }
        }
    }

    public static void onVideoDetailListFail(String msg) {
        if(_videoDetailListNotifyListeners != null) {
            for(IBleVideoDetailListNotifyListener listener : _videoDetailListNotifyListeners) {
                listener.onVideoDetailListFail(msg);
            }
        }
    }


    public static void addSimEnableNotifyListener(IBleSimEnableNotifyListener listener) {
        if(_simEnableListeners == null) {
            _simEnableListeners = new ArrayList<>();
        }
        _simEnableListeners.add(listener);
    }

    public static void removeSimEnableNotifyListener(IBleSimEnableNotifyListener listener) {
        if(_simEnableListeners == null) {
            _simEnableListeners = new ArrayList<>();
        }
        _simEnableListeners.remove(listener);
    }


    public static void postSimOpen(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onSimOpen();
            } else {
                onSimOpenFail(status);
            }
        } else {
            onSimOpenFail("unknow");
        }
    }


    public static void postSimClose(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                onSimClose();
            } else {
                onSimCloseFail(status);
            }
        } else {
            onSimCloseFail("unknow");
        }
    }


    public static void postSetTrainingMode(@NonNull JSONObject jsonRoot) {
        EventBus.getDefault().post(new SetTrainingModeEvent());
    }

    public static void postHighTemp(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            float temp = jsonRoot.getFloat(BleConstants.BLE_KEY_RESULT);
            EventBus.getDefault().post(new HighTempEvent(temp));
        }
    }

    public static void addRkipcNotifyListener(IBleRkipcListener listener) {
        if(_rkipcListeners == null) {
            _rkipcListeners = new ArrayList<>();
        }
        _rkipcListeners.add(listener);
    }

    public static void removeRkipcNotifyListener(IBleRkipcListener listener) {
        if(_rkipcListeners == null) {
            _rkipcListeners = new ArrayList<>();
        }
        _rkipcListeners.remove(listener);
    }

    public static void postRkipc(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new RkIpcEvent(1));
            } else {
                EventBus.getDefault().post(new RkIpcEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new RkIpcEvent(0, "unknow"));
        }
    }

    public static void postRkipcStop(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new RkIpcStopEvent(1));
            } else {
                EventBus.getDefault().post(new RkIpcStopEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new RkIpcEvent(0, "unknow"));
        }
    }

    public static void postAutoUploadStatus(@NonNull JSONObject jsonRoot) {
        AutoUploadStatusEvent e = new AutoUploadStatusEvent();
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                e.uploading = jsonRoot.getIntValue(BleConstants.BLE_KEY_ST) != 0;
                e.progress = jsonRoot.getIntValue(BleConstants.BLE_KEY_PROGRESS);
                e.isForceEnd = jsonRoot.getIntValue(BleConstants.BLE_KEY_FORCE_END)==1;
                e.hasUpload = jsonRoot.getIntValue(BleConstants.BLE_KEY_HAS_UPLOAD)==1;
                e.failed =  (List<String>) jsonRoot.get(BleConstants.BLE_KEY_UPLOAD_FAILED);
            }
        }
        EventBus.getDefault().post(e);
    }


    public static void postStartAutoUpload(@NonNull JSONObject jsonRoot) {
        AutoUploadStartEvent e = new AutoUploadStartEvent();
        EventBus.getDefault().post(e);
    }


    public static void postSoftwareInfo(@NonNull JSONObject jsonRoot) {
        SoftwareInfoEvent e = new SoftwareInfoEvent();
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                e.idle_v = jsonRoot.getString(BleConstants.BLE_KEY_IDLE_VERSION);
                e.daemon_v = jsonRoot.getString(BleConstants.BLE_KEY_DAEMON_VERSION);
                e.rkipc_v = jsonRoot.getString(BleConstants.BLE_KEY_RKIPC_VERSION);
            }
        }
        EventBus.getDefault().post(e);
    }

    public static void postSetEthernetDhcp(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        EventBus.getDefault().post(new SetEthernetDhcpEvent(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS) ? 1 : 0));
    }

    public static void postS4GTestSpeed(@NonNull JSONObject jsonRoot) {
        Log.d("asx","asx+" + jsonRoot.toString());
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                Double upload_speed = jsonRoot.getDoubleValue(BleConstants.BLE_KEY_4G_UPLOAD_SPEED);
                Double download_speed = jsonRoot.getDoubleValue(BleConstants.BLE_KEY_4G_DOWNLOAD_SPEED);
                EventBus.getDefault().post(new SpeedTestEvent(1,download_speed,upload_speed));
            }else{
                EventBus.getDefault().post(new SpeedTestEvent(0,0,0));
            }
        }else{
            EventBus.getDefault().post(new SpeedTestEvent(0,0,0));
        }

    }


    public static void postSetApn(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new SetApnEvent(1));
            } else {
                EventBus.getDefault().post(new SetApnEvent(0));
            }
        } else {
            EventBus.getDefault().post(new SetApnEvent(-1));
        }
    }

    public static void postWeewaStart(@NonNull JSONObject jsonRoot) {
        EventBus.getDefault().post(new DeviceWeewaStartEvent());
    }

    public static void postLowerPoweroff(@NonNull JSONObject jsonRoot) {
        EventBus.getDefault().post(new DeviceLowerPoweroffEvent());
    }

    public static void postPostProc(@NonNull JSONObject jsonRoot) {
        PostProc postProc = new PostProc();
        List<String> waiting = (List<String>) jsonRoot.get(BleConstants.BLE_KEY_WAIT);
        List<String> onGoing = (List<String>) jsonRoot.get(BleConstants.BLE_KEY_ONGOING);
        List<String> done = (List<String>) jsonRoot.get(BleConstants.BLE_KEY_DONE);
        List<String> error = (List<String>) jsonRoot.get(BleConstants.BLE_KEY_ERROR);

        postProc.wait = waiting;
        postProc.ongoing = onGoing;
        postProc.done = done;
        postProc.error = error;

        EventBus.getDefault().post(new PostProcStateEvent(postProc));

    }

    public static void postStartPostProc(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new PostProcStartEvent(1));
            } else {
                EventBus.getDefault().post(new PostProcStartEvent(0));
            }
        } else {
            EventBus.getDefault().post(new PostProcStartEvent(0));
        }

    }

    public static void postPostProcProgress(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_PROGRESS);
        if(suc) {
            int progress = jsonRoot.getIntValue(BleConstants.BLE_KEY_PROGRESS);
            EventBus.getDefault().post(new PostProcProgressEvent(progress));
        } else {
            EventBus.getDefault().post(new PostProcProgressEvent(-2));
        }
    }

    public static void postDeleteFileSub(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new DeleteFileEvent(1));
            } else {
                EventBus.getDefault().post(new DeleteFileEvent(0));
            }
        } else {
            EventBus.getDefault().post(new DeleteFileEvent(-1));
        }
    }

    public static void postDeleteVideoDir(@NonNull JSONObject jsonRoot) {
        String requestId=jsonRoot.getString("request_id");
        if (requestId!=null && !requestId.isEmpty()) {
            EventBus.getDefault().post(new com.xh.hotme.bluetooth.event.CameraVideoDeleteResultEvent(
                    requestId,jsonRoot.getString("device_id"),jsonRoot.getString("path"),jsonRoot.getString("r")));
            return; // Batch replies must not finish an unrelated single-match detail page.
        }
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new DeleteVideoDirEvent(1));
            } else {
                EventBus.getDefault().post(new DeleteVideoDirEvent(0));
            }
        } else {
            EventBus.getDefault().post(new DeleteVideoDirEvent(-1));
        }
    }


    public static void postAddGoal(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setHost_team(isHostTeam);
                EventBus.getDefault().post(new AddGoalEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new AddGoalEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new AddGoalEvent(-1, null));
        }
    }

    public static void postRemoveGoal(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setHost_team(isHostTeam);
                EventBus.getDefault().post(new RemoveGoalEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new RemoveGoalEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new RemoveGoalEvent(-1, null));
        }
    }


    public static void postGoalList(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                boolean hasGoals = jsonRoot.containsKey(BleConstants.BLE_KEY_GOALS);
                if(hasGoals) {
                    JSONArray goalJson = jsonRoot.getJSONArray(BleConstants.BLE_KEY_GOALS);
                    List<GoalBean> gaolList = new Gson().fromJson(goalJson.toJSONString(), new TypeToken<List<GoalBean>>() {
                    }.getType());
                    EventBus.getDefault().post(new GoalListEvent(1, gaolList));
                } else {
                    EventBus.getDefault().post(new GoalListEvent(0, null));
                }
            } else {
                EventBus.getDefault().post(new GoalListEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GoalListEvent(-1, null));
        }
    }

    public static void postFanSpeed(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FanSpeedEvent(1));
            } else {
                EventBus.getDefault().post(new FanSpeedEvent(0));
            }
        } else {
            EventBus.getDefault().post(new FanSpeedEvent(-1));
        }
    }

    public static void postCompetitionLabel(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new CompetitionLabelEvent(1));
            } else {
                EventBus.getDefault().post(new CompetitionLabelEvent(0));
            }
        } else {
            EventBus.getDefault().post(new CompetitionLabelEvent(-1));
        }
    }

    public static void postSetVideoName(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new SetVideoNameEvent(1));
            } else {
                EventBus.getDefault().post(new SetVideoNameEvent(0));
            }
        } else {
            EventBus.getDefault().post(new SetVideoNameEvent(-1));
        }
    }

    public static void postOSDControl(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new OSDControlEvent(1));
            } else {
                EventBus.getDefault().post(new OSDControlEvent(0));
            }
        } else {
            EventBus.getDefault().post(new OSDControlEvent(-1));
        }
    }

    public static void postStartUsb(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new StartUsbEvent(1));
            } else {
                EventBus.getDefault().post(new StartUsbEvent(0));
            }
        } else {
            EventBus.getDefault().post(new StartUsbEvent(-1));
        }
    }

    public static void postStopUsb(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new StopUsbEvent(1));
            } else {
                EventBus.getDefault().post(new StopUsbEvent(0));
            }
        } else {
            EventBus.getDefault().post(new StopUsbEvent(-1));
        }
    }

    public static void postGetOSDSettings(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        GetOSDSettingsEvent e = new GetOSDSettingsEvent();
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                e.hide_score = jsonRoot.getIntValue(BleConstants.BLE_KEY_HIDE_SCORE);
                e.hide_team = jsonRoot.getIntValue(BleConstants.BLE_KEY_HIDE_TEAM);
            }
        }
        EventBus.getDefault().post(e);
    }

    public static void postGameStatList(@NonNull JSONObject jsonRoot, BaseBluetoothBean requestBean) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                boolean hasGoals = jsonRoot.containsKey(BleConstants.BLE_KEY_STATS);
                if(hasGoals) {
                    JSONArray goalJson = jsonRoot.getJSONArray(BleConstants.BLE_KEY_STATS);
                    List<GameStatusListBean> gaolList = new Gson().fromJson(goalJson.toJSONString(), new TypeToken<List<GameStatusListBean>>() {
                    }.getType());
                    EventBus.getDefault().post(new GameStatListEvent(1, gaolList, requestBean));
                } else {
                    EventBus.getDefault().post(new GameStatListEvent(0, null, requestBean));
                }
            } else {
                EventBus.getDefault().post(new GameStatListEvent(0, null, requestBean));
            }
        } else {
            EventBus.getDefault().post(new GameStatListEvent(-1, null, requestBean));
        }
    }

    public static void postCompetitionScoreClear(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_team(isHostTeam);
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setType(type);
                EventBus.getDefault().post(new GameScoreClearEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameScoreClearEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameScoreClearEvent(-1, null));
        }
    }


    public static void postCompetitionScoreRemove(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_team(isHostTeam);
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setType(type);
                EventBus.getDefault().post(new GameScoreRemoveEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameScoreRemoveEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameScoreRemoveEvent(-1, null));
        }
    }

    public static void postCompetitionScoreAdd(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_team(isHostTeam);
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setType(type);
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_ID)){
                    goalBean.setPointId(jsonRoot.getString(BleConstants.BLE_KEY_ID));
                }
                EventBus.getDefault().post(new GameScoreAddEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameScoreAddEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameScoreAddEvent(-1, null));
        }
    }

    public static void postCompetitionScoreStatAdd(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_team(isHostTeam);
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setType(type);
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_ID)){
                    goalBean.setPointId(jsonRoot.getString(BleConstants.BLE_KEY_ID));
                }
                EventBus.getDefault().post(new GameScoreStatAddEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameScoreStatAddEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameScoreStatAddEvent(-1, null));
        }
    }


    public static void postCompetitionStatAdd(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);

                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_team(isHostTeam);
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setType(type);
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_TIME)) {
                    goalBean.setTime(jsonRoot.getIntValue(BleConstants.BLE_KEY_TIME));
                }
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_ID)){
                    goalBean.setPointId(jsonRoot.getString(BleConstants.BLE_KEY_ID));
                }
                EventBus.getDefault().post(new GameStatAddEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameStatAddEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameStatAddEvent(-1, null));
        }
    }

    public static void postCompetitionStatRemove(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setHost_team(isHostTeam);
                goalBean.setType(type);
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_ID)){
                    goalBean.setPointId(jsonRoot.getString(BleConstants.BLE_KEY_ID));
                }
                EventBus.getDefault().post(new GameStatRemoveEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameStatRemoveEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameStatRemoveEvent(-1, null));
        }
    }

    public static void postCompetitionStatEdit(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                int isHostTeam = jsonRoot.getIntValue(BleConstants.BLE_KEY_HOST_TEAM);
                int hostGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_HOST);
                int guestGoal = jsonRoot.getIntValue(BleConstants.BLE_KEY_GOAL_GUEST);
                int type = jsonRoot.getIntValue(BleConstants.BLE_KEY_TYPE);
                GoalStatusBean goalBean = new GoalStatusBean();
                goalBean.setHost_goal(hostGoal);
                goalBean.setGuest_goal(guestGoal);
                goalBean.setHost_team(isHostTeam);
                goalBean.setType(type);
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_TIME)) {
                    goalBean.setTime(jsonRoot.getIntValue(BleConstants.BLE_KEY_TIME));
                }
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_ID)){
                    goalBean.setPointId(jsonRoot.getString(BleConstants.BLE_KEY_ID));
                }
                if(jsonRoot.containsKey(BleConstants.BLE_KEY_REMOVE)){
                    goalBean.setRemove(jsonRoot.getIntValue(BleConstants.BLE_KEY_REMOVE));
                }
                EventBus.getDefault().post(new GameStatEditEvent(1, goalBean));
            } else {
                EventBus.getDefault().post(new GameStatEditEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new GameStatEditEvent(-1, null));
        }
    }

    public static void postAvsRtmpSetUrl(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new AvsRtmpSetUrlEvent(1));
            } else {
                EventBus.getDefault().post(new AvsRtmpSetUrlEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new AvsRtmpSetUrlEvent(-1, ""));
        }
    }

    public static void postFecLiveAddUrl(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FecRtmpAddUrlEvent(1));
            } else {
                EventBus.getDefault().post(new FecRtmpAddUrlEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new FecRtmpAddUrlEvent(-1, ""));
        }
    }

    public static void postFecLiveRemoveUrl(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FecRtmpRemoveUrlEvent(1));
            } else {
                EventBus.getDefault().post(new FecRtmpRemoveUrlEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new FecRtmpRemoveUrlEvent(-1, ""));
        }
    }

    public static void postFecLiveRemoveAllUrl(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FecRtmpRemoveAllUrlEvent(1));
            } else {
                EventBus.getDefault().post(new FecRtmpRemoveAllUrlEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new FecRtmpRemoveAllUrlEvent(-1, ""));
        }
    }

    public static void postFecPreviewStart(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FecRtmpPreviewStartEvent(1));
            } else {
                EventBus.getDefault().post(new FecRtmpPreviewStartEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new FecRtmpPreviewStartEvent(-1, ""));
        }
    }

    public static void postFecPreviewStop(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new FecRtmpPreviewStopEvent(1));
            } else {
                EventBus.getDefault().post(new FecRtmpPreviewStopEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new FecRtmpPreviewStopEvent(-1, ""));
        }
    }

    public static void postLiveStatus(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                LiveStatusResultBean bean = GsonUtils.GsonToBean(jsonRoot.toString(), LiveStatusResultBean.class);
                EventBus.getDefault().post(new LiveStatusResultEvent(1, bean));
            } else {
                EventBus.getDefault().post(new LiveStatusResultEvent(0, null));
            }
        } else {
            EventBus.getDefault().post(new LiveStatusResultEvent(-1, null));
        }
    }

    public static void postOTAStatus(@NonNull JSONObject jsonRoot) {
        OTAStatusEvent e = OTAStatusEvent.fromJson(jsonRoot);
        CameraManager.updateOTAStatus(e);
        EventBus.getDefault().post(e);
    }

    public static void postOTAStartDownload(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            int errCode = 0;
            if(jsonRoot.containsKey(BleConstants.BLE_KEY_ERROR_CODE)){
                errCode = jsonRoot.getInteger(BleConstants.BLE_KEY_ERROR_CODE);
            }
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new OTAStartDownloadEvent(1, errCode));
            } else {
                EventBus.getDefault().post(new OTAStartDownloadEvent(0, errCode));
            }
        } else {
            EventBus.getDefault().post(new OTAStartDownloadEvent(-1, 0));
        }
    }

    public static void postOTAStartInstall(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase("ota started")) {
                EventBus.getDefault().post(new OTAStartInstallEvent(1, status));
            } else {
                EventBus.getDefault().post(new OTAStartInstallEvent(0, status));
            }
        } else {
            EventBus.getDefault().post(new OTAStartInstallEvent(-1, ""));
        }
    }


    public static void postOTAInstallSuccess(@NonNull JSONObject jsonRoot) {

        EventBus.getDefault().post(new OTAInstallSucEvent(1));
    }
    public static void postSceneSetup(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new SceneSetupEvent(1));
            } else {
                EventBus.getDefault().post(new SceneSetupEvent(0));
            }
        } else {
            EventBus.getDefault().post(new SceneSetupEvent(-1));
        }
    }

    public static void postGetSettings(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            CameraSettings cameraSettings = GsonUtils.GsonToBean(jsonRoot.toString(), CameraSettings.class);

            EventBus.getDefault().post(new WeewaSettingsEvent(cameraSettings));
        } else {
            EventBus.getDefault().post(new WeewaSettingsEvent(null));
        }
    }

    public static void postUpdateSettings(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new WeewaSettingsUpdateEvent(1));
            } else {
                EventBus.getDefault().post(new WeewaSettingsUpdateEvent(0));
            }
        } else {
            EventBus.getDefault().post(new WeewaSettingsUpdateEvent(-1));
        }
    }

    public static void postUpdateSettingsEx(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new WeewaSettingsExUpdateEvent(1));
            } else {
                EventBus.getDefault().post(new WeewaSettingsExUpdateEvent(0));
            }
        } else {
            EventBus.getDefault().post(new WeewaSettingsExUpdateEvent(-1));
        }
    }


    public static void postNetChange(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_SIM);
        if(suc) {
            int sim_status = jsonRoot.getIntValue(BleConstants.BLE_KEY_SIM);
            int wifi_status = jsonRoot.getIntValue(BleConstants.BLE_KEY_WIFI);
            NetChangeInfo netChangeInfo = new NetChangeInfo();
            netChangeInfo.sim = sim_status;
            netChangeInfo.wifi = wifi_status;

            EventBus.getDefault().post(new NetChangeEvent(netChangeInfo));
        } else {
            EventBus.getDefault().post(new NetChangeEvent(null));
        }
    }


    public static void postGetSettingsEx(@NonNull JSONObject jsonRoot) {
        WeewaSettingsExEvent e = new WeewaSettingsExEvent();
        for(String key : jsonRoot.keySet()) {
            e.add(key, jsonRoot.getString(key));
        }
        EventBus.getDefault().post(e);
    }

    public static void postSetTeamName(@NonNull JSONObject jsonRoot) {

        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new SetTeamNameEvent(1));
            } else {
                EventBus.getDefault().post(new SetTeamNameEvent(0));
            }
        } else {
            EventBus.getDefault().post(new SetTeamNameEvent(-1));
        }

    }

    public static void postBeep(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new BeepEvent(1));
            } else {
                EventBus.getDefault().post(new BeepEvent(0));
            }
        } else {
            EventBus.getDefault().post(new BeepEvent(-1));
        }
    }


    public static void postPing(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new PingEvent(1));
            } else {
                EventBus.getDefault().post(new PingEvent(0));
            }
        } else {
            EventBus.getDefault().post(new PingEvent(-1));
        }
    }

    public static void postFactoryTestAudioStart(@NonNull JSONObject jsonRoot) {

        String path = jsonRoot.getString(BleConstants.BLE_KEY_PATH_FULL);
        EventBus.getDefault().post(new FTAudioStartEvent(1, path));

    }

    public static void postFactoryTestAudioStop(@NonNull JSONObject jsonRoot) {

        String path = jsonRoot.getString(BleConstants.BLE_KEY_PATH_FULL);
        EventBus.getDefault().post(new FTAudioStopEvent(1, path));
    }

    public static void postFactoryTestFastCharge(@NonNull JSONObject jsonRoot) {
        int charge = jsonRoot.getInteger(BleConstants.BLE_KEY_CHARGE);
        EventBus.getDefault().post(new FTFastChargeEvent(charge));
    }

    public static void postFactoryTestArecordStart(@NonNull JSONObject jsonRoot) {

        String path = jsonRoot.getString(BleConstants.BLE_KEY_PATH_FULL);
        EventBus.getDefault().post(new FTArecordStartEvent(1));

    }

    public static void postFactoryTestArecordStop(@NonNull JSONObject jsonRoot) {

        String path = jsonRoot.getString(BleConstants.BLE_KEY_PATH_FULL);
        EventBus.getDefault().post(new FTArecordStopEvent(1));
    }

    public static void postUploadLog(@NonNull JSONObject jsonRoot) {
        String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
        EventBus.getDefault().post(new UploadLogEvent(status));
    }

    public static void postMatchTimePause(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new MatchTimePauseEvent(1));
            } else {
                EventBus.getDefault().post(new MatchTimePauseEvent(0));
            }
        } else {
            EventBus.getDefault().post(new MatchTimePauseEvent(-1));
        }
    }
    public static void postMatchTimeResume(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new MatchTimeResumeEvent(1));
            } else {
                EventBus.getDefault().post(new MatchTimeResumeEvent(0));
            }
        } else {
            EventBus.getDefault().post(new MatchTimeResumeEvent(-1));
        }
    }
    
    public static void postMatchTimeReset(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new MatchTimeResetEvent(1));
            } else {
                EventBus.getDefault().post(new MatchTimeResetEvent(0));
            }
        } else {
            EventBus.getDefault().post(new MatchTimeResetEvent(-1));
        }
    }

    public static void postUploadCalib(@NonNull JSONObject jsonRoot) {
        boolean suc = jsonRoot.containsKey(BleConstants.BLE_KEY_RESULT);
        if(suc) {
            String status = jsonRoot.getString(BleConstants.BLE_KEY_RESULT);
            if(status.equalsIgnoreCase(BleConstants.BLE_RESULT_SUCCESS)) {
                EventBus.getDefault().post(new UploadCalibEvent(1));
            } else {
                EventBus.getDefault().post(new UploadCalibEvent(0));
            }
        } else {
            EventBus.getDefault().post(new UploadCalibEvent(-1));
        }
    }
}
