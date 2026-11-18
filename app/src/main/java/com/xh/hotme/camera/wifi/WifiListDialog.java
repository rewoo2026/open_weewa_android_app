package com.xh.hotme.camera.wifi;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener;
import com.xh.hotme.bluetooth.listener.IWifiListAdapterCallback;
import com.xh.hotme.databinding.DialogBluetoothListBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.widget.RecycleViewDivider;

import java.util.ArrayList;
import java.util.List;


@Keep
@SheetStyle(heightGap = 20)
public class WifiListDialog extends BaseDialog<DialogBluetoothListBinding> {
    // listener
    private IWifiListAdapterCallback _callback;

    private IBleScanRequestListener _scanRequestListener;

    WifiListAdapter _adapter;

    private List<WifiInfo> _dataList;

    Handler _handler;

    public WifiListDialog(@NonNull final Context context, String title, List<WifiInfo> wifiList, IWifiListAdapterCallback callback) {
        super(context);
        initData(title, wifiList, callback);
    }

    private void initData(String title, List<WifiInfo> wifiList, IWifiListAdapterCallback callback) {
        _callback = callback;
        viewBinding.tvTitle.setText(title);
        _dataList.addAll(wifiList);
        _adapter.setCallback(callback);
        _adapter.notifyDataSetChanged();
    }

    @Override
    protected void onInit() {
        _handler = new Handler();

        _dataList = new ArrayList<>();
        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_scanRequestListener != null) {
                    _scanRequestListener.onScanStop(0);
                }
                if(_callback != null) {
                    _callback.onCancel();
                }
                dismiss();
                return true;
            }
        });

//		// ok button
        viewBinding.refresh.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_scanRequestListener != null) {
                    _scanRequestListener.onScanStart(true);
                }
                return true;
            }
        });

        viewBinding.refresh.setVisibility(View.GONE);


        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewBinding.recyclerView.addItemDecoration(new RecycleViewDivider(getContext(), LinearLayoutManager.VERTICAL, getContext().getResources().getColor(R.color.bg_gray)));

        _adapter = new WifiListAdapter(getContext(), _dataList);

        viewBinding.recyclerView.setAdapter(_adapter);

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}
