package com.xh.hotme.me.setting;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityFeedBackBinding;
import com.xh.hotme.utils.ToastUtil;


/**
 * Create by zhaozhihui on 2019-09-14
 **/
public class FeedBackActivity extends BaseActivity<ActivityFeedBackBinding> {
    private static final String TAG = FeedBackActivity.class.getSimpleName();

    final int maxNumber = 200;

    public static void start(Context context) {
        Intent intent = new Intent(context, FeedBackActivity.class);
        context.startActivity(intent);
    }

    class MaxTextLengthFilter implements InputFilter {

        private final int mMaxLength;

        public MaxTextLengthFilter(int max) {
            mMaxLength = max;
        }

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            int keep = mMaxLength - (dest.length() - (dend - dstart));
            if(keep < (end - start)) {
                ToastUtil.s(getString(R.string.info_feedback_content_max_limit));
            }

            if(keep <= 0) {
                return "";
            } else if(keep >= end - start) {
                return null;
            } else {
                return source.subSequence(start, start + keep);
            }
        }
    }

    @Override
    protected void initView() {
        viewBinding.titlebar.tvTitle.setText(getString(R.string.feedback));

        viewBinding.etFeedbackContent.setFilters(new InputFilter[] { new MaxTextLengthFilter(maxNumber) });

        viewBinding.tvMaxNumber.setText("" + maxNumber);
        viewBinding.tvContentNumber.setText("0");

        //所有继承自TextView的类
        viewBinding.etFeedbackContent.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: s = " + s + ", start = " + start +
                    ", before = " + before + ", count = " + count);

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                String content = s.toString();
                try {
                    viewBinding.tvContentNumber.setText("" + content.length());
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        viewBinding.titlebar.ivBack.setOnClickListener(v -> finish());

        viewBinding.btnFeedback.setOnClickListener(v -> {
            String content = viewBinding.etFeedbackContent.getText().toString();
            if(TextUtils.isEmpty(content)) {
                ToastUtil.s(getString(R.string.info_feedback_content_empty));
            }
        });
    }

    @Override
    protected void initData() {

    }
}