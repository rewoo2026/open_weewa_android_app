package com.xh.hotme.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.bean.VersionResultBean;
import com.xh.hotme.listener.IProgressListener;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.UpdateUtil;


public class VersionDialog {
    private static final String TAG = VersionDialog.class.getSimpleName();
    private Dialog dialog;
    private ConfirmDialogListener mlistener;

    TextView tvTitle;
    TextView tvContent;
    TextView tvDownload;
    ImageView btcancel;
    ImageView titleBg;
    ProgressBar progressBar;

    View.OnClickListener downloadListener;

    UpdateUtil mUpdateUtil;


    public void showDialog(final Context context, final VersionResultBean version, ConfirmDialogListener listener) {

        dismiss();
        final boolean isCanCancel = version.getForceUpdateFlag() != 1;
        this.mlistener = listener;
        View dialogview = LayoutInflater.from(context).inflate(R.layout.dialog_common_version, null);
        dialog = new Dialog(context);
        //设置view
        dialog.setContentView(dialogview);
        dialog.setCanceledOnTouchOutside(isCanCancel);
        dialog.setCancelable(isCanCancel);
        //dialog默认是环绕内容的
        //通过window来设置位置、高宽
        Window window = dialog.getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
////        window.setGravity(Gravity.BOTTOM);
//        windowparams.height = ;
        windowparams.width = DensityUtil.dip2px(307);
//        设置背景透明,但是那个标题头还是在的，只是看不见了
        //注意：不设置背景，就不能全屏
        window.setBackgroundDrawableResource(android.R.color.transparent);

        btcancel = dialogview.findViewById(R.id.cancel_tv);
        tvTitle = dialogview.findViewById(R.id.title);
        tvContent = dialogview.findViewById(R.id.content);
        titleBg = dialogview.findViewById(R.id.lebox_upgrade_title_bg);
        tvDownload = dialogview.findViewById(R.id.btn_download);
        progressBar = dialogview.findViewById(R.id.progressBar);
//        donwloadLayout = (FrameLayout) dialogview.findViewById(R.id.layout_dialog);
        tvDownload.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        if(version != null) {
            tvTitle.setText(context.getString(R.string.find_new_version));
        }
        if(version != null && !TextUtils.isEmpty(version.getContent())) {
            tvContent.setText(version.getContent());
        }

        if(isCanCancel) {
            btcancel.setVisibility(View.VISIBLE);
        } else {
            btcancel.setVisibility(View.GONE);
        }

        btcancel.setOnClickListener(v -> {
                if(mlistener != null) {
                    mlistener.cancel();
                }
                dismiss();
            }
        );

        downloadListener = v -> {
            tvDownload.setVisibility(View.GONE);
            if(mlistener != null) {
                mlistener.ok();
            }

            if(mUpdateUtil != null) {
                if(mUpdateUtil.isDownload()) {
                    tvDownload.setVisibility(View.VISIBLE);
                    tvDownload.setText("安装");
                    if(!mUpdateUtil.isCancel()) {
                        boolean isGranted = context.getPackageManager().canRequestPackageInstalls();
                        if (isGranted)
                            //使用7.0安装模式
                            mUpdateUtil.installApk();
                        else{
                            // 用户授权
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            Activity act = (Activity) context;
                            act.startActivityForResult(intent, MainActivity.REQUEST_CODE_INSTALL_PERMISSION);
                        }
                    }
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    tvDownload.setOnClickListener(null);
                    mUpdateUtil.downloadApk(new IProgressListener() {
                        @Override
                        public void onProgressUpdate(final long progress, long totalBytesWritten, long totalBytesExpectedToWrite) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                AppTrace.d(TAG, "progress: " + progress);
                                try {
                                    progressBar.setProgress((int) progress);
//                                            tvDownload.setText("" + progress + "%");
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                        @Override
                        public void onComplete() {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                AppTrace.d(TAG, "onComplete");
                                try {
                                    if(!mUpdateUtil.isCancel()) {
                                        mUpdateUtil.installApk();
                                    }
                                    if(progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    tvDownload.setVisibility(View.VISIBLE);
                                    tvDownload.setText("安装");
                                    tvDownload.setOnClickListener(downloadListener);

                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                        @Override
                        public void abort(String msg) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                AppTrace.d(TAG, "download abort....");
                                try {
                                    progressBar.setProgress(0);
                                    progressBar.setVisibility(View.GONE);
                                    tvDownload.setVisibility(View.VISIBLE);
                                    tvDownload.setText("重新下载");
                                    tvDownload.setOnClickListener(downloadListener);
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    });
                }

            }
        };


        tvDownload.setOnClickListener(downloadListener);

        mUpdateUtil = new UpdateUtil();
        mUpdateUtil.setVersion(version);

        dialog.setOnDismissListener(dialog -> {

            mUpdateUtil.setCancel(true);

            if(mlistener != null) {
                mlistener.dismiss();
            }
        });

        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void installApp(){
        //使用7.0安装模式
        mUpdateUtil.installApk();
    }

    public void dismiss() {
        if(dialog != null) {
            dialog.dismiss();
            mlistener = null;
        }
    }

    public interface ConfirmDialogListener {
        void ok();

        void cancel();

        void dismiss();
    }
}
