package com.xh.hotme.me.setmeal.utils;

import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class StoragePurchasePolicy {
    private StoragePurchasePolicy() { }

    public static boolean hasActiveStorage(UserPackageCountBean.UserPackageData storage, long now) {
        if (storage == null || storage.countType != 2 || storage.expired || !(storage.totalCount > 0)) return false;
        if (storage.expireTime == null || storage.expireTime.trim().isEmpty()) return true;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        format.setLenient(false);
        String expiry = storage.expireTime.trim();
        ParsePosition position = new ParsePosition(0);
        Date date = format.parse(expiry, position);
        return date != null && position.getIndex() == expiry.length() && date.getTime() > now;
    }

    public static String upgradeOrderId(UserPackageCountBean.UserPackageData storage, long now) {
        if (!hasActiveStorage(storage, now) || !Integer.valueOf(1).equals(storage.upgradable)
                || storage.lastOrderId == null) return "";
        return storage.lastOrderId.trim();
    }
}
