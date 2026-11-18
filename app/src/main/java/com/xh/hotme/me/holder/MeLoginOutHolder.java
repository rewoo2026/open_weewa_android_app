package com.xh.hotme.me.holder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.ApiCallback;
import com.xh.hotme.me.MeModuleBean;
import com.xh.hotme.test.TestDeviceListActivity;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MeLoginOutHolder extends CommonViewHolder<MeModuleBean> {

    final Context _ctx;
    private final Button _sign_out;
    private final TextView _version_num;

    public static MeLoginOutHolder create(Context ctx, ViewGroup parent) {
        View convertView = LayoutInflater.from(ctx).inflate(R.layout.me_layout_login_out, parent, false);
        return new MeLoginOutHolder(convertView);
    }

    public MeLoginOutHolder(View itemView) {
        super(itemView);
        _ctx = itemView.getContext();

        _version_num = itemView.findViewById(R.id.version_num);
        String ver = BaseAppUtil.getAppVersionName();

        _version_num.setText(String.format(_ctx.getString(R.string.current_version), ver));

        _version_num.setOnLongClickListener(v -> {
            if(Constants.TEST_SERVER) {
                TestDeviceListActivity.start(_ctx);
            }
            return true;
        });

        _sign_out = itemView.findViewById(R.id.sign_out);

        _sign_out.setOnClickListener(view -> {
            ModalDialog d = new ModalDialog(_ctx);
            d.setMessage(_ctx.getString(R.string.out_sys_mg));
            d.setNegativeButton(_ctx.getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    d.dismiss();
                    return true;
                }
            });
            d.setPositiveButton(_ctx.getString(R.string.exit), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {

                    //网络未连接的时候直接清除本地数据即可
                    if(!NetUtil.isConnectedAvailableNetwork()) {
                        postLogout();
                        return true;
                    }

                    ApiUtil.logout(new ApiCallback() {
                        @Override
                        public void onSuccess() {
                            MainHandler.runOnUiThread(() -> postLogout());
                        }

                        @Override
                        public void onFail(String code, String message) {
                            MainHandler.runOnUiThread(() -> postLogout());
                        }
                    });


                    return true;
                }
            });
            d.show();

        });

        if(!LoginManager.isSignedIn()){
            _sign_out.setVisibility(View.GONE);
        }else{
            _sign_out.setVisibility(View.VISIBLE);
        }

        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

    }

    private void postLogout() {
        LoginManager.clearLoginInfo();
        CameraManager.release();
        BluetoothManager.getImpl().disconnectBle();
        DeviceManager.getImpl().clearDeviceList();
        CameraManager.setDeviceId("");

        if(!LoginManager.isLogon()){
            _sign_out.setVisibility(View.GONE);
        }else{
            _sign_out.setVisibility(View.VISIBLE);
        }


        EventBus.getDefault().post(new LoginEvent());

        MobileLoginActivity.start(_ctx);
    }

    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReload(LoginEvent event) {
        updateProfile();
    }

    @Override
    public void onBind(MeModuleBean model, int position) {
        updateProfile();
    }

    private void updateProfile() {

        if(!LoginManager.isSignedIn()) {
            _sign_out.setVisibility(View.GONE);
        } else {
            _sign_out.setVisibility(View.VISIBLE);
        }

    }

}
