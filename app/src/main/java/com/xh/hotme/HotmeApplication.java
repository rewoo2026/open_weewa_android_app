package com.xh.hotme;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.commonsdk.listener.OnGetOaidListener;
import com.weewa.lib.WeewaLib;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.broadcast.BleScanBroadcastReceiver;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.Constants;

import org.greenrobot.eventbus.EventBus;


public class HotmeApplication extends Application implements Application.ActivityLifecycleCallbacks {
    @Keep
    public static class AppEnterForegroundEvent {
    }

    @Keep
    public static class AppEnterBackgroundEvent {
    }

    private static final String TAG = "HotmeApplication";

    public static boolean isTest = false;

    public static boolean showModelSwitch = false;

    private static HotmeApplication sInstance;

    private static Context smContext;

    public static String mOAID="";

    private static class AppLifecycleListener implements DefaultLifecycleObserver {
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            AppTrace.d(TAG, "app enter foreground");
            EventBus.getDefault().post(new AppEnterForegroundEvent());
            com.xh.hotme.record.UnattendedRecordService.restore(HotmeApplication.getContext());
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            AppTrace.d(TAG, "app enter background");
            EventBus.getDefault().post(new AppEnterBackgroundEvent());
        }
    }

    public static HotmeApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.xh.hotme.record.OfflineRecordingReports.install();
        com.xh.hotme.utils.MainHandler.runOnUiThread(com.xh.hotme.record.LocalRecordingReports::flush, 3000);

        sInstance = this;

        Constants.TEST_SERVER = BaseAppUtil.getMetaBooleanValue(HotmeApplication.this, "TEST_SERVER");
        Constants.ROLE = BaseAppUtil.getMetaStringValue(HotmeApplication.this, "ROLE");
        com.xh.hotme.environment.RuntimeEnvironment.init(this);
        LoginManager.init();

        showModelSwitch = Constants.ROLE.equalsIgnoreCase("user")
                || Constants.ROLE.equalsIgnoreCase("greenKeeper")
                || Constants.ROLE.equalsIgnoreCase("collector")
                || Constants.ROLE.equalsIgnoreCase("tester")
                || Constants.isGztCrew();

        UMConfigure.setLogEnabled(false);
        UMConfigure.preInit(this, Constants.UMENG_APP_ID
            , "umeng");


        // 关闭日志
//        LogConfigurator.disableLog();

        // File videoFile = AppFileUtil.getVideoDir(HotmeApplication.this);
        //Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(HotmeApplication.this), videoFile.getPath());
        WeewaLib.Companion.shared().setRemoteDirectory(ConnectLogic.getImpl().getBasePath());

        BleScanBroadcastReceiver.getImpl().registerScanBroadcast();

        //下载
        FileDownloader.setupOnApplicationOnCreate(this)
            .connectionCreator(new FileDownloadUrlConnection
                .Creator(new FileDownloadUrlConnection.Configuration()
                .connectTimeout(15_000) // set connection timeout.
                .readTimeout(15_000) // set read timeout.
            ))
            .commit();

        DeviceManager.getImpl();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleListener());

        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        BleScanBroadcastReceiver.getImpl().unregisterScanBroadcast();
    }

    public static Context getContext() {
        if(smContext == null) {
            smContext = getInstance().getApplicationContext();
        }
        return smContext;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        ActivityStackManager.impl.pushActivity(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        ActivityStackManager.impl.setCurrentActivity(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        ActivityStackManager.impl.removeActivity(activity);
    }
}
