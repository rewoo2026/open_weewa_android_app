package com.xh.hotme.me.setmeal.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.ipaynow.plugin.api.IpaynowPlugin;
import com.ipaynow.plugin.manager.route.dto.ResponseParams;
import com.ipaynow.plugin.manager.route.impl.ReceivePayResult;
import com.ipaynow.plugin.view.IpaynowLoading;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.Constants;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.xh.hotme.me.UserDeviceTypeManager;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/17
 * @Description: 套餐支付
 */
public class SetMealPlayManager implements ReceivePayResult {
    private final String appId = "";
    private final String appKey = "";

    //测试用
//    private final String appId = "";
//    private final String appKey = "";


    private final IpaynowPlugin mIpaynowplugin;
    private final IpaynowLoading mLoadingDialog;
    private final HashMap<String, String> reqParams = new HashMap<>();

    private IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }

    public SetMealPlayManager(Context context) {
        mIpaynowplugin = IpaynowPlugin.getInstance().init(context);
        mIpaynowplugin.unCkeckEnvironment();

        mLoadingDialog = mIpaynowplugin.getDefaultLoading();
    }


    public void toPay(int type, String orderNo, String orderName, String orderAmt, String orderStartTime
        , String mhtOrderDetail) {
        try {
            orderAmt = OrderAmount.positiveFen(orderAmt);
        } catch (IllegalArgumentException error) {
            Toast.makeText(HotmeApplication.getContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        showLoading();

        if(TextUtils.isEmpty(mhtOrderDetail)) {
            mhtOrderDetail = orderName;
        }
        String escapedDetail = mhtOrderDetail.replace("&", "%26");

//        orderNo = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        AppTrace.e("orderNo  ----  " + orderNo);
        createPayMessage(orderNo, orderName, orderAmt, orderStartTime, escapedDetail);
        if(type == 1) {//支付宝
            reqParams.put("payChannelType", "12");
        } else {
            reqParams.put("payChannelType", "13");
        }

        GetMessage gm = new GetMessage();
        gm.execute();
    }

    private void createPayMessage(String mhtOrderNo, String orderName, String orderAmt, String mhtOrderStartTime
        , String mhtOrderDetail) {
        reqParams.put("funcode", "WP001");
        reqParams.put("version", "1.0.3");
        reqParams.put("appId", appId);
        reqParams.put("mhtOrderNo", mhtOrderNo);
        reqParams.put("mhtOrderName", orderName);
        reqParams.put("mhtOrderType", "02");
        reqParams.put("mhtCurrencyType", "156");
        reqParams.put("mhtOrderAmt", orderAmt);
        reqParams.put("mhtOrderDetail", mhtOrderDetail);
        reqParams.put("mhtOrderTimeOut", "3600");
        reqParams.put("mhtOrderStartTime", mhtOrderStartTime);

        String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
        if (deviceType.equals("6")||deviceType.equals("7")) {
            reqParams.put("notifyUrl", SdkApi.pay_notify_V2());
        }else{
            reqParams.put("notifyUrl", SdkApi.pay_notify());
        }

        reqParams.put("mhtCharset", "UTF-8");
        reqParams.put("deviceType", "01");
        reqParams.put("mhtSubAppId", Constants.WEIXIN_APP_ID);
        reqParams.put("mhtLimitPay", "1");
        reqParams.put("mhtSignType", "MD5");
    }


    private void showLoading() {
        mLoadingDialog.setLoadingMsg("正在生成订单");
        mLoadingDialog.show();
    }

    private void disLoading() {
        if(mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void onIpaynowTransResult(ResponseParams responseParams) {
        String respCode = responseParams.respCode;
        String errorCode = responseParams.errorCode;
        String errorMsg = responseParams.respMsg;
        StringBuilder temp = new StringBuilder();
        if(commonListener != null) {
            commonListener.onReturn(1, respCode);
        }
        if(respCode.equals("00")) {
            temp.append("交易状态:成功");
        } else if(respCode.equals("02")) {
            temp.append("交易状态:取消");
        } else if(respCode.equals("01")) {
            temp.append("交易状态:失败").append("\n").append("错误码:").append(errorCode).append("原因:" + errorMsg);
        } else if(respCode.equals("03")) {
            temp.append("交易状态:未知").append("\n").append("原因:" + errorMsg);
        } else {
            temp.append("respCode=").append(respCode).append("\n").append("respMsg=").append(errorMsg);
        }
//        "onIpaynowTransResult:"
        Toast.makeText(HotmeApplication.getContext(), temp.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPluginError(Throwable throwable) {

    }


    public class GetMessage extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... params) {

            String preSignStr = createFormString(reqParams, true, false);
            String needcheckmsg = "mhtSignature=" + Md5Util.md5(preSignStr + "&" + Md5Util.md5(appKey));
            needcheckmsg = preSignStr + "&" + needcheckmsg;
            return needcheckmsg;
        }

        protected void onPostExecute(String requestMessage) {
            mLoadingDialog.dismiss();
            // 如商户保留域mhtReserved包含特殊字符，在调起插件前对value做一次utf8编码
            String mhtReserved = reqParams.get("mhtReserved");
            if(!TextUtils.isEmpty(mhtReserved) && requestMessage.contains(mhtReserved))
                requestMessage = requestMessage.replace(mhtReserved, URLEncoder.encode(mhtReserved));
            // 设置支付结果回调接口，同时调起支付请求
            mIpaynowplugin
//                    .setMiniProgramEnv(0)
                .setCustomLoading(mLoadingDialog)
                .setCallResultReceiver(SetMealPlayManager.this)
                .pay(requestMessage);
        }

    }

    private String createFormString(Map<String, String> para, boolean sort, boolean encode) {
        List<String> keys = new ArrayList<>(para.keySet());
        if(sort) {
            Collections.sort(keys);
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = para.get(key);
            if(encode) {
                value = UrlEncodeUtils.urlEncode(value);
            }
            if(i == keys.size() - 1) {
                sb.append(key).append("=").append(value);
            } else {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        return sb.toString();
    }

}
