package com.xh.hotme.camera.wifi;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.utils.ClickGuard;

import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class WifiListAdapter extends BaseQuickAdapter<WifiInfo, BaseViewHolder> {
    private final static String TAG = "BluetoothDeviceAdapter";

    private IWifiListAdapterCallback mCallback;

    public IWifiListAdapterCallback getCallback() {
        return mCallback;
    }

    public void setCallback(IWifiListAdapterCallback callback) {
        this.mCallback = callback;
    }


    public WifiListAdapter(Context context, List data) {

        super(R.layout.dialog_wifilist_item, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, WifiInfo wifi) {

        TextView tv_ssid = baseViewHolder.findView(R.id.ssid);
        ImageView iv_level = baseViewHolder.findView(R.id.wifi_level);


        tv_ssid.setText(wifi.getSsid());
        if(wifi.isConnecting()) {
            tv_ssid.setTextColor(Color.BLUE);
        }
        int level = WifiManager.calculateSignalLevel(Integer.parseInt(wifi.getSignalLevel()), 5);
        if(wifi.getFlags() == null || wifi.getFlags().contains("WEP") || wifi.getFlags().contains("PSK") || wifi.getFlags().contains("EAP")) {
            iv_level.setImageResource(R.drawable.wifi_signal_lock);
        } else {
            iv_level.setImageResource(R.drawable.wifi_signal_open);
        }
        iv_level.setImageLevel(level);

        int position = baseViewHolder.getPosition();

        baseViewHolder.itemView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(mCallback != null) {
                    mCallback.onWifiClick(wifi, position);
                }
                return true;
            }
        });
    }


    @Override
    public long getItemId(int position) {
        return position;
    }
}
