package com.xh.hotme.live;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.xh.hotme.R;

import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityLiveThirdPlatformBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonStringResponse;
import com.xh.hotme.live.bean.HmLivePlatform;
import com.xh.hotme.live.bean.LiveLoginCode;
import com.xh.hotme.live.bean.LiveStreamAddress;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.imagepicker.ImagePickerCallback;
import com.xh.hotme.widget.imagepicker.LetoImagePicker;

import java.io.File;

public class LiveThirdPlatformActivity extends BaseActivity<ActivityLiveThirdPlatformBinding> implements ImagePickerCallback {
    private static final String TAG = LiveThirdPlatformActivity.class.getSimpleName();

    public static final String EXTRA_PRESELECT_PLATFORM_TYPE = "preselect_plt_type";
    public static final String EXTRA_COMPETITION_NAME = "competition_name";

    public static final String EXTRA_PLATFORM_TYPE = "plt_type";
    public static final String EXTRA_LIVE_PLATFORM_ID = "live_plt_id";
    public static final String EXTRA_PUSH_URL = "push_url";
    public static final String EXTRA_PUSH_KEY = "push_key";
    public static final String EXTRA_PULL_URL = "pull_url";
    public static final String EXTRA_COVER_URL = "cover_url";

    private String[] THIRD_PLATFORM_NAMES ;
    private View[] _pltGroupViews;
    private EditText[] _pltPushUrlViews;
    private EditText[] _pltPullUrlViews;
    private EditText[] _pltKeyViews;
    private int _plt = Constants.LIVE_PLATFORM_XQM;



    private static final class AddResult {
        String errMsg;
        String pushUrl;
        String pushKey;
        String pullUrl;
        HmLivePlatform livePlt;
    }

    private AddResult _addResult = null;

    // for wx
    private String _competitionName = "";
    private LiveLoginCode _3rdLoginCode = null;


    // image picker
    LetoImagePicker _imagePicker;

    String _coverUrl;

