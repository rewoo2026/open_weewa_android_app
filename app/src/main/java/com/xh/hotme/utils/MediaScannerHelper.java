package com.xh.hotme.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.util.List;

public class MediaScannerHelper {
    public static void scanFiles(Context context, List<String> filePaths) {
        MediaScannerConnection.scanFile(context,
                filePaths.toArray(new String[0]), null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        // 扫描完成的回调
                        AppTrace.d("MediaScannerHelper", "onScanCompleted: path=" + path );
                    }
                });
    }
}
