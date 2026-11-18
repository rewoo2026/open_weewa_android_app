package com.xh.hotme.live.dialog;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogQrCodeBinding;
import com.xh.hotme.utils.ClickGuard;

import java.io.OutputStream;


@Keep
public class QrCodeDialog extends BaseDialog<DialogQrCodeBinding> {
    public QrCodeDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    public static void saveImageToGallery(Context context, Bitmap bitmap, String title, String description) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);

        Uri imageUri = null;

        try {
            // 使用 MediaStore 插入图片到相册
            imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if(imageUri != null) {
                OutputStream os = context.getContentResolver().openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();

                // 发送广播通知系统刷新相册
                notifyGalleryScan(context, imageUri);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void notifyGalleryScan(Context context, Uri imageUri) {
        // 发送广播通知系统刷新相册
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        context.sendBroadcast(mediaScanIntent);
    }


    public void setBitmap(Bitmap bitmap) {
        viewBinding.image.setImageBitmap(bitmap);
    }

    public void setName(String name) {
        viewBinding.tvShareName.setText(getContext().getString(R.string.competition_name_label) + name);
    }

    @Override
    protected void onInit() {
        viewBinding.close.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();
                return false;
            }
        });
        viewBinding.tvSave.setOnClickListener(view -> {
            Bitmap bitmap = null;
            try {
                bitmap = Bitmap.createBitmap(viewBinding.topView.getWidth(),
                    viewBinding.topView.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                viewBinding.topView.draw(canvas);
            } catch(Exception e) {
                e.printStackTrace();
            }

            try {
                saveImageToGallery(getContext(), bitmap, getContext().getString(R.string.weewa_competition), "");
//                    String fileName = "share_" + System.currentTimeMillis() + ".jpg"; // 设置保存的文件名
//                    String path = view.getContext().getCacheDir() + "/" + fileName;
//                    File file = new File(path);
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
//
////                    MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
//
//                    notifyGalleryScan(context, path);
                dismiss();
            } catch(Exception e) {
                e.printStackTrace();
            }

        });
    }
}