    @Override
    protected void initView() {
        THIRD_PLATFORM_NAMES = getResources().getStringArray(R.array.third_platform);

        _imagePicker = LetoImagePicker.getInstance(this);

        // set title
        viewBinding.titlebar.tvTitle.setText(R.string.third_platform);
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            }
        });

        // preselect
        _plt = getIntent().getIntExtra(EXTRA_PRESELECT_PLATFORM_TYPE, Constants.LIVE_PLATFORM_WX_CHANNEL);
        _competitionName = getIntent().getStringExtra(EXTRA_COMPETITION_NAME);

        // platform chooser
        _pltGroupViews = new View[] {
                viewBinding.groupXqm.getRoot(),
                viewBinding.groupWx.getRoot(),
                viewBinding.groupDy.getRoot()
        };
        _pltPushUrlViews = new EditText[] {
                viewBinding.groupXqm.etPushUrl,
                viewBinding.groupWx.etPushUrl,
                viewBinding.groupDy.etPushUrl
        };
        _pltPullUrlViews = new EditText[] {
                viewBinding.groupXqm.etPullUrl,
                viewBinding.groupWx.etPullUrl,
                viewBinding.groupDy.etPullUrl
        };
        _pltKeyViews = new EditText[] {
                viewBinding.groupXqm.etPushKey,
                viewBinding.groupWx.etPushKey,
                viewBinding.groupDy.etPushKey
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.auto_complete_text_view_item, THIRD_PLATFORM_NAMES);
        viewBinding.pltName.setAdapter(adapter);
        viewBinding.pltName.setOnItemClickListener((parent, view, position, id) -> {
            _plt = position;
            togglePlatformGroups();
        });
        viewBinding.pltName.setText(THIRD_PLATFORM_NAMES[_plt]);
        viewBinding.groupPlt.setOnClickListener(v -> viewBinding.pltName.showDropDown());
        viewBinding.pltName.setOnClickListener(v -> viewBinding.pltName.showDropDown());
        togglePlatformGroups();

        // for wx
        viewBinding.groupWx.btnGetQrcode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getWxLoginCode();
                return true;
            }
        });
        viewBinding.groupWx.btnGetUrl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getWxLiveUrl();
                return true;
            }
        });

        viewBinding.groupWx.sbCoverConfig.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    viewBinding.groupWx.tvCoverDescription.setText(getString(R.string.cover_description_custom));
                    viewBinding.groupWx.tvUploadCover.setVisibility(View.VISIBLE);
                }else{
                    viewBinding.groupWx.tvCoverDescription.setText(getString(R.string.cover_description_default));
                    viewBinding.groupWx.tvUploadCover.setVisibility(View.GONE);
                }
            }
        });
        viewBinding.groupWx.tvUploadCover.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_3rdLoginCode == null){
                    ToastUtil.s(getString(R.string.info_get_login_token_of_wechat_video));
                    return true;
                }

                if(!checkStoragePermission(false)) {
                    return true;
                }
                _imagePicker.pickFromAlbum("live_cover.jpg", 1080, 1260, true, LiveThirdPlatformActivity.this);
                return true;
            }
        });

        // for dy
        viewBinding.groupDy.btnGetQrcode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getDyLoginCode();
                return true;
            }
        });
        viewBinding.groupDy.btnGetUrl.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getDyLiveUrl();
                return true;
            }
        });
        viewBinding.groupDy.btnGetCode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DialogUtil.showDialog("");
                ApiUtil.sendDyCode(_3rdLoginCode.token, new IGenericApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        ToastUtil.s(getString(R.string.info_input_login_token_and_verify));
                    }

                    @Override
                    public void onFail(String code, String message) {
                        ToastUtil.s(message);
                    }

                    @Override
                    public void onFinish() {
                        DialogUtil.dismissDialog();
                    }
                });
                return true;
            }
        });
        viewBinding.groupDy.btnValidateCode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String code = viewBinding.groupDy.etCode.getText().toString();
                if(TextUtils.isEmpty(code)) {
                    ToastUtil.s(getString(R.string.info_login_token_empty));
                    return true;
                }

                DialogUtil.showDialog("");
                ApiUtil.validateDyCode(_3rdLoginCode.token, code, new IGenericApiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean data) {
                        getDyLiveUrl();
                    }

                    @Override
                    public void onFail(String code, String message) {
                        ToastUtil.s(message);
                        DialogUtil.dismissDialog();
                    }

                    @Override
                    public void onFinish() {
                    }
                });
                return true;
            }
        });

        // ok button
        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _addResult = new AddResult();
                _addResult.pushUrl = _pltPushUrlViews[_plt].getText().toString().trim();
                _addResult.pushKey = _pltKeyViews[_plt].getText().toString();
                _addResult.pullUrl = _pltPullUrlViews[_plt].getText().toString().trim();
                addWorkflowStep1_validate();

                // ok
                return true;
            }
        });
    }

    @Override
    protected void initData() {

    }

    private void getWxLiveUrl() {
        if(_3rdLoginCode == null) {
            ToastUtil.s(getString(R.string.info_take_login_token_and_scan));
            return;
        }

        DialogUtil.showDialog("");
        ApiUtil.getWxLiveUrl(_competitionName, _3rdLoginCode.token, _coverUrl, new IGenericApiCallback<LiveStreamAddress>() {
            @Override
            public void onSuccess(LiveStreamAddress data) {
                viewBinding.groupWx.etPushUrl.setText(data.pushStreamAddress);
                viewBinding.groupWx.etPushKey.setText(data.pushStreamKey);
                viewBinding.groupWx.etPullUrl.setText(data.pullStreamAddress);
            }

            @Override
            public void onFail(String code, String message) {
                ToastUtil.s(message);
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }

    private void getDyLiveUrl() {
        if(_3rdLoginCode == null) {
            ToastUtil.s(getString(R.string.info_take_login_token_and_scan));
            return;
        }

        DialogUtil.showDialog("");
        ApiUtil.getDyLiveUrl(_competitionName, _3rdLoginCode.token, new IGenericApiCallback<LiveStreamAddress>() {
            @Override
            public void onSuccess(LiveStreamAddress data) {
                viewBinding.groupDy.etPushUrl.setText(data.pushStreamAddress);
                viewBinding.groupDy.etPushKey.setText(data.pushStreamKey);
                viewBinding.groupDy.etPullUrl.setText(data.pullStreamAddress);
                viewBinding.groupDy.groupCode.setVisibility(View.GONE);
            }

            @Override
            public void onFail(String code, String message) {
                if("2046".equals(code)) {
                    // 2046表示需要安全验证
                    viewBinding.groupDy.groupCode.setVisibility(View.VISIBLE);
                } else {
                    ToastUtil.s(message);
                }
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }

    private void getWxLoginCode() {
        DialogUtil.showDialog("");
        ApiUtil.getWxLoginCode(new IGenericApiCallback<LiveLoginCode>() {
            @Override
            public void onSuccess(LiveLoginCode data) {
                try {
                    _3rdLoginCode = data;
                    String b64Str = _3rdLoginCode.qrcode;
                    if(b64Str.startsWith("data:")) {
                        int idx = b64Str.indexOf(";base64,");
                        if(idx != -1) {
                            b64Str = b64Str.substring(idx + 8);
                        }
                    }
                    byte[] qrImgData = Base64.decode(b64Str, Base64.DEFAULT);
                    Bitmap qrImg = BitmapFactory.decodeByteArray(qrImgData, 0, qrImgData.length);
                    if(qrImg != null) {
                        viewBinding.groupWx.ivQrcode.setImageBitmap(qrImg);
                        viewBinding.groupWx.ivQrcode.setAlpha(1.0f);
                        viewBinding.groupWx.tvHintStep2.setVisibility(View.GONE);
                    } else {
                        _3rdLoginCode = null;
                        ToastUtil.s(getString(R.string.info_take_login_token_fail));
                    }
                } catch(Exception e) {
                    _3rdLoginCode = null;
                    ToastUtil.s(e.getLocalizedMessage());
                }
            }

            @Override
            public void onFail(String code, String message) {
                ToastUtil.s(message);
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }

    private void getDyLoginCode() {
        DialogUtil.showDialog("");
        ApiUtil.getDyLoginCode(new IGenericApiCallback<LiveLoginCode>() {
            @Override
            public void onSuccess(LiveLoginCode data) {
                try {
                    _3rdLoginCode = data;
                    String b64Str = _3rdLoginCode.qrcode;
                    if(b64Str.startsWith("data:")) {
                        int idx = b64Str.indexOf(";base64,");
                        if(idx != -1) {
                            b64Str = b64Str.substring(idx + 8);
                        }
                    }
                    byte[] qrImgData = Base64.decode(b64Str, Base64.DEFAULT);
                    Bitmap qrImg = BitmapFactory.decodeByteArray(qrImgData, 0, qrImgData.length);
                    if(qrImg != null) {
                        viewBinding.groupDy.ivQrcode.setImageBitmap(qrImg);
                        viewBinding.groupDy.ivQrcode.setAlpha(1.0f);
                        viewBinding.groupDy.tvHintStep2.setVisibility(View.GONE);
                    } else {
                        _3rdLoginCode = null;
                        ToastUtil.s(getString(R.string.info_take_login_token_fail));
                    }
                } catch(Exception e) {
                    _3rdLoginCode = null;
                    ToastUtil.s(e.getLocalizedMessage());
                }
            }

            @Override
            public void onFail(String code, String message) {
                ToastUtil.s(message);
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }

    private void togglePlatformGroups() {
        for(View v : _pltGroupViews) {
            v.setVisibility(View.GONE);
        }
        _pltGroupViews[_plt].setVisibility(View.VISIBLE);
    }

    private void addWorkflowStep1_validate() {
        DialogUtil.showDialog("");
        if(TextUtils.isEmpty(_addResult.pushUrl)) {
            _addResult.errMsg = "推流地址不能为空";
            addWorkflowStep4_wrapUp();
        } else if(TextUtils.isEmpty(_addResult.pullUrl)) {
            _addResult.errMsg = "拉流地址不能为空";
            addWorkflowStep4_wrapUp();
        } else {
            addWorkflowStep2_addLivePlatform();
        }
    }

    private void addWorkflowStep2_addLivePlatform() {
        ApiUtil.addLivePlatform(_plt, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _addResult.livePlt = data;
                addWorkflowStep3_updateLivePlatform();
            }

            @Override
            public void onFail(String code, String message) {
                _addResult.errMsg = message;
                addWorkflowStep4_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void addWorkflowStep3_updateLivePlatform() {
        _addResult.livePlt.pullStreamAddress = _addResult.pullUrl;
        _addResult.livePlt.pushStreamAddress = _addResult.pushUrl + _addResult.pushKey;
        ApiUtil.updateLivePlatform(_addResult.livePlt, new IGenericApiCallback<HmLivePlatform>() {
            @Override
            public void onSuccess(HmLivePlatform data) {
                _addResult.livePlt = data;
            }

            @Override
            public void onFail(String code, String message) {
                _addResult.errMsg = message;
            }

            @Override
            public void onFinish() {
                addWorkflowStep4_wrapUp();
            }
        });
    }

    private void addWorkflowStep4_wrapUp() {
        DialogUtil.dismissDialog();
        if(!TextUtils.isEmpty(_addResult.errMsg)) {
            ToastUtil.s(_addResult.errMsg);
        } else {
            // return result
            AppTrace.d(TAG, String.format("third platform used: plt: %d, live plt id: %s, push url: %s, key: %s, pull url: %s",
                _plt, _addResult.livePlt.domainId, _addResult.pushUrl, _addResult.pushKey, _addResult.pullUrl));
            Intent data = new Intent();
            data.putExtra(EXTRA_PLATFORM_TYPE, _plt);
            data.putExtra(EXTRA_LIVE_PLATFORM_ID, _addResult.livePlt.domainId);
            data.putExtra(EXTRA_PUSH_URL, _addResult.pushUrl);
            data.putExtra(EXTRA_PUSH_KEY, _addResult.pushKey);
            data.putExtra(EXTRA_PULL_URL, _addResult.pullUrl);
            data.putExtra(EXTRA_COVER_URL, _coverUrl);
            setResult(RESULT_OK, data);

            // finish
            finish();
        }
    }

    @Override
    public void onImagePicked(String path) {

        // load avatar jpg file
        File f = new File(path);
        if(f.exists()) {
            doUploadCover(f);
        } else {
            ToastUtil.s(getString(R.string.info_file_not_exist));
        }

    }

    @Override
    public void onImagePickingCancelled() {

    }


    private void doUploadCover(final File file) {
        showLoading(false, LiveThirdPlatformActivity.this.getString(R.string.loading));

        ApiUtil.uploadCover(file, _3rdLoginCode.token, new IGenericApiCallback<CommonStringResponse>() {
            @Override
            public void onSuccess(CommonStringResponse data) {
                AppTrace.d("uploadCover success");
                _coverUrl = (String) data.data;
                GlideUtil.load(LiveThirdPlatformActivity.this, _coverUrl, viewBinding.groupWx.ivCover);

            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d("uploadCover fail, code=" +code +" , message=" + message);
                ToastUtil.s(getString(R.string.info_cover_upload_fail));
            }

            @Override
            public void onFinish() {
                dismissLoading();
            }
        });
    }
}
