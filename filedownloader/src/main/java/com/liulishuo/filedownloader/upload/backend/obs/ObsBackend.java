/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.upload.backend.obs;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.liulishuo.filedownloader.BuildConfig;
import com.liulishuo.filedownloader.download.DownloadRunnable;
import com.liulishuo.filedownloader.download.ProcessCallback;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.util.FileDownloadHelper;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.UploadFileRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObsBackend {
    // 正式上传的桶(缺省)

    private  String OBS_HOST_NAME = "";
    private  String BUCKET_NAME = "";
    private  String ACCESS_KEY_ID = "";
    private  String SECRET_ACCESS_KEY = "";
    private  String END_POINT = "";


    private final UploadResumableClient _client;
    private long _transferredBytes;
    private final File _checkpointDir;

    private static final Pattern _obs_url_pattern = Pattern.compile("https://([\\da-z\\-]{3,63})\\.([^/:]+)(:[0-9]+)?/(.*)");

    private static final Map<Integer, ObsUploadInfo> _infoMap = new HashMap<>();

    public static final class ObsUrlTriple {
        public String bucket;
        public String host;
        public String key;
        public String endPoint;

        public boolean isValid() {
            return !TextUtils.isEmpty(bucket) && !TextUtils.isEmpty(host) && !TextUtils.isEmpty(key);
        }
    }

    public ObsBackend() {
        //初始化obs信息
        ObsInfo obsInfo = FileDownloadUtils.getObsConfig();
        if(obsInfo == null) {//使用缺省配置
            this.OBS_HOST_NAME = ObsConstant.OBS_DEFAULT_HOST_NAME;
            this.BUCKET_NAME = ObsConstant.OBS_DEFAULT_BUCKET_NAME;
            this.ACCESS_KEY_ID = ObsConstant.OBS_DEFAULT_ACCESS_KEY_ID;
            this.SECRET_ACCESS_KEY = ObsConstant.OBS_DEFAULT_SECRET_ACCESS_KEY;
            this.END_POINT = ObsConstant.OBS_DEFAULT_END_POINT;
            Log.d("[weewa]ObsClient", "reset to default config");

            ObsInfo obsInfoDefault = new ObsInfo();
            obsInfoDefault.hostName = ObsConstant.OBS_DEFAULT_HOST_NAME;
            obsInfoDefault.bucketName = ObsConstant.OBS_DEFAULT_BUCKET_NAME;
            obsInfoDefault.accessKeyId = ObsConstant.OBS_DEFAULT_ACCESS_KEY_ID;
            obsInfoDefault.secretAccessKey = ObsConstant.OBS_DEFAULT_SECRET_ACCESS_KEY;
            obsInfoDefault.endPoint = ObsConstant.OBS_DEFAULT_END_POINT;
            FileDownloadUtils.setObsConfig(obsInfoDefault);

        } else {
            this.OBS_HOST_NAME = obsInfo.hostName;
            this.BUCKET_NAME = obsInfo.bucketName;
            this.ACCESS_KEY_ID = obsInfo.accessKeyId;
            this.SECRET_ACCESS_KEY = obsInfo.secretAccessKey;
            this.END_POINT = obsInfo.endPoint;
        }

        // create client
        _client = new UploadResumableClient(newClient());

        // init other
        _transferredBytes = 0;
        _checkpointDir = new File(Environment.getExternalStorageDirectory(),
            FileDownloadHelper.getAppContext().getPackageName() + "/obs_upload");
        if(!_checkpointDir.exists()) {
            _checkpointDir.mkdirs();
        }
    }

    public static ObsClient newClient() {
        Log.d("[weewa]ObsBackend", "newClient");
        String accessKeyId = "";
        String secretAccessKey = "";
        String endPoint = "";
        ObsInfo obsInfo = FileDownloadUtils.getObsConfig();
        if(obsInfo == null) {//使用缺省配置
            Log.d("[weewa]ObsClient", "reset to default config");
            accessKeyId = ObsConstant.OBS_DEFAULT_ACCESS_KEY_ID;
            secretAccessKey = ObsConstant.OBS_DEFAULT_SECRET_ACCESS_KEY;
            endPoint = ObsConstant.OBS_DEFAULT_END_POINT;
        } else {
            accessKeyId = obsInfo.accessKeyId;
            secretAccessKey = obsInfo.secretAccessKey;
            endPoint = obsInfo.endPoint;
        }

        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(endPoint);
        return new ObsClient(accessKeyId, secretAccessKey, config);
    }

    public static ObsClient newClient(String host) {
        Log.d("[weewa]ObsBackend", "newClient2");

        String accessKeyId = "";
        String secretAccessKey = "";

        ObsInfo obsInfo = FileDownloadUtils.getObsConfig();
        if(obsInfo == null) {//使用缺省配置
            Log.d("[weewa]ObsClient", "reset to default config");
            accessKeyId = ObsConstant.OBS_DEFAULT_ACCESS_KEY_ID;
            secretAccessKey = ObsConstant.OBS_DEFAULT_SECRET_ACCESS_KEY;
        } else {
            accessKeyId = obsInfo.accessKeyId;
            secretAccessKey = obsInfo.secretAccessKey;
        }

        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(host);
        return new ObsClient(accessKeyId, secretAccessKey, config);
    }

    public static ObsUrlTriple parse(String url) {
        ObsUrlTriple t = new ObsUrlTriple();
        if(!TextUtils.isEmpty(url)) {
            Matcher m = _obs_url_pattern.matcher(url);
            if(m.matches()) {
                t.bucket = m.group(1);
                t.host = m.group(2);
                t.key = m.group(4);
            }
        }
        return t;
    }

    private static synchronized ObsUploadInfo getInfo(FileDownloadModel model) {
        return _infoMap.get(model.getId());
    }

    public static synchronized ObsUploadInfo removeInfo(int uploadId) {
        return _infoMap.remove(uploadId);
    }

    private static synchronized void setInfo(FileDownloadModel model, ObsUploadInfo info) {
        _infoMap.put(model.getId(), info);
    }

    public static void pause(FileDownloadModel model) {
        ObsUploadInfo info = getInfo(model);
        if(info != null) {
            info.client.abortMultipartUploadSilent();
            removeInfo(model.getId());
        }
    }

    public void uploadFile(FileDownloadModel model, DownloadRunnable hostRunnable, ProcessCallback callback) throws IOException {
        Log.d("[weewa]ObsBackend", "uploadFile...");
        if( TextUtils.isEmpty(OBS_HOST_NAME) || TextUtils.isEmpty(ACCESS_KEY_ID) || TextUtils.isEmpty(SECRET_ACCESS_KEY) || TextUtils.isEmpty(END_POINT)){
            Log.e("[weewa]ObsBackend", "not init obs info");
            return;
        }

        // ensure file exists
        File file = new File(model.getPath());
        if(!file.exists() && file.length() <= 0) {
            return;
        }

        // 计算总段数, 这里保持算法和相机一致, 这样手机这样上传就不会失败, 如果参数不一致就会失败
        long defaultPartSize = 20L * 1024L * 1024L;
        long maxParts = 16L;
        long minPartSize = file.length() / (maxParts - 1L);
        long partSize = Math.max(defaultPartSize, minPartSize);
        long partTotal = (file.length() + partSize - 1L) / partSize;

        // create request, use etag as object key(relative path)
        UploadFileRequest request = new UploadFileRequest(BUCKET_NAME, model.getETag());
        request.setUploadFile(model.getPath());
        request.setTaskNum((int)Math.min(partTotal, Math.max(1, partTotal % 3 + 3)));
        request.setPartSize(partSize);
        request.setEnableCheckpoint(true);
        request.setCheckpointFile(new File(_checkpointDir, "checkpoint_" + model.getId()).getAbsolutePath());
        request.setProgressListener(status -> {
            long bytes = status.getTransferredBytes();
            long delta = bytes - _transferredBytes;
            _transferredBytes = bytes;
            callback.onProgress(delta);
        });

        // save info
        ObsUploadInfo info = new ObsUploadInfo();
        info.client = _client;
        info.request = request;
        setInfo(model, info);

        try {
            // 进行断点续传上传
           CompleteMultipartUploadResult result = _client.uploadFileResume(request);
            Log.d("ObsBackend", "upload Url =" + result.getObjectUrl());
            callback.onCompleted(hostRunnable, model.getTotal(), model.getTotal());
        } catch(ObsException e) {
            e.printStackTrace();
            String msg = e.getLocalizedMessage().toLowerCase();
            if(msg.indexOf("unrepeatable") != -1) {
                callback.onCompleted(hostRunnable, model.getTotal(), model.getTotal());
            } else {
                throw new IOException(e.getLocalizedMessage());
            }
        } catch(Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            removeInfo(model.getId());
        }
    }
}
