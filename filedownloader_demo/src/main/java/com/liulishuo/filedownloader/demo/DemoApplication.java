package com.liulishuo.filedownloader.demo;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;

import cn.dreamtobe.threaddebugger.IThreadDebugger;
import cn.dreamtobe.threaddebugger.ThreadDebugger;
import cn.dreamtobe.threaddebugger.ThreadDebuggers;

/**
 * Created by Jacksgong on 12/17/15.
 */
public class DemoApplication extends Application {
    private final static String TAG = "FileDownloadApplication";
    public static Context CONTEXT;

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // for demo.
        CONTEXT = this;

        if(hasPermission()) {
            File extDir = new File(Environment.getExternalStorageDirectory(), "Download/fd_test");
            File[] files = extDir.listFiles(File::isFile);
            ArrayList<String> flist = new ArrayList<>();
            for(File f : files) {
                flist.add(f.getAbsolutePath());
            }
            Constant.SDCARD_BIG_FILES = flist.toArray(new String[0]);
        }

        // just for open the log in this demo project.
        FileDownloadLog.NEED_LOG = BuildConfig.DOWNLOAD_NEED_LOG;

        /**
         * just for cache Application's Context, and ':filedownloader' progress will NOT be launched
         * by below code, so please do not worry about performance.
         * @see FileDownloader#init(Context)
         */
        FileDownloader.setupOnApplicationOnCreate(this)
            .connectionCreator(new FileDownloadUrlConnection
                .Creator(new FileDownloadUrlConnection.Configuration()
                .connectTimeout(15_000) // set connection timeout.
                .readTimeout(15_000) // set read timeout.
            ))
            .commit();

        // below codes just for monitoring thread pools in the FileDownloader:
        IThreadDebugger debugger = ThreadDebugger.install(
            ThreadDebuggers.create() /** The ThreadDebugger with known thread Categories **/
                // add Thread Category
                .add("OkHttp").add("okio").add("Binder")
                .add(FileDownloadUtils.getThreadPoolName("Network"), "Network")
                .add(FileDownloadUtils.getThreadPoolName("Flow"), "FlowSingle")
                .add(FileDownloadUtils.getThreadPoolName("EventPool"), "Event")
                .add(FileDownloadUtils.getThreadPoolName("LauncherTask"), "LauncherTask")
                .add(FileDownloadUtils.getThreadPoolName("ConnectionBlock"), "Connection")
                .add(FileDownloadUtils.getThreadPoolName("RemitHandoverToDB"), "RemitHandoverToDB")
                .add(FileDownloadUtils.getThreadPoolName("BlockCompleted"), "BlockCompleted"),

            2000, /** The frequent of Updating Thread Activity information **/

            new ThreadDebugger.ThreadChangedCallback() {
                /**
                 * The threads changed callback
                 **/
                @Override
                public void onChanged(IThreadDebugger debugger) {
                    // callback this method when the threads in this application has changed.
                    Log.d(TAG, debugger.drawUpEachThreadInfoDiff());
                    Log.d(TAG, debugger.drawUpEachThreadSizeDiff());
                    Log.d(TAG, debugger.drawUpEachThreadSize());
                }
            });
    }
}
