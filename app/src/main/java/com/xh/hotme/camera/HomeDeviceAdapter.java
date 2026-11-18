package com.xh.hotme.camera;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener;
import com.xh.hotme.bluetooth.listener.IHomeDeviceAdapterCallback;
import com.xh.hotme.listener.IDeviceConnectRequestListener;
import com.xh.hotme.live.dialog.DevicesDialogUtils;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HomeDeviceAdapter extends BaseQuickAdapter<BindDeviceBean, BaseViewHolder> {

    private static final Logger log = LoggerFactory.getLogger(HomeDeviceAdapter.class);
    private IHomeDeviceAdapterCallback mCallback;

    final IDeviceConnectRequestListener mRequestCallback;

    ModalDialog mRemovedialog;

    final Context _ctx;

    private int deviceScene = 0;
    public static final int SCENE_TYE_RECORD = 1;
    public static final int SCENE_TYE_LIVE = 2;
    public static final int SCENE_TYE_OTHER = 3;

    public void setScene(int scene) {
        deviceScene = scene;
    }

    public int getScene() {
        return deviceScene;
    }


    public HomeDeviceAdapter(Context context, List data, IDeviceConnectRequestListener callback) {

        super(R.layout.bluetooth_device_list_item, data);
        _ctx = context;

        mRequestCallback = callback;
    }

    @Override
    protected void convert(BaseViewHolder helper, BindDeviceBean device) {

        int position = helper.getPosition();

        String deviceName = "";
        if(!TextUtils.isEmpty(device.getDeviceName())) {
            deviceName = device.getDeviceName() + "(" + BluetoothManager.getDisplayAddress(device.getBluetoothUuid()) + ")";
            deviceName = BluetoothManager.getDisplayDeviceName(deviceName);
        } else if(device.blueDevice != null && !TextUtils.isEmpty(device.blueDevice.name)) {
            deviceName = device.blueDevice.name + "(" + BluetoothManager.getDisplayAddress(device.getBluetoothUuid()) + ")";
            deviceName = BluetoothManager.getDisplayDeviceName(deviceName);
        } else {
            deviceName = "weewa(" + BluetoothManager.getDisplayAddress(device.getBluetoothUuid()) + ")";
        }
        ((TextView) helper.findView(R.id.tv_device_name)).setText(deviceName);

        TextView disconnectBtn = helper.getView(R.id.tv_disconnect);

        TextView statusView = helper.getView(R.id.tv_device_status);
        if(device.getStatus() == Constants.DEVICE_STATUS_OFFLINE) {
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                statusView.setText(getContext().getString(R.string.device_offline_local));
            } else {
                statusView.setText(getContext().getString(R.string.device_offline_remote));
            }
            statusView.setTextColor(getContext().getResources().getColor(R.color.color_bfbfbf));
        } else {
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                statusView.setText(getContext().getString(R.string.device_online_local));
            } else {
                statusView.setText(getContext().getString(R.string.device_online_remote));
            }
            statusView.setTextColor(getContext().getResources().getColor(R.color.text_green));
        }

        Button connectBtn = helper.findView(R.id.btn_device_connect);
        if(device.getStatus() == Constants.DEVICE_STATUS_CONNECT) {
            connectBtn.setBackgroundResource(R.drawable.device_fragment_device_item_btn_connect);
            connectBtn.setText(getContext().getString(R.string.bluetooth_device_list_connected));
            if(deviceScene == SCENE_TYE_LIVE) {
                if(device.getLiveStatus() == 1) {
                    connectBtn.setText(getContext().getString(R.string.live_ing));
                } else {
                    connectBtn.setText(getContext().getString(R.string.live));
                }
            } else if(deviceScene == SCENE_TYE_RECORD) {
                connectBtn.setText(getContext().getString(R.string.bluetooth_device_list_connected_record));
            }

            connectBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    try {
                        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                            if (BluetoothManager.getImpl() != null) {
                                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                                    BluetoothManager.getImpl().openBluetooth(_ctx, false);
                                    return true;
                                }
                            }
                            if(mRequestCallback != null) {
                                mRequestCallback.onRequestStart();
                            }
                            if(_scanRequestListener != null) {
                                if(device.blueDevice == null) {
                                    device.blueDevice = new Device(device.getBluetoothUuid(), 0, device.getDeviceName());
                                }
                                _scanRequestListener.onBluetoothDeviceClick(device.blueDevice, 0, Constants.DEVICE_ACTION_OPEN);
                            }
                        } else {
                            if(!NetUtil.isConnectedAvailableNetwork()){
                                ToastUtil.l(_ctx.getString(R.string.error_network_error));
                                return true;
                            }

                            if(mRequestCallback != null) {
                                mRequestCallback.onRequestStart();
                            }
                            if (_scanRequestListener != null) {
                                _scanRequestListener.onBluetoothDeviceClick(device, 0, Constants.DEVICE_ACTION_OPEN);
                            }
                        }

                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
            Log.d("asx","asx到底是哪个 --- HomeDevice");
            disconnectBtn.setVisibility(View.VISIBLE);
            disconnectBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {

                    if(BluetoothManager.getImpl() != null) {
                        if(!BluetoothManager.getImpl().enableBlueTooth()) {
                            BluetoothManager.getImpl().openBluetooth(_ctx, false);
                            return true;
                        }
                    }

                    ModalDialog dialog = new ModalDialog(_ctx);

                    dialog.setMessage(_ctx.getString(R.string.confirm_device_disconnect));
                    dialog.setNegativeButton(_ctx.getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            return true;
                        }
                    });
                    dialog.setPositiveButton(_ctx.getString(R.string.confirm), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {

                            try {
                                if(mRequestCallback != null) {
                                    mRequestCallback.onRequestStart();
                                }
                                if(_scanRequestListener != null) {
                                    if(device.blueDevice == null) {
                                        device.blueDevice = new Device(device.getBluetoothUuid(), 0, device.getDeviceName());
                                    }
                                    _scanRequestListener.onBluetoothDeviceClick(device.blueDevice, 0, Constants.DEVICE_ACTION_DISCONNECT);
                                }
                            } catch(Throwable e) {
                                e.printStackTrace();
                            }

                            device.setStatus(Constants.DEVICE_STATUS_ONLINE);

                            notifyDataSetChanged();

                            return true;
                        }
                    });

                    dialog.show();
                    return true;
                }
            });

        } else if(device.getStatus() == Constants.DEVICE_STATUS_OFFLINE) {
            connectBtn.setBackgroundResource(R.drawable.device_fragment_device_item_btn_gray);
            connectBtn.setText(getContext().getString(R.string.bluetooth_device_list_connect));
            connectBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                        if (BluetoothManager.getImpl() != null) {
                            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                                BluetoothManager.getImpl().openBluetooth(_ctx, false);
                                return true;
                            }
                        }
                        DevicesDialogUtils.showOfflineDialog(_ctx);
                    } else {
                        if(!NetUtil.isConnectedAvailableNetwork()){
                            ModalDialog dialog = new ModalDialog(_ctx);
                            dialog.setTitle(_ctx.getString(R.string.remote_model_no_network_title));
                            dialog.setMessage(_ctx.getString(R.string.remote_model_no_network_message));
                            dialog.hideNegativeButton();
                            dialog.setPositiveButton(_ctx.getString(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                            return true;
                        }

                        ModalDialog dialog = new ModalDialog(_ctx);
                        dialog.setTitle(_ctx.getString(R.string.remote_model_offline_title));
                        dialog.setMessage(_ctx.getString(R.string.remote_model_offline_message));
                        dialog.hideNegativeButton();
                        dialog.setPositiveButton(_ctx.getString(R.string.ok), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();

                    }
                    return true;
                }
            });

            disconnectBtn.setVisibility(View.GONE);/**/
        } else {
            connectBtn.setBackgroundResource(R.drawable.device_fragment_device_item_btn_connect);
            connectBtn.setText(getContext().getString(R.string.bluetooth_device_list_connect));
            connectBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                        if (BluetoothManager.getImpl() != null) {
                            if (!BluetoothManager.getImpl().enableBlueTooth()) {
                                BluetoothManager.getImpl().openBluetooth(_ctx, false);
                                return true;
                            }
                        }

                        if (mRequestCallback != null) {
                            mRequestCallback.onRequestStart();
                        }

                        if (_scanRequestListener != null) {
                            if (device.blueDevice == null) {
                                device.blueDevice = new Device(device.getBluetoothUuid(), 0, device.getDeviceName());
                            }
                            _scanRequestListener.onBluetoothDeviceClick(device.blueDevice, position, Constants.DEVICE_ACTION_CLICK);
                        }
                    } else {
                        if(mRequestCallback != null) {
                            mRequestCallback.onRequestStart();
                        }
                        if (_scanRequestListener != null) {
                            _scanRequestListener.onBluetoothDeviceClick(device, position, Constants.DEVICE_ACTION_CLICK);
                        }
                    }
                    return true;
                }
            });

            disconnectBtn.setVisibility(View.GONE);
            disconnectBtn.setOnClickListener(null);
        }
    }

    private void resetButtonStatus(BaseViewHolder helper) {
        ImageView removeView = helper.getView(R.id.iv_device_remove);
        removeView.setOnClickListener(null);
        if(removeView.getVisibility() == View.VISIBLE) {
            removeView.setVisibility(View.GONE);
        }
    }

    private void setRemoveButtonStatus(BaseViewHolder helper, BindDeviceBean device, int position, boolean isShow) {
        ImageView removeView = helper.getView(R.id.iv_device_remove);
        if(isShow) {
            removeView.setOnClickListener(v -> {

                if(mRemovedialog != null && mRemovedialog.isShowing()) {
                    mRemovedialog.dismiss();
                    mRemovedialog = null;
                }

                mRemovedialog = new ModalDialog(_ctx);
                mRemovedialog.setMessage(_ctx.getString(R.string.remove_device_dialog_title));
                mRemovedialog.setNegativeButton(_ctx.getString(R.string.remove_device_dialog_cancel), v12 -> {
                    mRemovedialog.dismiss();
                    resetButtonStatus(helper);
                });
                mRemovedialog.setPositiveButton(_ctx.getString(R.string.remove_device_dialog_confirm), v1 -> {
                    if(mCallback != null) {
                        mCallback.onRemoveDevice(device, position);
                    }
                    mRemovedialog.dismiss();
                });

                mRemovedialog.show();
            });
            if(removeView.getVisibility() == View.GONE) {
                removeView.setVisibility(View.VISIBLE);
            }
        } else {
            removeView.setOnClickListener(null);
            if(removeView.getVisibility() == View.VISIBLE) {
                removeView.setVisibility(View.GONE);
            }
        }
    }

    public IHomeDeviceAdapterCallback getCallback() {
        return mCallback;
    }

    public void setCallback(IHomeDeviceAdapterCallback callback) {
        this.mCallback = callback;
    }

    public IBleScanRequestListener _scanRequestListener;

    public void setScanRequestListener(IBleScanRequestListener l) {
        _scanRequestListener = l;
    }

    public IBleScanRequestListener getScanRequestListener() {
        return _scanRequestListener;
    }


}
