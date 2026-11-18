package com.xh.hotme.storage;

import android.app.Application;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLog;
import static org.junit.Assert.*;

/** Some Android/OEM configurations terminate the process on Log.wtf(). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35}, application = Application.class, shadows = DownloadLoggingTest.LogStorage.class)
public class DownloadLoggingTest {
    @Implements(AppFileUtil.class)
    public static class LogStorage {
        static File directory;
        @Implementation protected static File getAppDir() { return directory; }
    }

    @Before public void setup() {
        ShadowLog.setWtfIsFatal(true);
        AppTrace.setDebugMode(true);
        AppTrace.setOutputToFile(false);
        com.weewa.lib.AppTrace.setDebugMode(true);
        com.weewa.lib.AppTrace.setOutputToFile(false);
    }

    @After public void cleanup() {
        ShadowLog.setWtfIsFatal(false);
        AppTrace.setOutputToFile(true);
        com.weewa.lib.AppTrace.setOutputToFile(true);
    }

    @Test public void appProgressLogDoesNotTriggerFatalSystemPolicy() {
        AppTrace.d("ConnectLogic", "sessionProgress=0.999999");
    }

    @Test public void appTraceLogDoesNotTriggerFatalSystemPolicy() {
        AppTrace.t("TransferFilesActivity", "Download completed");
    }

    @Test public void libraryCompletionLogDoesNotTriggerFatalSystemPolicy() {
        com.weewa.lib.AppTrace.d("FileDual", "no more file to receive");
    }

    @Test public void libraryTraceLogDoesNotTriggerFatalSystemPolicy() {
        com.weewa.lib.AppTrace.t("FileSession", "Transfer stopped");
    }

    @Test public void concurrentProgressAndUiLogsProduceCompleteLines() throws Exception {
        LogStorage.directory = Files.createTempDirectory("download-logs-").toFile();
        AppTrace.setOutputToFile(true);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int worker = 0; worker < 8; worker++) {
                final int id = worker;
                tasks.add(workers.submit(() -> {
                    try { start.await(); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                    for (int i = 0; i < 100; i++) AppTrace.d("DownloadTest", id + ":" + i);
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) task.get(20, TimeUnit.SECONDS);
            File[] logs = LogStorage.directory.listFiles();
            assertEquals(1, logs.length);
            List<String> lines = Files.readAllLines(logs[0].toPath());
            assertEquals(800, lines.size());
            for (String line : lines) assertTrue(line, line.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}    d    \\[weewa\\]DownloadTest    \\d+:\\d+"));
        } finally {
            workers.shutdownNow();
            for (File log : LogStorage.directory.listFiles()) log.delete();
            LogStorage.directory.delete();
        }
    }

    @Test public void unavailableLogDirectoryDoesNotInterruptDownload() throws Exception {
        LogStorage.directory = File.createTempFile("blocked-download-log-", ".tmp");
        AppTrace.setOutputToFile(true);
        try { AppTrace.d("ConnectLogic", "Download completed"); }
        finally { LogStorage.directory.delete(); }
    }
}
