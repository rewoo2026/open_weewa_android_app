package com.xh.hotme.camera.ota;

import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;

/** Only the camera knows which of the production servers supplied its selected package. */
public final class CameraOtaOffer {
    private CameraOtaOffer() {}

    public static boolean isPending(OTAStatusEvent event) {
        return event.status == OTAStatus.OTA_CHECKING.getValue()
                || event.status == OTAStatus.OTA_CHECK_FAILED.getValue();
    }

    public static OTAVersionInfo fromStatus(OTAStatusEvent event) {
        if (event.status < OTAStatus.OTA_DOWNLOADABLE.getValue()
                || event.status > OTAStatus.OTA_UPGRADE_FALED.getValue()
                || event.versionCode <= 0 || event.remoteVersionCode <= 0
                || event.remoteVersionName == null || event.remoteVersionName.trim().isEmpty()) {
            return null;
        }
        // After installation the local code can already equal the selected remote code.
        boolean installed = event.status == OTAStatus.OTA_UPGRADED.getValue();
        if (event.remoteVersionCode < event.versionCode
                || (!installed && event.remoteVersionCode == event.versionCode)) return null;
        OTAVersionInfo info = new OTAVersionInfo();
        info.versionCode = event.remoteVersionCode;
        info.version = event.remoteVersionName;
        info.forceUpdateFlag = event.forceUpgrade ? "1" : "0";
        // get_ota_status has no release notes. Never reuse notes from a different server offer.
        return info;
    }
}
