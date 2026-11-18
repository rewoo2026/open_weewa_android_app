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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.demo.databinding.ActivitySingleUploadBinding;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;
import java.util.Random;

public class SingleUploadTaskTestActivity extends BaseActivity<ActivitySingleUploadBinding> {
    private int uploadId1;

    @Override
    protected void initView() {
        viewBinding.startBtn1.setOnClickListener(v -> uploadId1 = createUploadTask(1).start());
        viewBinding.pauseBtn1.setOnClickListener(v -> FileDownloader.getImpl().pause(uploadId1));
    }

    @Override
    protected void initData() {
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
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.getImpl().pause(uploadId1);
    }

    private BaseDownloadTask createUploadTask(final int position) {
        final ViewHolder tag;
        final String url;
        boolean isDir = false;
        String path;
        int count = Constant.SDCARD_BIG_FILES.length;
        int idx = new Random().nextInt(count);

        switch(position) {
            case 1:
                url = Constant.SDCARD_BIG_FILES[idx];
                tag = new ViewHolder(new WeakReference<>(this), viewBinding.progressBar1, null, viewBinding.speedTv1, 1);
                path = Constant.SDCARD_BIG_FILES[idx];
                tag.setFilenameTv(viewBinding.filenameTv1);
                break;
            default:
                url = "";
                tag = null;
                path = "";
                break;
        }

        return FileDownloader.getImpl().create(url)
            .setPath(path, isDir)
            .setUpload(true)
            .setEtag(FileDownloadUtils.getDefaultObjectKey(path))
            .setCallbackProgressTimes(300)
            .setMinIntervalUpdateSpeed(400)
            .setAutoRetryTimes(2)
            .setTag(tag)
            .setListener(new FileDownloadSampleListener() {
                @Override
                protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.pending(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updatePending(task);
                    Log.d("test", "pending, totalBytes: " + totalBytes + ", soFarBytes: " + soFarBytes);
                }

                @Override
                protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.progress(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updateProgress(soFarBytes, totalBytes,
                        task.getSpeed());
                    Log.d("test", "progress, totalBytes: " + totalBytes + ", soFarBytes: " + soFarBytes);
                }

                @Override
                protected void error(BaseDownloadTask task, Throwable e) {
                    super.error(task, e);
                    ((ViewHolder) task.getTag()).updateError(e, task.getSpeed());
                    Log.d("test", "error: " + e.getLocalizedMessage());
                }

                @Override
                protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                    super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updateConnected(etag, task.getFilename());
                    Log.d("test", "connected, totalBytes: " + totalBytes + ", soFarBytes: " + soFarBytes);
                }

                @Override
                protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.paused(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updatePaused(task.getSpeed());
                    Log.d("test", "paused, totalBytes: " + totalBytes + ", soFarBytes: " + soFarBytes);
                }

                @Override
                protected void completed(BaseDownloadTask task) {
                    super.completed(task);
                    ((ViewHolder) task.getTag()).updateCompleted(task);
                    Log.d("test", "completed, task path: " + task.getPath());
                }

                @Override
                protected void warn(BaseDownloadTask task) {
                    super.warn(task);
                    ((ViewHolder) task.getTag()).updateWarn();
                    Log.d("test", "warn, task path: " + task.getPath());
                }
            });
    }

    private static class ViewHolder {
        private final ProgressBar pb;
        private final TextView detailTv;
        private final TextView speedTv;
        private final int position;
        private TextView filenameTv;

        private final WeakReference<SingleUploadTaskTestActivity> weakReferenceContext;

        public ViewHolder(WeakReference<SingleUploadTaskTestActivity> weakReferenceContext,
                          final ProgressBar pb, final TextView detailTv, final TextView speedTv,
                          final int position) {
            this.weakReferenceContext = weakReferenceContext;
            this.pb = pb;
            this.detailTv = detailTv;
            this.position = position;
            this.speedTv = speedTv;
        }

        public void setFilenameTv(TextView filenameTv) {
            this.filenameTv = filenameTv;
        }

        private void updateSpeed(int speed) {
            speedTv.setText(String.format("%dKB/s", speed));
        }

        public void updateProgress(final int sofar, final int total, final int speed) {
            if(total == -1) {
                // chunked transfer encoding data
                pb.setIndeterminate(true);
            } else {
                pb.setMax(total);
                pb.setProgress(sofar);
            }

            updateSpeed(speed);

            if(detailTv != null) {
                detailTv.setText(String.format("sofar: %d total: %d", sofar, total));
            }
        }

        public void updatePending(BaseDownloadTask task) {
            if(filenameTv != null) {
                filenameTv.setText(task.getFilename());
            }
        }

        public void updatePaused(final int speed) {
            toast(String.format("paused %d", position));
            updateSpeed(speed);
            pb.setIndeterminate(false);
        }

        public void updateConnected(String etag, String filename) {
            if(filenameTv != null) {
                filenameTv.setText(filename);
            }
        }

        public void updateWarn() {
            toast(String.format("warn %d", position));
            pb.setIndeterminate(false);
        }

        public void updateError(final Throwable ex, final int speed) {
            toast(String.format("error %d %s", position, ex));
            updateSpeed(speed);
            pb.setIndeterminate(false);
            ex.printStackTrace();
        }

        public void updateCompleted(final BaseDownloadTask task) {

            toast(String.format("completed %d %s", position, task.getTargetFilePath()));

            if(detailTv != null) {
                detailTv.setText(String.format("sofar: %d total: %d",
                    task.getSmallFileSoFarBytes(), task.getSmallFileTotalBytes()));
            }

            updateSpeed(task.getSpeed());
            pb.setIndeterminate(false);
            pb.setMax(task.getSmallFileTotalBytes());
            pb.setProgress(task.getSmallFileSoFarBytes());
        }

        private void toast(final String msg) {
            if(this.weakReferenceContext != null && this.weakReferenceContext.get() != null) {
                Snackbar.make(weakReferenceContext.get().viewBinding.startBtn1, msg, Snackbar.LENGTH_LONG).show();
            }
        }

    }
}
