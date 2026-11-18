package com.xh.hotme.camera.ota;

import android.os.Handler;
import android.os.Looper;
import com.xh.hotme.bluetooth.bean.OTAStatusRequestBean;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;

/** A bounded, fresh camera query for the automatic upgrade prompt. */
public final class CameraOtaCheck {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable onOffer;
    private final Runnable poll = () -> CameraOtaCommands.send(new OTAStatusRequestBean());
    private final Runnable timeout = this::cancel;

    public void start(Runnable onOffer) {
        cancel();
        this.onOffer = onOffer;
        handler.postDelayed(timeout, 45000);
        if (CameraOtaCommands.send(new OTAStatusRequestBean(true))) {
            handler.postDelayed(poll, 1000);
        } else cancel();
    }

    public void onStatus(OTAStatusEvent event) {
        if (onOffer == null) return;
        handler.removeCallbacks(poll);
        if (CameraOtaOffer.isPending(event)) {
            handler.postDelayed(poll, 1000);
            return;
        }
        Runnable callback = onOffer;
        cancel();
        if (CameraOtaOffer.fromStatus(event) != null
                && event.remoteVersionCode > event.versionCode) callback.run();
    }

    public void cancel() {
        onOffer = null;
        handler.removeCallbacksAndMessages(null);
    }
}
