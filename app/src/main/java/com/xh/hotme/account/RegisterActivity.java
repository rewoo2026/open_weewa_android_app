package com.xh.hotme.account;

import static com.xh.hotme.utils.Constants.REQUEST_CODE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.R;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityRegisterBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import okhttp3.FormBody;

public class RegisterActivity extends BaseActivity<ActivityRegisterBinding> {
    EditText etAccount, etVerificationCode, etPassword, etConfirmPassword, etDeviceId;
    TextView btnSubmit;
    Button btnGetVerificationCode;

    private ImageView clear_view,show_password,show_confirm_password,clear_device_id;

    private static int _type = 0;
    private static String _account = "";

    // 0 为注册 1 为忘记密码
    public static void start(Context context) {
        start(context, _type);
    }

    public static void start(Context context, int type) {
        start(context,type,"");
    }

    public static void start(Context context, int type, String account) {
        if(null != context) {
            _type = type;
            _account = account;
            Intent intent = new Intent(context, RegisterActivity.class);
            intent.putExtra(REQUEST_CODE, type);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }


    @Override
    protected void initView() {
        TextView titleView = viewBinding.titleBar.tvTitle;
        if(_type == 0) {
            titleView.setText(getString(R.string.login_register_account));
        } else {
            titleView.setText(getString(R.string.forgot_password));
        }

        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });


