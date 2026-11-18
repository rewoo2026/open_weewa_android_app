package com.xh.hotme.storage;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dou361.ijkplayer.widget.AndroidMediaController;
import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.databinding.ActivityVideoPlayerBinding;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.widget.BluetoothLoadingDialog;

@FullscreenLandscape(immersive = true)
public class VideoPlayActivity extends BaseActivity<ActivityVideoPlayerBinding> {
    private final static String TAG = VideoPlayActivity.class.getSimpleName();

    CmoIjkVideoView _playerView;

    String _videoUrl = "";


    BluetoothLoadingDialog _connectingDialog;

    int retryCount = 0;


    private final Runnable _deviceUsageInfoRunnable = new Runnable() {
        @Override
        public void run() {
            _handler.postDelayed(_deviceUsageInfoRunnable, 1000);
        }
    };

    public static void start(Context context, String videoUrl) {
        Intent intent = new Intent(context, VideoPlayActivity.class);
        intent.putExtra(Constants.VIDEO_URL, videoUrl);
        context.startActivity(intent);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.VIDEO_URL, _videoUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            _videoUrl = savedInstanceState.getString(Constants.VIDEO_URL);
        }

        if(getIntent() != null) {
            _videoUrl = getIntent().getStringExtra(Constants.VIDEO_URL);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void initView() {

        _playerView = viewBinding.playerView;

        AndroidMediaController controller = new AndroidMediaController(this, false);
        _playerView.setMediaController(controller);

        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_playerView != null) {
                    if(_playerView.isPlaying()) {
                        _playerView.pause();
                    }
                    _playerView.release(true);
                }

                finish();

                return true;
            }
        });

    }

    @Override
    protected void initData() {
        startPlay();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppTrace.d(TAG, "onPause...");

        if(_playerView != null && _playerView.isPlaying()) {
            _playerView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume...");

        if(_playerView == null) {
            initView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AppTrace.d(TAG, "onStop...");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy...");

        if(_playerView != null) {
            _playerView.pause();
            _playerView.release(true);
        }

    }

    @Override
    protected int getStatusBarColor() {
        return 0xff000000;
    }

    @Override
    protected int getNavigationBarColor() {
        return 0xff000000;
    }

    @Override
    protected boolean isDarkStatusBar() {
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void startPlay() {
        retryCount = 0;


        if(_playerView.isPlaying()) {
//            viewBinding.playerView.stopPlayback();
            _playerView.release(false);
            _playerView.setRender(CmoIjkVideoView.RENDER_SURFACE_VIEW);
        }

        _playerView.setVideoPath(_videoUrl);
        _playerView.start();
    }

    private void showConnectingDialog(String message) {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(VideoPlayActivity.this);
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if(_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }


}
