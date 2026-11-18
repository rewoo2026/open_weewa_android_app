package com.xh.hotme.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.WebViewActivity;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setting.SimpleWebActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DeviceUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;

@Keep
public class PrivacyWebDialog extends Dialog {
	// views
	private TextView _okButton;
	private TextView _cancelButton;
	private View _closeButton;
	private TextView _titleView;
	private TextView _userProxy;
	private TextView _privacy;
	private WebView _webView;

	// listener
	private OnClickListener _listener;

	public static void show(final Activity ctx) {
		if(ctx == null){
			return;
		}
		if(ctx instanceof Activity && ctx.isDestroyed()){
			return;
		}
		
	}

	/**
	 * 显示隐私协议对话框
	 * @param act activity
	 * @param disagreeable true表示显示不同意按钮, false表示只有确定按钮
	 */
	public static void show(final Activity act, String content, boolean disagreeable) {
		try {
			final PrivacyWebDialog privacyDialog = new PrivacyWebDialog(act, String.format("%s%s", act.getString(R.string.welcome_to_use), act.getString(R.string.app_name)), content);
			privacyDialog.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						SharedPrefs.getImpl().setPrivateShowStatus(act, false);
					} else if (which == DialogInterface.BUTTON_NEGATIVE) {
						ModalDialog dialog = new ModalDialog(act);
						dialog.setMessage(act.getString(R.string.privacy_agreement_dialog_title));
						dialog.setNegativeButton(act.getString(R.string.privacy_agreement_dialog_btn_exit_app), new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (privacyDialog != null) {
									privacyDialog.dismiss();
								}
								act.finish();
							}
						});
						dialog.setPositiveButton(act.getString(R.string.privacy_agreement_dialog_btn_back), new View.OnClickListener() {
							@Override
							public void onClick(View v) {
							}
						});
						dialog.setMessageTextColor("#666666");
						dialog.setMessageTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
						dialog.setLeftButtonTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
						dialog.setRightButtonTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
						dialog.setLeftButtonTextColor("#999999");
						dialog.setRightButtonTextColor("#FF3D9AF0");
						dialog.show();
					}
				}
			});
			if(!disagreeable) {
				privacyDialog.setNegativeButtonVisible(false);
				privacyDialog.setPositiveButtonTitle("确定");
			}
			privacyDialog.show();
		} catch (Throwable e) {
		}
	}

	public PrivacyWebDialog(@NonNull final Context context, String title, String url) {
		super(context, R.style.hotme_custom_dialog);

		// load content view
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.privacy_dialog, null);

		// views
		TextView titleLabel = view.findViewById(R.id.title);
		_webView = view.findViewById(R.id.web);
		_okButton = view.findViewById(R.id.ok);
		_cancelButton = view.findViewById(R.id.cancel);
		_userProxy = view.findViewById(R.id.user_agreement);
		_privacy = view.findViewById(R.id.privacy_agreement);

		_cancelButton.setText(getContext().getString(R.string.privacy_agreement_dialog_btn_exit_app));

		_webView.getSettings().setTextZoom(80);
		_webView.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
		_webView.getSettings().setDefaultTextEncodingName("utf-8");

		_webView.getSettings().setAllowFileAccessFromFileURLs(false);  //webviewFile同源策略绕过漏洞
		_webView.getSettings().setAllowUniversalAccessFromFileURLs(false);  //webviewFile同源策略绕过漏洞
		_webView.getSettings().setAllowFileAccess(false);    //webviewFile同源策略绕过漏洞

		_webView.getSettings().setJavaScriptEnabled(false);

		_webView.getSettings().setSavePassword(false);   //不能使用密码保存

		try {
			_webView.removeJavascriptInterface("searchBoxJavaBridge_");
			_webView.removeJavascriptInterface("accessibility");
			_webView.removeJavascriptInterface("accessibilityTraversal");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// title
		titleLabel.setText(title);

		_webView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				AppTrace.e("onPageStarted", "url=" + url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				AppTrace.d("shouldOverrideUrlLoading", "url=" + url);
				if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp")) {
					WebViewActivity.start(context, "用户条款&隐私政策", url);
					return true;
				} else {
					try {
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url));
						view.getContext().startActivity(intent);
					} catch (ActivityNotFoundException e) {
						e.printStackTrace();
					}

					return true;
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				AppTrace.d("onPageFinished", "url=" + url);
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

				return super.shouldInterceptRequest(view, url);
			}

			@TargetApi(Build.VERSION_CODES.LOLLIPOP)
			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
				String url = request.getUrl().toString();
				AppTrace.d("InterceptRequest", String.format("url=%s", url));

				return super.shouldInterceptRequest(view, request);
			}


		});

		_cancelButton.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
			@Override
			public boolean onClicked() {
				if(_listener != null) {
					_listener.onClick(PrivacyWebDialog.this, DialogInterface.BUTTON_NEGATIVE);
				}
				return true;
			}
		});


		// ok button
		_okButton.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
			@Override
			public boolean onClicked() {
				if(_listener != null) {
					_listener.onClick(PrivacyWebDialog.this, DialogInterface.BUTTON_POSITIVE);
				}
				dismiss();
				return true;
			}
		});

		_userProxy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SimpleWebActivity.start(getContext(), "用户协议", SdkApi.user_agreement);
			}
		});

		_privacy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SimpleWebActivity.start(getContext(), "隐私协议", SdkApi.user_privacy);
			}
		});

		// web view
		if(url.startsWith("http")) {
			_webView.loadUrl(url);
		} else {
			_webView.loadDataWithBaseURL(null, url, "text/html", "utf-8", null);
		}

		// set content view
		setContentView(view);

		setCancelable(false);
		setCanceledOnTouchOutside(false);

		Window window = getWindow();
		window.setGravity(Gravity.CENTER);
		WindowManager.LayoutParams windowparams = window.getAttributes();
		windowparams.width = DeviceUtil.getWidth() - 2 * DensityUtil.px2dip(10);

	}

	public void setOnClickListener(OnClickListener listener){
		_listener = listener;
	}

	public void setNegativeButtonVisible(boolean visible) {
		_cancelButton.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public void setPositiveButtonTitle(String title) {
		_okButton.setText(title);
	}
	public void setNegativeButtonText(String title) {
		_cancelButton.setText(title);
	}
}
