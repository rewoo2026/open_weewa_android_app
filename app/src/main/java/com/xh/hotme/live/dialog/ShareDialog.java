package com.xh.hotme.live.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.BaseMediaObject;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMVideo;
import com.umeng.socialize.media.UMWeb;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogShareLiveBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;


@Keep
@SheetStyle
public class ShareDialog extends BaseDialog<DialogShareLiveBinding> {
    private String url;
    private String name;
    private String subName;
    private boolean _shareAsVideo = false;

    public void setName(String name) {
        this.name = name;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public void setShareAsVideo(boolean _shareAsVideo) {
        this._shareAsVideo = _shareAsVideo;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ShareDialog(@NonNull final Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onInit() {
        viewBinding.ivClose.setOnClickListener(view -> dismiss());

        Context context = getOwnerContext();
        assert context != null;
        viewBinding.ivWx.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                UMImage image = new UMImage(context, R.mipmap.share_image1);
                BaseMediaObject obj = null;
                if(_shareAsVideo) {
                    obj = new UMVideo(url);
                } else {
                    obj = new UMWeb(url);
                }
                obj.setTitle(name);//标题
                obj.setThumb(image);//缩略图
                obj.setDescription(subName);//描述

                ShareAction action = new ShareAction((Activity) context);
                if(_shareAsVideo) {
                    action.withMedia((UMVideo) obj);
                } else {
                    action.withMedia((UMWeb) obj);
                }

                action
                    .setPlatform(SHARE_MEDIA.WEIXIN)
                    .setCallback(new UMShareListener() {
                        @Override
                        public void onStart(SHARE_MEDIA share_media) {

                        }

                        @Override
                        public void onResult(SHARE_MEDIA share_media) {
                            ToastUtil.s(getContext().getString(R.string.info_share_success));
                            dismiss();
                        }

                        @Override
                        public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                            ToastUtil.s(getContext().getString(R.string.info_share_fail));
                            dismiss();
                        }

                        @Override
                        public void onCancel(SHARE_MEDIA share_media) {
                            ToastUtil.s(getContext().getString(R.string.info_share_cancel));
                            dismiss();
                        }
                    })
                    .share();
                return false;
            }
        });

        viewBinding.ivFriend.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                UMImage image = new UMImage(context, R.mipmap.ic_launcher);
                UMWeb web = new UMWeb(url);
                web.setTitle("热我相机");//标题
                web.setThumb(image);//缩略图
                web.setDescription(name);//描述

                new ShareAction((Activity) context)
                    .setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE)
                    .withMedia(web)
                    .setCallback(new UMShareListener() {
                        @Override
                        public void onStart(SHARE_MEDIA share_media) {

                        }

                        @Override
                        public void onResult(SHARE_MEDIA share_media) {
                            ToastUtil.s(getContext().getString(R.string.info_share_success));
                            dismiss();
                        }

                        @Override
                        public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                            ToastUtil.s(getContext().getString(R.string.info_share_fail));
                            dismiss();
                        }

                        @Override
                        public void onCancel(SHARE_MEDIA share_media) {
                            ToastUtil.s(getContext().getString(R.string.info_share_cancel));
                            dismiss();
                        }
                    })
                    .share();
                return false;
            }
        });


        viewBinding.ivLink.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ClipboardManager clip = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("url", url));
                ToastUtil.s(getContext().getString(R.string.copied));
                dismiss();
                return false;
            }
        });

        viewBinding.ivCode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    Bitmap bitmap = CodeUtils.createImage(url, 400, 400,
                        BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
                    QrCodeDialog erCodeDialog = new QrCodeDialog(context);
                    erCodeDialog.showOnCenter(0);
                    erCodeDialog.setName(name);
                    erCodeDialog.setBitmap(bitmap);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                dismiss();
                return false;
            }
        });

    }
}
