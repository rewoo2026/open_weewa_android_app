package com.xh.hotme.test.factory;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;


import com.alibaba.fastjson.JSONException;
import com.alipay.android.phone.scancode.export.adapter.MPRecognizeType;
import com.alipay.android.phone.scancode.export.adapter.MPScanError;
import com.alipay.android.phone.scancode.export.adapter.MPScanResult;
import com.alipay.android.phone.scancode.export.adapter.MPScanner;
import com.alipay.android.phone.scancode.export.listener.MPImageGrayListener;
import com.alipay.android.phone.scancode.export.listener.MPScanListener;
import com.alipay.mobile.common.logging.api.LoggerFactory;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityCustomscanBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BitmapUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.scan.APTextureView;
import com.xh.hotme.widget.scan.ScanView;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.List;

/**
 * date   : 2023/3/13
 * desc   :自定义扫码
 */
public class CustomScanActivity extends BaseActivity<ActivityCustomscanBinding> {
    private boolean isFirstStart = true;
    private boolean isPermissionGranted;
    private boolean isScanning;
    private boolean isPaused;
    private static final int REQUEST_CODE_PERMISSION = 1;
    private static final int REQUEST_CODE_PHOTO = 101;
    private Rect scanRect;

    private MPScanner mpScanner;
    private LinearLayout mTorchBtn;
    private ImageView mLightView;
    TextView mLightTextView;

    private APTextureView mTextureView;
    private ScanView mScanView;

