package com.xh.hotme.camera.softap;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.databinding.ActivitySoftApInfoBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.QRCodeUtil;
import com.xh.hotme.utils.ShareUtil;
import com.xh.hotme.utils.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraSoftApInfoActivity extends BaseActivity<ActivitySoftApInfoBinding> {
    private static final String TAG = CameraSoftApInfoActivity.class.getSimpleName();

    private final int reqestCode = -1;

    String ssid;
    String password;

    Bitmap _bitmap;

    public static void startActivity(Activity context, String ssid, String password) {
        if (null != context) {
            Intent intent = new Intent(context, CameraSoftApInfoActivity.class);
            intent.putExtra(Constants.INTENT_SSID, ssid);
            intent.putExtra(Constants.INTENT_PASSWORD, password);

            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_fast_transfer_qcode));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                finish();
                return true;
            }
        });

        viewBinding.titleBar.tvRight.setVisibility(View.VISIBLE);
        viewBinding.titleBar.tvRight.setText(getString(R.string.share));
        viewBinding.titleBar.ivRight.setVisibility(View.GONE);
        viewBinding.titleBar.tvRight.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (_bitmap == null) {
                    ToastUtil.s(getString(R.string.retry_later));
                    return true;
                }
                boolean isOk = saveImageToGallery(CameraSoftApInfoActivity.this, _bitmap, ssid + ".jpg");
                if (isOk) {
                    File file = new File(AppFileUtil.getDownloadDirectory(), ssid + ".jpg");
                    ShareUtil.shareLocalImageFileToWx(CameraSoftApInfoActivity.this, file.getPath());
                }
                return true;
            }
        });

        ForegroundColorSpan redSpan = new ForegroundColorSpan(getResources().getColor(R.color.qccde_text_red));
        String text = getString(R.string.qrcode_hint);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(redSpan, 2, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        viewBinding.softapHint.setText(builder);
    }

    @Override
    protected void initData() {
        if (getIntent() != null) {
            ssid = getIntent().getStringExtra(Constants.INTENT_SSID);
            password = getIntent().getStringExtra(Constants.INTENT_PASSWORD);
        }
        viewBinding.ssid.setText(ssid);
        viewBinding.password.setText(password);


        String address = BluetoothManager.getConnectDeviceAddress();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("s", ssid);
        jsonObject.put("p", password);
        jsonObject.put("b", address);

        new Thread(() -> runOnUiThread(() -> {
            int width = DensityUtil.dip2px(220);

            _bitmap = QRCodeUtil.generateBitmap(jsonObject.toJSONString(), width, width, 0);

            viewBinding.qrcode.setImageBitmap(_bitmap);
        })).start();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (_bitmap != null) {
            _bitmap.recycle();
        }
        _bitmap = null;

        //断开连接
        DeviceApiManager.disConnect();
    }

    /**
     * 保存图片到指定路径
     *
     * @param context
     * @param bitmap   要保存的图片
     * @param fileName 自定义图片名称
     * @return
     */
    public static boolean saveImageToGallery(Context context, Bitmap bitmap, String fileName) {
        // 保存图片至指定路径
        String storePath = AppFileUtil.getDownloadDirectory();
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            //通过io流的方式来压缩保存图片(80代表压缩20%)
            boolean isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            //发送广播通知系统图库刷新数据
            Uri uri = Uri.fromFile(file);
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            if (isSuccess) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


}
