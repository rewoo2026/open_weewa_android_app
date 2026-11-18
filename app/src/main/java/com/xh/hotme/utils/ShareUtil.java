package com.xh.hotme.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;

import java.io.File;
import java.util.List;

public class ShareUtil {
    // 判断微信版本是否为7.0.13及以上
    public static boolean checkVersionValid(IWXAPI api) {
        return api.getWXAppSupportAPI() >= 0x27000D00;
    }

    // 判断Android版本是否7.0及以上
    public static boolean checkAndroidNotBelowN() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
    }

    public static void shareLocalVideoFileToWx(Context context, String filePath) {
        String appId = Constants.WEIXIN_APP_ID; // 填应用AppId
        IWXAPI api = WXAPIFactory.createWXAPI(context, appId);

        if(checkVersionValid(api) && checkAndroidNotBelowN()) {
            File file = new File(filePath);
            String contentPath = getFileUri(context, file);

            // 使用contentPath作为文件路径进行分享
            contentShareLocalVideoFileToWx(context, contentPath);
        } else {
            // 使用原有方式传递文件路径进行分享
            nativeShareLocalVideoFileToWx(context, filePath);
        }
    }

    public static void contentShareLocalVideoFileToWx(Context context, String contentPath) {
        Uri uri = Uri.parse(contentPath);
        shareLocalVideoFileToWx(context, uri);
    }

    public static void nativeShareLocalVideoFileToWx(Context context, String fileName) {
        final Uri uri;
        final File file = new File(fileName);
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentapiVersion >= Build.VERSION_CODES.N) {//若SDK大于等于24  获取uri采用共享文件模式
            uri = FileProvider.getUriForFile(HotmeApplication.getContext(), "com.xh.hotme.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }

        shareLocalVideoFileToWx(context, uri);
    }

    public static void shareLocalVideoFileToWx(Context context, Uri uri) {

        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setType("video/*");
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        share.addCategory(Intent.CATEGORY_DEFAULT);
        share.setPackage("com.tencent.mm");
        //若需要分享到微信，只需要更改Package即可
        //share.setPackage("com.tencent.mm")
        String wxActClass = getWXinSendClassName();
        if(wxActClass == null) {
            ToastUtil.s(context.getString(R.string.info_install_wechat));
            return;
        }
        share.setComponent(new ComponentName("com.tencent.mm", wxActClass)); //com.tencent.mm.ui.tools.ShareImgUI
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(share.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(share);
        } else {
            ToastUtil.s(context.getString(R.string.info_no_wechat));
        }
    }

    public static String getFileUri(Context context, File file) {
        if(file == null || !file.exists()) {
            return null;
        }

        Uri contentUri = FileProvider.getUriForFile(context,
            "com.xh.hotme.fileprovider",  // 要与`AndroidManifest.xml`里配置的`authorities`一致，假设你的应用包名为com.example.app
            file);

        // 授权给微信访问路径
        context.grantUriPermission("com.tencent.mm",  // 这里填微信包名
            contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return contentUri.toString();   // contentUri.toString() 即是以"content://"开头的用于共享的路径
    }

    private static String getWXinSendClassName() {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("video/*");
        List<ResolveInfo> resInfos = HotmeApplication.getContext().getPackageManager().queryIntentActivities(share, 0);
        for(ResolveInfo resInfo : resInfos) {
            if(resInfo.activityInfo.name.contains("com.tencent.mm")) {
                return resInfo.activityInfo.name;
            }
        }
        return null;
    }


    public static void contentShareLocalImageFileToWx(Context context, String contentPath) {
        Uri uri = Uri.parse(contentPath);
        shareLocalImageFileToWx(context, uri);
    }

    public static void nativeShareLocalImageFileToWx(Context context, String fileName) {
        final Uri uri;
        final File file = new File(fileName);
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentapiVersion >= Build.VERSION_CODES.N) {//若SDK大于等于24  获取uri采用共享文件模式
            uri = FileProvider.getUriForFile(HotmeApplication.getContext(), "com.xh.hotme.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }

        shareLocalImageFileToWx(context, uri);
    }


    public static void shareLocalImageFileToWx(Context context, Uri uri) {

        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setType("image/*");
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        share.addCategory(Intent.CATEGORY_DEFAULT);
        share.setPackage("com.tencent.mm");
        //若需要分享到微信，只需要更改Package即可
        //share.setPackage("com.tencent.mm")
        String wxActClass = getWXinSendClassName();
        if(wxActClass == null) {
            ToastUtil.s(context.getString(R.string.info_install_wechat));
            return;
        }
        share.setComponent(new ComponentName("com.tencent.mm", wxActClass)); //com.tencent.mm.ui.tools.ShareImgUI
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(share.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(share);
        } else {
            ToastUtil.s(context.getString(R.string.info_no_wechat));
        }
    }


    public static void shareLocalImageFileToWx(Context context, String filePath) {
        String appId = Constants.WEIXIN_APP_ID; // 填应用AppId
        IWXAPI api = WXAPIFactory.createWXAPI(context, appId);

        if(checkVersionValid(api) && checkAndroidNotBelowN()) {
            File file = new File(filePath);
            String contentPath = getFileUri(context, file);

            // 使用contentPath作为文件路径进行分享
            contentShareLocalImageFileToWx(context, contentPath);
        } else {
            // 使用原有方式传递文件路径进行分享
            nativeShareLocalImageFileToWx(context, filePath);
        }
    }
}
