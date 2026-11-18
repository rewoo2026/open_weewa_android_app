package com.xh.hotme.test.factory;

import android.content.Context;
import android.content.Intent;



/**
 * date   : 2023/3/13
 * desc   :
 */
public class ScanHelper {

    private static class Holder {
        private static final ScanHelper instance = new ScanHelper();
    }

    private ScanCallback scanCallback;

    private ScanHelper() {
    }

    public static ScanHelper getInstance() {
        return Holder.instance;
    }

    public void scan(Context context, ScanCallback scanCallback) {
        if (context == null) {
            return;
        }
        this.scanCallback = scanCallback;
        context.startActivity(new Intent(context, CustomScanActivity.class));
    }

    public void notifyScanResult(boolean isProcessed, Intent resultData) {
        if (scanCallback != null) {
            scanCallback.onScanResult(isProcessed, resultData);
            scanCallback = null;
        }
    }

    public interface ScanCallback {
        void onScanResult(boolean isProcessed, Intent result);
    }
}
