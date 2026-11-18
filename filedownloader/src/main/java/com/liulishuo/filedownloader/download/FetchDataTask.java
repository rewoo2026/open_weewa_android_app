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

package com.liulishuo.filedownloader.download;

import android.os.SystemClock;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException;
import com.liulishuo.filedownloader.exception.FileDownloadNetworkPolicyException;
import com.liulishuo.filedownloader.database.FileDownloadDatabase;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;
import com.liulishuo.filedownloader.upload.backend.obs.ObsBackend;
import com.liulishuo.filedownloader.util.FileDownloadLog;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.liulishuo.filedownloader.model.FileDownloadModel.TOTAL_VALUE_IN_CHUNKED_RESOURCE;

/**
 * Fetch data from the provided connection.
 */
public class FetchDataTask {

    static final int BUFFER_SIZE = 1024 * 4;
    private final ProcessCallback callback;

    private final int connectionIndex;
    private final DownloadRunnable hostRunnable;
    private final FileDownloadConnection connection;
    private final boolean isWifiRequired;

    private final long startOffset;
    private final long endOffset;
    private final long contentLength;
    private final FileDownloadModel model;

    long currentOffset;
    private FileDownloadOutputStream outputStream;

    private volatile boolean paused;

    public void pause() {
        paused = true;
    }

    private FetchDataTask(FileDownloadModel model, FileDownloadConnection connection, ConnectionProfile connectionProfile,
                          DownloadRunnable host, int connectionIndex,
                          boolean isWifiRequired, ProcessCallback callback) {
        this.callback = callback;
        this.model = model;
        this.connection = connection;
        this.isWifiRequired = isWifiRequired;
        this.hostRunnable = host;
        this.connectionIndex = connectionIndex;
        this.database = CustomComponentHolder.getImpl().getDatabaseInstance();

        startOffset = connectionProfile.startOffset;
        endOffset = connectionProfile.endOffset;
        currentOffset = connectionProfile.currentOffset;
        contentLength = connectionProfile.contentLength;
    }

