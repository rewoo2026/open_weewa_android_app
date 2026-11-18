package com.xh.hotme.http;

import android.text.TextUtils;

import androidx.annotation.Keep;

import com.google.gson.JsonParseException;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.utils.AppTrace;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * OKHttp decode
 */
@Keep
public abstract class OkHttpCallbackJson<JSONObject> implements Callback {
    private static final String TAG = "LetoOKHttp";

    private String authkey;
    private final Type _resultType;

    public Class<JSONObject> var1;


    public OkHttpCallbackJson() {
        this(null, null);
    }

    public OkHttpCallbackJson(String authkey) {
        this(authkey, null);
    }

    public OkHttpCallbackJson(Type resultType) {
        this(null, resultType);
    }

    public OkHttpCallbackJson(String authkey, Type resultType) {
        this.authkey = authkey;
        _resultType = resultType;
    }

    public void setAuthkey(String authkey) {
        this.authkey = authkey;
    }


    @Override
    public void onFailure(Call call, IOException e) {
        AppTrace.d(TAG, "IOException =" + e.getLocalizedMessage());
        try {
            onFailure("-1", e != null ? e.getMessage() : "server exception");
        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                onFinish();
            } catch(Throwable thr) {

            }
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        try {
            if(response == null || response.body() == null) {
                onFailure("-1", "server response is null");
                return;
            }
            String t = response.body().string();
            if(TextUtils.isEmpty(t) || t.equalsIgnoreCase("null")) {
                onFailure("-1", "server response body is null");
                return;
            }

            try {
                if(response.body() != null) {
                    response.body().close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            AppTrace.d(TAG, "onResponse =" + t);

            org.json.JSONObject jsonObject = new org.json.JSONObject(t);

            LoginManager.updateLoginTime();


            onDataSuccess(jsonObject);

        } catch(JSONException e) {
            e.printStackTrace();
            try {
                onFailure("-2", "Json exception.");
            } catch(Throwable te) {

            }
        } catch(JsonParseException e) {
            e.printStackTrace();
            try {
                onFailure("-3", "Json parse exception.");
            } catch(Throwable te) {

            }
        } catch(Throwable e) {
            e.printStackTrace();
            try {
                onFailure("-10", "Inner error, please try again later.");
            } catch(Throwable te) {

            }
        } finally {
            try {
                onFinish();
            } catch(Throwable e) {

            }

        }
    }

    public void onFinish() {

    }

    public abstract void onDataSuccess(org.json.JSONObject data);

    public void onFailure(String code, String msg) {

    }

}
