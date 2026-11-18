package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityConfigScaleBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class ConfigScaleActivity extends BaseActivity<ActivityConfigScaleBinding> {
    private static final int MATRIX_5P = 0;
    private static final int MATRIX_8P = 1;
    private static final int MATRIX_11P = 2;
    private static final int MATRIX_BB = 3;
    private String[] _matrixStrValues = new String[4];
    private String[] _defValues = new String[] {
        "0,0.85,0.7,0.7,0.85,0.9,1.0,1.0,1.0,1.0",
        "0,0.7,0.3,0.6,0.7,0.75,1.0,1.0,1.0,1.0",
        "0,0.65,0.15,0.55,0.7,0.7,0.85,1.0,1.0,1.0",
        "0,0.7,0.3,0.6,0.7,0.75,1.0,1.0,1.0,1.0"
    };
    private boolean _changed = false;
    private boolean _pendingFinish = false;
    private EditText[] _editTextViews;
    private View[] _typeViews;
    private CheckBox[] _checkViews;
    private int _curType = MATRIX_5P;

    String str_request_timeout;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
        ToastUtil.s(str_request_timeout);
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        str_request_timeout = getString(R.string.request_timeout);

        viewBinding.titleBar.tvTitle.setText(R.string.config_scale);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        _editTextViews = new EditText[] {
            viewBinding.etNum0,
            viewBinding.etNum1,
            viewBinding.etNum2,
            viewBinding.etNum3,
            viewBinding.etNum4,
            viewBinding.etNum5,
            viewBinding.etNum6,
            viewBinding.etNum7,
            viewBinding.etNum8,
            viewBinding.etNum9
        };
        _typeViews = new View[] {
            viewBinding.cb5p,
            viewBinding.cb8p,
            viewBinding.cb11p,
            viewBinding.cbBb
        };
        _checkViews = new CheckBox[] {
            viewBinding.cb5pInternal,
            viewBinding.cb8pInternal,
            viewBinding.cb11pInternal,
            viewBinding.cbBbInternal
        };

        viewBinding.sbEnableScale.setOnClickListener(v -> {

            if(!DeviceApiManager.isConnected()){
                showDisconnectDialog();
                return;
            }

            UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
            bean.addSetting("video.1:enable_zoom_scale", viewBinding.sbEnableScale.isChecked() ? "1" : "0");
            boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
            if(isOk) {
                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                showLoading(viewBinding.sbEnableScale.isChecked() ? "打开自动缩放" : "关闭自动缩放");
            } else {
                showDisconnectToast();
            }
        });
        for(int i = 0; i < _typeViews.length; i++) {
            _typeViews[i].setTag(i);
            _typeViews[i].setOnClickListener(this::handleTypeClicked);
        }
        for(int i = 0; i < _checkViews.length; i++) {
            _checkViews[i].setTag(i);
            _checkViews[i].setOnClickListener(this::handleTypeClicked);
        }
        viewBinding.scaleContainer.setVisibility(viewBinding.sbEnableScale.isChecked() ? View.VISIBLE : View.GONE);
        _typeViews[_curType].setSelected(true);
        _checkViews[_curType].setChecked(true);
        viewBinding.btnResetDefault.setOnClickListener(v -> {
            _matrixStrValues[_curType] = _defValues[_curType];
            updateCurrentMatrixValues();
            _changed = true;
        });
        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!validateCurrentMatrixValues()) return true;

                // get new values
                String newValue = buildMatrixStringValue();
                if(_matrixStrValues[_curType]!=null && !_matrixStrValues[_curType].equals(newValue)) {
                    _changed = true;
                    _matrixStrValues[_curType] = newValue;
                }

                // if changed, update
                if(_changed) {
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("video.1:scale_5p", _matrixStrValues[MATRIX_5P]);
                    bean.addSetting("video.1:scale_8p", _matrixStrValues[MATRIX_8P]);
                    bean.addSetting("video.1:scale_11p", _matrixStrValues[MATRIX_11P]);
                    bean.addSetting("video.1:scale_bb", _matrixStrValues[MATRIX_BB]);
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _pendingFinish = true;
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                        showLoading("保存缩放矩阵参数");
                    } else {
                        showDisconnectToast();
                    }
                } else {
                    finish();
                }

                return true;
            }
        });
    }

    private void handleTypeClicked(View v) {
        if(!validateCurrentMatrixValues()) return;

        // get new values
        String newValue = buildMatrixStringValue();
        if(!_matrixStrValues[_curType].equals(newValue)) {
            _changed = true;
            _matrixStrValues[_curType] = newValue;
        }

        // switch ui
        _typeViews[_curType].setSelected(false);
        _checkViews[_curType].setChecked(false);
        _curType = (Integer) v.getTag();
        _typeViews[_curType].setSelected(true);
        _checkViews[_curType].setChecked(true);
        updateCurrentMatrixValues();
    }

    @Override
    protected void initData() {
        showLoading();
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("video.1:enable_zoom_scale");
        bean.addKey("video.1:scale_5p");
        bean.addKey("video.1:scale_8p");
        bean.addKey("video.1:scale_11p");
        bean.addKey("video.1:scale_bb");
        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    private void updateCurrentMatrixValues() {
        String strVal = _matrixStrValues[_curType];
        String[] parts = strVal.split(",");
        if(parts.length != _editTextViews.length) return;
        for(int i = 0; i < parts.length; i++) {
            _editTextViews[i].setText(parts[i]);
        }
    }

    private String buildMatrixStringValue() {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(EditText et : _editTextViews) {
            if(!first) {
                buf.append(",");
            }
            first = false;
            buf.append(et.getText().toString());
        }
        return buf.toString();
    }

    private boolean validateCurrentMatrixValues() {
        for(EditText et : _editTextViews) {
            try {
                Float.parseFloat(et.getText().toString());
            } catch(NumberFormatException e) {
                ToastUtil.s(getString(R.string.info_normal_float_value));
                return false;
            }
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettingsGot(WeewaSettingsExEvent e) {
        if(e.has("video.1:enable_zoom_scale")) {
            viewBinding.sbEnableScale.setChecked(e.get("video.1:enable_zoom_scale").equals("1"));
        }
        if(e.has("video.1:scale_5p")) {
            _matrixStrValues[MATRIX_5P] = e.get("video.1:scale_5p");
        }
        if(e.has("video.1:scale_8p")) {
            _matrixStrValues[MATRIX_8P] = e.get("video.1:scale_8p");
        }
        if(e.has("video.1:scale_11p")) {
            _matrixStrValues[MATRIX_11P] = e.get("video.1:scale_11p");
        }
        if(e.has("video.1:scale_bb")) {
            _matrixStrValues[MATRIX_BB] = e.get("video.1:scale_bb");
        }
        updateCurrentMatrixValues();
        viewBinding.scaleContainer.setVisibility(viewBinding.sbEnableScale.isChecked() ? View.VISIBLE : View.GONE);
        dismissLoading();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettingsUpdated(WeewaSettingsExUpdateEvent event) {
        dismissLoading();
        _handler.removeCallbacks(_timeOutRunnable);
        viewBinding.scaleContainer.setVisibility(viewBinding.sbEnableScale.isChecked() ? View.VISIBLE : View.GONE);
        if(_pendingFinish) {
            finish();
            _pendingFinish = false;
        }
    }
}
