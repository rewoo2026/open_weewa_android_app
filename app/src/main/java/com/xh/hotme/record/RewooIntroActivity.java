package com.xh.hotme.record;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenLandscape;
import com.xh.hotme.base.FullscreenPortrait;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.databinding.ActivityRewooIntroBinding;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;

import tv.danmaku.ijk.media.player.IMediaPlayer;


@FullscreenLandscape(immersive = true)
@FullscreenPortrait(immersive = false)
public class RewooIntroActivity extends BaseActivity<ActivityRewooIntroBinding> {
    private final static String TAG = RewooIntroActivity.class.getSimpleName();

    ImageView _fullView;

    FrameLayout _layoutPlayer;

    CmoIjkVideoView _jkplayerView;

    final String _url = "http://store.rewoo360.com/usedvideo.mp4";

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, RewooIntroActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _fullView = viewBinding.btnFullscreen;
        _layoutPlayer = viewBinding.layoutPlayer;
        _jkplayerView = viewBinding.player;

        _fullView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                requestOrientation(isPortrait);

                return true;
            }
        });
    }

    @Override
    protected View getTopInsetAwarableView() {
        return viewBinding.titleBar.getRoot();
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitleTrans(viewBinding.titleBar, "如何使用相机");
        viewBinding.btnPlay.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                viewBinding.btnPlay.setVisibility(View.GONE);
                if(viewBinding.player.isPlaying()) {
                    viewBinding.player.release(false);
                    viewBinding.player.setRender(1);
                }

                viewBinding.player.setVideoPath(_url);
                viewBinding.player.start();

                return true;
            }
        });
        viewBinding.player.setOnInfoListener((iMediaPlayer, states, i1) -> {
            AppTrace.d(TAG, "onInfo state: " + states);

            if(states == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START
                || states == IMediaPlayer.MEDIA_ERROR_TIMED_OUT
            ) {
                viewBinding.btnPlay.setVisibility(View.GONE);
                viewBinding.playerCover.setVisibility(View.GONE);
            }

            return true;
        });

        viewBinding.player.setOnPreparedListener(iMediaPlayer -> {
            AppTrace.d(TAG, "onPrepared IMediaPlayer");
            viewBinding.btnPlay.setVisibility(View.GONE);
            viewBinding.playerCover.setVisibility(View.GONE);
        });

        viewBinding.player.setOnCompletionListener(iMediaPlayer -> {
            viewBinding.btnPlay.setVisibility(View.VISIBLE);
            viewBinding.playerCover.setVisibility(View.VISIBLE);
        });

        viewBinding.player.setOnErrorListener((iMediaPlayer, i, i1) -> {
            viewBinding.btnPlay.setVisibility(View.VISIBLE);
            viewBinding.playerCover.setVisibility(View.VISIBLE);
            return false;
        });
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(_jkplayerView != null) {
            _jkplayerView.pause();
            _jkplayerView.release(true);
        }
    }

    /* 退出全屏 */
    @Override
    protected void exitLandscape() {
        // 旋转屏幕
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 将JJPlayerView从Activity的R.id.content移除，加入RecyclerView的ItemView下
        viewBinding.portraitLayout.setVisibility(View.VISIBLE);

        viewBinding.landscapePlayerContainer.removeView(_layoutPlayer);
        viewBinding.landscapeLayout.setVisibility(View.GONE);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.portraitPlayerContainer.addView(_layoutPlayer, params);

        _fullView.setImageResource(R.mipmap.fullscreen_expand);
    }

    /* 进入全屏 */
    @Override
    protected void enterLandscape() {
        // 将JJPlayerView从Activity的R.id.content移除，加入RecyclerView的ItemView下
        viewBinding.portraitLayout.setVisibility(View.GONE);

        viewBinding.portraitPlayerContainer.removeView(_layoutPlayer);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewBinding.landscapePlayerContainer.addView(_layoutPlayer, params);
        viewBinding.landscapeLayout.setVisibility(View.VISIBLE);

        _fullView.setImageResource(R.mipmap.fullscreen_skrink);
    }

    @Override
    protected int getStatusBarColor() {
        return 0;
    }

    @Override
    protected int getNavigationBarColor() {
        return 0;
    }
}