        etAccount = findViewById(R.id.et_register_account);
        clear_view = findViewById(R.id.clear);
        etVerificationCode = findViewById(R.id.et_register_verification_code);
        etPassword = findViewById(R.id.et_register_password);
        show_password = findViewById(R.id.iv_show_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        show_confirm_password = findViewById(R.id.iv_show_confirm_password);
        etDeviceId = findViewById(R.id.et_register_device_id);
        clear_device_id = findViewById(R.id.clear_device_id);
        btnGetVerificationCode = findViewById(R.id.btn_register_get_verification_code);
        btnSubmit = findViewById(R.id.btn_register_submit);

        if (_type == 0) {
           viewBinding.etRegisterDeviceIdView.setVisibility(View.VISIBLE);
        }else{
            viewBinding.etRegisterDeviceIdView.setVisibility(View.GONE);
            etAccount.setText(_account);
        }

        clear_view.setOnClickListener(v -> etAccount.setText(""));
        clear_device_id.setOnClickListener(v -> etDeviceId.setText(""));



        show_password.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                togglePasswordVisibility();
                return true;
            }
        });

        show_confirm_password.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                toggleConfirmPasswordVisibility();
                return true;
            }
        });



        btnGetVerificationCode.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                // 获取验证码
                getVerificationCode();
                return true;
            }
        });
        btnSubmit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                // 注册或者忘记密码
                submitRegister();
                return true;
            }
        });
    }

    @Override
    protected void initData() {

    }

    private boolean isConfirmPasswordVisible = false;
    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            // 隐藏密码
            etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            show_confirm_password.setImageResource(R.mipmap.ic_eye_close);
        } else {
            // 显示密码
            etConfirmPassword.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            show_confirm_password.setImageResource(R.mipmap.ic_eye_open);
        }
        // 移动光标到文本末尾
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
    }

    private boolean isPasswordVisible = false;
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // 隐藏密码
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            show_password.setImageResource(R.mipmap.ic_eye_close);
        } else {
            // 显示密码
            etPassword.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            show_password.setImageResource(R.mipmap.ic_eye_open);
        }
        // 移动光标到文本末尾
        etPassword.setSelection(etPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }


    private void getVerificationCode() {
        String account = etAccount.getText().toString().trim();
        if (TextUtils.isEmpty(account)) {
            ToastUtil.s(R.string.input_phone_or_email);
            return;
        }
        // 需要区分当前是手机还是邮箱
        String phoneRegex = "^1[0-9]{10}$";
        // 邮箱正则：简单验证
        String emailRegex = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$";

        String url = "";
        FormBody.Builder builder = new FormBody.Builder();
        if (account.matches(phoneRegex)) {
            // 是手机号

            url = SdkApi.mobile_get_code();
            builder.add("mobile", account);
        } else if (account.matches(emailRegex)) {
            // 是邮箱
            url = SdkApi.email_get_code();
            builder.add("email", account);
        } else {
            // 都不是，提示用户
            ToastUtil.s(R.string.input_phone_or_email);
            return;
        }

        OkHttpUtil.postFormData(url,builder,null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                MainHandler.runOnUiThread(() -> {
                    ToastUtil.s("发送成功");
                    postSendCodeSuc();
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    ToastUtil.s(message);
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {

                });
            }
        });
    }

    public void postSendCodeSuc() {
        startCodeTime(Constants.sms_count_down);
        etAccount.clearFocus();
        etVerificationCode.requestFocus();
    }

    final Handler handler = new Handler();
    private void startCodeTime(int time) {
        btnGetVerificationCode.setTag(time);
        if(time <= 0) {
            btnGetVerificationCode.setText(getString(R.string.login_btn_send_sms));
            btnGetVerificationCode.setClickable(true);
            return;
        } else {
            btnGetVerificationCode.setClickable(false);
            btnGetVerificationCode.setText(time + getString(R.string.second));
        }
        handler.postDelayed(() -> {
            int delayTime = (int) btnGetVerificationCode.getTag();
            startCodeTime(--delayTime);

        }, 1000);
    }


    private void submitRegister() {
        String account = etAccount.getText().toString().trim();
        String verificationCode = etVerificationCode.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();

        if (TextUtils.isEmpty(account)) {
//            Toast.makeText(this, R.string.input_phone_or_email, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.input_phone_or_email);
            return;
        }
        if (TextUtils.isEmpty(verificationCode)) {
//            Toast.makeText(this, R.string.login_hint_input_sms, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.login_hint_input_sms);
            return;
        }
        if (TextUtils.isEmpty(password)) {
//            Toast.makeText(this, R.string.login_input_password, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.login_input_password);
            return;
        }
        if (password.length() < 6) {
            ToastUtil.s(R.string.password_too_short);
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
//            Toast.makeText(this, R.string.login_confirm_password, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.login_confirm_password);
            return;
        }
        if (!password.equals(confirmPassword)) {
//            Toast.makeText(this, R.string.password_not_match, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.password_not_match);
            return;
        }
        if (TextUtils.isEmpty(deviceId) && _type == 0) {
//            Toast.makeText(this, R.string.input_device_id, Toast.LENGTH_SHORT).show();
            ToastUtil.s(R.string.input_device_id);
            return;
        }


        // 需要区分当前是手机还是邮箱
        String phoneRegex = "^1[0-9]{10}$";
        // 邮箱正则：简单验证
        String emailRegex = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$";

        String url = "";
        boolean isPhone = false;
        boolean isEmail = false;
        if (account.matches(phoneRegex)) {
            // 是手机号
             isPhone = true;
        } else if (account.matches(emailRegex)) {
            // 是邮箱
            isEmail = true;
        } else {
            // 都不是，提示用户
            ToastUtil.s(R.string.input_phone_or_email);
            return;
        }



        // 这里写的啰嗦一点
        if (isPhone && _type == 0) { // 手机号 注册
            url = SdkApi.mobile_register() + "?mobile=" + account + "&password=" + password + "&verificationCode=" + verificationCode + "&deviceId=" + deviceId;
        } else if (isPhone && _type == 1) { // 手机号 忘记密码
            url = SdkApi.mobile_reset_pwd() + "?mobile=" + account + "&password=" + password + "&verificationCode=" + verificationCode;
        } else if (isEmail && _type == 0) { // 邮箱 注册
            url = SdkApi.email_register() + "?email=" + account + "&password=" + password + "&verificationCode=" + verificationCode + "&deviceId=" + deviceId;
        } else if (isEmail && _type == 1) { // 邮箱 忘记密码
            url = SdkApi.email_reset_pwd() + "?email=" + account + "&password=" + password + "&verificationCode=" + verificationCode;
        }else {
            // 到这里就出错了
        }

//        ToastUtil.s(url);
        OkHttpUtil.put(url,null, new OkHttpCallbackDecode<Object>() {
            @Override
            public void onDataSuccess(Object data) {
                MainHandler.runOnUiThread(() -> {
                    if (_type == 0) {
                        ToastUtil.s(R.string.register_success);
                    }else{
                        ToastUtil.s(R.string.reset_password_success);
                    }
                    finish();
                });
            }

            @Override
            public void onFailure(String code, String message) {
                MainHandler.runOnUiThread(() -> {
                    ToastUtil.s(message);
                });
            }

            @Override
            public void onFinish() {
                MainHandler.runOnUiThread(() -> {

                });
            }
        });

        // 调用注册接口
//        ApiUtil.register(account, verificationCode, password, deviceId, new IGenericApiCallback<String>() {
//            @Override
//            public void onSuccess(String data) {
//                Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//
//            @Override
//            public void onFail(String code, String message) {
//                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onFinish() {
//                // 可添加完成逻辑
//            }
//        });
    }
}