    public void run() throws IOException, IllegalAccessException, IllegalArgumentException,
            FileDownloadGiveUpRetryException {

        if (paused) return;

        if(model.isUpload()) {
            switch(model.getBackend()) {
                case BaseDownloadTask.BACKEND_HUAWEI_OBS:
                    ObsBackend obs = new ObsBackend();
                    obs.uploadFile(model, hostRunnable, callback);
                    break;
            }
        } else {
            long contentLength = FileDownloadUtils.findContentLength(connectionIndex, connection);
            if(contentLength == TOTAL_VALUE_IN_CHUNKED_RESOURCE) {
                contentLength = FileDownloadUtils.findContentLengthFromContentRange(connection);
            }
            if(contentLength == 0) {
                throw new FileDownloadGiveUpRetryException(FileDownloadUtils.
                    formatString(
                        "there isn't any content need to download on %d-%d with the "
                            + "content-length is 0",
                        model.getId(), connectionIndex));
            }

            if(this.contentLength > 0 && contentLength != this.contentLength) {
                final String range;
                if(endOffset == ConnectionProfile.RANGE_INFINITE) {
                    range = FileDownloadUtils.formatString("range[%d-)", currentOffset);
                } else {
                    range = FileDownloadUtils.formatString("range[%d-%d)", currentOffset, endOffset);
                }
                throw new FileDownloadGiveUpRetryException(FileDownloadUtils.
                    formatString("require %s with contentLength(%d), but the "
                            + "backend response contentLength is %d on "
                            + "downloadId[%d]-connectionIndex[%d], please ask your backend "
                            + "dev to fix such problem.",
                        range, this.contentLength, contentLength, model.getId(), connectionIndex));
            }

            final long fetchBeginOffset = currentOffset;
            // start fetch
            InputStream inputStream = null;
            FileDownloadOutputStream outputStream = null;

            try {
                final boolean isSupportSeek = CustomComponentHolder.getImpl().isSupportSeek();
                if(hostRunnable != null && !isSupportSeek) {
                    throw new IllegalAccessException(
                        "can't using multi-download when the output stream can't support seek");
                }

                this.outputStream = outputStream = FileDownloadUtils.createOutputStream(model.getTempFilePath());
                if(isSupportSeek) {
                    outputStream.seek(currentOffset);
                }

                if(FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(this, "start fetch(%d): range [%d, %d), seek to[%d]",
                        connectionIndex, startOffset, endOffset, currentOffset);
                }

                inputStream = connection.getInputStream();

                byte[] buff = new byte[BUFFER_SIZE];

                if(paused) return;

                do {
                    int byteCount = inputStream.read(buff);
                    if(byteCount == -1) {
                        break;
                    }

                    outputStream.write(buff, 0, byteCount);

                    currentOffset += byteCount;

                    // callback progress
                    callback.onProgress(byteCount);

                    checkAndSync();

                    // check status
                    if(paused) return;

                    if(isWifiRequired && FileDownloadUtils.isNetworkNotOnWifiType()) {
                        throw new FileDownloadNetworkPolicyException();
                    }
                } while(true);
            } finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if(outputStream != null) sync();
                } finally {
                    if(outputStream != null) {
                        try {
                            outputStream.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

            final long fetchedLength = currentOffset - fetchBeginOffset;
            if(contentLength != TOTAL_VALUE_IN_CHUNKED_RESOURCE && contentLength != fetchedLength) {
                throw new FileDownloadGiveUpRetryException(
                    FileDownloadUtils.formatString("fetched length[%d] != content length[%d],"
                            + " range[%d, %d) offset[%d] fetch begin offset[%d]",
                        fetchedLength, contentLength,
                        startOffset, endOffset, currentOffset, fetchBeginOffset));
            }

            // callback completed
            callback.onCompleted(hostRunnable, startOffset, endOffset);
        }
    }

    private final FileDownloadDatabase database;
    private volatile long lastSyncBytes = 0;
    private volatile long lastSyncTimestamp = 0;

    private void checkAndSync() {
        final long now = SystemClock.elapsedRealtime();
        final long bytesDelta = currentOffset - lastSyncBytes;
        final long timestampDelta = now - lastSyncTimestamp;

        if (FileDownloadUtils.isNeedSync(bytesDelta, timestampDelta)) {
            sync();

            lastSyncBytes = currentOffset;
            lastSyncTimestamp = now;
        }
    }

    private void sync() {
        final long startTimestamp = SystemClock.uptimeMillis();

        boolean bufferPersistToDevice;
        try {
            outputStream.flushAndSync();
            bufferPersistToDevice = true;
        } catch (IOException e) {
            bufferPersistToDevice = false;
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(this, "Because of the system cannot guarantee that all "
                        + "the buffers have been synchronized with physical media, or write to file"
                        + "failed, we just not flushAndSync process to database too %s", e);
            }
        }

        if (bufferPersistToDevice) {
            final boolean isBelongMultiConnection = connectionIndex >= 0;
            if (isBelongMultiConnection) {
                // only need update the connection table.
                database.updateConnectionModel(model.getId(), connectionIndex, currentOffset);
            } else {
                // only need update the filedownloader table.
                callback.syncProgressFromCache();
            }

            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog
                        .d(this, "require flushAndSync id[%d] index[%d] offset[%d], consume[%d]",
                                model.getId(), connectionIndex, currentOffset,
                                SystemClock.uptimeMillis() - startTimestamp);
            }
        }
    }

    public static class Builder {
        DownloadRunnable downloadRunnable;
        FileDownloadConnection connection;
        ConnectionProfile connectionProfile;
        ProcessCallback callback;
        Boolean isWifiRequired;
        Integer connectionIndex;
        FileDownloadModel model;

        public Builder setConnection(FileDownloadConnection connection) {
            this.connection = connection;
            return this;
        }

        public Builder setModel(FileDownloadModel model) {
            this.model = model;
            return this;
        }

        public Builder setConnectionProfile(ConnectionProfile connectionProfile) {
            this.connectionProfile = connectionProfile;
            return this;
        }

        public Builder setCallback(ProcessCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setWifiRequired(boolean wifiRequired) {
            isWifiRequired = wifiRequired;
            return this;
        }

        public Builder setHost(DownloadRunnable downloadRunnable) {
            this.downloadRunnable = downloadRunnable;
            return this;
        }

        public Builder setConnectionIndex(int connectionIndex) {
            this.connectionIndex = connectionIndex;
            return this;
        }

        public FetchDataTask build() throws IllegalArgumentException {
            if (isWifiRequired == null || connection == null || connectionProfile == null
                    || callback == null || model.getTempFilePath() == null || model.getId() == 0
                    || connectionIndex == null) {
                throw new IllegalArgumentException();
            }

            return new FetchDataTask(model, connection, connectionProfile, downloadRunnable, connectionIndex,
                    isWifiRequired, callback);
        }

    }
}
