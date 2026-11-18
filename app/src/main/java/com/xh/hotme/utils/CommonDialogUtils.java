package com.xh.hotme.utils;

import android.content.Context;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.event.RemoteSwitchEvent;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;

public class CommonDialogUtils {
    public static void showAccessModelDialog(Context context, int model, IActionCallback commonListener){
        ModalDialog dialog = new ModalDialog(context);
        dialog.setTitle(context.getString(R.string.remote_model_switch_model));
        if(model == Constants.ACCESS_MODEL_LOCAL){
            dialog.setMessage(context.getString(R.string.remote_model_switch_model_message_remote));
        }else{
            dialog.setMessage(context.getString(R.string.remote_model_switch_model_message_local));
        }

        dialog.setNegativeButton(context.getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(commonListener!=null){
                    commonListener.onReturn(0);
                }

            }
        });
        dialog.setPositiveButton(context.getString(R.string.remote_model_switch), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(model == Constants.ACCESS_MODEL_LOCAL) {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;

                    //停止当前的蓝牙搜索
                    if(BluetoothManager.getImpl().isScanning()) {
                        BluetoothManager.getImpl().stopScan();
                    }
                    //断开当前蓝牙设备
                    BluetoothManager.getImpl().disconnectBle();
                } else {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
                    DeviceApiManager.disConnect();
                }
                if(commonListener!=null){
                    commonListener.onReturn(1);
                }
                EventBus.getDefault().post(new RemoteSwitchEvent());
            }
        });
        dialog.show();
    }

    //自定义，也不会做后处理
    public static void showAccessModelDialog(Context context, int model, String title, String message, IActionCallback commonListener){
        ModalDialog dialog = new ModalDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setNegativeButton(context.getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(commonListener!=null){
                    commonListener.onReturn(0);
                }

            }
        });
        dialog.setPositiveButton(context.getString(R.string.remote_model_switch), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(model == Constants.ACCESS_MODEL_LOCAL) {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;
                } else {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
                }
                if(commonListener!=null){
                    commonListener.onReturn(1);
                }
                EventBus.getDefault().post(new RemoteSwitchEvent());
            }
        });
        dialog.show();
    }
}
