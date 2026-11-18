package com.xh.hotme.storage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;

import com.dou361.ijkplayer.widget.IMediaController;
import com.xh.hotme.R;
import com.xh.hotme.live.view.CmoIjkVideoView;
import com.xh.hotme.utils.ClickGuard;

import java.lang.reflect.Field;

public class CustomMediaController extends MediaController implements IMediaController {

    CmoIjkVideoView playerVideoView;
    SeekBar seekBar;

    ImageView playBtn;
    ImageView voiceBtn;

    View mView;

    public CustomMediaController(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.custom_control_view, this);

        playBtn = findViewById(R.id.video_play_btn);
        voiceBtn = findViewById(R.id.video_voice_btn);
        seekBar = findViewById(R.id.seek_bar);

        playBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                //暂停 和播放：
                boolean tag = playBtn.getTag() == null ? false : (boolean) playBtn.getTag();
                if (playerVideoView.isPlaying()) {
                    playerVideoView.pause();
                    playBtn.setImageResource(R.mipmap.iv_video_play_white);
                } else {
                    playerVideoView.start();
                    playBtn.setImageResource(R.mipmap.iv_video_pause_white);
                }
                playBtn.setTag(!tag);

                return true;
            }
        });

        voiceBtn.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                //静音和外放：
                boolean tag = voiceBtn.getTag() == null ? false : (boolean) voiceBtn.getTag();

                voiceBtn.setImageResource( tag?R.mipmap.ic_video_voice_on_white: R.mipmap.ic_video_voice_off_white);

                voiceBtn.setTag(!tag);

                return true;
            }
        });
    }

    public void setPlayerView(CmoIjkVideoView videoView){

        playerVideoView = videoView;
    }

    @Override
    public void showOnce(View view) {

    }

//    @Override
//    public void setAnchorView(View view) {
//        super.setAnchorView(view);
//        mView = LayoutInflater.from(getContext()).inflate(
//                R.layout.custom_control_view, null);
//        try {
//            SeekBar sb = (SeekBar) LayoutInflater.from(getContext()).inflate(
//                    R.layout.video_seekbar, null);
//            Field mRoot = android.widget.MediaController.class
//                    .getDeclaredField("mRoot");
//            mRoot.setAccessible(true);
//            ViewGroup mRootVg = (ViewGroup) mRoot.get(this);
//            ViewGroup vg = findSeekBarParent(mRootVg);
//            int index = 1;
//            for (int i = 0; i < vg.getChildCount(); i++) {
//                if (vg.getChildAt(i) instanceof SeekBar) {
//                    index = i;
//                    break;
//                }
//            }
//            vg.removeViewAt(index);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//            params.weight = 1;
//            vg.addView(sb, index, params);
//            Field mProgress = android.widget.MediaController.class
//                    .getDeclaredField("mProgress");
//            mProgress.setAccessible(true);
//            mProgress.set(this, sb);
//            Field mSeekListener = android.widget.MediaController.class
//                    .getDeclaredField("mSeekListener");
//            mSeekListener.setAccessible(true);
//            sb.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener) mSeekListener
//                    .get(this));
//        } catch (NoSuchFieldException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IllegalArgumentException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private ViewGroup findSeekBarParent(ViewGroup vg) {
//        ViewGroup viewGroup = null;
//        for (int i = 0; i < vg.getChildCount(); i++) {
//            View view = vg.getChildAt(i);
//            if (view instanceof SeekBar) {
//                viewGroup = (ViewGroup) view.getParent();
//                break;
//            } else if (view instanceof ViewGroup) {
//                viewGroup = findSeekBarParent((ViewGroup) view);
//            } else {
//                continue;
//            }
//        }
//        return viewGroup;
//    }


}
