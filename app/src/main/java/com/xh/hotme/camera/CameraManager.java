package com.xh.hotme.camera;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceSettingMbrBean;
import com.xh.hotme.bean.DeviceSettingResultBean;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import com.xh.hotme.camera.ota.CameraOtaOffer;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.UpdateSettingsRequestBean;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.MD5;
import com.xh.hotme.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraManager {

    private final static String TAG = "CameraManager";

    private static CameraInfo _connectCameraInfo;

    private static final Map<String, OTAVersionInfo> _otaVersionMap = new HashMap<>();

    // key是config key, value是服务器端的配置对象bean
    public static Map<String, DeviceSettingResultBean> _globalTestDeviceDefaultConfig = null;
    public static Map<String, DeviceSettingResultBean> _globalNormalDeviceDefaultConfig = null;

    // key是device id, value是config哈希, 保存的是对特定某个设备的配置
    public static final Map<String, Map<String, DeviceSettingResultBean>> _deviceDefaultConfigMap = new HashMap<>();

    // key是device id, value是config哈希
    public static final Map<String, Map<String, DeviceSettingResultBean>> _deviceConfigMap = new HashMap<>();

    public static final Map<String, Boolean> _adbCheckedMap = new HashMap<>();

    public static boolean isCameraReachable() {
        // 判断一下相机是否能连接外网,如果还没取到网络信息的话,暂时认为可以
        if(_connectCameraInfo != null && _connectCameraInfo.deviceNetworkInfo != null) {
            return _connectCameraInfo.deviceNetworkInfo.isReachable();
        }
        return true;
    }

    public static void loadProprietaryDeviceDefaultConfig() {
        // if no connected device, return
        if(_connectCameraInfo == null || _connectCameraInfo.deviceInfo == null) {
            return;
        }

        // get device id
        String deviceId = _connectCameraInfo.deviceInfo.device_id;

        // check device config, if not exist, try load from file
        if(!_deviceDefaultConfigMap.containsKey(deviceId)) {
            Map<String, DeviceSettingResultBean> defCfg = AppFileUtil.loadProprietaryDeviceDefaultConfig(deviceId);
            if(defCfg != null) {
                _deviceDefaultConfigMap.put(deviceId, defCfg);
            }
        }

        // if still no, load from server
        if(!_deviceDefaultConfigMap.containsKey(deviceId)) {
            ApiUtil.getDeviceConfig(deviceId, isTestDevice() ? Constants.DEVICE_TYPE_TEST : Constants.DEVICE_TYPE_NORMAL, new IGenericApiCallback<List<DeviceSettingResultBean>>() {
                @Override
                public void onSuccess(List<DeviceSettingResultBean> data) {
                    Map<String, DeviceSettingResultBean> defCfg = new HashMap<>();
                    for(DeviceSettingResultBean bean : data) {
                        defCfg.put(bean.getConfigKey(), bean);
                    }
                    _deviceDefaultConfigMap.put(deviceId, defCfg);
                    AppFileUtil.saveProprietaryDeviceDefaultConfig(deviceId, defCfg);
                }

                @Override
                public void onFail(String code, String message) {
                }

                @Override
                public void onFinish() {
                }
            });
        }
    }

    public static void loadGlobalDeviceDefaultConfig() {
        if(_globalTestDeviceDefaultConfig == null) {
            _globalTestDeviceDefaultConfig = AppFileUtil.loadGlobalTestDeviceDefaultConfig();
        }
        if(_globalNormalDeviceDefaultConfig == null) {
            _globalNormalDeviceDefaultConfig = AppFileUtil.loadGlobalNormalDeviceDefaultConfig();
        }

        // 不管有没有, app启动的时候刷一次
        ApiUtil.getDeviceConfig("", Constants.DEVICE_TYPE_TEST, new IGenericApiCallback<List<DeviceSettingResultBean>>() {
            @Override
            public void onSuccess(List<DeviceSettingResultBean> data) {
                if(_globalTestDeviceDefaultConfig == null) {
                    _globalTestDeviceDefaultConfig = new HashMap<>();
                }
                _globalTestDeviceDefaultConfig.clear();
                for(DeviceSettingResultBean bean : data) {
                    _globalTestDeviceDefaultConfig.put(bean.getConfigKey(), bean);
                }
                AppFileUtil.saveGlobalTestDeviceDefaultConfig(_globalTestDeviceDefaultConfig);
            }

            @Override
            public void onFail(String code, String message) {
            }

            @Override
            public void onFinish() {
            }
        });
        ApiUtil.getDeviceConfig("", Constants.DEVICE_TYPE_NORMAL, new IGenericApiCallback<List<DeviceSettingResultBean>>() {
            @Override
            public void onSuccess(List<DeviceSettingResultBean> data) {
                if(_globalNormalDeviceDefaultConfig == null) {
                    _globalNormalDeviceDefaultConfig = new HashMap<>();
                }
                _globalNormalDeviceDefaultConfig.clear();
                for(DeviceSettingResultBean bean : data) {
                    _globalNormalDeviceDefaultConfig.put(bean.getConfigKey(), bean);
                }
                AppFileUtil.saveGlobalNormalDeviceDefaultConfig(_globalNormalDeviceDefaultConfig);
            }

            @Override
            public void onFail(String code, String message) {
            }

            @Override
            public void onFinish() {
            }
        });
    }

    public static void release() {
        clearOTAInfo();
        if(_connectCameraInfo != null) {
            _connectCameraInfo = null;
        }
    }

    public static CameraInfo getCameraInfo(String bluetoothId) {
        if(TextUtils.isEmpty(bluetoothId)) {
            return _connectCameraInfo;
        }

        if(_connectCameraInfo != null && bluetoothId.equalsIgnoreCase(_connectCameraInfo.bluetoothId)) {
            return _connectCameraInfo;
        }

        return null;
    }

    public static void newCamera(String bluetoothId) {
        if(TextUtils.isEmpty(bluetoothId)) {
            return;
        }

        if(_connectCameraInfo != null && bluetoothId.equalsIgnoreCase(_connectCameraInfo.bluetoothId)) {
            return;
        }

        _connectCameraInfo = null;
        clearOTAInfo();
        _connectCameraInfo = new CameraInfo(bluetoothId);

        DeviceInfo deviceInfo = getLocalDeviceInfo(bluetoothId);
        if(deviceInfo != null) {
            _connectCameraInfo.deviceInfo = deviceInfo;
            setDeviceId(_connectCameraInfo.deviceInfo.device_id);

        }
    }

    public static DeviceUsageInfo getUsageInfo(String bluetoothId) {

        if(_connectCameraInfo != null) {
            return _connectCameraInfo.deviceUsageInfo;
        }

        if(_connectCameraInfo != null && bluetoothId != null && bluetoothId.equalsIgnoreCase(_connectCameraInfo.bluetoothId)) {
            return _connectCameraInfo.deviceUsageInfo;
        }

        return null;
    }

    public static DeviceUsageInfo getUsageInfo() {

        if(_connectCameraInfo != null) {
            return _connectCameraInfo.deviceUsageInfo;
        }
        return null;
    }

    public static void setUsageInfo(DeviceUsageInfo userInfo) {
        if(_connectCameraInfo != null) {
            _connectCameraInfo.deviceUsageInfo = userInfo;
        }
    }

    public static DeviceInfo getDeviceInfo() {
        if(_connectCameraInfo != null) {
            return _connectCameraInfo.deviceInfo;
        }

        return null;
    }

    public static void setDeviceInfo(DeviceInfo deviceInfo) {
        if(_connectCameraInfo == null || deviceInfo == null) {
            return;
        }
        DeviceInfo previous = _connectCameraInfo.deviceInfo;
        if (previous != null && (previous.ota_version_code != deviceInfo.ota_version_code
                || !java.util.Objects.equals(previous.device_id, deviceInfo.device_id))) clearOTAInfo();
        _connectCameraInfo.deviceInfo = deviceInfo;

        saveLocalDeviceBaseInfo(_connectCameraInfo.bluetoothId, deviceInfo);

        //更新到cache list
        updateDeviceBaseInfoList(deviceInfo);

        //球场管理员版本才会有这个功能
        if(Constants.ROLE.equalsIgnoreCase("greenKeeper")) {
            ApiUtil.queryAllRecordTask(deviceInfo.device_id, null);
        }

        // 如果还没有获取设备的默认配置, 获取一下
        loadProprietaryDeviceDefaultConfig();

        // TODO 下面是根据服务器配置启停adb的代码, 但是是否启用, 还没想好, 相机本身的固件应该是关闭adb的, 而且
        // TODO service和collector版本本身就可以在相机测试里启停adb, 感觉没啥必要这里去检查
        // 如果全局配置还没获得, 返回
//        if(_globalDeviceDefaultConfig == null) return;
//
//        // 既然全局配置已经获得, 则检查相机是否应该打开adb, 假如adb的状态不对, 则设置adb的状态,
//        // 这个检查一次app启动只做一次, 以免用户后续手动切换了adb状态
//        if(_adbCheckedMap.containsKey(deviceInfo.device_id)) return;
//        boolean curAdb = deviceInfo.adb != 0;
//        boolean cfgAdb = getDeviceBooleanConfig(Constants.CAMERA_SETTING_ADB_ENABLE);
//        if(curAdb != cfgAdb) {
//            BluetoothManager.getImpl().sendCmdSimpleObj(cfgAdb ? new StartUsbRequestBean() : new StopUsbRequestBean());
//        }
//        _adbCheckedMap.put(deviceInfo.device_id, true);
    }

    public static void saveLocalDeviceBaseInfo(String bluetoothId, DeviceInfo deviceInfo) {
        SharedPrefs.getImpl().putString("device_" + bluetoothId, GsonUtils.GsonString(deviceInfo));
    }

    public static DeviceInfo getLocalDeviceInfo(String bluetoothId) {
        String deviceInfoStr = SharedPrefs.getImpl().getString("device_" + bluetoothId);
        if(TextUtils.isEmpty(deviceInfoStr)) {
            return null;
        }
        return GsonUtils.GsonToBean(deviceInfoStr, DeviceInfo.class);
    }


    public static void saveDeviceInfoList(List<DeviceInfo> deviceInfo) {
        SharedPrefs.getImpl().putString("device_list", GsonUtils.GsonString(deviceInfo));
    }

    public static List<DeviceInfo> getDeviceInfoList() {
        String deviceInfoStr = SharedPrefs.getImpl().getString("device_list");
        if(TextUtils.isEmpty(deviceInfoStr)) {
            return null;
        }
        List<DeviceInfo> deviceList = null;
        try {
            deviceList = new Gson().fromJson(deviceInfoStr, new TypeToken<List<DeviceInfo>>() {
            }.getType());
        } catch(JsonSyntaxException e) {
            e.printStackTrace();
        }

        return deviceList;
    }

    //更新并保存连接过的设备
    public static void updateDeviceBaseInfoList(DeviceInfo deviceInfo) {
        try {
            List<DeviceInfo> deviceInfoList = getDeviceInfoList();
            if(ListUtils.isValid(deviceInfoList)) {
                boolean isExit = false;
                for(DeviceInfo device : deviceInfoList) {
                    if(device.mac.equalsIgnoreCase(deviceInfo.mac)) {
                        deviceInfoList.remove(device);
                        break;
                    }
                }
                deviceInfoList.add(0, deviceInfo);
                return;
            }
            if(deviceInfoList == null) {
                deviceInfoList = new ArrayList<>();
            }
            deviceInfoList.add(0, deviceInfo);

            saveDeviceInfoList(deviceInfoList);

        } catch(Throwable e) {

        }/**/
    }

    public static NetworkBean getNetworkInfo(String bluetoothId) {
        if(TextUtils.isEmpty(bluetoothId)) {
            if(_connectCameraInfo != null) {
                return _connectCameraInfo.deviceNetworkInfo;
            }
        } else {
            if(_connectCameraInfo != null && !TextUtils.isEmpty(bluetoothId) && bluetoothId.equalsIgnoreCase(_connectCameraInfo.bluetoothId)) {
                return _connectCameraInfo.deviceNetworkInfo;
            }
        }

        return null;
    }

    public static void setNetworkInfo(NetworkBean networkBean) {
        if(_connectCameraInfo != null) {
            _connectCameraInfo.deviceNetworkInfo = networkBean;
        }
    }

    public static void updateSoftAp(NetworkBean.ApInfo softApInfo) {
        if(_connectCameraInfo != null && _connectCameraInfo.deviceNetworkInfo != null) {
            _connectCameraInfo.deviceNetworkInfo.setAp(softApInfo);
        }
    }


    public static String getDeviceBluetoothId() {
        if(_connectCameraInfo != null) {
            return _connectCameraInfo.bluetoothId;
        }
        return "";
    }

    public static boolean isTestDevice() {
        String deviceId = getDeviceId();
        if(!TextUtils.isEmpty(deviceId)) {
            return deviceId.startsWith("st");
        }
        return true;
    }

    public static String getConnectedDeviceId() {
        if(_connectCameraInfo != null && _connectCameraInfo.deviceInfo != null) {
            return _connectCameraInfo.deviceInfo.device_id;
        }
        return null;
    }

    public static String getDeviceId() {
        if(_connectCameraInfo != null && _connectCameraInfo.deviceInfo != null) {
            return _connectCameraInfo.deviceInfo.device_id;
        }
        return getDeviceIdLast();
    }

    public static String getDeviceIdLast() {
        if(_connectCameraInfo != null && _connectCameraInfo.deviceInfo != null) {
            return _connectCameraInfo.deviceInfo.device_id;
        }
        return getSaveDeviceIdLast();

    }

    public static String getSaveDeviceIdLast() {
        return SharedPrefs.getImpl()
            .getString("weewa_last_device_id", "");
    }

    public static void setDeviceId(String deviceId) {
        SharedPrefs.getImpl().saveString("weewa_last_device_id",
            deviceId);
    }



    public static String getDeviceName() {
        if(_connectCameraInfo != null && _connectCameraInfo.deviceInfo != null) {
            return _connectCameraInfo.deviceInfo.device_name;
        }
        return "热我相机";
    }

    public static boolean isReachable(){
        NetworkBean networkBean = CameraManager.getNetworkInfo("");
        if(networkBean!=null ){
            return networkBean.isReachable();
        }
        return false;
    }

    public static boolean isNetworkValid() {
        NetworkBean networkBean = CameraManager.getNetworkInfo("");
        return isNetworkValid(networkBean);
    }

    public static boolean isNetworkValid(NetworkBean networkBean) {
        // if null, not valid
        if(networkBean == null) return false;

        // if has wifi
        if(networkBean.getWifi() != null) {
            if(!networkBean.getWifi().ip.isEmpty() &&
                !networkBean.getWifi().ip.startsWith("169.254") && !networkBean.getWifi().s.isEmpty()) {   //如果wifi 正常
                return true;
            }
        }

        // if has 4g
        if(networkBean.getSim() != null) {
            if(networkBean.getSim().st == 1 && networkBean.getSim().has_card == 1 && networkBean.getSim().connected == 1) {  //如果4G正常
                return true;
            }
        }

        // if has ethernet
        if(networkBean.getEth() != null) {
            return networkBean.getEth().st == 1 && !networkBean.getEth().ip.isEmpty();
        }

        return false;
    }

    public static boolean hasWifi() {
        if(_connectCameraInfo == null ||
            _connectCameraInfo.deviceNetworkInfo == null ||
            _connectCameraInfo.deviceNetworkInfo.getWifi() == null) {
            return false;
        }
        return _connectCameraInfo.deviceNetworkInfo.getWifi().st == 1 && !_connectCameraInfo.deviceNetworkInfo.getWifi().s.isEmpty();
    }

    public static boolean hasEthernet() {
        if(_connectCameraInfo == null ||
            _connectCameraInfo.deviceNetworkInfo == null ||
            _connectCameraInfo.deviceNetworkInfo.getEth() == null) {
            return false;
        }
        return _connectCameraInfo.deviceNetworkInfo.getEth().st == 1;
    }

    public static boolean isSimValid() {
        if(_connectCameraInfo == null ||
                _connectCameraInfo.deviceNetworkInfo == null ||
                _connectCameraInfo.deviceNetworkInfo.getSim() == null) {
            return false;
        }
        return _connectCameraInfo.deviceNetworkInfo.getSim().st == 1 && _connectCameraInfo.deviceNetworkInfo.getSim().has_card == 1 && _connectCameraInfo.deviceNetworkInfo.getSim().connected == 1;
    }

    public static void updateOTAStatus(OTAStatusEvent event) {
        DeviceInfo deviceInfo = getDeviceInfo();
        if (deviceInfo == null) return;
        OTAVersionInfo offer = CameraOtaOffer.fromStatus(event);
        if (offer == null) _otaVersionMap.remove(deviceInfo.device_id);
        else _otaVersionMap.put(deviceInfo.device_id, offer);
    }

    public static OTAVersionInfo getOTAVersion() {
        DeviceInfo deviceInfo = getDeviceInfo();
        if(deviceInfo == null) {
            return null;
        }
        return _otaVersionMap.get(deviceInfo.device_id);
    }

    public static boolean hasOTAInfo(String deviceId) {
        return _otaVersionMap.containsKey(deviceId);
    }

    public static void clearOTAInfo() {
        _otaVersionMap.clear();
    }

    public static boolean isBindDevice(String address) {
        List<BindDeviceBean> deviceList = DeviceManager.getBindDeviceList();
        for(BindDeviceBean deviceBean : deviceList) {
            if(deviceBean.getBluetoothUuid().equalsIgnoreCase(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */

    public static int getAvsMBR(int motionType, int placeType) {
        // 按老板所说, 视频分析打开的时候, avs的mbr都按界面上那个处理
        if(getDeviceBooleanConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS)) {
            return getDeviceIntConfig(Constants.CAMERA_SETTING_AVS_MBR);
        }

        // 否则按照几人场来找
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return -1;

        List<DeviceSettingMbrBean> mbrBeanList = GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
        if(!mbrBeanList.isEmpty()) {
            for(DeviceSettingMbrBean mbrBean : mbrBeanList) {
                if(motionType == Constants.MOTION_TYPE_BASKETBALL || (mbrBean.place_type == placeType && motionType == mbrBean.motion_type)) {
                    if(mbrBean.mbr != null) {
                        return mbrBean.mbr.avs_video_mbr;
                    } else {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    public static int getFecMBR(int motionType, int placeType) {
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return -1;

        List<DeviceSettingMbrBean> mbrBeanList = GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
        for(DeviceSettingMbrBean mbrBean : mbrBeanList) {
            if(motionType == Constants.MOTION_TYPE_BASKETBALL || (mbrBean.place_type == placeType && motionType == mbrBean.motion_type)) {
                if(mbrBean.mbr != null) {
                    return mbrBean.mbr.fec_video_mbr;
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static int getLiveMBR(int motionType, int placeType) {
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return -1;

        List<DeviceSettingMbrBean> mbrBeanList = GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
        for(DeviceSettingMbrBean mbrBean : mbrBeanList) {
            if(motionType == Constants.MOTION_TYPE_BASKETBALL || (mbrBean.place_type == placeType && motionType == mbrBean.motion_type)) {
                if(mbrBean.mbr != null) {
                    return mbrBean.mbr.live_mbr;
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static List<DeviceSettingMbrBean> getMBRList() {
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return null;
        return GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
    }

    public static List<DeviceSettingMbrBean> getMBRList(int motionType) {
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return null;

        List<DeviceSettingMbrBean> mbrList = new ArrayList<>();
        List<DeviceSettingMbrBean> mbrBeanList = GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
        for(DeviceSettingMbrBean settingMbrBean : mbrBeanList) {
            if(settingMbrBean.motion_type == motionType) {
                mbrList.add(settingMbrBean);
            }
        }

        return mbrList;
    }

    public static List<DeviceSettingMbrBean> getMBRListByMotionTypeOrPlaceType(int motionType, int placeType, boolean skipPlaceType) {
        String mbrJson = getDeviceStringConfig(Constants.CAMERA_SETTING_MBR);
        if(TextUtils.isEmpty(mbrJson)) return null;

        List<DeviceSettingMbrBean> mbrList = new ArrayList<>();
        List<DeviceSettingMbrBean> mbrBeanList = GsonUtils.fromJsonList(mbrJson, DeviceSettingMbrBean.class);
        for(DeviceSettingMbrBean settingMbrBean : mbrBeanList) {
            if(skipPlaceType) {
                if(settingMbrBean.motion_type == motionType) {
                    mbrList.add(settingMbrBean);
                }
            } else {
                if(settingMbrBean.motion_type == motionType && placeType == settingMbrBean.place_type) {
                    mbrList.add(settingMbrBean);
                }
            }
        }
        return mbrList;
    }

    public static boolean updateMbrSetting(DeviceSettingMbrBean deviceSettingMbrBean) {
        List<DeviceSettingMbrBean> mbrBeanList = getMBRList();
        if(mbrBeanList == null) return false;

        for(DeviceSettingMbrBean settingMbrBean : mbrBeanList) {
            if(settingMbrBean.motion_type == deviceSettingMbrBean.motion_type && settingMbrBean.place_type == deviceSettingMbrBean.place_type) {
                settingMbrBean.mbr.avs_video_mbr = deviceSettingMbrBean.mbr.avs_video_mbr;
                settingMbrBean.mbr.fec_video_mbr = deviceSettingMbrBean.mbr.fec_video_mbr;
                settingMbrBean.mbr.live_mbr = deviceSettingMbrBean.mbr.live_mbr;
                settingMbrBean.mbr.local_rtmp_mbr = deviceSettingMbrBean.mbr.local_rtmp_mbr;

                // write back to file
                String newContent = GsonUtils.GsonString(mbrBeanList);
                setDeviceConfig(Constants.CAMERA_SETTING_MBR, newContent);
                return true;
            }
        }
        return false;
    }

    public static String calculateDigest(Map<String, String> params) {
        // need device info
        if(_connectCameraInfo == null || _connectCameraInfo.deviceInfo == null) {
            return "";
        }

        // append common
        DeviceInfo di = _connectCameraInfo.deviceInfo;
        params.put(BleConstants.BLE_KEY_DEVICE_NAME, di.device_name);
        params.put(BleConstants.BLE_KEY_DEVICE_ID, di.device_id);
        params.put(BleConstants.BLE_KEY_MAC, di.mac);
        params.put(BleConstants.BLE_KEY_DEVICE_ACCOUNT, di.account);

        // sort keys
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        // concat key value
        StringBuilder sb = new StringBuilder();
        for(String key : keys) {
            sb.append(key);
            sb.append(params.get(key));
        }
        sb.append(di.nonce);

        // get md5
        return MD5.md5(sb.toString());
    }

    public static String getDeviceStringConfig(String key) {
        // must connected
        String deviceId = getConnectedDeviceId();
        if(TextUtils.isEmpty(deviceId)) return null;

        // ensure device config is loaded
        if(!_deviceConfigMap.containsKey(deviceId)) {
            Map<String, DeviceSettingResultBean> cfgMap = AppFileUtil.loadProprietaryDeviceConfig(deviceId);
            if(cfgMap != null) {
                _deviceConfigMap.put(deviceId, cfgMap);
            }
        }

        // first look custom config
        if(_deviceConfigMap.containsKey(deviceId)) {
            DeviceSettingResultBean cfg = _deviceConfigMap.get(deviceId).get(key);
            if(cfg != null) {
                return cfg.getConfigValue();
            }
        }

        // then look default config
        if(_deviceDefaultConfigMap.containsKey(deviceId)) {
            DeviceSettingResultBean cfg = _deviceDefaultConfigMap.get(deviceId).get(key);
            if(cfg != null) {
                return cfg.getConfigValue();
            }
        }

        // final look global config
        if(isTestDevice()) {
            if(_globalTestDeviceDefaultConfig != null) {
                DeviceSettingResultBean cfg = _globalTestDeviceDefaultConfig.get(key);
                if(cfg != null) {
                    return cfg.getConfigValue();
                }
            }
        } else {
            if(_globalNormalDeviceDefaultConfig != null) {
                DeviceSettingResultBean cfg = _globalNormalDeviceDefaultConfig.get(key);
                if(cfg != null) {
                    return cfg.getConfigValue();
                }
            }
        }

        // nothing
        return null;
    }

    public static boolean getDeviceBooleanConfig(String key) {
        String v = getDeviceStringConfig(key);
        if(TextUtils.isEmpty(v)) return false;
        return "1".equals(v) || "true".equals(v);
    }

    public static int getDeviceIntConfig(String key) {
        String v = getDeviceStringConfig(key);
        if(TextUtils.isEmpty(v)) return 0;
        try {
            return Integer.parseInt(v);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public static void setDeviceConfig(String key, String value) {
        // must connected
        String deviceId = getConnectedDeviceId();
        if(TextUtils.isEmpty(deviceId)) return;

        // save it in config map
        Map<String, DeviceSettingResultBean> cfgMap = _deviceConfigMap.get(deviceId);
        if(cfgMap == null) {
            cfgMap = new HashMap<>();
            _deviceConfigMap.put(deviceId, cfgMap);
        }
        DeviceSettingResultBean cfg = new DeviceSettingResultBean();
        cfg.setConfigKey(key);
        cfg.setConfigValue(value);
        cfgMap.put(key, cfg);

        // save to file
        AppFileUtil.saveProprietaryDeviceConfig(deviceId, cfgMap);
    }

    public static void syncDefaultConfig() {
        UpdateSettingsRequestBean bean = new UpdateSettingsRequestBean();
        bean.setAutoUpload(getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD) ? 1 : 0);
        bean.setAutoUploadAvs(getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD_AVS) ? 1 : 0);
        DeviceApiManager.sendCmdSimpleObj(bean);
    }
}
