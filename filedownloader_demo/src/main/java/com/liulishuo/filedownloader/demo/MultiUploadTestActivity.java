/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.demo.databinding.ActivityMultiUploadTestBinding;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 12/25/15.
 */
public class MultiUploadTestActivity extends BaseActivity<ActivityMultiUploadTestBinding> {

    private final static String TAG = "MultitaskTestActivity";

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(!hasPermission()) {
            applyPermission();
        }
    }

    private boolean hasPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ;
        }
    }

    private void applyPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch(Exception e) {
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
    protected void onDestroy() {
        super.onDestroy();
        pause();
    }

    @Override
    protected void initView() {
        resetDisplayData();

        viewBinding.actionBtn.setTag(true);
        viewBinding.actionBtn.setOnClickListener(v -> {
            final boolean toStart = (boolean) viewBinding.actionBtn.getTag();
            if(toStart) {
                if(start()) {
                    viewBinding.actionBtn.setText(R.string.pause);
                    viewBinding.actionBtn.setTag(false);
                }
            } else {
                viewBinding.actionBtn.setText(R.string.start);
                pause();
                viewBinding.actionBtn.setTag(true);
            }

        });

        viewBinding.taskCountSb.setMax(Constant.URLS.length);
        viewBinding.taskCountSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewBinding.taskCountTv.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        viewBinding.avoidMissFrameCb.setChecked(FileDownloader.isEnabledAvoidDropFrame());
        viewBinding.avoidMissFrameCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                FileDownloader.enableAvoidDropFrame();
            } else {
                FileDownloader.disableAvoidDropFrame();
            }
        });
    }

    @Override
    protected void initData() {
    }

    private void resetDisplayData() {
        viewBinding.pendingPb.setProgress(0);
        viewBinding.connectedPb.setProgress(0);
        viewBinding.progressPb.setProgress(0);
        viewBinding.retryPb.setProgress(0);
        viewBinding.errorPb.setProgress(0);
        viewBinding.pausedPb.setProgress(0);
        viewBinding.completedWithOldPb.setProgress(0);
        viewBinding.completedPb.setProgress(0);
        viewBinding.warnPb.setProgress(0);
        viewBinding.overTaskPb.setProgress(0);

        viewBinding.pendingTv.setText(getString(R.string.multitask_test_pending, 0));
        viewBinding.connectedTv.setText(getString(R.string.multitask_test_connected, 0));
        viewBinding.progressTv.setText(getString(R.string.multitask_test_progress, 0));
        viewBinding.retryTv.setText(getString(R.string.multitask_test_retry, 0));
        viewBinding.errorTv.setText(getString(R.string.multitask_test_error, 0));
        viewBinding.pausedTv.setText(getString(R.string.multitask_test_paused, 0));
        viewBinding.completedWithOldTv.setText(getString(R.string.multitask_test_completed_reused, 0));
        viewBinding.completedTv.setText(getString(R.string.multitask_test_completed_downloading, 0));
        viewBinding.warnTv.setText(getString(R.string.multitask_test_warn, 0));

        viewBinding.pendingInfoTv.setText("");
        viewBinding.connectedInfoTv.setText("");
        viewBinding.retryInfoTv.setText("");
        viewBinding.progressInfoTv.setText("");
        viewBinding.errorInfoTv.setText("");
        viewBinding.pausedInfoTv.setText("");
        viewBinding.completedWithOldInfoTv.setText("");
        viewBinding.completedInfoTv.setText("");
        viewBinding.warnInfoTv.setText("");
    }

    private long start = 0;

    private boolean start() {
        final int count = Math.min(Constant.SDCARD_BIG_FILES.length, Integer.valueOf(viewBinding.taskCountTv.getText().toString()));
        if(count <= 0) {
            return false;
        }

        viewBinding.taskCountSb.setEnabled(false);

        viewBinding.pendingPb.setMax(count);
        viewBinding.connectedPb.setMax(count);
        viewBinding.progressPb.setMax(count);
        viewBinding.retryPb.setMax(count);
        viewBinding.errorPb.setMax(count);
        viewBinding.pausedPb.setMax(count);
        viewBinding.completedWithOldPb.setMax(count);
        viewBinding.completedPb.setMax(count);
        viewBinding.warnPb.setMax(count);
        viewBinding.overTaskPb.setMax(count);


        resetDisplayData();

        // 需要时再显示
        viewBinding.retryInfoTv.setVisibility(View.GONE);
        viewBinding.retryPb.setVisibility(View.GONE);
        viewBinding.retryTv.setVisibility(View.GONE);

        isStopTimer = false;
        viewBinding.timeConsumeTv.setTag(0);
        goTimeCount();


        start = System.currentTimeMillis();
        // =================== How to Download tasks: =============================
        downloadListener = createLis();
//        The first way-----------------------------:
//        for (int i = 0; i < count; i++) {
//            final String url = Constant.URLS[i];
//            FileDownloader.getImpl().create(url)
//                    .setListener(downloadListener)
//                    .setAutoRetryTimes(1)
//                    .setTag(i + 1)
//                    .setCallbackProgressTimes(0)
//                    .asInQueueTask()
//                    .enqueue();
//        }
//        FileDownloader.getImpl().start(downloadListener, serialRbtn.isChecked());

//        The second way----------------------------:

        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(downloadListener);

        final List<BaseDownloadTask> tasks = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            tasks.add(FileDownloader.getImpl()
                .create(Constant.SDCARD_BIG_FILES[i])
                .setPath(Constant.SDCARD_BIG_FILES[i], false)
                .setEtag(FileDownloadUtils.getDefaultObjectKey(Constant.SDCARD_BIG_FILES[i]))
                .setTag(i + 1)
                .setUpload(true)
            );
        }
        queueSet.disableCallbackProgressTimes(); // do not want each task's download progress's callback,
        // we just consider which task will completed.

        // auto retry 1 time if download fail
        queueSet.setAutoRetryTimes(1);

        if(viewBinding.serialRbtn.isChecked()) {
            // start download in serial order
            queueSet.downloadSequentially(tasks);
            // if your tasks are not a list, invoke such following will more readable:
//            queueSet.downloadSequentially(
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).addHeader(...,...),
//                    FileDownloader.getImpl().create(url).setPath(...)
//            );
        } else {
            // start parallel download
            queueSet.downloadTogether(tasks);
            // if your tasks are not a list, invoke such following will more readable:
//            queueSet.downloadTogether(
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).setPath(...),
//                    FileDownloader.getImpl().create(url).setSyncCallback(true)
//            );
        }
        queueSet.start();

        return true;
    }

    private FileDownloadListener downloadListener;

    private void pause() {
        FileDownloader.getImpl().pause(downloadListener);
        stopTimeCount();
        viewBinding.taskCountSb.setEnabled(true);
    }

    private void stopTimeCount() {
        if(isFinishing()) {
            return;
        }

        isStopTimer = true;
        viewBinding.timeConsumeTv.getHandler().removeCallbacks(timeCountRunnable);
        final long consume = System.currentTimeMillis() - start;
        viewBinding.timeConsumeTv.setText(String.valueOf(consume / 1000f));
    }

    private FileDownloadListener createLis() {
        return new FileDownloadListener() {

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                // 之所以加这句判断，是因为有些异步任务在pause以后，会持续回调pause回来，而有些任务在pause之前已经完成，
                // 但是通知消息还在线程池中还未回调回来，这里可以优化
                // 后面所有在回调中加这句都是这个原因
                if(task.getListener() != downloadListener) {
                    return;
                }
                viewBinding.pendingPb.setProgress(viewBinding.pendingPb.getProgress() + 1);
                viewBinding.pendingTv.setText(getString(R.string.multitask_test_pending, viewBinding.pendingPb.getProgress()));
                viewBinding.pendingInfoTv.append((int) task.getTag() + " | ");
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue,
                                     int soFarBytes, int totalBytes) {

                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                if(task.getListener() != downloadListener) {
                    return;
                }

                viewBinding.connectedPb.setProgress(viewBinding.connectedPb.getProgress() + 1);
                viewBinding.connectedTv.setText(getString(R.string.multitask_test_connected,
                    viewBinding.connectedPb.getProgress()));

                viewBinding.connectedInfoTv.append((int) task.getTag() + " | ");
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if(task.getListener() != downloadListener) {
                }
//                progressPb.setProgress(progressPb.getProgress() + 1);
//                progressTv.setText("progress: " + progressPb.getProgress());
//                progressInfoTv.append((int)task.getTag() + " | ");
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
                if(task.getListener() != downloadListener) {
                }
            }

            @Override
            protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
                super.retry(task, ex, retryingTimes, soFarBytes);
                if(task.getListener() != downloadListener) {
                    return;
                }

                viewBinding.retryInfoTv.setVisibility(View.VISIBLE);
                viewBinding.retryPb.setVisibility(View.VISIBLE);
                viewBinding.retryTv.setVisibility(View.VISIBLE);

                viewBinding.retryPb.setProgress(viewBinding.retryPb.getProgress() + 1);
                viewBinding.retryTv.setText(getString(R.string.multitask_test_retry, viewBinding.retryPb.getProgress()));
                viewBinding.retryInfoTv.append((int) task.getTag() + " | ");
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                if(task.getListener() != downloadListener) {
                    return;
                }

                if(task.isReusedOldFile()) {
                    viewBinding.completedWithOldPb.setProgress(viewBinding.completedWithOldPb.getProgress() + 1);
                    viewBinding.completedWithOldTv.setText(getString(R.string.multitask_test_completed_reused,
                        viewBinding.completedWithOldPb.getProgress()));
                    viewBinding.completedWithOldInfoTv.append((int) task.getTag() + " | ");
                } else {
                    viewBinding.completedPb.setProgress(viewBinding.completedPb.getProgress() + 1);
                    viewBinding.completedTv.
                        setText(getString(R.string.multitask_test_completed_uploading,
                            viewBinding.completedPb.getProgress()));

                    viewBinding.completedInfoTv.append((int) task.getTag() + " | ");
                }

                viewBinding.overTaskPb.setProgress(viewBinding.overTaskPb.getProgress() + 1);
                checkEndAll();
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if(task.getListener() != downloadListener) {
                    return;
                }
                viewBinding.pausedPb.setProgress(viewBinding.pausedPb.getProgress() + 1);
                viewBinding.pausedTv.setText(getString(R.string.multitask_test_paused, viewBinding.pausedPb.getProgress()));
                viewBinding.pausedInfoTv.append((int) task.getTag() + " | ");
                viewBinding.overTaskPb.setProgress(viewBinding.overTaskPb.getProgress() + 1);
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                if(task.getListener() != downloadListener) {
                    return;
                }
                viewBinding.errorPb.setProgress(viewBinding.errorPb.getProgress() + 1);
                viewBinding.errorTv.setText(getString(R.string.multitask_test_error, viewBinding.errorPb.getProgress()));
                viewBinding.errorInfoTv.append((int) task.getTag() + " | ");
                viewBinding.overTaskPb.setProgress(viewBinding.overTaskPb.getProgress() + 1);
                checkEndAll();
            }

            @Override
            protected void warn(BaseDownloadTask task) {
                if(task.getListener() != downloadListener) {
                    return;
                }

                viewBinding.warnPb.setProgress(viewBinding.warnPb.getProgress() + 1);
                viewBinding.warnTv.setText(getString(R.string.multitask_test_warn, viewBinding.warnPb.getProgress()));
                viewBinding.warnInfoTv.append((int) task.getTag() + " | ");
                viewBinding.overTaskPb.setProgress(viewBinding.overTaskPb.getProgress() + 1);
                checkEndAll();
            }
        };
    }

    private void checkEndAll() {
        final boolean isEndAll = viewBinding.overTaskPb.getProgress() >= Integer.valueOf(viewBinding.taskCountTv.getText().toString());
        if(isEndAll) {

            Log.d(TAG, String.format("start[%d] over[%d]", GlobalMonitor.getImpl().getMarkStart(),
                GlobalMonitor.getImpl().getMarkOver()));

            stopTimeCount();
            viewBinding.actionBtn.setTag(true);
            viewBinding.actionBtn.setText("Start");
            viewBinding.taskCountSb.setEnabled(true);
        }
    }

    private boolean isStopTimer = true;

    private void goTimeCount() {
        if(isFinishing()) {
            return;
        }
        final int time = (int) viewBinding.timeConsumeTv.getTag();
        viewBinding.timeConsumeTv.setText(String.valueOf(time));
        viewBinding.timeConsumeTv.getHandler().postDelayed(timeCountRunnable, 1000);
    }

    private final Runnable timeCountRunnable = () -> {
        if(isStopTimer) {
            return;
        }
        viewBinding.timeConsumeTv.setTag((int) viewBinding.timeConsumeTv.getTag() + 1);
        goTimeCount();
    };
}
