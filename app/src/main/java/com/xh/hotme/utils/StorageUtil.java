package com.xh.hotme.utils;

import android.os.Environment;
import android.os.StatFs;

public class StorageUtil {

    public static boolean isEnoughSpace(long size) {
        return size < getSDFreeSpace();
    }

    //检查SD卡是否有足够的空间
    public static boolean checkFreeSpace() {
        int minimum = 100; //要求sd卡最少可用空间已M为单位
        long size = minimum * 1024 * 1024;
        return getSDFreeSpace() > size;
    }

    public static long getSDFreeSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }
}
