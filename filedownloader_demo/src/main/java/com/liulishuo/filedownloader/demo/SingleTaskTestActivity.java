package com.liulishuo.filedownloader.demo;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.demo.databinding.ActivitySingleBinding;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Jacksgong on 12/21/15.
 */
public class SingleTaskTestActivity extends BaseActivity<ActivitySingleBinding> {
    @Override
    protected void initView() {
        llsApkFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmpdir1" + File.separator +
            Constant.LIULISHUO_CONTENT_DISPOSITION_FILENAME;
        llsApkDir = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmpdir1";
        normalTaskFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "tmp2";
        chunkedFilePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + "chunked_data_tmp1";

        initFilePathEqualDirAndFileName();
        initNormalDataAction();
        initChunkTransferEncodingDataAction();
    }

    @Override
    protected void initData() {
    }

    // test for the file path = dir path / content-disposition-filename
    // task1: set {@code llsApkFilePath}
    // task2: set {@code llsApkDir}
    private void initFilePathEqualDirAndFileName() {
        // task 1
        viewBinding.startBtn1.setOnClickListener(v -> downloadId1 = createDownloadTask(1).start());

        viewBinding.pauseBtn1.setOnClickListener(v -> FileDownloader.getImpl().pause(downloadId1));

        viewBinding.deleteBtn1.setOnClickListener(v -> {
            new File(llsApkFilePath).delete();
            new File(FileDownloadUtils.getTempPath(llsApkFilePath)).delete();
        });

        // task 2
        viewBinding.startBtn2.setOnClickListener(v -> downloadId2 = createDownloadTask(2).start());

        viewBinding.pauseBtn2.setOnClickListener(v -> FileDownloader.getImpl().pause(downloadId2));

        viewBinding.deleteBtn2.setOnClickListener(v -> new File(llsApkDir).delete());
    }

    // test for normal task.
    private void initNormalDataAction() {
        // task 3
        viewBinding.startBtn3.setOnClickListener(v -> downloadId3 = createDownloadTask(3).start());

        viewBinding.pauseBtn3.setOnClickListener(v -> FileDownloader.getImpl().pause(downloadId3));

        viewBinding.deleteBtn3.setOnClickListener(v -> {
            new File(normalTaskFilePath).delete();
            new File(FileDownloadUtils.getTempPath(normalTaskFilePath)).delete();
        });
    }

    // test for chunked downloading.
    private void initChunkTransferEncodingDataAction() {
        // task 4
        viewBinding.startBtn4.setOnClickListener(v -> downloadId4 = createDownloadTask(4).start());

        viewBinding.pauseBtn4.setOnClickListener(v -> FileDownloader.getImpl().pause(downloadId4));

        viewBinding.deleteBtn4.setOnClickListener(v -> {
            new File(chunkedFilePath).delete();
            new File(FileDownloadUtils.getTempPath(chunkedFilePath)).delete();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.getImpl().pause(downloadId1);
        FileDownloader.getImpl().pause(downloadId2);
    }

    private BaseDownloadTask createDownloadTask(final int position) {
        final ViewHolder tag;
        final String url;
        boolean isDir = false;
        String path;

        switch(position) {
            case 1:
                url = Constant.LIULISHUO_APK_URL;
                tag = new ViewHolder(new WeakReference<>(this), viewBinding.progressBar1, null, viewBinding.speedTv1, 1);
                path = llsApkFilePath;
                tag.setFilenameTv(viewBinding.filenameTv1);
                break;
            case 2:
                url = Constant.LIULISHUO_APK_URL;
                tag = new ViewHolder(new WeakReference<>(this), viewBinding.progressBar2, null, viewBinding.speedTv2, 2);
                path = llsApkDir;
                isDir = true;
                tag.setFilenameTv(viewBinding.filenameTv2);
                break;
            case 3:
                url = Constant.BIG_FILE_URLS[2];
                tag = new ViewHolder(new WeakReference<>(this), viewBinding.progressBar3, null, viewBinding.speedTv3, 3);
                path = normalTaskFilePath;
                break;
            default:
                url = Constant.CHUNKED_TRANSFER_ENCODING_DATA_URLS[0];
                tag = new ViewHolder(new WeakReference<>(this), viewBinding.progressBar4, viewBinding.detailTv4, viewBinding.speedTv4, 4);
                path = chunkedFilePath;
                break;

        }

        return FileDownloader.getImpl().create(url)
            .setPath(path, isDir)
            .setCallbackProgressTimes(300)
            .setMinIntervalUpdateSpeed(400)
            .setTag(tag)
            .setListener(new FileDownloadSampleListener() {

                @Override
                protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.pending(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updatePending(task);
                }

                @Override
                protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.progress(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updateProgress(soFarBytes, totalBytes,
                        task.getSpeed());
                }

                @Override
                protected void error(BaseDownloadTask task, Throwable e) {
                    super.error(task, e);
                    ((ViewHolder) task.getTag()).updateError(e, task.getSpeed());
                }

                @Override
                protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                    super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updateConnected(etag, task.getFilename());
                }

                @Override
                protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    super.paused(task, soFarBytes, totalBytes);
                    ((ViewHolder) task.getTag()).updatePaused(task.getSpeed());
                }

                @Override
                protected void completed(BaseDownloadTask task) {
                    super.completed(task);
                    ((ViewHolder) task.getTag()).updateCompleted(task);
                }

                @Override
                protected void warn(BaseDownloadTask task) {
                    super.warn(task);
                    ((ViewHolder) task.getTag()).updateWarn();
                }
            });
    }

    private static class ViewHolder {
        private final ProgressBar pb;
        private final TextView detailTv;
        private final TextView speedTv;
        private final int position;
        private TextView filenameTv;

        private final WeakReference<SingleTaskTestActivity> weakReferenceContext;

        public ViewHolder(WeakReference<SingleTaskTestActivity> weakReferenceContext,
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

    private int downloadId1;
    private int downloadId2;
    private int downloadId3;
    private int downloadId4;


    private String llsApkFilePath;
    private String llsApkDir;
    private String normalTaskFilePath;
    private String chunkedFilePath;
}