    @Override
    protected void initView() {
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mTextureView = findViewById(R.id.surface_view);
        mScanView = findViewById(R.id.scan_view);
        mLightView = findViewById(R.id.iv_light);
        mLightTextView = findViewById(R.id.tv_torch);
        mTorchBtn = findViewById(R.id.llt_torch);
        mTorchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTorch();
            }
        });

        initMPScanner();
        checkCameraPermission();
    }

    private void initMPScanner() {
        mpScanner = new MPScanner(this);
        mpScanner.setRecognizeType(
                MPRecognizeType.QR_CODE,
                MPRecognizeType.BAR_CODE,
                MPRecognizeType.DM_CODE,
                MPRecognizeType.PDF417_CODE
        );
        mpScanner.setMPScanListener(new MPScanListener() {
            @Override
            public void onConfiguration() {
                mpScanner.setDisplayView(mTextureView);
            }

            @Override
            public void onStart() {
                if (!isPaused) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                initScanRect();
                                mScanView.onStartScan();
                            }
                        }
                    });
                }
            }

            @Override
            public void onSuccess(List<MPScanResult> results) {
                if (results == null || results.isEmpty() || results.get(0) == null) {
                    return;
                }
                mpScanner.beep();
                onScanSuccess(results.get(0));
            }

            @Override
            public void onError(MPScanError mpScanError) {
                if (!isPaused) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtil.s(getString(R.string.scan_error_camera_open_error));
                        }
                    });
                }
            }
        });
        mpScanner.setMPImageGrayListener(new MPImageGrayListener() {
            @Override
            public void onGetImageGray(int gray) {
                // 注意：该回调在昏暗环境下可能会连续多次执行
//                if (gray < MPImageGrayListener.LOW_IMAGE_GRAY) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Utils.toast(CustomScanActivity.this, "光线太暗，请打开手电筒");
//                        }
//                    });
//                }
            }
        });
        findViewById(R.id.llt_loc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFromGallery();
            }
        });
    }

    @Override
    protected void initData() {

    }

    private void switchTorch() {
        boolean torchOn = mpScanner.switchTorch();
        mTorchBtn.setSelected(torchOn);
        if (torchOn) {
            mLightView.setImageResource(R.mipmap.ic_scan_torch);
            mLightTextView.setText(getString(R.string.scan_close_torch));
        } else {
            mLightView.setImageResource(R.mipmap.ic_scan_torch_off);
            mLightTextView.setText(getString(R.string.scan_open_torch));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        if (isScanning) {
            stopScan();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        if (!isFirstStart && isPermissionGranted) {
            startScan();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mpScanner.release();
    }

    private void checkCameraPermission() {
        if (PermissionChecker.checkSelfPermission(
                this, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION);
        } else {
            onPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            int length = Math.min(permissions.length, grantResults.length);
            for (int i = 0; i < length; i++) {
                if (TextUtils.equals(permissions[i], Manifest.permission.CAMERA)) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        ToastUtil.s(getString(R.string.scan_error_camera_no_permission));
                    } else {
                        onPermissionGranted();
                    }
                    break;
                }
            }
        }
    }

    private boolean isJSONValid(String str) {
        try {
            com.alibaba.fastjson.JSONObject.parseObject(str);
        } catch (JSONException ex) {
            try {
                com.alibaba.fastjson.JSONObject.parseArray(str);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }


    private void onScanSuccess(final MPScanResult result) {
        if (result == null || TextUtils.isEmpty(result.getText())) {
            return;
        }
        AppTrace.d(TAG, ">>> onScanSuccess " + result.getText() + " , " + result.getMPRecognizeType().toString());
        if (isJSONValid(result.getText()) && result.getText().contains("\"s\"") && result.getText().contains("\"p\"") && result.getText().contains("\"b\""))
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //{"s":"ssid","p":"password"},
                    try {
                        JSONObject jsonObject = new JSONObject(result.getText());
                        EventBus.getDefault().post(new ScanEvent(jsonObject.toString()));
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                if (result == null) {
//                    notifyScanResult(true, null);
//                } else {
//                    Intent intent = new Intent();
//                    intent.setData(Uri.parse(result.getText()));
//                    notifyScanResult(true, intent);
//                    EventBus.getDefault().post(new DiscernEvent(result.getText()));
//                }
//                CustomScanActivity.this.finish();

                }
            });
    }

    private void startScan() {
        try {
            mpScanner.openCameraAndStartScan();
            isScanning = true;
        } catch (Exception e) {
            isScanning = false;
            LoggerFactory.getTraceLogger().error(TAG, "startScan: Exception " + e.getMessage());
        }
    }

    private void stopScan() {
        mpScanner.closeCameraAndStopScan();
        mScanView.onStopScan();
        isScanning = false;
        if (isFirstStart) {
            isFirstStart = false;
        }
    }

    private void initScanRect() {
        if (scanRect == null) {
            scanRect = mScanView.getScanRect(
                    mpScanner.getCamera(), mTextureView.getWidth(), mTextureView.getHeight());

            float cropWidth = mScanView.getCropWidth();
            LoggerFactory.getTraceLogger().debug(TAG, "cropWidth: " + cropWidth);
            if (cropWidth > 0) {
                // 预览放大 ＝ 屏幕宽 ／ 裁剪框宽
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                float screenWith = wm.getDefaultDisplay().getWidth();
                float screenHeight = wm.getDefaultDisplay().getHeight();
                float previewScale = screenWith / cropWidth;
                if (previewScale < 1.0f) {
                    previewScale = 1.0f;
                }
                if (previewScale > 1.5f) {
                    previewScale = 1.5f;
                }
                LoggerFactory.getTraceLogger().debug(TAG, "previewScale: " + previewScale);
                Matrix transform = new Matrix();
                transform.setScale(previewScale, previewScale, screenWith / 2, screenHeight / 2);
                mTextureView.setTransform(transform);
            }
        }
        mpScanner.setScanRegion(scanRect);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PHOTO);
    }

    private void onPermissionGranted() {
        isPermissionGranted = true;
        startScan();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        notifyScanResult(false, null);
    }

    private void notifyScanResult(boolean isProcessed, Intent resultData) {
        ScanHelper.getInstance().notifyScanResult(isProcessed, resultData);
    }

    private void scanFromUri(Uri uri) {
        try {
            final Bitmap bitmap = BitmapUtil.uri2Bitmap(this, uri);
            if (bitmap == null) {
                notifyScanResult(true, null);
                finish();
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MPScanResult mpScanResult = mpScanner.scanFromBitmap(bitmap);
                        mpScanner.beep();
                        onScanSuccess(mpScanResult);
                    }
                }, "scanFromUri").start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_PHOTO) {
            scanFromUri(data.getData());
        }
    }
}
