package com.xh.hotme.http;

import android.text.TextUtils;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.MainHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.Collection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * OKHttp decode
 */
@Keep
public abstract class OkHttpCallbackDecode<E> implements Callback {
    private static final String TAG = "LetoOKHttp";

    private String authkey;
    private final Type _resultType;
    private final long environmentRevision = com.xh.hotme.environment.RuntimeEnvironment.revision();

    private boolean staleEnvironment() {
        return environmentRevision != com.xh.hotme.environment.RuntimeEnvironment.revision();
    }

    public Class<E> var1;

    public OkHttpCallbackDecode(Class<E> var1) {
        this(null, null);
        this.var1 = var1;
    }


    public OkHttpCallbackDecode() {
        this(null, null);
    }

    public OkHttpCallbackDecode(String authkey) {
        this(authkey, null);
    }

    public OkHttpCallbackDecode(Type resultType) {
        this(null, resultType);
    }

    public OkHttpCallbackDecode(String authkey, Type resultType) {
        this.authkey = authkey;
        _resultType = resultType;
    }

    public void setAuthkey(String authkey) {
        this.authkey = authkey;
    }


    @Override
    public void onFailure(Call call, IOException e) {
        if (staleEnvironment()) return;
        AppTrace.d(TAG, "IOException =" + e.getLocalizedMessage());
        try {
            if(e instanceof SocketTimeoutException) {
                onFailure("-2", HotmeApplication.getContext().getString(R.string.network_error_socket_timeout));
            } else if( TextUtils.isEmpty(e.getMessage()) && e.getMessage().startsWith("Connection reset")){
                onFailure("-3", HotmeApplication.getContext().getString(R.string.server_error_http_connect_reset));
            } else {
                onFailure("-1", e != null ? e.getMessage() : "server exception");
            }

        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (!staleEnvironment()) onFinish();
            } catch(Throwable thr) {

            }
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (staleEnvironment()) {
            if (response != null) response.close();
            return;
        }
        try {
            if(response == null) {
                onFailure("-1", "server response is null");
                return;
            }
            if (!response.isSuccessful()) {
                if (response.code() == 401) unauthorized();
                onFailure(String.valueOf(response.code()), "服务暂不可用（HTTP " + response.code() + "）");
                response.close();
                return;
            }
            if (response.body() == null) {
                onFailure("-1", "server response is null");
                return;
            }
            String t = response.body().string();
            if (staleEnvironment()) return;
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

            JSONObject object = new JSONObject(t);
            String data = object.optString("data");
            Integer code = object.optInt("code", -1);
            String msg = object.optString("msg");
            String token = object.optString("token");


            if(code == 401) {
                unauthorized();
            }

            if (code < 0) {
                onFailure("-2", "服务器返回数据格式异常");
                return;
            }

            if(code >= 300) {
                onFailure("" + code, msg);
                return;
            }

            if (code == 205) {
                onFailure("" + code, msg);
                return;
            }

            LoginManager.updateLoginTime();

            // 加的密码登录的两个接口，没有data。。。这个不会影响其他接口吧
            if (TextUtils.isEmpty(data) || "null".equals(data))  {
                if (!TextUtils.isEmpty(token) && !("null".equals(token))) {
                    data = "{\"token\":\"" + token + "\"}";
                }
            }

            if(TextUtils.isEmpty(data) || "null".equals(data)) {//数据是null的

                onDataSuccess(null);
                return;
            }

            Gson g = new Gson();
            E dataObject;
            if(var1 != null) {
                dataObject = g.fromJson(t, var1);
            } else {
                dataObject = _resultType == null ? g.fromJson(data, getTClass()) : g.fromJson(data, _resultType);

            }
            onDataSuccess(dataObject);

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
                onFailure("-10", "Inner error, please try again later.1");
            } catch(Throwable te) {

            }
        } finally {
            try {
                if (!staleEnvironment()) onFinish();
            } catch(Throwable e) {

            }

        }
    }

    public void onFinish() {

    }

    private void unauthorized() {
        MainHandler.runOnUiThread(() -> {
            if (staleEnvironment()) return;
            LoginManager.clearLoginInfo();
            MobileLoginActivity.start(ActivityStackManager.impl.getCurrentActivity());
        });
    }

    public abstract void onDataSuccess(E data);

    public void onFailure(String code, String msg) {

    }

    protected Class<E> getTClass() {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        Type resultType = type.getActualTypeArguments()[0];
        if(resultType instanceof Class) {
            return (Class<E>) resultType;
        } else {
            // 处理集合
            try {
                Field field = resultType.getClass().getDeclaredField("rawTypeName");
                field.setAccessible(true);
                String rawTypeName = (String) field.get(resultType);
                return (Class<E>) Class.forName(rawTypeName);
            } catch(Exception e) {
                return (Class<E>) Collection.class;
            }
        }
    }
}
