package com.xh.hotme.utils;

import android.text.TextUtils;

import com.xh.hotme.bean.VersionResultBean;
import com.xh.hotme.listener.IProgressListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateUtil {
    /**
     * 提示版本更新的对话框
     */

    VersionResultBean version;

    String apkFile;
    String _apkTempFile;

    boolean cancel = false;

    public UpdateUtil() {
    }

    public void setVersion(VersionResultBean version) {
        this.version = version;
        //增加版本名称，避免买量下载链接url相同，而导致文件安装错误
        apkFile = AppFileUtil.getApkFilePath(version.getPkgUrl(), "_" + version.getVersionName());

        _apkTempFile = AppFileUtil.getApkFileTempPath(version.getPkgUrl() + "_" + version.getVersionName());
    }

    public boolean isDownload() {
        File file = new File(apkFile);
        return file != null && file.exists();
    }

    /**
     * 安装apk
     */
    public boolean installApk() {
        File file = new File(apkFile);
        if(!file.exists()) {
            return false;
        }
        return BaseAppUtil.installApk(file);
    }

    public void downloadApk(final IProgressListener listener) {
        JSONObject header = null;
        final String url = version.getPkgUrl();

        try {
            if(TextUtils.isEmpty(_apkTempFile)) {
                //增加版本名称，避免买量下载链接url相同，而导致文件安装错误
                _apkTempFile = AppFileUtil.getApkFileTempPath(version.getPkgUrl() + "_" + version.getVersionName());
            }
            final File tempFile = new File(_apkTempFile);

            if(tempFile.exists()) {
                tempFile.delete();
            }

            Headers headers = Headers.of(OkHttpUtil.parseJsonToMap(header));
            Request request = new Request.Builder().headers(headers).url(url).build();

            OkHttpUtil.downLoadFile(request, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    if(listener != null) {
                        listener.abort(e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if(response == null || response.body() == null) {
                        AppTrace.e("UpdateUtil", "服务器无响应");
                        if(listener != null) {
                            listener.abort("服务器无响应");
                        }
                        return;
                    }
                    File tempFile = null;
                    InputStream is = null;
                    FileOutputStream os = null;
                    long sum = 0, total = 0;
                    try {
                        if(TextUtils.isEmpty(_apkTempFile)) {
                            //增加版本名称，避免买量下载链接url相同，而导致文件安装错误
                            _apkTempFile = AppFileUtil.getApkFileTempPath(version.getPkgUrl() + "_" + version.getVersionName());
                        }

                        tempFile = new File(_apkTempFile);
                        is = response.body().byteStream();
                        os = new FileOutputStream(tempFile);
                        total = response.body().contentLength();

                        byte[] buffer = new byte[4096];
                        int len;
                        while((len = is.read(buffer)) >= 0) {
                            os.write(buffer, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / total * 100);

                            if(cancel) {
                                // 删除文件
                                deleteFile(tempFile);

                                AppTrace.d("version update cancel");
                                break;
                            }
                            if(null != listener) {
                                listener.onProgressUpdate(progress, sum, total);
                            }
                        }
                        os.flush();
                        if(cancel) {
                            if(null != listener) {
                                listener.abort("cancel");
                            }
                        } else {

                            //rename
                            FileUtil.renameFile(_apkTempFile, apkFile);

                            if(null != listener) {
                                listener.onComplete();
                            }
                        }

                    } catch(IOException e) {
                        // 删除文件
                        deleteFile(tempFile);

                        if(null != listener) {
                            listener.abort(e.getMessage());
                        }
                    } finally {
                        IOUtil.closeAll(is, os);
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
            if(listener != null) {
                listener.abort(e.getMessage());
            }
        }
    }


    public void setCancel(boolean status) {
        this.cancel = status;
    }

    public boolean isCancel() {
        return cancel;
    }


    public void deleteFile(File file) {
        try {
            if(file != null && file.exists()) {
                file.delete();
                file = null;
            }

        } catch(Throwable t) {

        }

    }
}
