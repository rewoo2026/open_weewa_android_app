package com.xh.hotme.me.setmeal.utils;

import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class StoragePurchasePolicyTest {
    private long now() throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse("2026-10-09 22:00:00").getTime();
    }

    private UserPackageCountBean.UserPackageData storage() {
        UserPackageCountBean.UserPackageData value = new UserPackageCountBean.UserPackageData();
        value.countType = 2;
        value.totalCount = 60;
        value.remainCount = 0;
        value.upgradable = 1;
        value.lastOrderId = "previous-storage-order";
        value.expireTime = "2026-10-13 23:59:59";
        return value;
    }

    @Test public void zeroCapacityWithHistoryUsesNewPurchaseLikeTheReportedUser() throws Exception {
        UserPackageCountBean.UserPackageData value = storage();
        value.totalCount = 0;
        value.remainCount = -0.01;
        assertFalse(StoragePurchasePolicy.hasActiveStorage(value, now()));
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
    }

    @Test public void activePaidStorageKeepsUpgradeEvenWhenItsSpaceIsFull() throws Exception {
        assertTrue(StoragePurchasePolicy.hasActiveStorage(storage(), now()));
        assertEquals("previous-storage-order", StoragePurchasePolicy.upgradeOrderId(storage(), now()));
    }

    @Test public void expiredStorageCanBuyTheSameCapacityAgainWithoutOldCredit() throws Exception {
        UserPackageCountBean.UserPackageData value = storage();
        value.expireTime = "2026-10-09 22:00:00";
        assertFalse(StoragePurchasePolicy.hasActiveStorage(value, now()));
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
        value.expireTime = "2026-10-13 23:59:59";
        value.expired = true;
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
    }

    @Test public void nonUpgradableOrInvalidEntitlementsNeverDeductAnOldOrder() throws Exception {
        UserPackageCountBean.UserPackageData value = storage();
        value.upgradable = 0;
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
        value.upgradable = 1;
        value.expireTime = "2026-02-30 23:59:59";
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
        value.expireTime = "not-a-date";
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(value, now()));
        assertEquals("", StoragePurchasePolicy.upgradeOrderId(null, now()));
    }
}
