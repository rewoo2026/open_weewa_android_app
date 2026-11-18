package com.xh.hotme.http;

import android.util.Log;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.bean.FanSpeedRequestBean;
import com.xh.hotme.bluetooth.bean.SetTokenRequestBean;
import com.xh.hotme.account.CameraTokenSynchronizer;
import com.xh.hotme.bluetooth.listener.IBleVideoDetailListNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleVideoListNotifyListener;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.OkHttpUtil;

import org.json.JSONArray;

import java.util.ArrayList;

import okhttp3.HttpUrl;


public class DeviceHttpManager {
    private static final String TAG = "DeviceHttpManager";

    private static String mHostIP = "10.201.126.1";
    private static String mHost;
    private static final int mHostPost = 38888;

    public static void setHttpHost(String ip) {
        mHostIP = ip;
        mHost = "http://:" + ip + ":" + mHostPost;
    }

    public static HttpUrl getHttpUrl(String endPoint) {

        HttpUrl build = new HttpUrl.Builder().scheme("http").host(mHostIP).port(mHostPost).addPathSegment(endPoint).build();

        return build;
    }


    public static boolean sendCmdStartRecord(RecordInfoBean recordInfoBean) {
        AppTrace.d(TAG, "sendCmdNetStatus ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_RECORDER_START);

        org.json.JSONObject obj = new org.json.JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_RECORDER_START);
            obj.put(BleConstants.BLE_KEY_CAMPAIGN_NAME, recordInfoBean.campaignName);
            obj.put(BleConstants.BLE_KEY_VIDEO_NAME, recordInfoBean.name);
            obj.put(BleConstants.BLE_KEY_MAX_RATE, recordInfoBean.maxRate);
            obj.put(BleConstants.BLE_KEY_AUTHOR, recordInfoBean.author);
            obj.put(BleConstants.BLE_KEY_VIDEO_WIDTH, recordInfoBean.width);
            obj.put(BleConstants.BLE_KEY_VIDEO_HEIGHT, recordInfoBean.height);
            obj.put(BleConstants.BLE_KEY_VIDEO_ADDRESS, recordInfoBean.place);
            obj.put(BleConstants.BLE_KEY_VIDEO_TIME, recordInfoBean.time);
            obj.put(BleConstants.BLE_KEY_VIDEO_CITY, recordInfoBean.city);
            obj.put(BleConstants.BLE_KEY_VIDEO_PLACE_TYPE, recordInfoBean.placeType);
            obj.put(BleConstants.BLE_KEY_MOTION_TYPE, recordInfoBean.motionType);
            obj.put(BleConstants.BLE_KEY_HOST_TEAM, recordInfoBean.hostTeam);
            obj.put(BleConstants.BLE_KEY_GUSET_TEAM, recordInfoBean.guestTeam);
        } catch(org.json.JSONException e) {
            e.printStackTrace();
        }
        AppTrace.d(TAG, "sendCmdStopRkipc status:" + obj);
        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                AppTrace.d(TAG, "sendCmdStopRkipc success: " + GsonUtils.GsonString(data));
                BluetoothHandle.postRkipcStop(data);
            }

            @Override
            public void onFailure(String code, String message) {
                AppTrace.d(TAG, "sendCmdStopRkipc fail: " + message);
                BluetoothHandle.postRkipcStop(new JSONObject());
            }
        });
        return true;
    }

    public static boolean sendCmdSetWatermark(String watermarkBase64Bitmap) {
        Log.d(TAG, "sendCmdSetWatermark ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_SET_WATERMARK);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_SET_WATERMARK);
            obj.put(BleConstants.BLE_KEY_BITMAP, watermarkBase64Bitmap);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                BluetoothHandle.postSetWatermark(data);
            }

            @Override
            public void onFailure(String code, String msg) {
                BluetoothHandle.postSetWatermark(new JSONObject());
            }
        });

        return true;
    }

    public static boolean sendCmdDeleteWatermark() {
        Log.d(TAG, "sendCmdDeleteWatermark ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_DELETE_WATERMARK);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_DELETE_WATERMARK);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                BluetoothHandle.postDeleteWatermark(data);
            }

            @Override
            public void onFailure(String code, String msg) {
                BluetoothHandle.postDeleteWatermark(new JSONObject());
            }
        });

        return true;
    }

    public static boolean sendCmdVideoList(IBleVideoListNotifyListener listener) {
        AppTrace.d(TAG, "sendCmdVideoList ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_VIDEO_LIST_URL);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_LIST);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                CameraVideoListBean bean = new Gson().fromJson(data.toJSONString(), CameraVideoListBean.class);
                if(bean != null && bean.videos != null) {
                    if(listener != null) {
                        listener.onVideoList(bean.videos);
                    }

                } else {
                    if(listener != null) {
                        listener.onVideoList(null);
                    }
                }
            }

            @Override
            public void onFailure(String code, String msg) {
                super.onFailure(code, msg);
                if(listener != null) {
                    listener.onVideoListFail(msg);
                }
            }

        });
        return true;
    }


    public static boolean sendCmdVideoDetailList(String path, IBleVideoDetailListNotifyListener listener) {
        AppTrace.d(TAG, "sendCmdVideoDetailList ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_VIDEO_DETAIL_LIST);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DETAIL_LIST);
            obj.put(BleConstants.BLE_KEY_PATH, path);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                CameraVideoDetailListBean bean = new Gson().fromJson(data.toJSONString(), CameraVideoDetailListBean.class);
                if(bean != null && bean.videos != null) {
                    if(listener != null) {
                        listener.onVideoDetailList(bean.videos);
                    }

                } else {
                    if(listener != null) {
                        listener.onVideoDetailList(null);
                    }
                }
            }

            @Override
            public void onFailure(String code, String msg) {
                super.onFailure(code, msg);
                if(listener != null) {
                    listener.onVideoDetailListFail(msg);
                }
            }

        });
        return true;
    }

    /**
     * 获取视频后处理状态
     *
     * @return
     */
    public static boolean sendCmdDeleteFileSub(String path, ArrayList<String> files) {
        Log.d(TAG, "sendCmdDeleteFileSub ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_VIDEO_DELETE_SUB);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DELETE_SUB);
            obj.put(BleConstants.BLE_KEY_PATH_FULL, path);
            JSONArray array = new JSONArray();
            for(String fileName : files) {
                array.put(fileName);
            }
            obj.put(BleConstants.BLE_KEY_SUB, array);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                BluetoothHandle.postDeleteFileSub(data);
            }

            @Override
            public void onFailure(String code, String msg) {
                BluetoothHandle.postDeleteFileSub(new JSONObject());
            }
        });
        return true;
    }

    public static boolean sendCmdDeleteVideoDir(String path) {
        Log.d(TAG, "sendCmdDeleteVideoDir ...");
        HttpUrl url = getHttpUrl(BleConstants.BLE_CMD_VIDEO_DELETE);
        JSONObject obj = new JSONObject();
        try {
            obj.put(BleConstants.BLE_CMD, BleConstants.BLE_CMD_VIDEO_DELETE);
            obj.put(BleConstants.BLE_KEY_PATH_FULL, path);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        OkHttpUtil.postData(url, obj.toString(), null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                BluetoothHandle.postDeleteVideoDir(data);

            }

            @Override
            public void onFailure(String code, String msg) {
                BluetoothHandle.postDeleteVideoDir(new JSONObject());
            }
        });
        return true;
    }

    /**
     * @return
     */
    public static boolean sendCmdSimpleObj(BaseBluetoothBean baseBluetoothBean) {
        AppTrace.t(TAG, "sendCmdSimpleObj:" + baseBluetoothBean.c);
        HttpUrl url = getHttpUrl(baseBluetoothBean.c);
        String jsonStr = GsonUtils.GsonString(baseBluetoothBean);

        OkHttpUtil.postData(url, jsonStr, null, new DeviceHttpCallbackDecode<JSONObject>() {
            @Override
            public void onDataSuccess(JSONObject data) {
                BluetoothHandle.handle(data, baseBluetoothBean);
            }

            @Override
            public void onFailure(String code, String msg) {
                JSONObject object = new JSONObject();
                object.put(BleConstants.BLE_CMD, baseBluetoothBean.c);
                object.put(BleConstants.BLE_KEY_RESULT, msg);
                BluetoothHandle.handle(object, baseBluetoothBean);
            }
        });
        return true;
    }

    public static boolean sendToken(String token, CameraTokenSynchronizer.Callback callback) {
        OkHttpUtil.postData(getHttpUrl(BleConstants.BLE_CMD_SET_TOKEN),
                GsonUtils.GsonString(new SetTokenRequestBean(token)), null,
                new DeviceHttpCallbackDecode<JSONObject>() {
                    @Override
                    public void onDataSuccess(JSONObject data) {
                        callback.onResult(data != null && BleConstants.BLE_RESULT_SUCCESS
                                .equalsIgnoreCase(data.getString(BleConstants.BLE_KEY_RESULT)));
                    }

                    @Override
                    public void onFailure(String code, String message) {
                        callback.onResult(false);
                    }
                });
        return true;
    }
}
