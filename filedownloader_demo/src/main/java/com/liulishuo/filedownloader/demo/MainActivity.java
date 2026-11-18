package com.liulishuo.filedownloader.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.liulishuo.filedownloader.FileDownloadMonitor;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.demo.databinding.ActivityMainBinding;
import com.liulishuo.filedownloader.demo.performance.PerformanceTestActivity;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    @Override
    protected void initView() {
    }

    @Override
    protected void initData() {
        // 这是只是为了全局监控。如果你有需求需要全局监控（比如用于打点/统计）可以使用这个方式，如果没有类似需求就不需要
        // 如果你有这个需求，实现FileDownloadMonitor.IMonitor接口，也使用FileDownloadMonitor.setGlobalMonitor
        // 注册进去即可
        // You do not have to add below code to your project only if you need monitor the global
        // FileDownloader Engine for statistic or others
        // If you have such requirement, just implement FileDownloadMonitor.IMonitor, and register it
        // use FileDownloadDownloader.setGlobalMonitor the same as below code.
        FileDownloadMonitor.setGlobalMonitor(GlobalMonitor.getImpl());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(!hasPermission()) {
            applyPermission();
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ;
        }
    }

    private void applyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(hasPermission() && Constant.SDCARD_BIG_FILES.length <= 0) {
            File extDir = new File(Environment.getExternalStorageDirectory(), "Download/fd_test");
            File[] files = extDir.listFiles(File::isFile);
            ArrayList<String> flist = new ArrayList<>();
            for(File f : files) {
                flist.add(f.getAbsolutePath());
            }
            Constant.SDCARD_BIG_FILES = flist.toArray(new String[0]);
        }
    }

    public void onClickMultitask(final View view) {
        startActivity(new Intent(this, MultitaskTestActivity.class));
    }

    public void onClickSingle(final View view) {
        startActivity(new Intent(this, SingleTaskTestActivity.class));
    }

    public void onClickMultiUpload(final View view) {
        startActivity(new Intent(this, MultiUploadTestActivity.class));
    }

    public void onClickSingleUpload(final View view) {
        startActivity(new Intent(this, SingleUploadTaskTestActivity.class));
    }

    public void onClickHybridTest(final View view) {
        startActivity(new Intent(this, HybridTestActivity.class));
    }

    public void onClickTasksManager(final View view) {
        startActivity(new Intent(this, TasksManagerDemoActivity.class));
    }

    public void onClickTasksManagerUpload(final View view) {
        startActivity(new Intent(this, TasksManagerUploadDemoActivity.class));
    }

    public void onClickPerformance(final View view) {
        startActivity(new Intent(this, PerformanceTestActivity.class));
    }

    public void onClickNotification(final View view) {
        startActivity(new Intent(this, NotificationSampleActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_github) {
            openGitHub();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openGitHub() {
        Uri uri = Uri.parse(getString(R.string.app_github_url));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbind and stop service manually if idle
        FileDownloader.getImpl().unBindServiceIfIdle();

        FileDownloadMonitor.releaseGlobalMonitor();
    }
}
