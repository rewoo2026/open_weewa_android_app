package com.xh.hotme.live.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.LiveActivity;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.widget.ModalDialog;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/4
 * @Description:
 */
public class DevicesDialogUtils {

    //相机未找到
    public static void showOfflineDialog(Context context) {
        if(context instanceof BaseActivity) {
            ((BaseActivity<?>)context).showHintDialog(
                context.getString(R.string.dialog_camera_not_found),
                context.getString(R.string.dialog_camera_reboot),
                context.getString(R.string.dialog_camera_found_ok),
                null
            );
        }
    }

    public static boolean openWifiSetting(Activity _ctx) {
        return openWifiSetting(_ctx, null);
    }


    public static boolean isShow = false;
    @SuppressLint("StaticFieldLeak")
    public static ModalDialog mRemovedialog;

    public static void dismiss() {
        if(mRemovedialog != null) {
            mRemovedialog.dismiss();
            mRemovedialog = null;
        }
    }

    //检查网络弹框
    public static boolean openWifiSetting(Activity _ctx, IActionCallback commonListener) {

        if(isShow) {
            return true;
        }
        if(!NetUtil.isConnectedAvailableNetwork()) {
            isShow = true;
            if(mRemovedialog == null) {
                mRemovedialog = new ModalDialog(_ctx);
            }
            mRemovedialog.dismiss();
            mRemovedialog.setMessage(_ctx.getString(R.string.info_phone_network_exception));
            mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> {
                mRemovedialog.dismiss();
                if(commonListener != null) {
                    commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
                }
                if(_ctx instanceof LiveActivity) {
                    _ctx.finish();
                }

            });
            mRemovedialog.setPositiveButton(_ctx.getString(R.string.setting), v -> {
                mRemovedialog.dismiss();
                _ctx.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            });
            mRemovedialog.setOnDismissListener(dialogInterface -> {
                isShow = false;
                mRemovedialog = null;
            });

            mRemovedialog.show();
            return true;
        }

        return false;
    }

    //直播中断
    public static void liveStreamInterruption(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setMessage(_ctx.getString(R.string.dialog_live_offline));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.live_continue), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
    }

    //直播中断
    public static ModalDialog liveError(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.info_live_fail));
        mRemovedialog.setMessage(_ctx.getString(R.string.info_live_fail_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.ok), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
        return mRemovedialog;
    }

    //直播中断
    public static ModalDialog liveCameraNoNetworkError(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.info_camera_network_exception));
        mRemovedialog.setMessage(_ctx.getString(R.string.info_camera_network_exception_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.reboot), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
        return mRemovedialog;
    }

    //直播中断
    public static ModalDialog livePhoneError(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.info_phone_network_exception));
        mRemovedialog.setMessage(_ctx.getString(R.string.info_phone_network_exception_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.refresh), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
        return mRemovedialog;
    }

    public static ModalDialog liveSocketError(Context _ctx, IActionCallback commonListener) {
        ModalDialog dialog = new ModalDialog(_ctx);
        dialog.setTitle(_ctx.getString(R.string.info_connect_live_server_exception));
        dialog.setMessage(_ctx.getString(R.string.info_connect_live_server_exception_message));
        dialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> dialog.dismiss());
        dialog.setPositiveButton(_ctx.getString(R.string.live_end), v -> {
            dialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        dialog.show();
        return dialog;
    }

    //确定停止直播
    public static void stopLive(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.info_stop_live_title));
        mRemovedialog.setMessage(_ctx.getString(R.string.info_stop_live_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.live_end), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
    }


    //暂停直播
    public static void pauseLive(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.info_pause_live_title));
        mRemovedialog.setMessage(_ctx.getString(R.string.info_pause_live_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.info_pause_live_message_btn), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });

        mRemovedialog.show();
    }

    //断开相机
    public static void disconnectNetWork(Context _ctx, IActionCallback commonListener) {
        ModalDialog mRemovedialog = new ModalDialog(_ctx);
        mRemovedialog.setTitle(_ctx.getString(R.string.dialog_network_outside_title));
        mRemovedialog.setMessage(_ctx.getString(R.string.dialog_network_outside_message));
        mRemovedialog.setNegativeButton(_ctx.getString(R.string.cancel), v -> mRemovedialog.dismiss());
        mRemovedialog.setPositiveButton(_ctx.getString(R.string.ok), v -> {
            mRemovedialog.dismiss();
            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_OK);
        });
        mRemovedialog.setOnDismissListener(dialogInterface -> commonListener.onReturn(3));
        mRemovedialog.show();
    }
}
