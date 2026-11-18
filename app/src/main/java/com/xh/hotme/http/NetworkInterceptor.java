package com.xh.hotme.http;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.utils.NetUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkInterceptor implements Interceptor {

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        // 检查网络连接是否可用
        if(!NetUtil.isConnectedAvailableNetwork()) {
            // 这里可以抛出自定义的异常或者返回特定的错误响应
            throw new IOException("网络不可用");
        }

        // 继续执行原有的请求
        return chain.proceed(request);
    }


}
